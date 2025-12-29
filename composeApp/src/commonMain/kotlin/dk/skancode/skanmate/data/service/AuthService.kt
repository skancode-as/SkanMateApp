package dk.skancode.skanmate.data.service

import com.russhwolf.settings.ExperimentalSettingsApi
import com.russhwolf.settings.Settings
import com.russhwolf.settings.set
import dk.skancode.skanmate.data.model.PinCredentialType
import dk.skancode.skanmate.data.model.SignInResult
import dk.skancode.skanmate.data.model.SignInUnprocessableEntityDetails
import dk.skancode.skanmate.data.model.TenantModel
import dk.skancode.skanmate.data.model.UserSignInDTO
import dk.skancode.skanmate.data.model.UserModel
import dk.skancode.skanmate.data.store.AuthStore
import dk.skancode.skanmate.data.store.LocalAuthStore
import dk.skancode.skanmate.data.store.ServerErrorCodes
import dk.skancode.skanmate.platformSettingsFactory
import dk.skancode.skanmate.util.InternalStringResource
import dk.skancode.skanmate.util.jsonSerializer
import dk.skancode.skanmate.util.snackbar.DefaultErrorOptions
import dk.skancode.skanmate.util.snackbar.UserMessageServiceImpl
import dk.skancode.skanmate.util.string
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.ExperimentalSerializationApi
import skanmate.composeapp.generated.resources.Res
import skanmate.composeapp.generated.resources.auth_screen_email_label
import skanmate.composeapp.generated.resources.auth_screen_pin_label
import skanmate.composeapp.generated.resources.auth_screen_sign_in_failed
import kotlin.concurrent.atomics.ExperimentalAtomicApi

interface AuthService {
    val userFlow: SharedFlow<UserModel?>
    val tenantFlow: SharedFlow<TenantModel?>

    suspend fun signIn(email: String, pw: String): SignInResult
    suspend fun signOut()
}

private const val AUTH_TOKEN_SETTINGS_KEY = "authToken"

@OptIn(ExperimentalSerializationApi::class, ExperimentalSettingsApi::class,
    ExperimentalAtomicApi::class
)
class AuthServiceImpl(
    val authStore: AuthStore,
    val localAuthStore: LocalAuthStore,
    val externalScope: CoroutineScope,
    settingsFactory: Settings.Factory = platformSettingsFactory,
    val connectivityService: ConnectivityService = ConnectivityService.instance,
) : AuthService {
    private val settings = settingsFactory.create("user_info")
    val tokenFlow = MutableSharedFlow<String?>(1)
    private val _userFlow = MutableSharedFlow<UserModel?>(1)
    override val userFlow: SharedFlow<UserModel?>
        get() = _userFlow

    private val _tenantFlow = MutableSharedFlow<TenantModel?>(1)
    override val tenantFlow: SharedFlow<TenantModel?>
        get() = _tenantFlow

    init {
        externalScope.launch {
            val token = settings.getStringOrNull(AUTH_TOKEN_SETTINGS_KEY)
            tokenFlow.emit(token)

            if (token != null) {
                if (connectivityService.isConnected()) {
                    val user = fetchUserInfo(token)
                    _userFlow.emit(user)

                    val tenant = fetchTenantInfo(token)
                    _tenantFlow.emit(tenant)
                } else {
                    val (user, tenant) = localAuthStore.loadUserAndTenant(token)
                    _userFlow.emit(user)
                    _tenantFlow.emit(tenant)
                }
            } else {
                invalidateLocalData()
            }

            tokenFlow.collect { token ->
                println("Auth token has been updated: $token")
                settings[AUTH_TOKEN_SETTINGS_KEY] = token
                if (token == null) {
                    invalidateLocalData()
                }
            }
        }
    }

    override suspend fun signIn(email: String, pw: String): SignInResult {
        if (!connectivityService.isConnected()) {
            return SignInResult(null, null)
        }

        val signInRes = authStore.signIn(
            UserSignInDTO(
                email = email,
                credential = pw,
                type = PinCredentialType,
            ),
        )
        if (!signInRes.ok || signInRes.data == null) {
            val (message, description) = when (signInRes.errorCode) {
                ServerErrorCodes.UNPROCESSABLE_ENTITY -> {
                    val desc = if (signInRes.details != null) {
                        val details = jsonSerializer.decodeFromJsonElement(
                            SignInUnprocessableEntityDetails.serializer(),
                            signInRes.details
                        )

                        listOf(
                            if (details.email != null) (InternalStringResource(Res.string.auth_screen_email_label) to details.email.joinToString(
                                "\n  -  "
                            )) else null,
                            if (details.credential != null) (InternalStringResource(Res.string.auth_screen_pin_label) to details.credential.joinToString(
                                "\n  -  "
                            )) else null,
                        ).mapNotNull {
                            if (it != null) {
                                val (res, err) = it
                                "${res.string()}:\n  -  $err"
                            } else null
                        }.joinToString("\n")
                    } else null

                    InternalStringResource(
                        Res.string.auth_screen_sign_in_failed,
                        listOf(signInRes.msg)
                    ) to desc
                }

                else -> InternalStringResource(
                    Res.string.auth_screen_sign_in_failed,
                    listOf(signInRes.msg)
                ) to null
            }

            UserMessageServiceImpl.displayError(
                message,
                options = DefaultErrorOptions.copy(description = description)
            )

            return SignInResult(user = null, tenant = null)
        }

        tokenFlow.emit(signInRes.data.token)

        val user = fetchUserInfo(signInRes.data.token)
        _userFlow.emit(user)

        val tenant = fetchTenantInfo(signInRes.data.token)
        _tenantFlow.emit(tenant)

        if (user != null && tenant != null) {
            localAuthStore.storeUserAndTenant(signInRes.data.token, user, tenant)
        }

        return SignInResult(user, tenant)
    }

    override suspend fun signOut() {
        tokenFlow.emit(null)
        invalidateLocalData()
    }

    private suspend fun invalidateLocalData() {
        println("Invalidating local auth data")

        _userFlow.emit(null)
        _tenantFlow.emit(null)
        localAuthStore.invalidateLocalData()
    }

    private suspend fun fetchUserInfo(token: String): UserModel? {
        val res = authStore.getUserInfo(token)

        println("AuthStore.getUserInfo response: $res")

        if (!res.ok || res.data == null) return null

        return UserModel(
            id = res.data.id,
            tenantId = res.data.tenantId,
            name = res.data.name,
            email = res.data.email,
            emailVerified = res.data.emailVerified,
            role = res.data.role,
            image = res.data.image,
            active = res.data.active,
            createdAt = res.data.createdAt,
            updatedAt = res.data.updatedAt,
        )
    }

    private suspend fun fetchTenantInfo(token: String): TenantModel? {
        val res = authStore.getTenantInfo(token)
        if (!res.ok || res.data == null) return null

        return TenantModel(
            id = res.data.id,
            name = res.data.name,
            slug = res.data.slug,
            logo = res.data.logo,
            createdAt = res.data.createdAt,
            updatedAt = res.data.updatedAt,
        )
    }
}
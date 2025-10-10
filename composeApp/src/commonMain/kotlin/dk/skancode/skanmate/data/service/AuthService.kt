package dk.skancode.skanmate.data.service

import com.russhwolf.settings.Settings
import com.russhwolf.settings.get
import com.russhwolf.settings.set
import dk.skancode.skanmate.data.model.PinCredentialType
import dk.skancode.skanmate.data.model.SignInResult
import dk.skancode.skanmate.data.model.TenantModel
import dk.skancode.skanmate.data.model.UserCredentialType
import dk.skancode.skanmate.data.model.UserSignInDTO
import dk.skancode.skanmate.data.model.UserModel
import dk.skancode.skanmate.data.store.AuthStore
import dk.skancode.skanmate.platformSettingsFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

interface AuthService {
    val userFlow: SharedFlow<UserModel?>
    val tenantFlow: SharedFlow<TenantModel?>

    suspend fun signIn(email: String, pw: String): SignInResult
    suspend fun signOut()
}

private const val AUTH_TOKEN_SETTINGS_KEY = "authToken"

class AuthServiceImpl(
    val authStore: AuthStore,
    externalScope: CoroutineScope,
    settingsFactory: Settings.Factory = platformSettingsFactory,
): AuthService {
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
                val user = fetchUserInfo(token)
                _userFlow.emit(user)

                val tenant = fetchTenantInfo(token)
                _tenantFlow.emit(tenant)
            } else {
                _userFlow.emit(null)
                _tenantFlow.emit(null)
            }

            tokenFlow.collect { token ->
                println("Auth token has been updated: $token")
                settings[AUTH_TOKEN_SETTINGS_KEY] = token
            }
        }
    }

    override suspend fun signIn(email: String, pw: String): SignInResult {
        val signInRes = authStore.signIn(
            UserSignInDTO(
                email = email,
                credential = pw,
                type = PinCredentialType,
            ),
        )
        if (!signInRes.ok || signInRes.data == null)
            return SignInResult(user = null, tenant = null)

        tokenFlow.emit(signInRes.data.token)

        println(signInRes.data.token)
        val user = fetchUserInfo(signInRes.data.token)
        _userFlow.emit(user)

        val tenant = fetchTenantInfo(signInRes.data.token)
        _tenantFlow.emit(tenant)

        return SignInResult(user, tenant)
    }

    override suspend fun signOut() {
        tokenFlow.emit(null)
        _userFlow.emit(null)
        _tenantFlow.emit(null)
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

    suspend fun fetchTenantInfo(token: String): TenantModel? {
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
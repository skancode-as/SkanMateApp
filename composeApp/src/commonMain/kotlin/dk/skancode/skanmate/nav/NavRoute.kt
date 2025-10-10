package dk.skancode.skanmate.nav

import kotlinx.serialization.Serializable

@Serializable
sealed class NavRoute {
    @Serializable
    data object InitializerScreen: NavRoute()
    @Serializable
    data object AuthScreen: NavRoute()
    @Serializable
    sealed class App(): NavRoute() {
        @Serializable
        data object MainScreen: App()
        @Serializable
        data class TableScreen(val tableId: String): App()
    }
}
package dk.skancode.skanmate.nav

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import dk.skancode.skanmate.ui.component.AuthenticationRequired
import dk.skancode.skanmate.ui.screen.AuthScreen
import dk.skancode.skanmate.ui.screen.InitializerScreen
import dk.skancode.skanmate.ui.screen.MainScreen
import dk.skancode.skanmate.ui.screen.TableScreen
import dk.skancode.skanmate.ui.viewmodel.AuthViewModel
import dk.skancode.skanmate.ui.viewmodel.ConnectivityViewModel
import dk.skancode.skanmate.ui.viewmodel.InitializerViewModel
import dk.skancode.skanmate.ui.viewmodel.TableViewModel

@Composable
fun AppNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    authViewModel: AuthViewModel,
    initializerViewModel: InitializerViewModel,
    tableViewModel: TableViewModel,
    connectivityViewModel: ConnectivityViewModel,
) {
    val onUnauthorized = {
        navController.navigate(NavRoute.AuthScreen) {
            popUpTo<NavRoute.AuthScreen> {
                inclusive = true
            }
        }
    }

    NavHost(
        modifier = modifier,
        navController = navController,
        startDestination = NavRoute.InitializerScreen,
    ) {
        composable<NavRoute.InitializerScreen> {
            InitializerScreen(
                initializerViewModel,
            ) { route ->
                navController.navigate(route) {
                    popUpTo<NavRoute.InitializerScreen> {
                        inclusive = true
                    }
                }
            }
        }

        composable<NavRoute.AuthScreen> {
            AuthScreen(
                viewModel = authViewModel,
                connectivityViewModel = connectivityViewModel,
            ) {
                navController.navigate(NavRoute.App.MainScreen) {
                    popUpTo<NavRoute.AuthScreen> {
                        inclusive = true
                    }
                }
            }
        }

        composable<NavRoute.App.MainScreen> {
            AuthenticationRequired(
                authViewModel = authViewModel,
                onUnauthorized = onUnauthorized,
            ) {
                MainScreen(
                    tableViewModel = tableViewModel,
                    navigateTable = { route ->
                        navController.navigate(route)
                    }
                ) {
                    authViewModel.signOut()
                }
            }
        }

        composable<NavRoute.App.TableScreen> { entry ->
            val route = entry.toRoute<NavRoute.App.TableScreen>()
            AuthenticationRequired(
                authViewModel = authViewModel,
                onUnauthorized = onUnauthorized,
            ) {
                TableScreen(
                    id = route.tableId,
                    viewModel = tableViewModel,
                    navigateBack = {
                        navController.popBackStack<NavRoute.App.MainScreen>(inclusive = false)
                    }
                )
            }
        }
    }
}

package com.dariusepure.caractivitylog.ui

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.dariusepure.caractivitylog.ui.auth.SignInScreen
import com.dariusepure.caractivitylog.ui.auth.SignUpScreen
import com.dariusepure.caractivitylog.ui.cars.AddCarScreen
import com.dariusepure.caractivitylog.ui.cars.CarDetailsScreen
import com.dariusepure.caractivitylog.ui.cars.CarListScreen
import com.dariusepure.caractivitylog.ui.cars.MileageHistoryScreen
import com.dariusepure.caractivitylog.ui.cars.InspectionHistoryScreen
import com.dariusepure.caractivitylog.ui.cars.TechnicalSheetScreen

import com.dariusepure.caractivitylog.ui.cars.DiagnosisScreen
import com.dariusepure.caractivitylog.ui.cars.FuelHistoryScreen
import com.dariusepure.caractivitylog.ui.cars.RecycleBinScreen
import androidx.hilt.navigation.compose.hiltViewModel
import com.dariusepure.caractivitylog.ui.theme.ThemeViewModel

sealed class Screen(val route: String) {
    data object SignIn : Screen("signin")
    data object SignUp : Screen("signup")
    data object CarList : Screen("carlist")
    data object CarDetails : Screen("cardetails/{carId}") {
        fun createRoute(carId: String) = "cardetails/$carId"
    }
    data object MileageHistory : Screen("mileagehistory/{carId}") {
        fun createRoute(carId: String) = "mileagehistory/$carId"
    }
    data object InspectionHistory : Screen("inspectionhistory/{carId}") {
        fun createRoute(carId: String) = "inspectionhistory/$carId"
    }
    data object AddCar : Screen("addcar")
    data object TechnicalSheet : Screen("technicalsheet/{carId}") {
        fun createRoute(carId: String) = "technicalsheet/$carId"
    }
    data object EditCar : Screen("editcar/{carId}") {
        fun createRoute(carId: String) = "editcar/$carId"
    }
    data object Diagnosis : Screen("diagnosis/{carId}") {
        fun createRoute(carId: String) = "diagnosis/$carId"
    }
    data object FuelHistory : Screen("fuelhistory/{carId}") {
        fun createRoute(carId: String) = "fuelhistory/$carId"
    }
    data object RecycleBin : Screen("recyclebin")
}

@Composable
fun AppNavigation(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    startDestination: String = Screen.CarList.route,
    themeViewModel: ThemeViewModel = hiltViewModel()
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(Screen.SignIn.route) {
            SignInScreen(
                onSignedIn = {
                    navController.navigate(Screen.CarList.route) {
                        popUpTo(Screen.SignIn.route) { inclusive = true }
                    }
                },
                onSignUpClick = {
                    navController.navigate(Screen.SignUp.route)
                }
            )
        }
        composable(Screen.SignUp.route) {
            SignUpScreen(
                onSignedIn = {
                    navController.navigate(Screen.CarList.route) {
                        popUpTo(Screen.SignIn.route) { inclusive = true }
                    }
                },
                onBackToSignIn = {
                    navController.popBackStack()
                }
            )
        }
        composable(Screen.CarList.route) {
            CarListScreen(
                onCarClick = { carId ->
                    navController.navigate(Screen.CarDetails.createRoute(carId))
                },
                onAddCarClick = {
                    navController.navigate(Screen.AddCar.route)
                },
                onEditCarClick = { carId ->
                    navController.navigate(Screen.EditCar.createRoute(carId))
                },
                onRecycleBinClick = {
                    navController.navigate(Screen.RecycleBin.route)
                },
                onLogout = {
                    navController.navigate(Screen.SignIn.route) {
                        popUpTo(navController.graph.id) { inclusive = true }
                    }
                },
                themeViewModel = themeViewModel
            )
        }
        composable(Screen.CarDetails.route) { backStackEntry ->
            val carId = backStackEntry.arguments?.getString("carId") ?: return@composable
            CarDetailsScreen(
                carId = carId,
                onBack = { navController.popBackStack() },
                onMileageClick = {
                    navController.navigate(Screen.MileageHistory.createRoute(carId))
                },
                onInspectionClick = {
                    navController.navigate(Screen.InspectionHistory.createRoute(carId))
                },
                onTechnicalSheetClick = {
                    navController.navigate(Screen.TechnicalSheet.createRoute(carId))
                },
                onDiagnosisClick = {
                    navController.navigate(Screen.Diagnosis.createRoute(carId))
                },
                onFuelClick = {
                    navController.navigate(Screen.FuelHistory.createRoute(carId))
                }
            )
        }
        composable(Screen.MileageHistory.route) { backStackEntry ->
            val carId = backStackEntry.arguments?.getString("carId") ?: return@composable
            MileageHistoryScreen(
                carId = carId,
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.InspectionHistory.route) { backStackEntry ->
            val carId = backStackEntry.arguments?.getString("carId") ?: return@composable
            InspectionHistoryScreen(
                carId = carId,
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.TechnicalSheet.route) { backStackEntry ->
            val carId = backStackEntry.arguments?.getString("carId") ?: return@composable
            TechnicalSheetScreen(
                carId = carId,
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.AddCar.route) {
            AddCarScreen(
                onCarSaved = {
                    navController.popBackStack()
                },
                onBack = {
                    navController.popBackStack()
                }
            )
        }
        composable(Screen.EditCar.route) { backStackEntry ->
            val carId = backStackEntry.arguments?.getString("carId") ?: return@composable
            AddCarScreen(
                carId = carId,
                onCarSaved = {
                    navController.popBackStack()
                },
                onBack = {
                    navController.popBackStack()
                }
            )
        }
        composable(Screen.Diagnosis.route) { backStackEntry ->
            val carId = backStackEntry.arguments?.getString("carId") ?: return@composable
            DiagnosisScreen(
                carId = carId,
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.FuelHistory.route) { backStackEntry ->
            val carId = backStackEntry.arguments?.getString("carId") ?: return@composable
            FuelHistoryScreen(
                carId = carId,
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.RecycleBin.route) {
            RecycleBinScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}

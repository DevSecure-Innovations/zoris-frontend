package com.example.myapplication.ui.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.myapplication.viewmodel.DashboardViewModel
import com.example.myapplication.ui.screens.home.HomeScreen
import com.example.myapplication.ui.screens.dashboard.DashboardScreen
import com.example.myapplication.ui.screens.threats.ThreatsScreen

@Composable
fun AppNavigation(navController: NavHostController) {
    val sharedViewModel: DashboardViewModel = viewModel()

    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        composable(Screen.Home.route) {
            HomeScreen(viewModel = sharedViewModel)
        }

        composable(Screen.Threats.route) {
             ThreatsScreen(viewModel = sharedViewModel)
        }

        composable(Screen.Dashboard.route) {
            DashboardScreen(viewModel = sharedViewModel)
        }
    }
}
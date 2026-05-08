package com.posan.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.posan.app.ui.backup.BackupScreen
import com.posan.app.ui.categories.CategoriesScreen
import com.posan.app.ui.customers.CustomersScreen
import com.posan.app.ui.dashboard.DashboardScreen
import com.posan.app.ui.login.LoginScreen
import com.posan.app.ui.login.SessionViewModel
import com.posan.app.ui.plntemplates.PlnTokenTemplatesScreen
import com.posan.app.ui.plntoken.PlnTokenScreen
import com.posan.app.ui.pos.PosScreen
import com.posan.app.ui.printer.PrinterDeviceScreen
import com.posan.app.ui.printsettings.PrintSettingsScreen
import com.posan.app.ui.products.ProductFormScreen
import com.posan.app.ui.products.ProductsScreen
import com.posan.app.ui.report.ReportScreen
import com.posan.app.ui.stock.StockScreen
import com.posan.app.ui.transactions.TransactionDetailScreen
import com.posan.app.ui.transactions.TransactionsScreen
import com.posan.app.ui.users.UsersScreen

object Routes {
    const val LOGIN = "login"
    const val DASHBOARD = "dashboard"
    const val POS = "pos"
    const val PRODUCTS = "products"
    const val PRODUCT_FORM = "products/form"
    const val CATEGORIES = "categories"
    const val CUSTOMERS = "customers"
    const val TRANSACTIONS = "transactions"
    const val TRANSACTION_DETAIL = "transactions/detail"
    const val REPORT = "report"
    const val STOCK = "stock"
    const val USERS = "users"
    const val PRINT_SETTINGS = "print_settings"
    const val PRINTER_DEVICES = "printer_devices"
    const val BACKUP = "backup"
    const val PLN_TOKEN = "pln_token"
    const val PLN_TEMPLATES = "pln_templates"
}

@Composable
fun PosanRoot() {
    val navController = rememberNavController()
    val sessionVm: SessionViewModel = hiltViewModel()
    val userId by sessionVm.userId.collectAsState(initial = null)
    var seeded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (!seeded) {
            sessionVm.ensureSeed()
            seeded = true
        }
    }

    val startDestination = if (userId != null) Routes.DASHBOARD else Routes.LOGIN

    NavHost(navController = navController, startDestination = startDestination) {
        composable(Routes.LOGIN) {
            LoginScreen(onLoggedIn = {
                navController.navigate(Routes.DASHBOARD) {
                    popUpTo(Routes.LOGIN) { inclusive = true }
                }
            })
        }
        composable(Routes.DASHBOARD) {
            DashboardScreen(
                onNavigate = { navController.navigate(it) },
                onLogout = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
        composable(Routes.POS) {
            PosScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.PRODUCTS) {
            ProductsScreen(
                onBack = { navController.popBackStack() },
                onAdd = { navController.navigate("${Routes.PRODUCT_FORM}?id=0") },
                onEdit = { id -> navController.navigate("${Routes.PRODUCT_FORM}?id=$id") }
            )
        }
        composable(
            route = "${Routes.PRODUCT_FORM}?id={id}",
            arguments = listOf(navArgument("id") { type = NavType.LongType; defaultValue = 0L })
        ) { entry ->
            val id = entry.arguments?.getLong("id") ?: 0L
            ProductFormScreen(productId = id, onDone = { navController.popBackStack() })
        }
        composable(Routes.CATEGORIES) {
            CategoriesScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.CUSTOMERS) {
            CustomersScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.TRANSACTIONS) {
            TransactionsScreen(
                onBack = { navController.popBackStack() },
                onDetail = { id -> navController.navigate("${Routes.TRANSACTION_DETAIL}/$id") }
            )
        }
        composable(
            route = "${Routes.TRANSACTION_DETAIL}/{id}",
            arguments = listOf(navArgument("id") { type = NavType.LongType })
        ) { entry ->
            val id = entry.arguments?.getLong("id") ?: 0L
            TransactionDetailScreen(transactionId = id, onBack = { navController.popBackStack() })
        }
        composable(Routes.REPORT) {
            ReportScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.STOCK) {
            StockScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.USERS) {
            UsersScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.PRINT_SETTINGS) {
            PrintSettingsScreen(
                onBack = { navController.popBackStack() },
                onPickDevice = { navController.navigate(Routes.PRINTER_DEVICES) }
            )
        }
        composable(Routes.PRINTER_DEVICES) {
            PrinterDeviceScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.BACKUP) {
            BackupScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.PLN_TOKEN) {
            PlnTokenScreen(
                onBack = { navController.popBackStack() },
                onManageCustomers = { navController.navigate(Routes.CUSTOMERS) },
                onManageTemplates = { navController.navigate(Routes.PLN_TEMPLATES) }
            )
        }
        composable(Routes.PLN_TEMPLATES) {
            PlnTokenTemplatesScreen(onBack = { navController.popBackStack() })
        }
    }
}

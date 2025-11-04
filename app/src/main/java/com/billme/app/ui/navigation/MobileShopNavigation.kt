package com.billme.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.billme.app.core.scanner.UnifiedIMEIScanner
import com.billme.app.data.repository.SetupPreferencesRepository
import com.billme.app.ui.screen.addproduct.AddProductScreen
import com.billme.app.ui.screen.addpurchase.AddPurchaseScreen
import com.billme.app.ui.screen.backup.BackupManagementScreen
import com.billme.app.ui.screen.billing.BillingScreen
import com.billme.app.ui.screen.dashboard.DashboardScreen
import com.billme.app.ui.screen.inventory.InventoryScreen
import com.billme.app.ui.screen.invoicehistory.InvoiceHistoryScreen
import com.billme.app.ui.screen.quickbill.QuickBillScreen
import com.billme.app.ui.screen.reports.ReportsScreen
import com.billme.app.ui.screen.settings.SettingsScreen
import com.billme.app.ui.screen.settings.FlexibleGSTSettingsScreen
import com.billme.app.ui.screen.settings.SignatureManagementScreen
import com.billme.app.ui.screen.setup.SetupWizardScreen
import dagger.hilt.android.EntryPointAccessors
import com.billme.app.BillMeApplication

@Composable
fun MobileShopNavigation(
    navController: NavHostController = rememberNavController(),
    setupRepository: SetupPreferencesRepository
) {
    // Check if setup is completed
    val isSetupCompleted by setupRepository.isSetupCompleted.collectAsState(initial = true)
    
    // Determine start destination based on setup status
    val startDestination = if (isSetupCompleted) {
        NavigationDestination.Dashboard.route
    } else {
        NavigationDestination.SetupWizard.route
    }
    
    // Get the unified IMEI scanner instance using EntryPoint
    val context = LocalContext.current
    val unifiedIMEIScanner = remember {
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            UnifiedIMEIScannerEntryPoint::class.java
        ).unifiedIMEIScanner()
    }
    
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // Setup Wizard
        composable(NavigationDestination.SetupWizard.route) {
            SetupWizardScreen(
                onSetupComplete = {
                    // Navigate to dashboard after setup
                    navController.navigate(NavigationDestination.Dashboard.route) {
                        // Clear back stack to prevent going back to setup
                        popUpTo(NavigationDestination.SetupWizard.route) {
                            inclusive = true
                        }
                    }
                }
            )
        }
        
        composable(NavigationDestination.Dashboard.route) {
            DashboardScreen(
                onNavigateToQuickBill = {
                    navController.navigate(NavigationDestination.QuickBill.route)
                },
                onNavigateToBilling = {
                    navController.navigate(NavigationDestination.Billing.route)
                },
                onNavigateToInventory = {
                    navController.navigate(NavigationDestination.Inventory.route)
                },
                onNavigateToAddPurchase = {
                    navController.navigate(NavigationDestination.AddPurchase.route)
                },
                onNavigateToAddProduct = {
                    navController.navigate(NavigationDestination.AddProduct.route)
                },
                onNavigateToReports = {
                    navController.navigate(NavigationDestination.Reports.route)
                },
                onNavigateToSettings = {
                    navController.navigate(NavigationDestination.Settings.route)
                },
                onNavigateToInvoiceHistory = {
                    navController.navigate(NavigationDestination.InvoiceHistory.route)
                },
                onNavigateToBackup = {
                    navController.navigate(NavigationDestination.BackupManagement.route)
                }
            )
        }
        
        composable(NavigationDestination.QuickBill.route) {
            QuickBillScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        composable(NavigationDestination.Billing.route) {
            BillingScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                unifiedIMEIScanner = unifiedIMEIScanner
            )
        }
        
        composable(NavigationDestination.Inventory.route) {
            InventoryScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToAddPurchase = {
                    navController.navigate(NavigationDestination.AddPurchase.route)
                },
                onNavigateToAddProduct = {
                    navController.navigate(NavigationDestination.AddProduct.route)
                },
                unifiedIMEIScanner = unifiedIMEIScanner
            )
        }
        
        composable(NavigationDestination.AddProduct.route) {
            AddProductScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToInventory = {
                    // Simply pop back to previous screen (likely Dashboard or Inventory)
                    navController.popBackStack()
                },
                unifiedIMEIScanner = unifiedIMEIScanner
            )
        }
        
        composable(NavigationDestination.AddPurchase.route) {
            AddPurchaseScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        composable(NavigationDestination.Reports.route) {
            ReportsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToInvoiceHistory = {
                    navController.navigate(NavigationDestination.InvoiceHistory.route)
                }
            )
        }
        
        composable(NavigationDestination.InvoiceHistory.route) {
            InvoiceHistoryScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        composable(NavigationDestination.Settings.route) {
            SettingsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToGSTSettings = {
                    navController.navigate(NavigationDestination.GSTSettings.route)
                },
                onNavigateToBackup = {
                    navController.navigate(NavigationDestination.BackupManagement.route)
                },
                onNavigateToSignatureManagement = {
                    navController.navigate(NavigationDestination.SignatureManagement.route)
                }
            )
        }
        
        composable(NavigationDestination.BackupManagement.route) {
            BackupManagementScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        composable(NavigationDestination.GSTSettings.route) {
            FlexibleGSTSettingsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        composable(NavigationDestination.SignatureManagement.route) {
            SignatureManagementScreen(
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }
    }
}

sealed class NavigationDestination(val route: String) {
    object SetupWizard : NavigationDestination("setup_wizard")
    object Dashboard : NavigationDestination("dashboard")
    object QuickBill : NavigationDestination("quick_bill")
    object Billing : NavigationDestination("billing")
    object Inventory : NavigationDestination("inventory")
    object AddProduct : NavigationDestination("add_product")
    object AddPurchase : NavigationDestination("add_purchase")
    object Reports : NavigationDestination("reports")
    object InvoiceHistory : NavigationDestination("invoice_history")
    object Settings : NavigationDestination("settings")
    object GSTSettings : NavigationDestination("gst_settings")
    object BackupManagement : NavigationDestination("backup_management")
    object SignatureManagement : NavigationDestination("signature_management")
}

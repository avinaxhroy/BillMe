package com.billme.app.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.billme.app.ui.component.*
import com.billme.app.ui.theme.*
import com.billme.app.ui.viewmodel.GSTSettingsViewModel
import kotlinx.coroutines.launch

/**
 * GST Settings Screen with tabbed interface
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GSTSettingsScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: GSTSettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    
    var showSnackbar by remember { mutableStateOf<String?>(null) }
    
    // Tab configuration
    val tabs = listOf(
        GSTTab.Configuration to "Configuration",
        GSTTab.RateManagement to "Rate Management"
    )
    
    val pagerState = rememberPagerState(pageCount = { tabs.size })
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "GST Settings",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = "Configure GST rates and settings",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    AnimatedIconButton(
                        onClick = onNavigateBack,
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back"
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        snackbarHost = {
            SnackbarHost(
                hostState = remember { SnackbarHostState() }.apply {
                    LaunchedEffect(showSnackbar) {
                        showSnackbar?.let { message ->
                            showSnackbar(message)
                            showSnackbar = null
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Tab Row
            TabRow(
                selectedTabIndex = pagerState.currentPage,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                tabs.forEachIndexed { index, (_, title) ->
                    Tab(
                        selected = pagerState.currentPage == index,
                        onClick = {
                            scope.launch {
                                pagerState.animateScrollToPage(index)
                            }
                        },
                        text = { 
                            Text(
                                text = title,
                                fontWeight = if (pagerState.currentPage == index) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        icon = {
                            Icon(
                                when (tabs[index].first) {
                                    GSTTab.Configuration -> Icons.Default.Settings
                                    GSTTab.RateManagement -> Icons.Default.Receipt
                                },
                                contentDescription = title
                            )
                        }
                    )
                }
            }
            
            // Pager Content
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                when (tabs[page].first) {
                    GSTTab.Configuration -> {
                        GSTConfigurationPage(
                            uiState = uiState,
                            onConfigChange = viewModel::updateConfiguration,
                            onSave = {
                                viewModel.saveConfiguration()
                                showSnackbar = "GST configuration saved successfully"
                            }
                        )
                    }
                    
                    GSTTab.RateManagement -> {
                        GSTRateManagementPage(
                            uiState = uiState,
                            onAddRate = { rate ->
                                viewModel.addGSTRate(rate)
                                showSnackbar = "GST rate added successfully"
                            },
                            onUpdateRate = { rate ->
                                viewModel.updateGSTRate(rate)
                                showSnackbar = "GST rate updated successfully"
                            },
                            onDeleteRate = { rateId ->
                                viewModel.deleteGSTRate(rateId)
                                showSnackbar = "GST rate deleted successfully"
                            }
                        )
                    }
                }
            }
        }
    }
    
    // Handle loading states
    if (uiState.isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                CircularProgressIndicator()
                Text(
                    text = "Loading GST settings...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
    
    // Handle error states
    uiState.error?.let { errorMessage ->
        LaunchedEffect(errorMessage) {
            showSnackbar = "Error: $errorMessage"
            viewModel.clearError()
        }
    }
}

@Composable
private fun GSTConfigurationPage(
    uiState: GSTSettingsUiState,
    onConfigChange: (com.billme.app.data.local.entity.GSTConfiguration) -> Unit,
    onSave: () -> Unit
) {
    GSTSettingsComponent(
        gstConfig = uiState.configuration,
        onConfigChange = onConfigChange,
        onSave = onSave,
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
private fun GSTRateManagementPage(
    uiState: GSTSettingsUiState,
    onAddRate: (com.billme.app.data.local.entity.GSTRate) -> Unit,
    onUpdateRate: (com.billme.app.data.local.entity.GSTRate) -> Unit,
    onDeleteRate: (Long) -> Unit
) {
    GSTRateManagementComponent(
        gstRates = uiState.gstRates,
        onAddRate = onAddRate,
        onUpdateRate = onUpdateRate,
        onDeleteRate = onDeleteRate,
        modifier = Modifier.fillMaxSize()
    )
}

/**
 * Tab definitions for GST Settings
 */
private enum class GSTTab {
    Configuration,
    RateManagement
}

/**
 * UI State for GST Settings Screen
 */
data class GSTSettingsUiState(
    val configuration: com.billme.app.data.local.entity.GSTConfiguration? = null,
    val gstRates: List<com.billme.app.data.local.entity.GSTRate> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)
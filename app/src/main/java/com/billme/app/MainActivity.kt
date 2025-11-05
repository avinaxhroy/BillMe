package com.billme.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.billme.app.core.backup.BackupScheduler
import com.billme.app.data.preferences.ThemeMode
import com.billme.app.data.repository.SetupPreferencesRepository
import com.billme.app.ui.navigation.MobileShopNavigation
import com.billme.app.ui.theme.MobileShopTheme
import com.billme.app.ui.theme.ThemeViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    @Inject
    lateinit var setupRepository: SetupPreferencesRepository
    
    @Inject
    lateinit var backupScheduler: BackupScheduler
    
    private val themeViewModel: ThemeViewModel by viewModels()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Initialize daily backup scheduler
        // This will schedule backups if not already scheduled
        backupScheduler.scheduleDailyBackup()
        setContent {
            val themeMode by themeViewModel.themeMode.collectAsState()
            val useDynamicColor by themeViewModel.useDynamicColor.collectAsState()
            val isSystemInDarkTheme = isSystemInDarkTheme()
            
            val darkTheme = when (themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> isSystemInDarkTheme
            }
            
            MobileShopTheme(
                darkTheme = darkTheme,
                dynamicColor = useDynamicColor
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MobileShopNavigation(setupRepository = setupRepository)
                }
            }
        }
    }
}
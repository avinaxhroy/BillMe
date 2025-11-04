package com.billme.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.billme.app.core.backup.BackupScheduler
import com.billme.app.data.repository.SetupPreferencesRepository
import com.billme.app.ui.navigation.MobileShopNavigation
import com.billme.app.ui.theme.MobileShopTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    @Inject
    lateinit var setupRepository: SetupPreferencesRepository
    
    @Inject
    lateinit var backupScheduler: BackupScheduler
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Initialize daily backup scheduler
        // This will schedule backups if not already scheduled
        backupScheduler.scheduleDailyBackup()
        setContent {
            MobileShopTheme {
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
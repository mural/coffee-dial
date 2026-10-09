package com.coffeedial

import android.app.Application
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.coffeedial.auth.AndroidAuthRepository
import com.coffeedial.backup.BackupFiles
import com.coffeedial.ui.App
import kotlinx.coroutines.launch

class CoffeeDialApplication : Application() {
    val authRepository by lazy { AndroidAuthRepository(this, BuildConfig.GOOGLE_WEB_CLIENT_ID) }
    val backupFiles = BackupFiles()
    val repository by lazy { createRepository(this) }
}

class MainActivity : ComponentActivity() {
    private val authRepository by lazy {
        (application as CoffeeDialApplication).authRepository
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        authRepository.attach(this)
        handleIntent(intent)
        enableEdgeToEdge()
        val app = application as CoffeeDialApplication
        setContent {
            BackupPickers(app.backupFiles)
            PhotoPickerHost(app.backupFiles.photos)
            App(app.repository, app.backupFiles, authRepository)
        }
    }
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }
    override fun onResume() {
        super.onResume()
        authRepository.attach(this)
        authRepository.onResume()
    }
    override fun onDestroy() {
        authRepository.detach(this)
        super.onDestroy()
    }
    private fun handleIntent(intent: Intent?) {
        val uri = intent?.data ?: return
        intent.data = null
        lifecycleScope.launch { authRepository.handleCallback(uri) }
    }
}

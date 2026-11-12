package com.example.lab1

import android.animation.ObjectAnimator
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.animation.doOnEnd
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.example.lab1.network.RetrofitClient
import com.example.lab1.ui.theme.Lab1Theme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Manifest.permission.ACCESS_LOCAL_NETWORK only exists from compileSdk 37
private const val LOCAL_NETWORK_PERMISSION = "android.permission.ACCESS_LOCAL_NETWORK"

class MainActivity : ComponentActivity() {

    private val requestLocalNetworkPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (!granted) {
                Toast.makeText(
                    this,
                    "Local network access is required to reach the backend server",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        // 1. Install the splash screen (BEFORE super.onCreate)
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        // Before the first network request (the auth interceptor reads the token through it)
        RetrofitClient.init(applicationContext)

        requestLocalNetworkPermissionIfNeeded()

        // 2. Keep the splash visible while loading
        var isLoading = true
        lifecycleScope.launch {
            delay(1500L) // replace with real initialization logic
            isLoading = false
        }
        splashScreen.setKeepOnScreenCondition { isLoading }

        // 3. Fade-out animation on exit
        splashScreen.setOnExitAnimationListener { splashScreenViewProvider ->
            val view = splashScreenViewProvider.view
            ObjectAnimator.ofFloat(view, View.ALPHA, 1f, 0f).apply {
                duration = 500L
                interpolator = AccelerateDecelerateInterpolator()
                doOnEnd { splashScreenViewProvider.remove() }
            }.start()
        }

        enableEdgeToEdge()
        setContent {
            Lab1Theme {
                AppNavGraph()
            }
        }
    }

    private fun requestLocalNetworkPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= 37 && // Android 17+
            checkSelfPermission(LOCAL_NETWORK_PERMISSION) != PackageManager.PERMISSION_GRANTED
        ) {
            requestLocalNetworkPermission.launch(LOCAL_NETWORK_PERMISSION)
        }
    }
}

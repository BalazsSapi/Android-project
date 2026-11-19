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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.animation.doOnEnd
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.example.lab1.network.AuthRepository
import com.example.lab1.network.RetrofitClient
import com.example.lab1.ui.theme.Lab1Theme
import com.example.lab1.utils.SessionManager
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Manifest.permission.ACCESS_LOCAL_NETWORK csak API 37-es compileSdk-tól létezik
private const val LOCAL_NETWORK_PERMISSION = "android.permission.ACCESS_LOCAL_NETWORK"

class MainActivity : ComponentActivity() {

    // A splash addig marad, amíg az auto-login ellenőrzés (és a minimális megjelenítési idő) tart
    private var isLoading by mutableStateOf(true)

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
        // 1. Splash screen telepítése (super.onCreate ELŐTT)
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        // Az első hálózati kérés előtt (az auth interceptor innen éri el a tokent)
        RetrofitClient.init(applicationContext)

        requestLocalNetworkPermissionIfNeeded()

        // 2. A splash látszik, amíg a betöltés tart: minimális idő + auto-login ellenőrzés
        if (savedInstanceState == null) {
            lifecycleScope.launch {
                val minDisplayTime = async { delay(1500L) } // minimális megjelenítési idő
                autoLogin()
                minDisplayTime.await()
                isLoading = false
            }
        } else {
            // Újraindítás (pl. forgatás): a session már ellenőrizve volt
            isLoading = false
        }
        splashScreen.setKeepOnScreenCondition { isLoading }

        // 3. Fade-out animáció kilépéskor
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
                // A navigáció az ellenőrzés után indul, így a start destination már a friss session alapján dől el
                if (!isLoading) {
                    AppNavGraph()
                }
            }
        }
    }

    // Auto-login: a mentett refresh tokennel új tokeneket kérünk (POST /auth/local/refresh).
    // Ha nincs érvényes session vagy a frissítés sikertelen, a session törlődik, és a Login képernyő jön.
    private suspend fun autoLogin() {
        val sessionManager = SessionManager.getInstance(applicationContext)
        if (!sessionManager.isLoggedIn) return

        val refreshToken = sessionManager.refreshToken
        if (refreshToken.isNullOrBlank()) {
            sessionManager.clear()
            return
        }

        AuthRepository().refreshTokens(refreshToken)
            .onSuccess { tokens ->
                if (tokens.accessToken.isNullOrBlank()) {
                    sessionManager.clear()
                } else {
                    sessionManager.updateTokens(tokens.accessToken, tokens.refreshToken ?: refreshToken)
                }
            }
            .onFailure { sessionManager.clear() }
    }

    private fun requestLocalNetworkPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= 37 && // Android 17+
            checkSelfPermission(LOCAL_NETWORK_PERMISSION) != PackageManager.PERMISSION_GRANTED
        ) {
            requestLocalNetworkPermission.launch(LOCAL_NETWORK_PERMISSION)
        }
    }
}

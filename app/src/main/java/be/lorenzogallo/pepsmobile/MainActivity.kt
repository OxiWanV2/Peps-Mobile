package be.lorenzogallo.pepsmobile

import android.os.Bundle
import android.os.SystemClock
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import be.lorenzogallo.pepsmobile.di.AppContainer
import be.lorenzogallo.pepsmobile.ui.common.LocalAppContainer
import be.lorenzogallo.pepsmobile.ui.common.appViewModel
import be.lorenzogallo.pepsmobile.ui.home.HomeScreen
import be.lorenzogallo.pepsmobile.ui.login.AuthViewModel
import be.lorenzogallo.pepsmobile.ui.login.LoginScreen
import be.lorenzogallo.pepsmobile.ui.theme.PepsTheme
import kotlinx.coroutines.launch

class MainActivity : FragmentActivity() {
    private lateinit var container: AppContainer
    private val locked = mutableStateOf(false)
    private var stoppedAt = 0L
    private var promptShowing = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        container = (application as PepsApp).container

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                container.prefs.secureScreen.collect { applySecureFlag(it) }
            }
        }

        locked.value = if (savedInstanceState == null) shouldLock() else savedInstanceState.getBoolean(KEY_LOCKED, false)

        setContent {
            PepsTheme {
                CompositionLocalProvider(LocalAppContainer provides container) {
                    if (locked.value) {
                        LockScreen(onUnlock = ::showUnlockPrompt)
                    } else {
                        PepsRoot()
                    }
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(KEY_LOCKED, locked.value)
    }

    override fun onStart() {
        super.onStart()
        val elapsed = SystemClock.elapsedRealtime() - stoppedAt
        if (!locked.value && stoppedAt != 0L && elapsed > LOCK_AFTER_MS && shouldLock()) {
            locked.value = true
        }
    }

    override fun onResume() {
        super.onResume()
        if (locked.value) showUnlockPrompt()
    }

    override fun onStop() {
        super.onStop()
        stoppedAt = SystemClock.elapsedRealtime()
    }

    private fun shouldLock(): Boolean =
        container.prefs.biometricLock.value &&
            container.repository.hasStoredCredentials() &&
            BiometricManager.from(this).canAuthenticate(AUTHENTICATORS) == BiometricManager.BIOMETRIC_SUCCESS

    private fun showUnlockPrompt() {
        if (promptShowing || !locked.value) return
        promptShowing = true
        val prompt = BiometricPrompt(
            this,
            ContextCompat.getMainExecutor(this),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    promptShowing = false
                    locked.value = false
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    promptShowing = false
                }

                override fun onAuthenticationFailed() {
                }
            },
        )
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Déverrouiller Pep's Mobile")
            .setSubtitle("Confirmez votre identité pour accéder à vos données scolaires")
            .setAllowedAuthenticators(AUTHENTICATORS)
            .setConfirmationRequired(false)
            .build()
        prompt.authenticate(info)
    }

    private fun applySecureFlag(enabled: Boolean) {
        if (enabled) {
            window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }

    private companion object {
        const val KEY_LOCKED = "locked"
        const val LOCK_AFTER_MS = 2L * 60 * 1000
        const val AUTHENTICATORS = BIOMETRIC_WEAK or DEVICE_CREDENTIAL
    }
}

@Composable
private fun PepsRoot() {
    val authViewModel: AuthViewModel = appViewModel()
    val loggedIn by authViewModel.loggedIn.collectAsStateWithLifecycle()
    if (loggedIn) HomeScreen(authViewModel) else LoginScreen(authViewModel)
}

@Composable
private fun LockScreen(onUnlock: () -> Unit) {
    LaunchedEffect(Unit) { onUnlock() }
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.Lock,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(16.dp))
            Text("Pep's Mobile est verrouillé", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Authentifiez-vous pour consulter vos cours, votre journal de classe et vos résultats.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(24.dp))
            Button(onClick = onUnlock) { Text("Déverrouiller") }
        }
    }
}

package be.lorenzogallo.pepsmobile.ui.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import be.lorenzogallo.pepsmobile.BuildConfig
import be.lorenzogallo.pepsmobile.data.network.PepsApi
import be.lorenzogallo.pepsmobile.ui.common.LocalAppContainer
import be.lorenzogallo.pepsmobile.ui.common.appViewModel
import be.lorenzogallo.pepsmobile.ui.login.AuthViewModel

@Composable
fun SettingsScreen(
    authViewModel: AuthViewModel,
    onBack: () -> Unit,
    onOpenDashboardSettings: () -> Unit,
    profileViewModel: ProfileViewModel = appViewModel(),
) {
    val prefs = LocalAppContainer.current.prefs
    val biometricLock by prefs.biometricLock.collectAsStateWithLifecycle()
    val secureScreen by prefs.secureScreen.collectAsStateWithLifecycle()
    val loggingOut by authViewModel.loggingOut.collectAsStateWithLifecycle()
    val profile by profileViewModel.state.collectAsStateWithLifecycle()
    var confirmLogout by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text("Réglages") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            SectionTitle("Compte")
            ListItem(
                headlineContent = {
                    Text(
                        text = profile.profile?.displayName?.takeIf { it.isNotBlank() }
                            ?: authViewModel.storedEmail()
                            ?: "Compte PepSchool",
                        style = MaterialTheme.typography.titleMedium,
                    )
                },
                supportingContent = {
                    Column {
                        authViewModel.storedEmail()?.let { Text(it) }
                        Text(PepsApi.HOST, style = MaterialTheme.typography.bodySmall)
                    }
                },
                leadingContent = { Avatar(profile) },
            )
            ListItem(
                headlineContent = { Text("Se déconnecter") },
                supportingContent = { Text("Efface les identifiants, la session et le profil de cet appareil") },
                leadingContent = { Icon(Icons.AutoMirrored.Outlined.Logout, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                trailingContent = {
                    TextButton(onClick = { confirmLogout = true }, enabled = !loggingOut) { Text("Déconnexion") }
                },
            )
            HorizontalDivider()

            SectionTitle("Accueil")
            ListItem(
                headlineContent = { Text("Personnaliser l'accueil") },
                supportingContent = { Text("Choisir et ordonner les widgets affichés") },
                leadingContent = { Icon(Icons.Outlined.Tune, contentDescription = null) },
                trailingContent = { Icon(Icons.Filled.ChevronRight, contentDescription = null) },
                modifier = Modifier.clickable(onClick = onOpenDashboardSettings),
            )
            ListItem(
                headlineContent = { Text("Widget d'écran d'accueil") },
                supportingContent = {
                    Text("Ajoutez le widget « Journal du jour » depuis le menu widgets de votre téléphone : cours restants et prochaine évaluation, mis à jour toutes les heures.")
                },
                leadingContent = { Icon(Icons.Outlined.Widgets, contentDescription = null) },
            )
            HorizontalDivider()

            SectionTitle("Sécurité")
            ListItem(
                headlineContent = { Text("Verrouillage biométrique") },
                supportingContent = { Text("Empreinte, visage ou code de l'appareil à l'ouverture de l'application") },
                leadingContent = { Icon(Icons.Outlined.Fingerprint, contentDescription = null) },
                trailingContent = { Switch(checked = biometricLock, onCheckedChange = prefs::setBiometricLock) },
            )
            ListItem(
                headlineContent = { Text("Bloquer les captures d'écran") },
                supportingContent = { Text("Masque aussi le contenu dans l'aperçu des applications récentes") },
                leadingContent = { Icon(Icons.Outlined.Security, contentDescription = null) },
                trailingContent = { Switch(checked = secureScreen, onCheckedChange = prefs::setSecureScreen) },
            )
            ListItem(
                headlineContent = { Text("Information de confidentialité") },
                supportingContent = {
                    Text(
                        "Toutes les informations sont conservées dans votre téléphone et sont uniquement prises en charge par le site pepschool.be.",
                    )
                },
                leadingContent = { Icon(Icons.Outlined.Shield, contentDescription = null) },
            )
            HorizontalDivider()

            SectionTitle("À propos")
            ListItem(
                headlineContent = { Text("Pep's Mobile ${BuildConfig.VERSION_NAME}") },
                supportingContent = { Text("Client Android pour PepSchool produit par Lorenzo Gallo.\n© 2026 PepSchool - Tous droits réservés.\nApplication non-officiel.") },
                leadingContent = { Icon(Icons.Outlined.Info, contentDescription = null) },
            )
        }
    }

    if (confirmLogout) {
        AlertDialog(
            onDismissRequest = { confirmLogout = false },
            title = { Text("Se déconnecter ?") },
            text = { Text("Les identifiants enregistrés et la session seront supprimés de cet appareil.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmLogout = false
                    authViewModel.logout()
                }) { Text("Déconnexion") }
            },
            dismissButton = { TextButton(onClick = { confirmLogout = false }) { Text("Annuler") } },
        )
    }
}

@Composable
private fun Avatar(state: ProfileState) {
    val bitmap = state.avatar
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Photo de profil",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Surface(color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.fillMaxSize()) {
                Box(contentAlignment = Alignment.Center) {
                    val initials = state.profile?.let { p ->
                        listOf(p.firstName, p.lastName).mapNotNull { it.firstOrNull()?.uppercaseChar() }.joinToString("")
                    }.orEmpty()
                    if (initials.isNotEmpty()) {
                        Text(initials, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    } else {
                        Icon(Icons.Outlined.Person, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp),
    )
}

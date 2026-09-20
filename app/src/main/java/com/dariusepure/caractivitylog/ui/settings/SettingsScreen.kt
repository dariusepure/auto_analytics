package com.dariusepure.caractivitylog.ui.settings

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material3.*
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.dariusepure.caractivitylog.ui.common.DropdownPositionProvider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dariusepure.caractivitylog.R
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import com.dariusepure.caractivitylog.ui.common.supportedLanguages
import com.dariusepure.caractivitylog.ui.theme.SettingsEvent
import com.dariusepure.caractivitylog.ui.theme.SettingsViewModel
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onLogout: () -> Unit,
    viewModel: SettingsViewModel
) {
    val isDarkMode by viewModel.isDarkMode.collectAsStateWithLifecycle()
    val unitSystem by viewModel.unitSystem.collectAsStateWithLifecycle()
    val notifyItp by viewModel.notifyItp.collectAsStateWithLifecycle()
    val notifyInsurance by viewModel.notifyInsurance.collectAsStateWithLifecycle()
    val notifyVignette by viewModel.notifyVignette.collectAsStateWithLifecycle()
    val userEmail by viewModel.userEmail.collectAsStateWithLifecycle()
    val isAnonymous by viewModel.isAnonymous.collectAsStateWithLifecycle()
    val systemDark = androidx.compose.foundation.isSystemInDarkTheme()
    val currentDark = isDarkMode ?: systemDark

    var languageMenuExpanded by remember { mutableStateOf(false) }
    var unitMenuExpanded by remember { mutableStateOf(false) }
    
    var languageMenuWidth by remember { mutableStateOf(0.dp) }
    var unitMenuWidth by remember { mutableStateOf(0.dp) }
    val density = LocalDensity.current

    // Dialog states
    var showChangePasswordDialog by remember { mutableStateOf(false) }
    var showDeleteAccountDialog by remember { mutableStateOf(false) }
    
    val snackbarHostState = remember { SnackbarHostState() }
    val context = androidx.compose.ui.platform.LocalContext.current
    
    LaunchedEffect(Unit) {
        viewModel.settingsEvent.collectLatest { event ->
            when (event) {
                is SettingsEvent.PasswordChanged -> {
                    showChangePasswordDialog = false
                    snackbarHostState.showSnackbar("Password updated!")
                }
                is SettingsEvent.AccountDeleted -> {
                    showDeleteAccountDialog = false
                    onLogout()
                }
                is SettingsEvent.DataExported -> {
                    val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                        type = "text/csv"
                        putExtra(android.content.Intent.EXTRA_SUBJECT, "Auto Analytics Export")
                        putExtra(android.content.Intent.EXTRA_TEXT, event.csvContent)
                    }
                    context.startActivity(android.content.Intent.createChooser(intent, "Exportă datele"))
                }
                is SettingsEvent.Error -> {
                    snackbarHostState.showSnackbar(event.message)
                }
            }
        }
    }

    val locales = AppCompatDelegate.getApplicationLocales()
    val currentLocale = if (!locales.isEmpty) locales.get(0)?.language ?: "en" else "en"
    val currentLanguage = supportedLanguages.find { it.code == currentLocale } ?: supportedLanguages[0]

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.common_settings)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Account Info Header
            val userData by viewModel.userData.collectAsStateWithLifecycle()
            
            (userData ?: userEmail?.let { email -> com.dariusepure.caractivitylog.domain.User("", email) })?.let { user ->
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            modifier = Modifier.size(48.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text(
                                text = if (isAnonymous) stringResource(R.string.settings_guest_user) else stringResource(R.string.settings_account_signed_in_as),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (!isAnonymous && user.name.isNotBlank()) {
                                Text(
                                    text = user.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (user.email.isNotBlank()) {
                                    Text(
                                        text = user.email,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            } else {
                                Text(
                                    text = if (isAnonymous) stringResource(R.string.settings_guest_hint) else user.email,
                                    style = if (isAnonymous) MaterialTheme.typography.bodySmall else MaterialTheme.typography.titleMedium,
                                    fontWeight = if (isAnonymous) FontWeight.Normal else FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            // Appearance Section
            SettingsSection(title = stringResource(R.string.settings_section_appearance)) {
                SettingsItem(
                    label = stringResource(if (currentDark) R.string.theme_light_mode else R.string.theme_dark_mode),
                    icon = if (currentDark) Icons.Default.LightMode else Icons.Default.DarkMode,
                    onClick = { viewModel.toggleTheme(currentDark) },
                    trailing = {
                        Switch(
                            checked = currentDark,
                            onCheckedChange = { viewModel.toggleTheme(currentDark) }
                        )
                    }
                )
            }

            // Notifications Section
            SettingsSection(title = "Notificări") {
                SettingsItem(
                    label = "Alerte ITP",
                    icon = Icons.Default.NotificationsActive,
                    onClick = { viewModel.setNotifyItp(!notifyItp) },
                    trailing = {
                        Switch(checked = notifyItp, onCheckedChange = { viewModel.setNotifyItp(it) })
                    }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)
                SettingsItem(
                    label = "Alerte Asigurare",
                    icon = Icons.Default.Security,
                    onClick = { viewModel.setNotifyInsurance(!notifyInsurance) },
                    trailing = {
                        Switch(checked = notifyInsurance, onCheckedChange = { viewModel.setNotifyInsurance(it) })
                    }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)
                SettingsItem(
                    label = "Alerte Rovinietă",
                    icon = Icons.Default.ConfirmationNumber,
                    onClick = { viewModel.setNotifyVignette(!notifyVignette) },
                    trailing = {
                        Switch(checked = notifyVignette, onCheckedChange = { viewModel.setNotifyVignette(it) })
                    }
                )
            }

            // Regional Section
            SettingsSection(title = stringResource(R.string.settings_section_regional)) {
                // Language
                ExposedDropdownMenuBox(
                    expanded = languageMenuExpanded,
                    onExpandedChange = { languageMenuExpanded = it },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    SettingsItem(
                        label = stringResource(R.string.common_language),
                        icon = Icons.Outlined.Language,
                        onClick = { languageMenuExpanded = true },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
                            .onGloballyPositioned { languageMenuWidth = with(density) { it.size.width.toDp() } },
                        trailing = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "${currentLanguage.flag} ${currentLanguage.name}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = languageMenuExpanded)
                            }
                        }
                    )
                    if (languageMenuExpanded) {
                        Popup(
                            onDismissRequest = { languageMenuExpanded = false },
                            popupPositionProvider = DropdownPositionProvider(),
                            properties = PopupProperties(focusable = false, clippingEnabled = false)
                        ) {
                            Surface(
                                modifier = Modifier.width(languageMenuWidth),
                                shape = RoundedCornerShape(4.dp),
                                tonalElevation = 3.dp,
                                shadowElevation = 3.dp
                            ) {
                                Column {
                                    supportedLanguages.forEach { language ->
                                        DropdownMenuItem(
                                            text = { Text("${language.flag} ${language.name}") },
                                            onClick = {
                                                val appLocale: LocaleListCompat = LocaleListCompat.forLanguageTags(language.code)
                                                AppCompatDelegate.setApplicationLocales(appLocale)
                                                languageMenuExpanded = false
                                            },
                                            trailingIcon = {
                                                if (language.code == currentLocale) {
                                                    Icon(Icons.Default.CheckCircle, null, Modifier.size(18.dp))
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )

                // Unit System
                ExposedDropdownMenuBox(
                    expanded = unitMenuExpanded,
                    onExpandedChange = { unitMenuExpanded = it },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    SettingsItem(
                        label = stringResource(R.string.unit_system_label),
                        icon = Icons.Default.Speed,
                        onClick = { unitMenuExpanded = true },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
                            .onGloballyPositioned { unitMenuWidth = with(density) { it.size.width.toDp() } },
                        trailing = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = if (unitSystem == com.dariusepure.caractivitylog.domain.UnitSystem.METRIC)
                                        stringResource(R.string.unit_system_metric_label)
                                    else stringResource(R.string.unit_system_imperial_label),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = unitMenuExpanded)
                            }
                        }
                    )
                    if (unitMenuExpanded) {
                        Popup(
                            onDismissRequest = { unitMenuExpanded = false },
                            popupPositionProvider = DropdownPositionProvider(),
                            properties = PopupProperties(focusable = false, clippingEnabled = false)
                        ) {
                            Surface(
                                modifier = Modifier.width(unitMenuWidth),
                                shape = RoundedCornerShape(4.dp),
                                tonalElevation = 3.dp,
                                shadowElevation = 3.dp
                            ) {
                                Column {
                                    com.dariusepure.caractivitylog.domain.UnitSystem.entries.forEach { system ->
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    if (system == com.dariusepure.caractivitylog.domain.UnitSystem.METRIC)
                                                        stringResource(R.string.unit_system_metric_label)
                                                    else stringResource(R.string.unit_system_imperial_label)
                                                )
                                            },
                                            onClick = {
                                                viewModel.setUnitSystem(system)
                                                unitMenuExpanded = false
                                            },
                                            trailingIcon = {
                                                if (system == unitSystem) {
                                                    Icon(Icons.Default.CheckCircle, null, Modifier.size(18.dp))
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Data Section
            SettingsSection(title = "Date și Backup") {
                SettingsItem(
                    label = "Exportă Istoric (CSV)",
                    icon = Icons.Default.FileDownload,
                    onClick = { viewModel.exportDataToCsv() }
                )
            }

            // Account Section
            SettingsSection(title = stringResource(R.string.settings_section_account)) {
                SettingsItem(
                    label = stringResource(R.string.auth_logout),
                    icon = Icons.AutoMirrored.Filled.Logout,
                    onClick = {
                        viewModel.signOut()
                        onLogout()
                    }
                )

                if (!isAnonymous && viewModel.isPasswordUser) {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                    SettingsItem(
                        label = stringResource(R.string.settings_change_password),
                        icon = Icons.Default.Lock,
                        onClick = { showChangePasswordDialog = true }
                    )
                }

                if (!isAnonymous) {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                    SettingsItem(
                        label = stringResource(R.string.settings_delete_account),
                        icon = Icons.Default.DeleteForever,
                        onClick = { showDeleteAccountDialog = true },
                        labelColor = MaterialTheme.colorScheme.error,
                        iconColor = MaterialTheme.colorScheme.error
                    )
                }
            }

            // About Section
            SettingsSection(title = "Despre") {
                SettingsItem(
                    label = stringResource(R.string.common_powered_by),
                    icon = Icons.Default.Info,
                    onClick = {
                        try {
                            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://dariusdevworks.com"))
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                )
            }
        }
    }

    if (showChangePasswordDialog) {
        ChangePasswordDialog(
            onDismiss = { showChangePasswordDialog = false },
            onConfirm = { current, new -> viewModel.changePassword(current, new) }
        )
    }

    if (showDeleteAccountDialog) {
        DeleteAccountDialog(
            isPasswordUser = viewModel.isPasswordUser,
            onDismiss = { showDeleteAccountDialog = false },
            onConfirm = { password -> viewModel.deleteAccount(password) }
        )
    }
}

@Composable
fun ChangePasswordDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_change_password_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = currentPassword,
                    onValueChange = { currentPassword = it },
                    label = { Text(stringResource(R.string.settings_current_password_label)) },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    label = { Text(stringResource(R.string.settings_new_password_label)) },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = { Text(stringResource(R.string.settings_confirm_new_password_label)) },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = null
                            )
                        }
                    }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(currentPassword, newPassword) },
                enabled = currentPassword.isNotBlank() && newPassword.isNotBlank() && newPassword == confirmPassword
            ) {
                Text(stringResource(R.string.common_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_cancel))
            }
        }
    )
}

@Composable
fun DeleteAccountDialog(
    isPasswordUser: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String?) -> Unit
) {
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_delete_account_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(stringResource(R.string.settings_delete_account_message))
                if (isPasswordUser) {
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text(stringResource(R.string.auth_password_label)) },
                        placeholder = { Text(stringResource(R.string.settings_delete_account_reauth_hint)) },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = null
                                )
                            }
                        }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(if (isPasswordUser) password else null) },
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                enabled = !isPasswordUser || password.isNotBlank()
            ) {
                Text(stringResource(R.string.settings_delete_account_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_cancel))
            }
        }
    )
}

@Composable
fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                content()
            }
        }
    }
}

@Composable
fun SettingsItem(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    labelColor: Color = MaterialTheme.colorScheme.onSurface,
    iconColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    trailing: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.width(16.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = labelColor,
            modifier = Modifier.weight(1f)
        )
        trailing?.invoke()
    }
}

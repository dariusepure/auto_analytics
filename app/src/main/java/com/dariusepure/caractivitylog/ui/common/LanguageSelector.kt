package com.dariusepure.caractivitylog.ui.common

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import com.dariusepure.caractivitylog.R

data class Language(
    val name: String,
    val code: String,
    val flag: String
)

val supportedLanguages = listOf(
    Language("English", "en", "🇺🇸"),
    Language("Română", "ro", "🇷🇴")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageDialog(
    onDismiss: () -> Unit
) {
    val locales = AppCompatDelegate.getApplicationLocales()
    val currentLocale = if (!locales.isEmpty) locales.get(0)?.language ?: "en" else "en"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.common_language)) },
        text = {
            Column {
                supportedLanguages.forEach { language ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val appLocale: LocaleListCompat = LocaleListCompat.forLanguageTags(language.code)
                                AppCompatDelegate.setApplicationLocales(appLocale)
                                onDismiss()
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${language.flag} ${language.name}",
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyLarge
                        )
                        if (language.code == currentLocale) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_cancel))
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageSelector(
    modifier: Modifier = Modifier
) {
    var showDialog by remember { mutableStateOf(false) }

    if (showDialog) {
        LanguageDialog(onDismiss = { showDialog = false })
    }

    Box(modifier = modifier) {
        val tooltipState = rememberTooltipState()
        TooltipBox(
            positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
            tooltip = {
                PlainTooltip {
                    Text(stringResource(R.string.common_language))
                }
            },
            state = tooltipState
        ) {
            IconButton(onClick = { showDialog = true }) {
                Icon(
                    imageVector = Icons.Outlined.Language,
                    contentDescription = stringResource(R.string.common_language),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}


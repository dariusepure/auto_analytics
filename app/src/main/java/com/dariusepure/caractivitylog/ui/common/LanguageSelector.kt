package com.dariusepure.caractivitylog.ui.common

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
fun LanguageSelector(
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val locales = AppCompatDelegate.getApplicationLocales()
    val currentLocale = if (!locales.isEmpty) locales.get(0)?.language ?: "en" else "en"

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
            IconButton(onClick = { expanded = true }) {
                Icon(
                    imageVector = Icons.Outlined.Language,
                    contentDescription = stringResource(R.string.common_language),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            supportedLanguages.forEach { language ->
                DropdownMenuItem(
                    text = {
                        Text(text = "${language.flag} ${language.name}")
                    },
                    onClick = {
                        val appLocale: LocaleListCompat = LocaleListCompat.forLanguageTags(language.code)
                        AppCompatDelegate.setApplicationLocales(appLocale)
                        expanded = false
                    },
                    trailingIcon = {
                        if (language.code == currentLocale) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                )
            }
        }
    }
}

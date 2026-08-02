package com.dariusepure.caractivitylog.ui.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.dariusepure.caractivitylog.R
import com.dariusepure.caractivitylog.data.prefs.supportedCurrencies

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurrencyDialog(
    currentCurrencyCode: String,
    onCurrencySelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.common_currency)) },
        text = {
            Column {
                supportedCurrencies.forEach { currency ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onCurrencySelected(currency.code)
                                onDismiss()
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "(${currency.symbol}) ${currency.name}",
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyLarge
                        )
                        if (currency.code == currentCurrencyCode) {
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
fun CurrencySelector(
    currentCurrencyCode: String,
    onCurrencySelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showDialog by remember { mutableStateOf(false) }

    if (showDialog) {
        CurrencyDialog(
            currentCurrencyCode = currentCurrencyCode,
            onCurrencySelected = onCurrencySelected,
            onDismiss = { showDialog = false }
        )
    }

    Box(modifier = modifier) {
        val tooltipState = rememberTooltipState()
        TooltipBox(
            positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
            tooltip = {
                PlainTooltip {
                    Text(stringResource(R.string.common_currency))
                }
            },
            state = tooltipState
        ) {
            IconButton(onClick = { showDialog = true }) {
                Icon(
                    imageVector = Icons.Outlined.Payments,
                    contentDescription = stringResource(R.string.common_currency),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

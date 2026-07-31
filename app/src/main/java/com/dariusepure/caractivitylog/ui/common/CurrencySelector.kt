package com.dariusepure.caractivitylog.ui.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.dariusepure.caractivitylog.R
import com.dariusepure.caractivitylog.data.prefs.supportedCurrencies

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurrencySelector(
    currentCurrencyCode: String,
    onCurrencySelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

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
            IconButton(onClick = { expanded = true }) {
                Icon(
                    imageVector = Icons.Outlined.Payments,
                    contentDescription = stringResource(R.string.common_currency),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            supportedCurrencies.forEach { currency ->
                DropdownMenuItem(
                    text = {
                        Text(text = "(${currency.symbol}) ${currency.name}")
                    },
                    onClick = {
                        onCurrencySelected(currency.code)
                        expanded = false
                    },
                    trailingIcon = {
                        if (currency.code == currentCurrencyCode) {
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

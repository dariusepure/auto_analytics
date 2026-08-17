package com.dariusepure.caractivitylog.ui.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dariusepure.caractivitylog.R
import com.dariusepure.caractivitylog.domain.Insurance
import com.dariusepure.caractivitylog.domain.MileageLog
import com.dariusepure.caractivitylog.domain.TireSet
import com.dariusepure.caractivitylog.domain.VehicleInspection
import com.dariusepure.caractivitylog.domain.Vignette
import com.dariusepure.caractivitylog.ui.cars.europeanCountries
import com.dariusepure.caractivitylog.ui.theme.statusExpiredRed
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InsuranceItem(
    insurance: Insurance,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val isExpired = CarFormatters.isInspectionExpired(null) // Mock logic for simplicity
    val statusColor = if (CarFormatters.isInspectionExpired(null)) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = insurance.provider,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            val details = mutableListOf<String>()
            details.add(CarFormatters.formatDate(insurance.date))
            
            Text(
                text = details.joinToString(" \u00B7 "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Text(
                text = stringResource(R.string.formatter_inspection_valid_until, CarFormatters.formatDate(insurance.expiryDate)),
                style = MaterialTheme.typography.labelSmall,
                color = if (insurance.expiryDate.before(java.util.Date())) statusExpiredRed else MaterialTheme.colorScheme.secondary
            )
        }

        ActionButtons(
            onEdit = onEditClick,
            onDelete = onDeleteClick
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VignetteItem(
    vignette: Vignette,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val country = europeanCountries.find { it.name == vignette.country }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (country != null) {
                    Text(text = country.flag, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.width(8.dp))
                }
                Text(
                    text = vignette.country,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            val details = mutableListOf<String>()
            details.add(CarFormatters.formatDate(vignette.date))
            
            Text(
                text = details.joinToString(" \u00B7 "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Text(
                text = stringResource(R.string.formatter_inspection_valid_until, CarFormatters.formatDate(vignette.expiryDate)),
                style = MaterialTheme.typography.labelSmall,
                color = if (vignette.expiryDate.before(java.util.Date())) statusExpiredRed else MaterialTheme.colorScheme.secondary
            )
        }

        ActionButtons(
            onEdit = onEditClick,
            onDelete = onDeleteClick
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TireSetItem(
    tireSet: TireSet,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val context = LocalContext.current
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(tireSet.season.labelRes),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                if (tireSet.isActive) {
                    Spacer(Modifier.width(8.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = MaterialTheme.shapes.extraSmall
                    ) {
                        Text(
                            text = stringResource(R.string.tire_on_vehicle),
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
            }
            Text(
                text = stringResource(R.string.tire_summary, tireSet.brand, tireSet.width, tireSet.ratio, tireSet.diameter),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            val subDetails = mutableListOf<String>()
            val dotText = when {
                tireSet.dotWeek != null && tireSet.dotYear != null -> {
                    val weekStr = tireSet.dotWeek.toString().padStart(2, '0')
                    val yearStr = tireSet.dotYear.toString().let { if (it.length > 2) it.takeLast(2) else it.padStart(2, '0') }
                    "$weekStr$yearStr"
                }
                tireSet.dotWeek != null -> tireSet.dotWeek.toString().padStart(2, '0')
                tireSet.dotYear != null -> tireSet.dotYear.toString()
                else -> null
            }
            if (!dotText.isNullOrBlank()) subDetails.add("DOT $dotText")
            
            if (subDetails.isNotEmpty()) {
                Text(
                    text = subDetails.joinToString(" \u00B7 "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        ActionButtons(
            onEdit = onEditClick,
            onDelete = onDeleteClick
        )
    }
}

@Composable
fun SpecificationCard(specifications: List<Pair<String, String>>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            specifications.forEachIndexed { index, (label, value) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp)
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AutoSizeText(
                        text = label,
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        minFontSize = 9.sp,
                        maxLines = 2
                    )
                    Text(
                        text = value.ifBlank { "-" },
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1.5f),
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = androidx.compose.ui.text.style.TextAlign.End
                    )
                }
                if (index < specifications.size - 1) {
                    HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InspectionItem(
    inspection: VehicleInspection,
    unit: String,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "${inspection.mileage.roundToInt()} $unit",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = CarFormatters.formatDate(inspection.date),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = stringResource(R.string.formatter_inspection_valid_until, CarFormatters.formatDate(inspection.expiryDate)),
                style = MaterialTheme.typography.labelSmall,
                color = if (inspection.expiryDate.before(java.util.Date())) statusExpiredRed else MaterialTheme.colorScheme.secondary
            )
        }

        ActionButtons(
            onEdit = onEditClick,
            onDelete = onDeleteClick
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MileageItem(
    log: MileageLog,
    unit: String,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "${log.km.roundToInt()} $unit",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = CarFormatters.formatDate(log.date),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        ActionButtons(
            onEdit = onEditClick,
            onDelete = onDeleteClick
        )
    }
}

@Composable
fun BentoCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxSize(),
            horizontalAlignment = horizontalAlignment,
            content = content
        )
    }
}

@Composable
fun StatusBadge(
    label: String,
    color: Color
) {
    Surface(
        color = color.copy(alpha = 0.12f),
        contentColor = color,
        shape = MaterialTheme.shapes.extraSmall,
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun StatItem(
    label: String, 
    value: String, 
    color: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = label, 
            style = MaterialTheme.typography.labelSmall, 
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value, 
            style = MaterialTheme.typography.titleLarge, 
            fontWeight = FontWeight.Bold, 
            color = color
        )
    }
}

/**
 * Text component that automatically scales down font size to fit in 1 line.
 */
@Composable
fun AutoSizeText(
    text: String,
    style: androidx.compose.ui.text.TextStyle,
    modifier: Modifier = Modifier,
    minFontSize: TextUnit = 10.sp,
    maxLines: Int = 1,
    softWrap: Boolean = maxLines > 1,
    textAlign: TextAlign? = null,
    color: Color = Color.Unspecified
) {
    Text(
        text = text,
        modifier = modifier,
        style = style,
        maxLines = maxLines,
        softWrap = softWrap,
        overflow = TextOverflow.Ellipsis,
        textAlign = textAlign,
        color = color
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActionButtons(
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    editLabel: String = stringResource(R.string.common_edit),
    deleteLabel: String = stringResource(R.string.common_delete),
    accentColor: Color = Color(0xFF1A73E8),
    deleteColor: Color = statusExpiredRed,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.spacedBy(8.dp),
    verticalAlignment: Alignment.Vertical = Alignment.CenterVertically
) {
    Row(
        modifier = modifier,
        verticalAlignment = verticalAlignment,
        horizontalArrangement = horizontalArrangement
    ) {
        val editTooltipState = rememberTooltipState()
        TooltipBox(
            positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
            tooltip = { PlainTooltip { Text(editLabel) } },
            state = editTooltipState
        ) {
            FilledIconButton(
                onClick = onEdit,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = accentColor,
                    contentColor = Color.White
                ),
                modifier = Modifier.size(36.dp)
            ) {
                Icon(Icons.Default.Edit, null, modifier = Modifier.size(18.dp))
            }
        }
        
        val deleteTooltipState = rememberTooltipState()
        TooltipBox(
            positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
            tooltip = { PlainTooltip { Text(deleteLabel) } },
            state = deleteTooltipState
        ) {
            FilledIconButton(
                onClick = onDelete,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = deleteColor,
                    contentColor = Color.White
                ),
                modifier = Modifier.size(36.dp)
            ) {
                Icon(Icons.Default.Delete, null, modifier = Modifier.size(18.dp))
            }
        }
    }
}


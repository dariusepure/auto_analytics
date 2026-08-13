package com.dariusepure.caractivitylog.ui.cars

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.TireRepair
import androidx.compose.material.icons.outlined.TireRepair
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dariusepure.caractivitylog.R
import com.dariusepure.caractivitylog.domain.TireSet
import com.dariusepure.caractivitylog.domain.TireSeason
import com.dariusepure.caractivitylog.domain.displayName
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.sp
import com.dariusepure.caractivitylog.ui.common.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TireHistoryScreen(
    carId: String,
    onBack: () -> Unit,
    viewModel: TireHistoryViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }
    var editingTireSet by remember { mutableStateOf<TireSet?>(null) }
    var tireSetToDelete by remember { mutableStateOf<TireSet?>(null) }

    LaunchedEffect(carId) {
        viewModel.loadData(carId)
    }

    if (tireSetToDelete != null) {
        DeleteConfirmationDialog(
            onConfirm = {
                viewModel.deleteTireSet(carId, tireSetToDelete!!.id)
                tireSetToDelete = null
            },
            onDismiss = { tireSetToDelete = null }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.tire_management_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = Color(0xFF1A73E8),
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.tire_add_title))
            }
        }
    ) { padding ->
        when (val s = state) {
            TireHistoryUiState.Loading -> LoadingState()
            is TireHistoryUiState.Error -> ErrorState(message = s.message, onRetry = { viewModel.loadData(carId) })
            is TireHistoryUiState.Success -> {
                val carAccentColor = Color(0xFF1A73E8)

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        TireStatsCard(s.stats)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.tire_management_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    if (s.tireSets.isEmpty()) {
                        item {
                            EmptyState(
                                icon = Icons.Outlined.TireRepair,
                                title = stringResource(R.string.tire_empty),
                                subtitle = ""
                            )
                        }
                    }

                    items(s.tireSets) { tireSet ->
                        TireSetLogItem(
                            tireSet = tireSet,
                            onEdit = { editingTireSet = tireSet },
                            onDelete = { tireSetToDelete = tireSet }
                        )
                    }
                    
                    item { Spacer(Modifier.height(80.dp)) }
                }

                if (showAddDialog || editingTireSet != null) {
                    AddTireSetDialog(
                        existingTireSet = editingTireSet,
                        onDismiss = { 
                            showAddDialog = false
                            editingTireSet = null
                        },
                        onConfirm = { tireSet ->
                            if (editingTireSet != null) {
                                viewModel.updateTireSet(carId, tireSet.copy(id = editingTireSet!!.id))
                            } else {
                                viewModel.addTireSet(carId, tireSet)
                            }
                            showAddDialog = false
                            editingTireSet = null
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun AddTireSetDialog(
    existingTireSet: TireSet? = null,
    onDismiss: () -> Unit,
    onConfirm: (TireSet) -> Unit
) {
    var season by remember { mutableStateOf(existingTireSet?.season ?: TireSeason.SUMMER) }
    var brand by remember { mutableStateOf(existingTireSet?.brand ?: "") }
    var brandExpanded by remember { mutableStateOf(false) }
    var width by remember { mutableStateOf(existingTireSet?.width?.toString() ?: "") }
    var ratio by remember { mutableStateOf(existingTireSet?.ratio?.toString() ?: "") }
    var diameter by remember { mutableStateOf(existingTireSet?.diameter?.toString() ?: "") }
    var dotWeek by remember { mutableStateOf(existingTireSet?.dotWeek?.toString() ?: "") }
    var dotYear by remember { mutableStateOf(existingTireSet?.dotYear?.toString() ?: "") }
    var isActive by remember { mutableStateOf(existingTireSet?.isActive ?: false) }
    var seasonExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existingTireSet == null) stringResource(R.string.tire_add_title) else stringResource(R.string.tire_edit_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = stringResource(season.labelRes),
                        onValueChange = { },
                        readOnly = true,
                        label = { Text(stringResource(R.string.tire_season_label)) },
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            IconButton(onClick = { seasonExpanded = true }) {
                                Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null)
                            }
                        }
                    )
                    Box(modifier = Modifier.matchParentSize().clickable { seasonExpanded = true })
                    DropdownMenu(
                        expanded = seasonExpanded,
                        onDismissRequest = { seasonExpanded = false }
                    ) {
                        TireSeason.entries.forEach { s ->
                            DropdownMenuItem(
                                text = { Text(stringResource(s.labelRes)) },
                                onClick = {
                                    season = s
                                    seasonExpanded = false
                                }
                            )
                        }
                    }
                }

                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = brand,
                        onValueChange = { input ->
                            brand = input.lowercase().replaceFirstChar { it.uppercase() }
                        },
                        label = { Text(stringResource(R.string.tire_brand_label)) },
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            IconButton(onClick = { brandExpanded = true }) {
                                Icon(Icons.Default.ArrowDropDown, "dropdown")
                            }
                        },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            capitalization = KeyboardCapitalization.Characters
                        )
                    )
                    Box(modifier = Modifier.matchParentSize().clickable { brandExpanded = true })
                    DropdownMenu(
                        expanded = brandExpanded,
                        onDismissRequest = { brandExpanded = false },
                        modifier = Modifier.fillMaxWidth(0.9f).sizeIn(maxHeight = 300.dp)
                    ) {
                        tireBrands.forEach { b ->
                            DropdownMenuItem(
                                text = { Text(b) },
                                onClick = {
                                    brand = if (b == "OTHER") "" else b
                                    brandExpanded = false
                                }
                            )
                        }
                    }
                }

                Text(
                    text = stringResource(R.string.car_tire_size_label),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = width,
                        onValueChange = { if (it.all { char -> char.isDigit() }) width = it },
                        label = { AutoSizeText(text = stringResource(R.string.car_tire_width_label), style = MaterialTheme.typography.bodyMedium, minFontSize = 10.sp) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                        suffix = { Text("mm") }
                    )
                    OutlinedTextField(
                        value = ratio,
                        onValueChange = { if (it.all { char -> char.isDigit() }) ratio = it },
                        label = { AutoSizeText(text = stringResource(R.string.car_tire_ratio_label), style = MaterialTheme.typography.bodyMedium, minFontSize = 10.sp) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                        suffix = { Text("%") }
                    )
                    OutlinedTextField(
                        value = diameter,
                        onValueChange = { if (it.all { char -> char.isDigit() }) diameter = it },
                        label = { AutoSizeText(text = stringResource(R.string.car_tire_diam_label), style = MaterialTheme.typography.bodyMedium, minFontSize = 10.sp) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                        suffix = { Text("\"") }
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val dotWeekInt = dotWeek.toIntOrNull()
                    val isWeekInvalid = dotWeek.isNotBlank() && (dotWeekInt == null || dotWeekInt !in 1..53)
                    
                    OutlinedTextField(
                        value = dotWeek,
                        onValueChange = { if (it.length <= 2 && it.all { char -> char.isDigit() }) dotWeek = it },
                        label = { AutoSizeText(text = stringResource(R.string.tire_dot_week_label), style = MaterialTheme.typography.bodySmall, minFontSize = 8.sp) },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                        isError = isWeekInvalid,
                        supportingText = if (isWeekInvalid) {
                            { Text(stringResource(R.string.validation_dot_week_range)) }
                        } else null
                    )
                    OutlinedTextField(
                        value = dotYear,
                        onValueChange = { if (it.length <= 4 && it.all { char -> char.isDigit() }) dotYear = it },
                        label = { AutoSizeText(text = stringResource(R.string.tire_dot_year_label), style = MaterialTheme.typography.bodySmall, minFontSize = 8.sp) },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isActive, onCheckedChange = { isActive = it })
                    Text(stringResource(R.string.tire_active_label))
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        TireSet(
                            season = season,
                            brand = brand,
                            width = width.toIntOrNull() ?: 0,
                            ratio = ratio.toIntOrNull() ?: 0,
                            diameter = diameter.toIntOrNull() ?: 0,
                            dotWeek = dotWeek.toIntOrNull(),
                            dotYear = dotYear.toIntOrNull(),
                            isActive = isActive
                        )
                    )
                },
                enabled = brand.isNotBlank() && width.isNotBlank() && 
                        (dotWeek.isBlank() || (dotWeek.toIntOrNull() in 1..53)),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
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
fun TireStatsCard(stats: TireStats) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatItem(
                    label = "Active Set", 
                    value = stats.activeTireSet?.brand ?: "None"
                )
                StatItem(
                    label = "Total Sets", 
                    value = "${stats.totalSets}"
                )
            }
            if (stats.activeTireSet != null) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(
                        R.string.tire_summary,
                        stats.activeTireSet.brand,
                        stats.activeTireSet.width,
                        stats.activeTireSet.ratio,
                        stats.activeTireSet.diameter
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
fun TireSetLogItem(
    tireSet: TireSet,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.TireRepair, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = tireSet.brand,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (tireSet.isActive) {
                        Spacer(Modifier.width(8.dp))
                        Surface(
                            color = Color(0xFF4CAF50),
                            shape = MaterialTheme.shapes.extraSmall
                        ) {
                            Text(
                                text = stringResource(R.string.tire_on_vehicle),
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White
                            )
                        }
                    }
                }
                Text(
                    text = stringResource(tireSet.season.labelRes),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary
                )
                Text(
                    text = stringResource(
                        R.string.tire_summary,
                        "",
                        tireSet.width,
                        tireSet.ratio,
                        tireSet.diameter
                    ).trim().replace("  ", " "),
                    style = MaterialTheme.typography.bodySmall
                )
                if (tireSet.dotWeek != null && tireSet.dotYear != null) {
                    Text(
                        text = "DOT ${String.format("%02d%02d", tireSet.dotWeek, tireSet.dotYear % 100)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            ActionButtons(
                onEdit = onEdit,
                onDelete = onDelete
            )
        }
    }
}

/*
 * Copyright (C) 2026 Darius Epure (Darius DevWorks)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.dariusepure.caractivitylog.ui.cars

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.dariusepure.caractivitylog.R
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dariusepure.caractivitylog.ui.common.*
import com.dariusepure.caractivitylog.domain.TireSet
import com.dariusepure.caractivitylog.domain.TireSeason
import com.dariusepure.caractivitylog.domain.displayName
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TireHistoryScreen(
    carId: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CarDetailsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }
    var editingTireSet by remember { mutableStateOf<TireSet?>(null) }
    var tireSetToDelete by remember { mutableStateOf<TireSet?>(null) }

    LaunchedEffect(carId) {
        viewModel.loadCarData(carId)
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

    Scaffold(
        modifier = modifier,
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
            CarDetailsUiState.Loading -> LoadingState()
            is CarDetailsUiState.Error -> ErrorState(message = s.message, onRetry = { viewModel.loadCarData(carId) })
            is CarDetailsUiState.Success -> {
                val car = s.car

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 16.dp)
                ) {
                    item {
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = car.displayName,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(Modifier.height(8.dp))
                    }

                    if (s.tireSets.isEmpty()) {
                        item {
                            Text(
                                text = stringResource(R.string.tire_empty),
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(vertical = 16.dp)
                            )
                        }
                    } else {
                        items(s.tireSets) { tireSet ->
                            TireSetItem(
                                tireSet = tireSet,
                                onEditClick = { editingTireSet = tireSet },
                                onDeleteClick = { tireSetToDelete = tireSet }
                            )
                            HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                        }
                    }
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
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = width,
                        onValueChange = { if (it.all { char -> char.isDigit() }) width = it },
                        label = { Text(stringResource(R.string.car_tire_width_label)) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                    )
                    Text("/", modifier = Modifier.padding(horizontal = 4.dp), style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(
                        value = ratio,
                        onValueChange = { if (it.all { char -> char.isDigit() }) ratio = it },
                        label = { Text(stringResource(R.string.car_tire_ratio_label)) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                    )
                    Text("R", modifier = Modifier.padding(horizontal = 4.dp), style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(
                        value = diameter,
                        onValueChange = { if (it.all { char -> char.isDigit() }) diameter = it },
                        label = { Text(stringResource(R.string.car_tire_diam_label)) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val dotWeekInt = dotWeek.toIntOrNull()
                    val isWeekInvalid = dotWeek.isNotBlank() && (dotWeekInt == null || dotWeekInt !in 1..53)
                    
                    OutlinedTextField(
                        value = dotWeek,
                        onValueChange = { if (it.length <= 2 && it.all { char -> char.isDigit() }) dotWeek = it },
                        label = { Text(stringResource(R.string.tire_dot_week_label)) },
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
                        label = { Text(stringResource(R.string.tire_dot_year_label)) },
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


package com.dariusepure.caractivitylog.ui.cars

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dariusepure.caractivitylog.R
import com.dariusepure.caractivitylog.domain.Car
import com.dariusepure.caractivitylog.domain.displayName
import com.dariusepure.caractivitylog.ui.common.*
import com.dariusepure.caractivitylog.ui.theme.SettingsViewModel
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType

@Composable
fun CarCard(
    car: Car,
    onClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AutoSizeText(
                        text = car.displayName,
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                }
                Spacer(Modifier.height(4.dp))
                
                val summary = CarFormatters.getCarSummary(context, car)
                if (summary.isNotEmpty()) {
                    Text(
                        text = summary,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            
            ActionButtons(
                onEdit = onEditClick,
                onDelete = onDeleteClick
            )
        }
    }
}

@Composable
fun CarListScreen(
    onCarClick: (String) -> Unit,
    onAddCarClick: () -> Unit,
    onEditCarClick: (String) -> Unit,
    onSettingsClick: () -> Unit,
    viewModel: CarListViewModel = hiltViewModel(),
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val sortOrder by viewModel.sortOrder.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val haptic = LocalHapticFeedback.current

    val onDeleteCarLambda = remember(viewModel, haptic) {
        { carId: String ->
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            viewModel.onDeleteCar(carId)
        }
    }
    
    val onSortOrderChangeLambda = remember(viewModel) {
        { order: CarSortOrder -> viewModel.onSortOrderChanged(order) }
    }
    
    val onSearchQueryChangeLambda = remember(viewModel) {
        { query: String -> viewModel.onSearchQueryChanged(query) }
    }

    InnerCarListScreen(
        onCarClick = onCarClick,
        onAddCarClick = onAddCarClick,
        onEditCarClick = onEditCarClick,
        onDeleteCar = onDeleteCarLambda,
        onSortOrderChange = onSortOrderChangeLambda,
        onSearchQueryChange = onSearchQueryChangeLambda,
        onSettingsClick = onSettingsClick,
        searchQuery = searchQuery,
        currentSortOrder = sortOrder,
        state = state
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InnerCarListScreen(
    onCarClick: (String) -> Unit,
    onAddCarClick: () -> Unit,
    onEditCarClick: (String) -> Unit,
    onDeleteCar: (String) -> Unit,
    onSortOrderChange: (CarSortOrder) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onSettingsClick: () -> Unit,
    searchQuery: String,
    currentSortOrder: CarSortOrder,
    state: CarListUiState,
    modifier: Modifier = Modifier,
) {
    var sortMenuExpanded by remember { mutableStateOf(false) }
    var carToDelete by remember { mutableStateOf<String?>(null) }

    if (carToDelete != null) {
        DeleteConfirmationDialog(
            onConfirm = {
                onDeleteCar(carToDelete!!)
                carToDelete = null
            },
            onDismiss = { carToDelete = null }
        )
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { 
                    AutoSizeText(
                        text = stringResource(R.string.car_list_title),
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                actions = {
                    Box {
                        val sortTooltipState = rememberTooltipState()
                        TooltipBox(
                            positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                            tooltip = { PlainTooltip { Text(stringResource(R.string.common_sort)) } },
                            state = sortTooltipState
                        ) {
                            IconButton(onClick = { sortMenuExpanded = true }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Sort,
                                    contentDescription = stringResource(R.string.common_sort)
                                )
                            }
                        }
                        DropdownMenu(
                            expanded = sortMenuExpanded,
                            onDismissRequest = { sortMenuExpanded = false }
                        ) {
                            CarSortOrder.entries.forEach { order ->
                                DropdownMenuItem(
                                    text = { 
                                        Text(
                                            text = stringResource(order.labelRes),
                                            fontWeight = if (order == currentSortOrder) FontWeight.Bold else FontWeight.Normal
                                        ) 
                                    },
                                    onClick = {
                                        onSortOrderChange(order)
                                        sortMenuExpanded = false
                                    },
                                    leadingIcon = {
                                        if (order == currentSortOrder) {
                                            Icon(Icons.Default.CheckCircle, null, Modifier.size(18.dp))
                                        }
                                    }
                                )
                            }
                        }
                    }

                    val settingsTooltipState = rememberTooltipState()
                    TooltipBox(
                        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                        tooltip = { PlainTooltip { Text(stringResource(R.string.common_settings)) } },
                        state = settingsTooltipState
                    ) {
                        FilledIconButton(
                            onClick = onSettingsClick,
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = Color(0xFF1C1B1F),
                                contentColor = Color.White
                            ),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Default.Settings, null, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            val tooltipState = rememberTooltipState()
            TooltipBox(
                positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                tooltip = {
                    PlainTooltip {
                        Text(stringResource(R.string.car_add_button))
                    }
                },
                state = tooltipState
            ) {
                FloatingActionButton(
                    onClick = onAddCarClick,
                    containerColor = Color(0xFF1A73E8),
                    contentColor = Color.White
                ) {
                    Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.car_add_button))
                }
            }
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Search Bar
            Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                TextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(stringResource(R.string.car_list_search_placeholder)) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchQueryChange("") }) {
                                Icon(Icons.Default.Clear, contentDescription = null)
                            }
                        }
                    },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent
                    )
                )
            }

            Box(modifier = Modifier.weight(1f)) {
                when (state) {
                    CarListUiState.Loading -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(5) { CarCardSkeleton() }
                            item { Spacer(Modifier.height(80.dp)) }
                        }
                    }
                    CarListUiState.Empty -> EmptyState(
                        title = if (searchQuery.isEmpty()) stringResource(R.string.car_list_empty_title) else stringResource(R.string.car_list_no_results_title),
                        subtitle = if (searchQuery.isEmpty()) stringResource(R.string.car_list_empty_subtitle) else stringResource(R.string.car_list_no_results_subtitle),
                        icon = Icons.Outlined.DirectionsCar,
                    )
                    is CarListUiState.Success -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            items(
                                items = state.cars,
                                key = { it.id },
                                contentType = { "car_card" }
                            ) { car ->
                                CarCard(
                                    car = car,
                                    onClick = { onCarClick(car.id) },
                                    onEditClick = { onEditCarClick(car.id) },
                                    onDeleteClick = { carToDelete = car.id }
                                )
                            }
                            item(contentType = { "spacer" }) { Spacer(Modifier.height(80.dp)) }
                        }
                    }
                    is CarListUiState.Error -> ErrorState(
                        message = state.message,
                        onRetry = { },
                    )
                }
            }
        }
    }
}

package com.dariusepure.caractivitylog.ui.cars

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.Image
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Sort
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.outlined.DirectionsCar
import com.dariusepure.caractivitylog.ui.common.CarFormatters
import com.dariusepure.caractivitylog.ui.theme.ThemeViewModel
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.Hyphens
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import com.dariusepure.caractivitylog.R
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.dariusepure.caractivitylog.domain.Car
import com.dariusepure.caractivitylog.ui.common.AutoSizeText
import com.dariusepure.caractivitylog.ui.common.CarCardSkeleton
import com.dariusepure.caractivitylog.ui.common.EmptyState
import com.dariusepure.caractivitylog.ui.common.ErrorState
import com.dariusepure.caractivitylog.ui.common.LoadingState
import com.dariusepure.caractivitylog.ui.common.DeleteConfirmationDialog
import com.dariusepure.caractivitylog.ui.common.LanguageSelector
import com.dariusepure.caractivitylog.ui.common.toRelativeString
import com.dariusepure.caractivitylog.util.LocalImageHelper
import com.dariusepure.caractivitylog.domain.displayName

@OptIn(ExperimentalMaterial3Api::class)
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
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                val localImage = remember(car.id) { LocalImageHelper.getCarImageFile(context, car.id) }
                if (localImage != null) {
                    AsyncImage(
                        model = localImage,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Outlined.DirectionsCar,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AutoSizeText(
                        text = car.displayName,
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    
                    Spacer(Modifier.width(8.dp))
                    
                    if (!car.isSynced) {
                        Icon(
                            imageVector = Icons.Default.Sync,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(16.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF4CAF50), // Green
                            modifier = Modifier.size(14.dp)
                        )
                    }
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
                
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.car_last_update, car.updatedAt.toRelativeString(context)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            
            val editTooltipState = rememberTooltipState()
            TooltipBox(
                positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                tooltip = {
                    PlainTooltip {
                        Text(stringResource(R.string.common_edit))
                    }
                },
                state = editTooltipState
            ) {
                IconButton(onClick = onEditClick) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = stringResource(R.string.car_edit_content_description),
                        tint = Color(0xFF2196F3) // Force Blue for Edit
                    )
                }
            }
            
            val deleteTooltipState = rememberTooltipState()
            TooltipBox(
                positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                tooltip = {
                    PlainTooltip {
                        Text(stringResource(R.string.common_delete))
                    }
                },
                state = deleteTooltipState
            ) {
                IconButton(onClick = onDeleteClick) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = stringResource(R.string.car_delete_content_description),
                        tint = Color.Red
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CarGridCard(
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
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                val localImage = remember(car.id) { LocalImageHelper.getCarImageFile(context, car.id) }
                if (localImage != null) {
                    AsyncImage(
                        model = localImage,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Outlined.DirectionsCar,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(Modifier.height(12.dp))

            AutoSizeText(
                text = car.displayName,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center
            )
            
            if (car.licensePlate.isNotBlank()) {
                Text(
                    text = car.licensePlate,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                val editTooltipState = rememberTooltipState()
                TooltipBox(
                    positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                    tooltip = {
                        PlainTooltip {
                            Text(stringResource(R.string.common_edit))
                        }
                    },
                    state = editTooltipState
                ) {
                    IconButton(onClick = onEditClick, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = stringResource(R.string.car_edit_content_description),
                            tint = Color(0xFF2196F3),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                
                val deleteTooltipState = rememberTooltipState()
                TooltipBox(
                    positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                    tooltip = {
                        PlainTooltip {
                            Text(stringResource(R.string.common_delete))
                        }
                    },
                    state = deleteTooltipState
                ) {
                    IconButton(onClick = onDeleteClick, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = stringResource(R.string.car_delete_content_description),
                            tint = Color.Red,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CarListScreen(
    onCarClick: (String) -> Unit,
    onAddCarClick: () -> Unit,
    onEditCarClick: (String) -> Unit,
    onRecycleBinClick: () -> Unit,
    onLogout: () -> Unit,
    viewModel: CarListViewModel = hiltViewModel(),
    themeViewModel: ThemeViewModel = hiltViewModel(),
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val sortOrder by viewModel.sortOrder.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val viewMode by viewModel.viewMode.collectAsStateWithLifecycle()
    val isDarkMode by themeViewModel.isDarkMode.collectAsStateWithLifecycle()
    val systemDark = androidx.compose.foundation.isSystemInDarkTheme()
    val currentDark = isDarkMode ?: systemDark

    InnerCarListScreen(
        onCarClick = onCarClick,
        onAddCarClick = onAddCarClick,
        onEditCarClick = onEditCarClick,
        onRecycleBinClick = onRecycleBinClick,
        onDeleteCar = { carId -> viewModel.onDeleteCar(carId) },
        onLogoutClick = {
            viewModel.signOut()
            onLogout()
        },
        onThemeToggle = { themeViewModel.toggleTheme(currentDark) },
        onSortOrderChange = { viewModel.onSortOrderChanged(it) },
        onSearchQueryChange = { viewModel.onSearchQueryChanged(it) },
        onViewModeChange = { viewModel.onViewModeChanged(it) },
        searchQuery = searchQuery,
        currentSortOrder = sortOrder,
        currentViewMode = viewMode,
        isDark = currentDark,
        state = state
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InnerCarListScreen(
    onCarClick: (String) -> Unit,
    onAddCarClick: () -> Unit,
    onEditCarClick: (String) -> Unit,
    onRecycleBinClick: () -> Unit,
    onDeleteCar: (String) -> Unit,
    onLogoutClick: () -> Unit,
    onThemeToggle: () -> Unit,
    onSortOrderChange: (CarSortOrder) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onViewModeChange: (CarListViewMode) -> Unit,
    searchQuery: String,
    currentSortOrder: CarSortOrder,
    currentViewMode: CarListViewMode,
    isDark: Boolean,
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
            onDismiss = { carToDelete = null },
            title = stringResource(R.string.common_delete),
            message = stringResource(R.string.car_move_to_recycle_bin_msg),
            confirmText = stringResource(R.string.common_delete),
            isPermanent = false
        )
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.car_list_title)) },
                actions = {
                    val viewModeTooltipState = rememberTooltipState()
                    TooltipBox(
                        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                        tooltip = {
                            PlainTooltip {
                                Text(
                                    if (currentViewMode == CarListViewMode.LIST) stringResource(R.string.car_view_mode_grid)
                                    else stringResource(R.string.car_view_mode_list)
                                )
                            }
                        },
                        state = viewModeTooltipState
                    ) {
                        IconButton(
                            onClick = {
                                onViewModeChange(
                                    if (currentViewMode == CarListViewMode.LIST) CarListViewMode.GRID
                                    else CarListViewMode.LIST
                                )
                            }
                        ) {
                            Icon(
                                imageVector = if (currentViewMode == CarListViewMode.LIST) Icons.Default.GridView else Icons.AutoMirrored.Filled.List,
                                contentDescription = if (currentViewMode == CarListViewMode.LIST) stringResource(R.string.car_view_mode_grid) else stringResource(R.string.car_view_mode_list)
                            )
                        }
                    }
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
                    val binTooltipState = rememberTooltipState()
                    TooltipBox(
                        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                        tooltip = { PlainTooltip { Text(stringResource(R.string.recycle_bin_title)) } },
                        state = binTooltipState
                    ) {
                        IconButton(onClick = onRecycleBinClick) {
                            Icon(
                                imageVector = Icons.Default.DeleteForever,
                                contentDescription = stringResource(R.string.recycle_bin_title)
                            )
                        }
                    }

                    val themeTooltipState = rememberTooltipState()
                    TooltipBox(
                        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                        tooltip = { PlainTooltip { Text(stringResource(if (isDark) R.string.theme_light_mode else R.string.theme_dark_mode)) } },
                        state = themeTooltipState
                    ) {
                        IconButton(onClick = onThemeToggle) {
                            Icon(
                                imageVector = if (isDark) Icons.Default.LightMode else Icons.Default.DarkMode,
                                contentDescription = null
                            )
                        }
                    }

                    val logoutTooltipState = rememberTooltipState()
                    TooltipBox(
                        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                        tooltip = { PlainTooltip { Text(stringResource(R.string.auth_logout)) } },
                        state = logoutTooltipState
                    ) {
                        IconButton(onClick = onLogoutClick) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Logout,
                                contentDescription = null
                            )
                        }
                    }
                    LanguageSelector()
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
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
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
                    CarListUiState.Loading -> if (currentViewMode == CarListViewMode.LIST) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(5) { CarCardSkeleton() }
                            item { Spacer(Modifier.height(80.dp)) }
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(6) { CarCardSkeleton() }
                        }
                    }
                    CarListUiState.Empty -> EmptyState(
                        title = if (searchQuery.isEmpty()) stringResource(R.string.car_list_empty_title) else stringResource(R.string.car_list_no_results_title),
                        subtitle = if (searchQuery.isEmpty()) stringResource(R.string.car_list_empty_subtitle) else stringResource(R.string.car_list_no_results_subtitle),
                        icon = Icons.Outlined.DirectionsCar,
                    )
                    is CarListUiState.Success -> if (currentViewMode == CarListViewMode.LIST) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            items(state.cars, key = { it.id }) { car ->
                                CarCard(
                                    car = car,
                                    onClick = { onCarClick(car.id) },
                                    onEditClick = { onEditCarClick(car.id) },
                                    onDeleteClick = { carToDelete = car.id }
                                )
                            }
                            item { Spacer(Modifier.height(80.dp)) }
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(state.cars, key = { it.id }) { car ->
                                CarGridCard(
                                    car = car,
                                    onClick = { onCarClick(car.id) },
                                    onEditClick = { onEditCarClick(car.id) },
                                    onDeleteClick = { carToDelete = car.id }
                                )
                            }
                            item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) { 
                                Spacer(Modifier.height(80.dp)) 
                            }
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

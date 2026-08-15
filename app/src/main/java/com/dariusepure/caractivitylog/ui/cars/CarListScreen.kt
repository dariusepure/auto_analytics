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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.outlined.DirectionsCar
import com.dariusepure.caractivitylog.ui.common.CarFormatters
import com.dariusepure.caractivitylog.ui.theme.SettingsViewModel
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import com.dariusepure.caractivitylog.ui.common.LanguageDialog
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.dariusepure.caractivitylog.ui.common.supportedLanguages
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import com.dariusepure.caractivitylog.R
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dariusepure.caractivitylog.domain.Car
import com.dariusepure.caractivitylog.ui.common.ActionButtons
import com.dariusepure.caractivitylog.ui.common.AutoSizeText
import com.dariusepure.caractivitylog.ui.common.CarCardSkeleton
import com.dariusepure.caractivitylog.ui.common.EmptyState
import com.dariusepure.caractivitylog.ui.common.ErrorState
import com.dariusepure.caractivitylog.ui.common.LoadingState
import com.dariusepure.caractivitylog.ui.common.DeleteConfirmationDialog
import com.dariusepure.caractivitylog.ui.common.LanguageSelector
import com.dariusepure.caractivitylog.ui.common.toRelativeString
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
    onLogout: () -> Unit,
    viewModel: CarListViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel(),
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val sortOrder by viewModel.sortOrder.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val isDarkMode by settingsViewModel.isDarkMode.collectAsStateWithLifecycle()
    val unitSystem by settingsViewModel.unitSystem.collectAsStateWithLifecycle()
    val systemDark = androidx.compose.foundation.isSystemInDarkTheme()
    val currentDark = isDarkMode ?: systemDark
    val haptic = LocalHapticFeedback.current

    val onDeleteCarLambda = remember(viewModel, haptic) {
        { carId: String ->
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            viewModel.onDeleteCar(carId)
        }
    }
    
    val onLogoutClickLambda = remember(viewModel, onLogout) {
        {
            viewModel.signOut()
            onLogout()
        }
    }
    
    val onThemeToggleLambda = remember(settingsViewModel, currentDark) {
        { settingsViewModel.toggleTheme(currentDark) }
    }
    
    val onUnitSystemChangeLambda = remember(settingsViewModel) {
        { system: com.dariusepure.caractivitylog.domain.UnitSystem -> settingsViewModel.setUnitSystem(system) }
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
        onLogoutClick = onLogoutClickLambda,
        onThemeToggle = onThemeToggleLambda,
        onUnitSystemChange = onUnitSystemChangeLambda,
        onSortOrderChange = onSortOrderChangeLambda,
        onSearchQueryChange = onSearchQueryChangeLambda,
        searchQuery = searchQuery,
        currentSortOrder = sortOrder,
        currentUnitSystem = unitSystem,
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
    onDeleteCar: (String) -> Unit,
    onLogoutClick: () -> Unit,
    onThemeToggle: () -> Unit,
    onUnitSystemChange: (com.dariusepure.caractivitylog.domain.UnitSystem) -> Unit,
    onSortOrderChange: (CarSortOrder) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    searchQuery: String,
    currentSortOrder: CarSortOrder,
    currentUnitSystem: com.dariusepure.caractivitylog.domain.UnitSystem,
    isDark: Boolean,
    state: CarListUiState,
    modifier: Modifier = Modifier,
) {
    var sortMenuExpanded by remember { mutableStateOf(false) }
    var showSettingsSheet by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var carToDelete by remember { mutableStateOf<String?>(null) }

    val sheetState = rememberModalBottomSheetState()

    if (carToDelete != null) {
        DeleteConfirmationDialog(
            onConfirm = {
                onDeleteCar(carToDelete!!)
                carToDelete = null
            },
            onDismiss = { carToDelete = null }
        )
    }

    if (showLanguageDialog) {
        LanguageDialog(onDismiss = { showLanguageDialog = false })
    }

    if (showSettingsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSettingsSheet = false },
            sheetState = sheetState
        ) {
            SettingsSheetContent(
                isDark = isDark,
                currentUnitSystem = currentUnitSystem,
                onThemeToggle = onThemeToggle,
                onUnitSystemChange = onUnitSystemChange,
                onLogoutClick = onLogoutClick
            )
        }
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
                            onClick = { showSettingsSheet = true },
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

@Composable
private fun SettingsSheetContent(
    isDark: Boolean,
    currentUnitSystem: com.dariusepure.caractivitylog.domain.UnitSystem,
    onThemeToggle: () -> Unit,
    onUnitSystemChange: (com.dariusepure.caractivitylog.domain.UnitSystem) -> Unit,
    onLogoutClick: () -> Unit
) {
    var languageMenuExpanded by remember { mutableStateOf(false) }
    var unitMenuExpanded by remember { mutableStateOf(false) }

    val locales = AppCompatDelegate.getApplicationLocales()
    val currentLocale = if (!locales.isEmpty) locales.get(0)?.language ?: "en" else "en"
    val currentLanguage = supportedLanguages.find { it.code == currentLocale } ?: supportedLanguages[0]

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 32.dp, start = 16.dp, end = 16.dp)
    ) {
        Text(
            text = stringResource(R.string.common_settings),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(vertical = 16.dp)
        )

        ListItem(
            headlineContent = { Text(stringResource(if (isDark) R.string.theme_light_mode else R.string.theme_dark_mode)) },
            leadingContent = { Icon(if (isDark) Icons.Default.LightMode else Icons.Default.DarkMode, null) },
            trailingContent = {
                Switch(checked = isDark, onCheckedChange = { onThemeToggle() })
            },
            modifier = Modifier.clickable { onThemeToggle() }
        )

        ListItem(
            headlineContent = { Text(stringResource(R.string.common_language)) },
            leadingContent = { Icon(Icons.Outlined.Language, null) },
            trailingContent = {
                Box {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { languageMenuExpanded = true }
                    ) {
                        Text(
                            text = "${currentLanguage.flag} ${currentLanguage.name}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                    DropdownMenu(
                        expanded = languageMenuExpanded,
                        onDismissRequest = { languageMenuExpanded = false },
                        offset = androidx.compose.ui.unit.DpOffset(x = 0.dp, y = 4.dp)
                    ) {
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
            },
            modifier = Modifier.clickable { languageMenuExpanded = true }
        )

        ListItem(
            headlineContent = { Text(stringResource(R.string.unit_system_label)) },
            leadingContent = { Icon(Icons.Default.Sync, null) },
            trailingContent = {
                Box {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { unitMenuExpanded = true }
                    ) {
                        Text(
                            text = if (currentUnitSystem == com.dariusepure.caractivitylog.domain.UnitSystem.METRIC)
                                stringResource(R.string.unit_system_metric_label)
                            else stringResource(R.string.unit_system_imperial_label),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                    DropdownMenu(
                        expanded = unitMenuExpanded,
                        onDismissRequest = { unitMenuExpanded = false },
                        offset = androidx.compose.ui.unit.DpOffset(x = 0.dp, y = 4.dp)
                    ) {
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
                                    onUnitSystemChange(system)
                                    unitMenuExpanded = false
                                },
                                trailingIcon = {
                                    if (system == currentUnitSystem) {
                                        Icon(Icons.Default.CheckCircle, null, Modifier.size(18.dp))
                                    }
                                }
                            )
                        }
                    }
                }
            },
            modifier = Modifier.clickable { unitMenuExpanded = true }
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        ListItem(
            headlineContent = {
                Text(
                    text = stringResource(R.string.auth_logout),
                    color = MaterialTheme.colorScheme.error
                )
            },
            leadingContent = {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Logout,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            modifier = Modifier.clickable { onLogoutClick() }
        )
    }
}


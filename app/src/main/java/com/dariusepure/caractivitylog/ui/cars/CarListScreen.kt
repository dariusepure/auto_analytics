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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.dariusepure.caractivitylog.domain.Car
import com.dariusepure.caractivitylog.ui.common.EmptyState
import com.dariusepure.caractivitylog.ui.common.ErrorState
import com.dariusepure.caractivitylog.ui.common.LoadingState
import com.dariusepure.caractivitylog.ui.common.toRelativeString
import com.dariusepure.caractivitylog.domain.displayName

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
                    .size(56.dp) // Increased box size from 48dp
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                val logoRes = BrandHelper.getLogoResource(context, car.make)
                if (logoRes != 0) {
                    Image(
                        painter = painterResource(id = logoRes),
                        contentDescription = car.make,
                        modifier = Modifier.size(44.dp), // Increased logo size from 32dp
                        contentScale = ContentScale.Fit
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
                    Text(
                        text = car.displayName,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    
                    Spacer(Modifier.width(8.dp))
                    
                    if (!car.isSynced) {
                        Icon(
                            imageVector = Icons.Default.Sync,
                            contentDescription = "Pending Sync",
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(16.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Synced",
                            tint = Color(0xFF4CAF50), // Green
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                
                val summary = CarFormatters.getCarSummary(car)
                if (summary.isNotEmpty()) {
                    Text(
                        text = summary,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Last update: ${car.updatedAt.toRelativeString()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            
            IconButton(onClick = onEditClick) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit car",
                    tint = Color(0xFF2196F3) // Force Blue for Edit
                )
            }
            
            IconButton(onClick = onDeleteClick) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete car",
                    tint = Color.Red
                )
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
        currentSortOrder = sortOrder,
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
    currentSortOrder: CarSortOrder,
    isDark: Boolean,
    state: CarListUiState,
    modifier: Modifier = Modifier,
) {
    var sortMenuExpanded by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Your cars") },
                actions = {
                    Box {
                        IconButton(onClick = { sortMenuExpanded = true }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Sort,
                                contentDescription = "Sort cars"
                            )
                        }
                        DropdownMenu(
                            expanded = sortMenuExpanded,
                            onDismissRequest = { sortMenuExpanded = false }
                        ) {
                            CarSortOrder.entries.forEach { order ->
                                DropdownMenuItem(
                                    text = { 
                                        Text(
                                            text = order.label,
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
                    IconButton(onClick = onRecycleBinClick) {
                        Icon(
                            imageVector = Icons.Default.DeleteForever,
                            contentDescription = "Recycle Bin"
                        )
                    }
                    IconButton(onClick = onThemeToggle) {
                        Icon(
                            imageVector = if (isDark) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = "Toggle Theme"
                        )
                    }
                    IconButton(onClick = onLogoutClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Logout,
                            contentDescription = "Log out"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddCarClick,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Add car") },
            )
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (state) {
                CarListUiState.Loading -> LoadingState(label = "Loading your cars")
                CarListUiState.Empty -> EmptyState(
                    title = "No cars yet",
                    subtitle = "Tap Add car to get started.",
                    icon = Icons.Outlined.DirectionsCar,
                )
                is CarListUiState.Success -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(state.cars, key = { it.id }) { car ->
                        CarCard(
                            car = car,
                            onClick = { onCarClick(car.id) },
                            onEditClick = { onEditCarClick(car.id) },
                            onDeleteClick = { onDeleteCar(car.id) }
                        )
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

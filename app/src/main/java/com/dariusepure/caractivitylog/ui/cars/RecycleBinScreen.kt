package com.dariusepure.caractivitylog.ui.cars

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import com.dariusepure.caractivitylog.R
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dariusepure.caractivitylog.domain.Car
import com.dariusepure.caractivitylog.domain.displayName
import com.dariusepure.caractivitylog.ui.common.*
import com.dariusepure.caractivitylog.ui.cars.BrandHelper
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecycleBinScreen(
    onBack: () -> Unit,
    viewModel: RecycleBinViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.recycle_bin_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (state) {
                RecycleBinUiState.Loading -> LoadingState(label = stringResource(R.string.common_loading))
                RecycleBinUiState.Empty -> EmptyState(
                    title = stringResource(R.string.recycle_bin_empty_title),
                    subtitle = stringResource(R.string.recycle_bin_empty_subtitle),
                    icon = Icons.Outlined.DirectionsCar
                )
                is RecycleBinUiState.Success -> {
                    val cars = (state as RecycleBinUiState.Success).cars
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(cars, key = { it.id }) { car ->
                            DeletedCarCard(
                                car = car,
                                onRestore = { viewModel.onRestoreCar(car.id) },
                                onDeletePermanently = { viewModel.onPermanentlyDeleteCar(car.id) }
                            )
                        }
                    }
                }
                is RecycleBinUiState.Error -> ErrorState(
                    message = (state as RecycleBinUiState.Error).message,
                    onRetry = {}
                )
            }
        }
    }
}

@Composable
fun DeletedCarCard(
    car: Car,
    onRestore: () -> Unit,
    onDeletePermanently: () -> Unit
) {
    val context = LocalContext.current
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        DeleteConfirmationDialog(
            onConfirm = {
                onDeletePermanently()
                showDeleteDialog = false
            },
            onDismiss = { showDeleteDialog = false }
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
                val logoRes = BrandHelper.getLogoResource(context, car.make)
                if (logoRes != 0) {
                    Image(
                        painter = painterResource(id = logoRes),
                        contentDescription = car.make,
                        modifier = Modifier.size(44.dp),
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
                Text(
                    text = car.displayName,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                val deletedAtStr = car.deletedAt?.let {
                    val df = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
                    df.format(it)
                } ?: "Unknown"

                Text(
                    text = stringResource(R.string.recycle_bin_deleted_on, deletedAtStr),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(onClick = onRestore) {
                Icon(
                    imageVector = Icons.Default.Restore,
                    contentDescription = stringResource(R.string.recycle_bin_restore),
                    tint = Color(0xFF4CAF50)
                )
            }

            IconButton(onClick = { showDeleteDialog = true }) {
                Icon(
                    imageVector = Icons.Default.DeleteForever,
                    contentDescription = stringResource(R.string.recycle_bin_delete_forever),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

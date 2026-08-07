package com.dariusepure.caractivitylog.ui.cars

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.dariusepure.caractivitylog.R
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dariusepure.caractivitylog.ui.common.*
import com.dariusepure.caractivitylog.domain.VehicleInspection
import com.dariusepure.caractivitylog.domain.displayName
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InspectionHistoryScreen(
    carId: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CarDetailsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }
    var editingInspection by remember { mutableStateOf<VehicleInspection?>(null) }
    var inspectionToDelete by remember { mutableStateOf<VehicleInspection?>(null) }

    LaunchedEffect(carId) {
        viewModel.loadCarData(carId)
    }

    if (inspectionToDelete != null) {
        DeleteConfirmationDialog(
            onConfirm = {
                viewModel.deleteInspection(carId, inspectionToDelete!!.id)
                inspectionToDelete = null
            },
            onDismiss = { inspectionToDelete = null }
        )
    }

    if (showAddDialog || editingInspection != null) {
        val successState = state as? CarDetailsUiState.Success
        val car = successState?.car
        val existingLogs = successState?.mileageLogs ?: emptyList()
        val country = europeanCountries.find { it.code == car?.plateCountry }
        val unitLabel = if (country?.usesMiles == true) "mi" else "km"
        
        AddInspectionDialog(
            existingInspection = editingInspection,
            existingLogs = existingLogs,
            unit = unitLabel,
            accentColor = Color(0xFF2196F3),
            onAccentColor = Color.White,
            onDismiss = { 
                showAddDialog = false
                editingInspection = null
            },
            onConfirm = { inspection ->
                if (editingInspection != null) {
                    viewModel.updateInspection(carId, inspection.copy(id = editingInspection!!.id))
                } else {
                    viewModel.addInspection(carId, inspection)
                }
                showAddDialog = false
                editingInspection = null
            }
        )
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.inspection_history_title)) },
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
                containerColor = Color(0xFF2196F3),
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.inspection_add_title))
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

                    if (s.inspections.isEmpty()) {
                        item {
                            Text(
                                text = stringResource(R.string.inspection_empty),
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(vertical = 16.dp)
                            )
                        }
                    } else {
                        items(s.inspections) { inspection ->
                            val country = europeanCountries.find { it.code == car.plateCountry }
                            val unitLabel = if (country?.usesMiles == true) "mi" else "km"
                            val displayMileage = CarFormatters.fromCanonicalDistance(inspection.mileage, country?.usesMiles == true)
                            
                            InspectionItem(
                                inspection = inspection.copy(mileage = displayMileage),
                                unit = unitLabel,
                                onEditClick = { editingInspection = inspection },
                                onDeleteClick = { inspectionToDelete = inspection }
                            )
                            HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                        }
                    }
                }
            }
        }
    }
}

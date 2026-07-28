package com.dariusepure.caractivitylog.ui.cars

import android.graphics.ImageDecoder
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dariusepure.caractivitylog.domain.MileageLog
import com.dariusepure.caractivitylog.domain.ScannedMileageEntry
import com.dariusepure.caractivitylog.ui.common.MileageLineChart
import com.dariusepure.caractivitylog.ui.common.CarFormatters
import com.dariusepure.caractivitylog.ui.common.ErrorState
import com.dariusepure.caractivitylog.ui.common.LoadingState
import com.dariusepure.caractivitylog.ui.common.MileageItem
import com.dariusepure.caractivitylog.domain.displayName
import java.util.Date
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MileageHistoryScreen(
    carId: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CarDetailsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    
    var showAddMileageDialog by remember { mutableStateOf(false) }
    var editingMileageLog by remember { mutableStateOf<MileageLog?>(null) }
    var scannedEntries by remember { mutableStateOf<List<ScannedMileageEntry>>(emptyList()) }

    val photoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            val bitmap = if (Build.VERSION.SDK_INT < 28) {
                MediaStore.Images.Media.getBitmap(context.contentResolver, it)
            } else {
                val source = ImageDecoder.createSource(context.contentResolver, it)
                ImageDecoder.decodeBitmap(source)
            }
            viewModel.scanImage(bitmap)
        }
    }

    val pdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            viewModel.scanDocument(it, "application/pdf")
        }
    }

    LaunchedEffect(carId) {
        viewModel.loadCarData(carId)
    }

    LaunchedEffect(Unit) {
        viewModel.scannedMileageEvent.collect { entries ->
            scannedEntries = entries
        }
    }

    if (scannedEntries.isNotEmpty()) {
        ScannedMileageConfirmationDialog(
            entries = scannedEntries,
            onDismiss = { scannedEntries = emptyList() },
            onConfirm = { selectedEntries ->
                viewModel.addBatchMileage(carId, selectedEntries)
                scannedEntries = emptyList()
            }
        )
    }

    if (showAddMileageDialog || editingMileageLog != null) {
        val successState = state as? CarDetailsUiState.Success
        val existingLogs = successState?.mileageLogs ?: emptyList()
        val car = successState?.car
        val country = europeanCountries.find { it.code == car?.plateCountry }
        val unitLabel = if (country?.usesMiles == true) "mi" else "km"

        AddMileageDialog(
            existingLog = editingMileageLog,
            existingLogs = existingLogs,
            unit = unitLabel,
            onDismiss = { 
                showAddMileageDialog = false
                editingMileageLog = null
            },
            onConfirm = { value, date ->
                val usesMiles = country?.usesMiles == true
                val canonicalValue = CarFormatters.toCanonicalDistance(value, usesMiles)
                
                val logToEdit = editingMileageLog
                if (logToEdit != null) {
                    viewModel.updateMileage(carId, logToEdit.copy(km = canonicalValue, date = date))
                } else {
                    viewModel.addMileage(carId, canonicalValue, date)
                }
                showAddMileageDialog = false
                editingMileageLog = null
            }
        )
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Mileage History") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddMileageDialog = true },
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Mileage")
            }
        }
    ) { padding ->
        when (val s = state) {
            CarDetailsUiState.Loading -> LoadingState()
            is CarDetailsUiState.Error -> ErrorState(message = s.message, onRetry = { viewModel.loadCarData(carId) })
            is CarDetailsUiState.Success -> {
                val car = s.car
                val country = europeanCountries.find { it.code == car.plateCountry }
                val unitLabel = if (country?.usesMiles == true) "mi" else "km"

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

                        if (s.mileageLogs.size >= 2) {
                            val chartData = s.mileageLogs.map { 
                                val displayKm = CarFormatters.fromCanonicalDistance(it.km, country?.usesMiles == true)
                                it.date to displayKm 
                            }
                            MileageLineChart(
                                data = chartData,
                                unit = unitLabel,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }

                        Spacer(Modifier.height(16.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { photoLauncher.launch("image/*") },
                                modifier = Modifier.weight(1f),
                                enabled = !s.isScanning,
                                contentPadding = PaddingValues(horizontal = 8.dp)
                            ) {
                                if (s.isScanning) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                } else {
                                    Icon(Icons.Default.AutoAwesome, null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Scan Photo", textAlign = TextAlign.Center)
                                }
                            }

                            Button(
                                onClick = { pdfLauncher.launch("application/pdf") },
                                modifier = Modifier.weight(1f),
                                enabled = !s.isScanning,
                                contentPadding = PaddingValues(horizontal = 8.dp)
                            ) {
                                if (s.isScanning) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                } else {
                                    Icon(Icons.Default.AutoAwesome, null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Scan PDF", textAlign = TextAlign.Center)
                                }
                            }
                        }
                    }

                    if (s.mileageLogs.isEmpty()) {
                        item {
                            Text(
                                text = "No mileage records found.",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(vertical = 16.dp)
                            )
                        }
                    } else {
                        items(s.mileageLogs) { log ->
                            val displayValue = CarFormatters.fromCanonicalDistance(log.km, country?.usesMiles == true)
                            MileageItem(
                                log = log.copy(km = displayValue),
                                unit = unitLabel,
                                onEditClick = { editingMileageLog = log },
                                onDeleteClick = { viewModel.deleteMileage(carId, log.id) }
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
fun ScannedMileageConfirmationDialog(
    entries: List<ScannedMileageEntry>,
    onDismiss: () -> Unit,
    onConfirm: (List<ScannedMileageEntry>) -> Unit
) {
    var selectedEntries by remember { mutableStateOf(entries) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Confirm Scanned Records") },
        text = {
            Column {
                Text(
                    "We found ${entries.size} mileage records. Please confirm which ones to add.",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(entries) { entry ->
                        val isSelected = entry in selectedEntries
                        Surface(
                            onClick = {
                                selectedEntries = if (isSelected) {
                                    selectedEntries - entry
                                } else {
                                    selectedEntries + entry
                                }
                            },
                            shape = MaterialTheme.shapes.medium,
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${entry.km.roundToInt()} km",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = entry.date ?: "No date found",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                                if (isSelected) {
                                    Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(selectedEntries) },
                enabled = selectedEntries.isNotEmpty()
            ) {
                Text("Add Selected (${selectedEntries.size})")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

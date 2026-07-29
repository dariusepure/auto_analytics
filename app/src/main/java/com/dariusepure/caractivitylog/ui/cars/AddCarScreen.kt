package com.dariusepure.caractivitylog.ui.cars

import androidx.compose.material3.ButtonDefaults
import androidx.compose.foundation.clickable
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.os.Build
import android.provider.MediaStore
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.TextButton
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.res.stringResource
import com.dariusepure.caractivitylog.R
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.HorizontalDivider
import androidx.compose.animation.AnimatedVisibility
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.ui.res.painterResource
import coil.compose.AsyncImage
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Checkbox
import androidx.compose.material3.AlertDialog
import androidx.compose.ui.text.font.FontWeight
import com.dariusepure.caractivitylog.domain.ScannedCarData
import com.dariusepure.caractivitylog.ui.common.CarFormatters
import com.dariusepure.caractivitylog.ui.common.CarTranslations
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCarScreen(
    carId: String? = null,
    onCarSaved: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AddCarViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val fuelTypes = listOf("Petrol", "Diesel", "Electric", "Hybrid", "LPG")
    val engineLayouts = listOf("Transverse", "Longitudinal")
    val cylinderLayouts = listOf("Inline", "V", "W", "Boxer")
    val aspirationOptions = listOf("Naturally Aspirated", "Turbocharged", "Supercharged", "Twin-Turbo", "Quad-Turbo", "Electric")
    val emissionStandards = listOf("Non-Euro", "Euro 1", "Euro 2", "Euro 3", "Euro 4", "Euro 5", "Euro 6")
    val gearboxTypes = listOf("Manual", "Automatic", "CVT", "DCT", "AMT")
    val brakeOptions = listOf("Ventilated Discs", "Solid Discs", "Drums", "Ceramic Discs")
    val frontSuspensionOptions = listOf("MacPherson", "Double Wishbone", "Multi-link")
    val rearSuspensionOptions = listOf("Torsion Beam", "Multi-link", "Solid Axle")
    val drivetrainOptions = listOf("FWD", "RWD", "AWD", "4WD")
    val vehicleTypes = listOf("Saloon", "Estate", "Hatchback", "MPV", "SUV", "Coupe", "Convertible", "Van", "Pickup")

    var name by remember { mutableStateOf("") } // Car Title / Nickname
    var licensePlate by remember { mutableStateOf("") }
    var selectedCountry by remember { mutableStateOf<Country?>(null) }
    var make by remember { mutableStateOf("") }
    var model by remember { mutableStateOf("") }
    var vin by remember { mutableStateOf("") }
    var year by remember { mutableStateOf("") }
    var engineSize by remember { mutableStateOf("") }
    var fuelType by remember { mutableStateOf("") }
    var color by remember { mutableStateOf("") }
    var power by remember { mutableStateOf("") }
    var powerUnit by remember { mutableStateOf("hp") }
    var torque by remember { mutableStateOf("") }
    var engineCode by remember { mutableStateOf("") }
    var engineLayout by remember { mutableStateOf("") }
    var cylinderLayout by remember { mutableStateOf("") }
    var emissionStandard by remember { mutableStateOf("") }
    var topSpeed by remember { mutableStateOf("") }
    var aspiration by remember { mutableStateOf("") }
    var numberOfCylinders by remember { mutableStateOf("") }
    var valvesPerCylinder by remember { mutableStateOf("") }

    var length by remember { mutableStateOf("") }
    var width by remember { mutableStateOf("") }
    var height by remember { mutableStateOf("") }
    var wheelbase by remember { mutableStateOf("") }
    var trackWidth by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }
    var numberOfSeats by remember { mutableStateOf("") }
    var numberOfDoors by remember { mutableStateOf("") }
    var bootSpace by remember { mutableStateOf("") }
    var tireWidth by remember { mutableStateOf("") }
    var tireAspectRatio by remember { mutableStateOf("") }
    var tireDiameter by remember { mutableStateOf("") }
    var fuelTankCapacity by remember { mutableStateOf("") }
    var batteryCapacity by remember { mutableStateOf("") }
    var drivetrain by remember { mutableStateOf("") }
    var gearboxType by remember { mutableStateOf("") }
    var gears by remember { mutableStateOf("") }
    var fuelSystem by remember { mutableStateOf("") }
    var frontSuspension by remember { mutableStateOf("") }
    var rearSuspension by remember { mutableStateOf("") }
    var frontBrakes by remember { mutableStateOf("") }
    var rearBrakes by remember { mutableStateOf("") }
    var vehicleType by remember { mutableStateOf("") }
    var manufacturingCountry by remember { mutableStateOf("") }

    val photoPicker = rememberLauncherForActivityResult(
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

    val pdfPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            viewModel.scanDocument(it, "application/pdf")
        }
    }

    var pendingScannedData by remember { mutableStateOf<ScannedCarData?>(null) }

    LaunchedEffect(Unit) {
        viewModel.scannedDataEvent.collect { data ->
            pendingScannedData = data
        }
    }

    if (pendingScannedData != null) {
        ScannedCarDataConfirmationDialog(
            data = pendingScannedData!!,
            onDismiss = { pendingScannedData = null },
            onConfirm = { selectedData ->
                selectedData.make?.let { make = it }
                selectedData.model?.let { model = it }
                selectedData.vin?.let { vin = it.uppercase() }
                selectedData.year?.let { year = it.roundToInt().toString() }
                selectedData.fuelType?.let { if (it in fuelTypes) fuelType = it }
                selectedData.engineSize?.let { engineSize = it.roundToInt().toString() }
                selectedData.power?.let { power = it.roundToInt().toString() }
                selectedData.powerUnit?.let { powerUnit = it }
                selectedData.torque?.let { torque = it.roundToInt().toString() }
                selectedData.color?.let { color = it }
                selectedData.registrationPlate?.let { licensePlate = it.uppercase() }
                selectedData.numberOfSeats?.let { numberOfSeats = it.roundToInt().toString() }
                selectedData.numberOfDoors?.let { numberOfDoors = it.roundToInt().toString() }
                selectedData.weight?.let { weight = it.roundToInt().toString() }
                selectedData.engineCode?.let { engineCode = it }
                selectedData.emissionStandard?.let {
                    if (it in emissionStandards) emissionStandard = it
                    else if (it.contains("Euro", ignoreCase = true)) {
                        val standard = emissionStandards.find { s -> it.contains(s.takeLast(1)) }
                        if (standard != null) emissionStandard = standard
                    }
                }
                selectedData.gearboxType?.let { if (it in gearboxTypes) gearboxType = it }
                selectedData.drivetrain?.let { if (it in drivetrainOptions) drivetrain = it }
                selectedData.engineLayout?.let { if (it in engineLayouts) engineLayout = it }
                selectedData.cylinderLayout?.let { if (it in cylinderLayouts) cylinderLayout = it }
                selectedData.fuelTankCapacity?.let { fuelTankCapacity = it.toString() }
                selectedData.topSpeed?.let { topSpeed = it.roundToInt().toString() }
                pendingScannedData = null
            }
        )
    }

    var identityExpanded by remember { mutableStateOf(true) }
    var registrationExpanded by remember { mutableStateOf(false) }
    var engineExpanded by remember { mutableStateOf(false) }
    var dimensionsExpanded by remember { mutableStateOf(false) }

    var countryExpanded by remember { mutableStateOf(false) }
    var manufacturingCountryExpanded by remember { mutableStateOf(false) }
    var makeExpanded by remember { mutableStateOf(false) }
    var fuelTypeExpanded by remember { mutableStateOf(false) }
    var engineLayoutExpanded by remember { mutableStateOf(false) }
    var emissionStandardExpanded by remember { mutableStateOf(false) }
    var aspirationExpanded by remember { mutableStateOf(false) }
    var cylinderLayoutExpanded by remember { mutableStateOf(false) }
    var drivetrainExpanded by remember { mutableStateOf(false) }
    var gearboxTypeExpanded by remember { mutableStateOf(false) }
    var frontBrakesExpanded by remember { mutableStateOf(false) }
    var rearBrakesExpanded by remember { mutableStateOf(false) }
    var frontSuspensionExpanded by remember { mutableStateOf(false) }
    var rearSuspensionExpanded by remember { mutableStateOf(false) }
    var vehicleTypeExpanded by remember { mutableStateOf(false) }

    var powerUnitExpanded by remember { mutableStateOf(false) }
    val powerUnits = listOf("hp", "kw")

    val handleBack = {
        if (carId != null && (name.isNotBlank() || (make.isNotBlank() && model.isNotBlank())) && (vin.isEmpty() || vin.length == 17)) {
            viewModel.onAddOrUpdateCar(
                name = name,
                licensePlate = licensePlate,
                plateCountry = selectedCountry?.code ?: "",
                make = make,
                model = model,
                vin = vin,
                year = year,
                engineSize = engineSize,
                fuelType = fuelType,
                fuelSystem = fuelSystem,
                color = color,
                power = power,
                powerUnit = powerUnit,
                torque = torque,
                engineCode = engineCode,
                engineLayout = engineLayout,
                cylinderLayout = cylinderLayout,
                emissionStandard = emissionStandard,
                length = length,
                width = width,
                height = height,
                wheelbase = wheelbase,
                trackWidth = trackWidth,
                fuelTankCapacity = fuelTankCapacity,
                batteryCapacity = batteryCapacity,
                drivetrain = drivetrain,
                gearboxType = gearboxType,
                gears = gears,
                frontSuspension = frontSuspension,
                rearSuspension = rearSuspension,
                aspiration = aspiration,
                frontBrakes = frontBrakes,
                rearBrakes = rearBrakes,
                vehicleType = vehicleType,
                manufacturingCountry = manufacturingCountry,
                topSpeed = topSpeed,
                weight = weight,
                numberOfSeats = numberOfSeats,
                numberOfCylinders = numberOfCylinders,
                valvesPerCylinder = valvesPerCylinder,
                numberOfDoors = numberOfDoors,
                bootSpace = bootSpace,
                tireWidth = tireWidth,
                tireAspectRatio = tireAspectRatio,
                tireDiameter = tireDiameter
            )
        } else {
            onBack()
        }
    }

    BackHandler(onBack = handleBack)

    LaunchedEffect(carId) {
        if (carId != null) {
            viewModel.loadCar(carId)
            val car = viewModel.getCarData(carId)
            if (car != null) {
                name = car.name
                licensePlate = car.licensePlate
                selectedCountry = europeanCountries.find { it.code == car.plateCountry }
                
                make = car.make
                model = car.model
                vin = car.vin
                year = car.year.takeIf { it != 0 }?.toString() ?: ""
                engineSize = car.engineSize
                fuelType = car.fuelType
                fuelSystem = car.fuelSystem
                color = car.color

                power = car.power.takeIf { it != 0 }?.toString() ?: ""
                powerUnit = car.powerUnit.ifBlank { "hp" }
                torque = car.torque.takeIf { it != 0 }?.toString() ?: ""
                engineCode = car.engineCode
                engineLayout = car.engineLayout
                cylinderLayout = car.cylinderLayout
                emissionStandard = car.emissionStandard
                aspiration = car.aspiration
                
                val displayTopSpeed = CarFormatters.fromCanonicalSpeed(car.topSpeed, selectedCountry?.usesMiles == true)
                topSpeed = displayTopSpeed.takeIf { it != 0.0 }?.roundToInt()?.toString() ?: ""
                
                numberOfCylinders = car.numberOfCylinders.takeIf { it != 0 }?.toString() ?: ""
                valvesPerCylinder = car.valvesPerCylinder.takeIf { it != 0 }?.toString() ?: ""
                
                length = car.length.takeIf { it != 0 }?.toString() ?: ""
                width = car.width.takeIf { it != 0 }?.toString() ?: ""
                height = car.height.takeIf { it != 0 }?.toString() ?: ""
                wheelbase = car.wheelbase.takeIf { it != 0 }?.toString() ?: ""
                trackWidth = car.trackWidth.takeIf { it != 0 }?.toString() ?: ""
                weight = car.weight.takeIf { it != 0 }?.toString() ?: ""
                numberOfSeats = car.numberOfSeats.takeIf { it != 0 }?.toString() ?: ""
                numberOfDoors = car.numberOfDoors.takeIf { it != 0 }?.toString() ?: ""
                bootSpace = car.bootSpace.takeIf { it != 0 }?.toString() ?: ""
                tireWidth = car.tireWidth.takeIf { it != 0 }?.toString() ?: ""
                tireAspectRatio = car.tireAspectRatio.takeIf { it != 0 }?.toString() ?: ""
                tireDiameter = car.tireDiameter.takeIf { it != 0 }?.toString() ?: ""
                
                fuelTankCapacity = car.fuelTankCapacity.takeIf { it != 0.0 }?.toString() ?: ""
                batteryCapacity = car.batteryCapacity.takeIf { it != 0.0 }?.toString() ?: ""
                drivetrain = car.drivetrain
                gearboxType = car.gearboxType
                gears = car.gears
                frontSuspension = car.frontSuspension
                rearSuspension = car.rearSuspension
                frontBrakes = car.frontBrakes
                rearBrakes = car.rearBrakes
                vehicleType = car.vehicleType
                manufacturingCountry = car.manufacturingCountry
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.navigationEvent.collect {
            onCarSaved()
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(if (carId == null) stringResource(R.string.car_add_title) else stringResource(R.string.car_edit_title)) },
                navigationIcon = {
                    IconButton(onClick = handleBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            (state as? AddCarState.Error)?.let { error ->
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Text(
                        text = error.message,
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            // --- IDENTITY SECTION ---
            CollapsibleSection(
                title = stringResource(R.string.car_identity_section),
                isExpanded = identityExpanded,
                onToggle = { identityExpanded = !identityExpanded }
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.car_title_label)) },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    singleLine = true,
                    placeholder = { 
                        val fallback = "$make $model".trim()
                        Text(if (fallback.isNotBlank()) stringResource(R.string.car_title_placeholder, fallback) else stringResource(R.string.car_title_placeholder_fallback)) 
                    },
                    enabled = state !is AddCarState.Pending
                )

                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { photoPicker.launch("image/*") },
                        modifier = Modifier.weight(1f),
                        enabled = state !is AddCarState.Pending && state !is AddCarState.Scanning,
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        if (state is AddCarState.Scanning) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Icon(Icons.Default.AutoAwesome, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(stringResource(R.string.car_scan_photo), textAlign = TextAlign.Center)
                        }
                    }

                    Button(
                        onClick = { pdfPicker.launch("application/pdf") },
                        modifier = Modifier.weight(1f),
                        enabled = state !is AddCarState.Pending && state !is AddCarState.Scanning,
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        if (state is AddCarState.Scanning) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Icon(Icons.Default.AutoAwesome, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(stringResource(R.string.car_scan_pdf), textAlign = TextAlign.Center)
                        }
                    }
                }

                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = make,
                        onValueChange = { make = it },
                        label = { Text(stringResource(R.string.car_make_label)) },
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = {
                            val logoRes = BrandHelper.getLogoResource(context, make)
                            if (logoRes != 0) {
                                Image(
                                    painter = painterResource(id = logoRes),
                                    contentDescription = null,
                                    modifier = Modifier.size(32.dp),
                                    contentScale = ContentScale.Fit
                                )
                            } else {
                                Icon(Icons.Outlined.DirectionsCar, null, modifier = Modifier.size(24.dp))
                            }
                        },
                        trailingIcon = {
                            Icon(
                                Icons.Default.ArrowDropDown,
                                "dropdown",
                                Modifier.clickable { makeExpanded = true })
                        }
                    )
                    DropdownMenu(
                        expanded = makeExpanded,
                        onDismissRequest = { makeExpanded = false },
                        modifier = Modifier.fillMaxWidth(0.9f).sizeIn(maxHeight = 300.dp)
                    ) {
                        carBrands.forEach { brand ->
                            if (brand != "Other") {
                                DropdownMenuItem(
                                    text = { Text(brand) },
                                    onClick = {
                                        make = brand
                                        makeExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = model,
                    onValueChange = { model = it },
                    label = { Text(stringResource(R.string.car_model_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = state !is AddCarState.Pending
                )

                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = year,
                    onValueChange = { if (it.all { char -> char.isDigit() }) year = it },
                    label = { Text(stringResource(R.string.car_year_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    enabled = state !is AddCarState.Pending
                )

                Spacer(Modifier.height(8.dp))

                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = getVehicleTypeLabel(context, vehicleType),
                        onValueChange = { },
                        readOnly = true,
                        label = { Text(stringResource(R.string.car_vehicle_type_label)) },
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            Icon(
                                Icons.Default.ArrowDropDown,
                                "dropdown",
                                Modifier.clickable { vehicleTypeExpanded = true })
                        }
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable { vehicleTypeExpanded = true }
                    )
                    DropdownMenu(
                        expanded = vehicleTypeExpanded,
                        onDismissRequest = { vehicleTypeExpanded = false },
                        modifier = Modifier.fillMaxWidth(0.9f).sizeIn(maxHeight = 300.dp)
                    ) {
                        vehicleTypes.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(getVehicleTypeLabel(context, type)) },
                                onClick = {
                                    vehicleType = type
                                    vehicleTypeExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = color,
                    onValueChange = { color = it.uppercase() },
                    label = { Text(stringResource(R.string.car_color_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = state !is AddCarState.Pending
                )

                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = vin,
                    onValueChange = { input ->
                        val filtered = input.uppercase().filter { it.isLetterOrDigit() && it !in listOf('I', 'O', 'Q') }
                        if (filtered.length <= 17) vin = filtered
                    },
                    label = { Text(stringResource(R.string.car_vin_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = state !is AddCarState.Pending,
                    supportingText = {
                        Column {
                            if (vin.isNotEmpty()) {
                                Text("${vin.length}/17")
                                if (vin.length < 17) {
                                    Text(stringResource(R.string.car_vin_remaining, 17 - vin.length), color = MaterialTheme.colorScheme.secondary)
                                }
                            }
                            Text(stringResource(R.string.car_vin_invalid_chars), style = MaterialTheme.typography.bodySmall)
                        }
                    },
                    isError = vin.isNotEmpty() && vin.length != 17
                )
            }

            Spacer(Modifier.height(16.dp))

            // --- REGISTRATION SECTION ---
            CollapsibleSection(
                title = stringResource(R.string.car_registration_section),
                isExpanded = registrationExpanded,
                onToggle = { registrationExpanded = !registrationExpanded }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Country Selection (Horizontal Layout)
                    Row(
                        modifier = Modifier
                            .clickable { countryExpanded = true }
                            .padding(vertical = 12.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = selectedCountry?.flag ?: "🌍",
                            style = MaterialTheme.typography.headlineSmall
                        )
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.car_country_label),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            Text(
                                text = selectedCountry?.code ?: stringResource(R.string.car_select_country),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                        Icon(Icons.Default.ArrowDropDown, null)

                        DropdownMenu(
                            expanded = countryExpanded,
                            onDismissRequest = { countryExpanded = false },
                            modifier = Modifier.sizeIn(maxHeight = 300.dp)
                        ) {
                            europeanCountries.forEach { country ->
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(country.flag)
                                            Spacer(Modifier.width(8.dp))
                                            Text(country.name)
                                        }
                                    },
                                    onClick = {
                                        val previousUsesMiles = selectedCountry?.usesMiles ?: false
                                        selectedCountry = country
                                        countryExpanded = false

                                        if (previousUsesMiles != country.usesMiles && topSpeed.isNotBlank()) {
                                            val currentSpeed = topSpeed.toDoubleOrNull() ?: 0.0
                                            val converted = if (country.usesMiles) {
                                                currentSpeed / 1.609344
                                            } else {
                                                currentSpeed * 1.609344
                                            }
                                            topSpeed = converted.roundToInt().toString()
                                        }
                                    }
                                )
                            }
                        }
                    }

                    Spacer(Modifier.width(16.dp))

                    OutlinedTextField(
                        value = licensePlate,
                        onValueChange = { licensePlate = it.uppercase() },
                        label = { Text(stringResource(R.string.car_license_plate_label)) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        enabled = state !is AddCarState.Pending,
                        supportingText = {
                            selectedCountry?.plateHint?.let { hint ->
                                Text(stringResource(R.string.car_license_plate_hint, hint), style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    )
                }

                Spacer(Modifier.height(8.dp))

                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = manufacturingCountry,
                        onValueChange = { },
                        readOnly = true,
                        label = { Text(stringResource(R.string.car_manufacturing_country_label)) },
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            Icon(Icons.Default.ArrowDropDown, null, Modifier.clickable { manufacturingCountryExpanded = true })
                        }
                    )
                    Box(modifier = Modifier.matchParentSize().clickable { manufacturingCountryExpanded = true })
                    
                    DropdownMenu(
                        expanded = manufacturingCountryExpanded,
                        onDismissRequest = { manufacturingCountryExpanded = false },
                        modifier = Modifier.sizeIn(maxHeight = 300.dp)
                    ) {
                        europeanCountries.forEach { country ->
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(country.flag)
                                        Spacer(Modifier.width(8.dp))
                                        Text(country.name)
                                    }
                                },
                                onClick = {
                                    manufacturingCountry = country.name
                                    manufacturingCountryExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // --- ENGINE SECTION ---
            CollapsibleSection(
                title = stringResource(R.string.car_engine_section),
                isExpanded = engineExpanded,
                onToggle = { engineExpanded = !engineExpanded }
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = power,
                        onValueChange = { if (it.all { char -> char.isDigit() }) power = it },
                        label = { Text(stringResource(R.string.car_power_label)) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        enabled = state !is AddCarState.Pending
                    )
                    Spacer(Modifier.width(8.dp))
                    Box(modifier = Modifier.width(100.dp)) {
                        OutlinedTextField(
                            value = powerUnit,
                            onValueChange = { },
                            readOnly = true,
                            label = { Text(stringResource(R.string.common_unit)) },
                            trailingIcon = {
                                Icon(
                                    Icons.Default.ArrowDropDown,
                                    "dropdown",
                                    Modifier.clickable { powerUnitExpanded = true })
                            },
                            modifier = Modifier.clickable { powerUnitExpanded = true }
                        )
                        Box(modifier = Modifier.matchParentSize().clickable { powerUnitExpanded = true })

                        DropdownMenu(
                            expanded = powerUnitExpanded,
                            onDismissRequest = { powerUnitExpanded = false }
                        ) {
                            powerUnits.forEach { unit ->
                                DropdownMenuItem(
                                    text = { Text(unit) },
                                    onClick = {
                                        powerUnit = unit
                                        powerUnitExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = torque,
                    onValueChange = { if (it.all { char -> char.isDigit() }) torque = it },
                    label = { Text(stringResource(R.string.car_torque_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    enabled = state !is AddCarState.Pending
                )

                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = engineCode,
                    onValueChange = { engineCode = it.uppercase() },
                    label = { Text(stringResource(R.string.car_engine_code_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = state !is AddCarState.Pending
                )

                Spacer(Modifier.height(8.dp))

                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = getEngineLayoutLabel(context, engineLayout),
                        onValueChange = { },
                        readOnly = true,
                        label = { Text(stringResource(R.string.car_engine_layout_label)) },
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            Icon(
                                Icons.Default.ArrowDropDown,
                                "dropdown",
                                Modifier.clickable { engineLayoutExpanded = true })
                        }
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable { engineLayoutExpanded = true }
                    )
                    DropdownMenu(
                        expanded = engineLayoutExpanded,
                        onDismissRequest = { engineLayoutExpanded = false },
                        modifier = Modifier.fillMaxWidth(0.9f)
                    ) {
                        engineLayouts.forEach { layout ->
                            DropdownMenuItem(
                                text = { Text(getEngineLayoutLabel(context, layout)) },
                                onClick = {
                                    engineLayout = layout
                                    engineLayoutExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = getCylinderLayoutLabel(context, cylinderLayout),
                        onValueChange = { },
                        readOnly = true,
                        label = { Text(stringResource(R.string.car_cylinder_layout_label)) },
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            Icon(
                                Icons.Default.ArrowDropDown,
                                "dropdown",
                                Modifier.clickable { cylinderLayoutExpanded = true })
                        }
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable { cylinderLayoutExpanded = true }
                    )
                    DropdownMenu(
                        expanded = cylinderLayoutExpanded,
                        onDismissRequest = { cylinderLayoutExpanded = false },
                        modifier = Modifier.fillMaxWidth(0.9f)
                    ) {
                        cylinderLayouts.forEach { layout ->
                            DropdownMenuItem(
                                text = { Text(getCylinderLayoutLabel(context, layout)) },
                                onClick = {
                                    cylinderLayout = layout
                                    cylinderLayoutExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = getEmissionStandardLabel(context, emissionStandard),
                        onValueChange = { },
                        readOnly = true,
                        label = { Text(stringResource(R.string.car_emission_standard_label)) },
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            Icon(
                                Icons.Default.ArrowDropDown,
                                "dropdown",
                                Modifier.clickable { emissionStandardExpanded = true })
                        }
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable { emissionStandardExpanded = true }
                    )
                    DropdownMenu(
                        expanded = emissionStandardExpanded,
                        onDismissRequest = { emissionStandardExpanded = false },
                        modifier = Modifier.fillMaxWidth(0.9f)
                    ) {
                        emissionStandards.forEach { standard ->
                            DropdownMenuItem(
                                text = { Text(getEmissionStandardLabel(context, standard)) },
                                onClick = {
                                    emissionStandard = standard
                                    emissionStandardExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = getAspirationLabel(context, aspiration),
                        onValueChange = { },
                        readOnly = true,
                        label = { Text(stringResource(R.string.car_aspiration_label)) },
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            Icon(
                                Icons.Default.ArrowDropDown,
                                "dropdown",
                                Modifier.clickable { aspirationExpanded = true })
                        }
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable { aspirationExpanded = true }
                    )
                    DropdownMenu(
                        expanded = aspirationExpanded,
                        onDismissRequest = { aspirationExpanded = false },
                        modifier = Modifier.fillMaxWidth(0.9f)
                    ) {
                        aspirationOptions.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(getAspirationLabel(context, option)) },
                                onClick = {
                                    aspiration = option
                                    aspirationExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = engineSize,
                    onValueChange = { engineSize = it },
                    label = { Text(stringResource(R.string.car_engine_size_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = state !is AddCarState.Pending
                )

                Spacer(Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = numberOfCylinders,
                        onValueChange = { if (it.all { char -> char.isDigit() }) numberOfCylinders = it },
                        label = { Text(stringResource(R.string.car_cylinders_label)) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        enabled = state !is AddCarState.Pending
                    )
                    Spacer(Modifier.width(8.dp))
                    OutlinedTextField(
                        value = valvesPerCylinder,
                        onValueChange = { if (it.all { char -> char.isDigit() }) valvesPerCylinder = it },
                        label = { Text(stringResource(R.string.car_valves_per_cyl_label)) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        enabled = state !is AddCarState.Pending
                    )
                    Spacer(Modifier.width(8.dp))
                    OutlinedTextField(
                        value = topSpeed,
                        onValueChange = { if (it.all { char -> char.isDigit() }) topSpeed = it },
                        label = { Text(stringResource(R.string.car_top_speed_label)) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        enabled = state !is AddCarState.Pending
                    )
                }

                Spacer(Modifier.height(8.dp))

                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = getFuelTypeLabel(context, fuelType),
                        onValueChange = { 
                            fuelType = it 
                            fuelSystem = "" // Reset subtype when main type changes
                        },
                        label = { Text(stringResource(R.string.car_fuel_type_label)) },
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            Icon(
                                Icons.Default.ArrowDropDown,
                                "dropdown",
                                Modifier.clickable { fuelTypeExpanded = true })
                        }
                    )
                    DropdownMenu(
                        expanded = fuelTypeExpanded,
                        onDismissRequest = { fuelTypeExpanded = false },
                        modifier = Modifier.fillMaxWidth(0.9f)
                    ) {
                        fuelTypes.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(getFuelTypeLabel(context, type)) },
                                onClick = {
                                    fuelType = type
                                    fuelSystem = ""
                                    fuelTypeExpanded = false
                                }
                            )
                        }
                    }
                }

                if (fuelType == "Petrol" || fuelType == "LPG" || fuelType == "Diesel") {
                    Spacer(Modifier.height(8.dp))
                    var fuelSystemExpanded by remember { mutableStateOf(false) }
                    val fuelSystemOptions = when (fuelType) {
                        "Petrol", "LPG" -> listOf("Carburetor", "Multi Point Injection", "Direct Injection")
                        "Diesel" -> listOf("Injection Pump", "Pumpe Duse", "Common Rail")
                        else -> emptyList()
                    }

                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = getFuelSystemLabel(context, fuelSystem),
                            onValueChange = { fuelSystem = it },
                            label = { Text(if (fuelType == "Diesel") stringResource(R.string.car_fuel_system_label) else stringResource(R.string.car_injection_system_label)) },
                            modifier = Modifier.fillMaxWidth(),
                            trailingIcon = {
                                Icon(
                                    Icons.Default.ArrowDropDown,
                                    "dropdown",
                                    Modifier.clickable { fuelSystemExpanded = true })
                            }
                        )
                        DropdownMenu(
                            expanded = fuelSystemExpanded,
                            onDismissRequest = { fuelSystemExpanded = false },
                            modifier = Modifier.fillMaxWidth(0.9f)
                        ) {
                            fuelSystemOptions.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(getFuelSystemLabel(context, option)) },
                                    onClick = {
                                        fuelSystem = option
                                        fuelSystemExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    Box(modifier = Modifier.weight(1.5f)) {
                        OutlinedTextField(
                            value = getGearboxTypeLabel(context, gearboxType),
                            onValueChange = { gearboxType = it },
                            label = { Text(stringResource(R.string.car_gearbox_type_label)) },
                            modifier = Modifier.fillMaxWidth(),
                            trailingIcon = {
                                Icon(
                                    Icons.Default.ArrowDropDown,
                                    "dropdown",
                                    Modifier.clickable { gearboxTypeExpanded = true })
                            }
                        )
                        DropdownMenu(
                            expanded = gearboxTypeExpanded,
                            onDismissRequest = { gearboxTypeExpanded = false },
                            modifier = Modifier.fillMaxWidth(0.9f)
                        ) {
                            gearboxTypes.forEach { type ->
                                DropdownMenuItem(
                                    text = { Text(getGearboxTypeLabel(context, type)) },
                                    onClick = {
                                        gearboxType = type
                                        gearboxTypeExpanded = false
                                    }
                                )
                            }
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    val isCvt = gearboxType == "CVT"
                    OutlinedTextField(
                        value = if (isCvt) stringResource(R.string.common_not_applicable) else gears,
                        onValueChange = { if (!isCvt) gears = it },
                        label = { Text(stringResource(R.string.car_gears_label)) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        enabled = state !is AddCarState.Pending && !isCvt
                    )
                }

                Spacer(Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = getSuspensionLabel(context, frontSuspension),
                            onValueChange = { },
                            readOnly = true,
                            label = { Text(stringResource(R.string.car_front_suspension_label)) },
                            modifier = Modifier.fillMaxWidth(),
                            trailingIcon = {
                                Icon(Icons.Default.ArrowDropDown, null, Modifier.clickable { frontSuspensionExpanded = true })
                            }
                        )
                        Box(modifier = Modifier.matchParentSize().clickable { frontSuspensionExpanded = true })
                        DropdownMenu(
                            expanded = frontSuspensionExpanded,
                            onDismissRequest = { frontSuspensionExpanded = false }
                        ) {
                            frontSuspensionOptions.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(getSuspensionLabel(context, option)) },
                                    onClick = {
                                        frontSuspension = option
                                        frontSuspensionExpanded = false
                                    }
                                )
                            }
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = getSuspensionLabel(context, rearSuspension),
                            onValueChange = { },
                            readOnly = true,
                            label = { Text(stringResource(R.string.car_rear_suspension_label)) },
                            modifier = Modifier.fillMaxWidth(),
                            trailingIcon = {
                                Icon(Icons.Default.ArrowDropDown, null, Modifier.clickable { rearSuspensionExpanded = true })
                            }
                        )
                        Box(modifier = Modifier.matchParentSize().clickable { rearSuspensionExpanded = true })
                        DropdownMenu(
                            expanded = rearSuspensionExpanded,
                            onDismissRequest = { rearSuspensionExpanded = false }
                        ) {
                            rearSuspensionOptions.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(getSuspensionLabel(context, option)) },
                                    onClick = {
                                        rearSuspension = option
                                        rearSuspensionExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                if (fuelType == "Hybrid" || (fuelType != "Electric" && fuelType.isNotBlank())) {
                    OutlinedTextField(
                        value = fuelTankCapacity,
                        onValueChange = { if (it.all { char -> char.isDigit() || char == '.' }) fuelTankCapacity = it },
                        label = { Text(stringResource(R.string.car_fuel_tank_capacity_label)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        enabled = state !is AddCarState.Pending
                    )
                }

                if (fuelType == "Hybrid" || fuelType == "Electric") {
                    if (fuelType == "Hybrid") Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = batteryCapacity,
                        onValueChange = { if (it.all { char -> char.isDigit() || char == '.' }) batteryCapacity = it },
                        label = { Text(stringResource(R.string.car_battery_capacity_label)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        enabled = state !is AddCarState.Pending
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // --- DIMENSIONS SECTION ---
            CollapsibleSection(
                title = stringResource(R.string.car_dimensions_section),
                isExpanded = dimensionsExpanded,
                onToggle = { dimensionsExpanded = !dimensionsExpanded }
            ) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = length,
                        onValueChange = { if (it.all { char -> char.isDigit() }) length = it },
                        label = { Text(stringResource(R.string.car_length_label)) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        enabled = state !is AddCarState.Pending
                    )
                    Spacer(Modifier.width(8.dp))
                    OutlinedTextField(
                        value = width,
                        onValueChange = { if (it.all { char -> char.isDigit() }) width = it },
                        label = { Text(stringResource(R.string.car_width_label)) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        enabled = state !is AddCarState.Pending
                    )
                    Spacer(Modifier.width(8.dp))
                    OutlinedTextField(
                        value = height,
                        onValueChange = { if (it.all { char -> char.isDigit() }) height = it },
                        label = { Text(stringResource(R.string.car_height_label)) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        enabled = state !is AddCarState.Pending
                    )
                }

                Spacer(Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = wheelbase,
                        onValueChange = { if (it.all { char -> char.isDigit() }) wheelbase = it },
                        label = { Text(stringResource(R.string.car_wheelbase_label)) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        enabled = state !is AddCarState.Pending
                    )
                    Spacer(Modifier.width(8.dp))
                    OutlinedTextField(
                        value = trackWidth,
                        onValueChange = { if (it.all { char -> char.isDigit() }) trackWidth = it },
                        label = { Text(stringResource(R.string.car_track_width_label)) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        enabled = state !is AddCarState.Pending
                    )
                }

                Spacer(Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = weight,
                        onValueChange = { if (it.all { char -> char.isDigit() }) weight = it },
                        label = { Text(stringResource(R.string.car_weight_label)) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        enabled = state !is AddCarState.Pending
                    )
                    Spacer(Modifier.width(8.dp))
                    OutlinedTextField(
                        value = numberOfSeats,
                        onValueChange = { if (it.all { char -> char.isDigit() }) numberOfSeats = it },
                        label = { Text(stringResource(R.string.car_seats_label)) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        enabled = state !is AddCarState.Pending
                    )
                }

                Spacer(Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = numberOfDoors,
                        onValueChange = { if (it.all { char -> char.isDigit() }) numberOfDoors = it },
                        label = { Text(stringResource(R.string.car_doors_label)) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        enabled = state !is AddCarState.Pending
                    )
                    Spacer(Modifier.width(8.dp))
                    OutlinedTextField(
                        value = bootSpace,
                        onValueChange = { if (it.all { char -> char.isDigit() }) bootSpace = it },
                        label = { Text(stringResource(R.string.car_boot_label)) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        enabled = state !is AddCarState.Pending
                    )
                }

                Spacer(Modifier.height(8.dp))

                Text(
                    text = stringResource(R.string.car_tire_size_label),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                )
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = tireWidth,
                        onValueChange = { if (it.all { char -> char.isDigit() }) tireWidth = it },
                        label = { Text(stringResource(R.string.car_tire_width_label)) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        enabled = state !is AddCarState.Pending
                    )
                    Text("/", modifier = Modifier.padding(horizontal = 4.dp), style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(
                        value = tireAspectRatio,
                        onValueChange = { if (it.all { char -> char.isDigit() }) tireAspectRatio = it },
                        label = { Text(stringResource(R.string.car_tire_ratio_label)) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        enabled = state !is AddCarState.Pending
                    )
                    Text("R", modifier = Modifier.padding(horizontal = 4.dp), style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(
                        value = tireDiameter,
                        onValueChange = { if (it.all { char -> char.isDigit() }) tireDiameter = it },
                        label = { Text(stringResource(R.string.car_tire_diam_label)) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        enabled = state !is AddCarState.Pending
                    )
                }

                Spacer(Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = getBrakesLabel(context, frontBrakes),
                            onValueChange = { },
                            readOnly = true,
                            label = { Text(stringResource(R.string.car_front_brakes_label)) },
                            modifier = Modifier.fillMaxWidth(),
                            trailingIcon = {
                                Icon(Icons.Default.ArrowDropDown, null, Modifier.clickable { frontBrakesExpanded = true })
                            }
                        )
                        Box(modifier = Modifier.matchParentSize().clickable { frontBrakesExpanded = true })
                        DropdownMenu(
                            expanded = frontBrakesExpanded,
                            onDismissRequest = { frontBrakesExpanded = false }
                        ) {
                            brakeOptions.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(getBrakesLabel(context, option)) },
                                    onClick = {
                                        frontBrakes = option
                                        frontBrakesExpanded = false
                                    }
                                )
                            }
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = getBrakesLabel(context, rearBrakes),
                            onValueChange = { },
                            readOnly = true,
                            label = { Text(stringResource(R.string.car_rear_brakes_label)) },
                            modifier = Modifier.fillMaxWidth(),
                            trailingIcon = {
                                Icon(Icons.Default.ArrowDropDown, null, Modifier.clickable { rearBrakesExpanded = true })
                            }
                        )
                        Box(modifier = Modifier.matchParentSize().clickable { rearBrakesExpanded = true })
                        DropdownMenu(
                            expanded = rearBrakesExpanded,
                            onDismissRequest = { rearBrakesExpanded = false }
                        ) {
                            brakeOptions.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(getBrakesLabel(context, option)) },
                                    onClick = {
                                        rearBrakes = option
                                        rearBrakesExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = getDrivetrainLabel(context, drivetrain),
                        onValueChange = { },
                        readOnly = true,
                        label = { Text(stringResource(R.string.car_drivetrain_label)) },
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            Icon(
                                Icons.Default.ArrowDropDown,
                                "dropdown",
                                Modifier.clickable { drivetrainExpanded = true })
                        }
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable { drivetrainExpanded = true }
                    )
                    DropdownMenu(
                        expanded = drivetrainExpanded,
                        onDismissRequest = { drivetrainExpanded = false },
                        modifier = Modifier.fillMaxWidth(0.9f)
                    ) {
                        drivetrainOptions.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(getDrivetrainLabel(context, option)) },
                                onClick = {
                                    drivetrain = option
                                    drivetrainExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = {
                    viewModel.onAddOrUpdateCar(
                        name = name,
                        licensePlate = licensePlate,
                        plateCountry = selectedCountry?.code ?: "",
                        make = make,
                        model = model,
                        vin = vin,
                        year = year,
                        engineSize = engineSize,
                        fuelType = fuelType,
                        fuelSystem = fuelSystem,
                        color = color,
                        power = power,
                        powerUnit = powerUnit,
                        torque = torque,
                        engineCode = engineCode,
                        engineLayout = engineLayout,
                        cylinderLayout = cylinderLayout,
                        emissionStandard = emissionStandard,
                        length = length,
                        width = width,
                        height = height,
                        wheelbase = wheelbase,
                        trackWidth = trackWidth,
                        fuelTankCapacity = fuelTankCapacity,
                        batteryCapacity = batteryCapacity,
                        drivetrain = drivetrain,
                        gearboxType = gearboxType,
                        gears = gears,
                        frontSuspension = frontSuspension,
                        rearSuspension = rearSuspension,
                        vehicleType = vehicleType,
                        manufacturingCountry = manufacturingCountry,
                        topSpeed = topSpeed,
                        weight = weight,
                        numberOfSeats = numberOfSeats,
                        numberOfCylinders = numberOfCylinders,
                        valvesPerCylinder = valvesPerCylinder,
                        numberOfDoors = numberOfDoors,
                        bootSpace = bootSpace,
                        tireWidth = tireWidth,
                        tireAspectRatio = tireAspectRatio,
                        tireDiameter = tireDiameter,
                        aspiration = aspiration,
                        frontBrakes = frontBrakes,
                        rearBrakes = rearBrakes
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = (name.isNotBlank() || (make.isNotBlank() && model.isNotBlank())) && state !is AddCarState.Pending
            ) {
                if (state is AddCarState.Pending) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(stringResource(R.string.common_save))
                }
            }
        }
    }
}

@Composable
fun ScannedCarDataConfirmationDialog(
    data: ScannedCarData,
    onDismiss: () -> Unit,
    onConfirm: (ScannedCarData) -> Unit
) {
    val context = LocalContext.current
    // We create a map of keys to values and labels for easy display
    val fields = remember(data) {
        val list = mutableListOf<Triple<String, String, String>>() // Label, Value, Key
        data.make?.let { list.add(Triple(context.getString(R.string.car_make_label), it, "make")) }
        data.model?.let { list.add(Triple(context.getString(R.string.car_model_label), it, "model")) }
        data.vin?.let { list.add(Triple(context.getString(R.string.car_vin_label), it, "vin")) }
        data.year?.let { list.add(Triple(context.getString(R.string.car_year_label), it.roundToInt().toString(), "year")) }
        data.fuelType?.let { list.add(Triple(context.getString(R.string.car_fuel_type_label), getFuelTypeLabel(context, it), "fuelType")) }
        data.engineSize?.let { list.add(Triple(context.getString(R.string.car_engine_size_label), "${it.roundToInt()} cc", "engineSize")) }
        data.power?.let { list.add(Triple(context.getString(R.string.car_power_label), "${it.roundToInt()} ${data.powerUnit ?: "hp"}", "power")) }
        data.torque?.let { list.add(Triple(context.getString(R.string.car_torque_label), "${it.roundToInt()} Nm", "torque")) }
        data.color?.let { list.add(Triple(context.getString(R.string.car_color_label), it, "color")) }
        data.registrationPlate?.let { list.add(Triple(context.getString(R.string.car_license_plate_label), it, "registrationPlate")) }
        data.numberOfSeats?.let { list.add(Triple(context.getString(R.string.car_seats_label), it.roundToInt().toString(), "numberOfSeats")) }
        data.numberOfDoors?.let { list.add(Triple(context.getString(R.string.car_doors_label), it.roundToInt().toString(), "numberOfDoors")) }
        data.weight?.let { list.add(Triple(context.getString(R.string.car_weight_label), "${it.roundToInt()} kg", "weight")) }
        data.engineCode?.let { list.add(Triple(context.getString(R.string.car_engine_code_label), it, "engineCode")) }
        data.emissionStandard?.let { list.add(Triple(context.getString(R.string.car_emission_standard_label), getEmissionStandardLabel(context, it), "emissionStandard")) }
        data.gearboxType?.let { list.add(Triple(context.getString(R.string.car_gearbox_type_label), getGearboxTypeLabel(context, it), "gearboxType")) }
        data.drivetrain?.let { list.add(Triple(context.getString(R.string.car_drivetrain_label), getDrivetrainLabel(context, it), "drivetrain")) }
        data.engineLayout?.let { list.add(Triple(context.getString(R.string.car_engine_layout_label), getEngineLayoutLabel(context, it), "engineLayout")) }
        data.cylinderLayout?.let { list.add(Triple(context.getString(R.string.car_cylinder_layout_label), getCylinderLayoutLabel(context, it), "cylinderLayout")) }
        data.fuelTankCapacity?.let { list.add(Triple(context.getString(R.string.car_fuel_tank_capacity_label), "$it L", "fuelTankCapacity")) }
        data.topSpeed?.let { list.add(Triple(context.getString(R.string.car_top_speed_label), "${it.roundToInt()}", "topSpeed")) }
        list
    }

    var selectedKeys by remember { mutableStateOf(fields.map { it.third }.toSet()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.car_confirm_scanned_data)) },
        text = {
            Column {
                Text(
                    stringResource(R.string.car_scanned_data_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(fields) { (label, value, key) ->
                        val isSelected = key in selectedKeys
                        Surface(
                            onClick = {
                                selectedKeys = if (isSelected) selectedKeys - key else selectedKeys + key
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
                                    Text(text = label, style = MaterialTheme.typography.labelSmall)
                                    Text(
                                        text = value,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    // Filter original data based on selected keys
                    val confirmedData = ScannedCarData(
                        make = if ("make" in selectedKeys) data.make else null,
                        model = if ("model" in selectedKeys) data.model else null,
                        vin = if ("vin" in selectedKeys) data.vin else null,
                        year = if ("year" in selectedKeys) data.year else null,
                        fuelType = if ("fuelType" in selectedKeys) data.fuelType else null,
                        engineSize = if ("engineSize" in selectedKeys) data.engineSize else null,
                        power = if ("power" in selectedKeys) data.power else null,
                        powerUnit = data.powerUnit,
                        torque = if ("torque" in selectedKeys) data.torque else null,
                        color = if ("color" in selectedKeys) data.color else null,
                        registrationPlate = if ("registrationPlate" in selectedKeys) data.registrationPlate else null,
                        numberOfSeats = if ("numberOfSeats" in selectedKeys) data.numberOfSeats else null,
                        numberOfDoors = if ("numberOfDoors" in selectedKeys) data.numberOfDoors else null,
                        weight = if ("weight" in selectedKeys) data.weight else null,
                        engineCode = if ("engineCode" in selectedKeys) data.engineCode else null,
                        emissionStandard = if ("emissionStandard" in selectedKeys) data.emissionStandard else null,
                        gearboxType = if ("gearboxType" in selectedKeys) data.gearboxType else null,
                        drivetrain = if ("drivetrain" in selectedKeys) data.drivetrain else null,
                        engineLayout = if ("engineLayout" in selectedKeys) data.engineLayout else null,
                        cylinderLayout = if ("cylinderLayout" in selectedKeys) data.cylinderLayout else null,
                        fuelTankCapacity = if ("fuelTankCapacity" in selectedKeys) data.fuelTankCapacity else null,
                        topSpeed = if ("topSpeed" in selectedKeys) data.topSpeed else null,
                        mileage = if ("mileage" in selectedKeys) data.mileage else null
                    )
                    onConfirm(confirmedData)
                },
                enabled = selectedKeys.isNotEmpty()
            ) {
                Text(stringResource(R.string.common_apply_selected, selectedKeys.size))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_cancel))
            }
        }
    )
}

private fun getFuelTypeLabel(context: android.content.Context, type: String): String = CarTranslations.getFuelTypeLabel(context, type)
private fun getGearboxTypeLabel(context: android.content.Context, type: String): String = CarTranslations.getGearboxTypeLabel(context, type)
private fun getEngineLayoutLabel(context: android.content.Context, layout: String): String = CarTranslations.getEngineLayoutLabel(context, layout)
private fun getCylinderLayoutLabel(context: android.content.Context, layout: String): String = CarTranslations.getCylinderLayoutLabel(context, layout)
private fun getAspirationLabel(context: android.content.Context, option: String): String = CarTranslations.getAspirationLabel(context, option)
private fun getFuelSystemLabel(context: android.content.Context, option: String): String = CarTranslations.getFuelSystemLabel(context, option)
private fun getSuspensionLabel(context: android.content.Context, option: String): String = CarTranslations.getSuspensionLabel(context, option)
private fun getBrakesLabel(context: android.content.Context, option: String): String = CarTranslations.getBrakesLabel(context, option)
private fun getDrivetrainLabel(context: android.content.Context, option: String): String = CarTranslations.getDrivetrainLabel(context, option)
private fun getEmissionStandardLabel(context: android.content.Context, standard: String): String = CarTranslations.getEmissionStandardLabel(context, standard)
private fun getVehicleTypeLabel(context: android.content.Context, type: String): String = CarTranslations.getVehicleTypeLabel(context, type)

@Composable
private fun CollapsibleSection(
    title: String,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggle() }
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = if (isExpanded) stringResource(R.string.common_collapse) else stringResource(R.string.common_expand),
                tint = MaterialTheme.colorScheme.primary
            )
        }
        AnimatedVisibility(visible = isExpanded) {
            Column {
                content()
            }
        }
        HorizontalDivider(
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.outlineVariant
        )
    }
}

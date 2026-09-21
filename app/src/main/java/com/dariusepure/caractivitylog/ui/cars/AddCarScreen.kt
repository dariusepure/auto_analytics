package com.dariusepure.caractivitylog.ui.cars

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.activity.compose.rememberLauncherForActivityResult
import android.widget.Toast
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
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.window.Popup
import com.dariusepure.caractivitylog.ui.common.DropdownPositionProvider
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.platform.LocalDensity
import com.dariusepure.caractivitylog.R
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.HorizontalDivider
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.ui.res.painterResource
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Checkbox
import androidx.compose.material3.AlertDialog
import androidx.compose.ui.text.font.FontWeight
import com.dariusepure.caractivitylog.domain.ScannedCarData
import com.dariusepure.caractivitylog.ui.common.AutoSizeText
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
    viewModel: AddCarViewModel = hiltViewModel(),
    @Suppress("UNUSED_PARAMETER") windowSizeClass: WindowSizeClass? = null,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val unitSystem by viewModel.unitSystem.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val fuelTypes = listOf("Petrol", "Diesel", "Electric", "Hybrid", "LPG", "CNG", "Hydrogen")
    val engineLayouts = listOf("Transverse", "Longitudinal")
    val cylinderLayouts = listOf("Inline", "V", "W", "Boxer")
    val aspirationOptions = listOf("Naturally Aspirated", "Turbocharged", "Supercharged")
    val emissionStandards = listOf("Non-Euro", "Euro 1", "Euro 2", "Euro 3", "Euro 4", "Euro 5", "Euro 6")
    val gearboxTypes = listOf("Manual", "Automatic", "CVT", "DCT", "AMT")
    val brakeOptions = listOf("Ventilated Discs", "Solid Discs", "Drums", "Ceramic Discs")
    val frontSuspensionOptions = listOf("MacPherson", "Double Wishbone", "Multi-link")
    val rearSuspensionOptions = listOf("Torsion Beam", "Multi-link", "Solid Axle")
    val drivetrainOptions = listOf("FWD", "RWD", "AWD")
    val vehicleTypes = listOf(
        "Saloon", "Estate", "Hatchback", "Liftback", "MPV", "SUV", "Crossover", "Coupe", "Convertible", "Van", "Pickup",
        "Fastback", "Targa", "Roadster", "Spider", "Coupe-Cabriolet", "Shooting Brake", "Minivan",
    )
    val carColors = listOf("White", "Black", "Silver", "Gray", "Blue", "Red", "Brown", "Green", "Yellow", "Orange")

    val sortedColors = remember(context) {
        carColors.sortedBy { CarTranslations.getColorLabel(context, it) }
    }

    val sortedCountries = remember(context) {
        europeanCountries.sortedBy { CarTranslations.getCountryName(context, it.code, it.name) }
    }

    var licensePlate by remember { mutableStateOf("") }
    var selectedCountry by remember { mutableStateOf<Country?>(null) }
    val usesMiles = unitSystem == com.dariusepure.caractivitylog.domain.UnitSystem.IMPERIAL
    val consumptionUnit = if (usesMiles) "mpg" else "L/100km"
    var make by remember { mutableStateOf("") }
    var model by remember { mutableStateOf("") }
    var generation by remember { mutableStateOf("") }
    var engineVariant by remember { mutableStateOf("") }
    var vin by remember { mutableStateOf("") }
    var showVinError by remember { mutableStateOf(value = false) }
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
    var acceleration0to100 by remember { mutableStateOf("") }
    var fuelConsumptionCombined by remember { mutableStateOf("") }
    var fuelConsumptionUrban by remember { mutableStateOf("") }
    var fuelConsumptionExtraUrban by remember { mutableStateOf("") }
    var co2Emissions by remember { mutableStateOf("") }

    var length by remember { mutableStateOf("") }
    var width by remember { mutableStateOf("") }
    var height by remember { mutableStateOf("") }
    var wheelbase by remember { mutableStateOf("") }
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

    var airbags by remember { mutableStateOf("") }

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

    var dataToConfirm by remember { mutableStateOf<ScannedCarData?>(null) }

    LaunchedEffect(Unit) {
        viewModel.scannedDataEvent.collect { dataList ->
            if (dataList.isNotEmpty()) {
                dataToConfirm = dataList.first()
            }
        }
    }

    if (dataToConfirm != null) {
        ScannedCarDataConfirmationDialog(
            data = dataToConfirm!!,
            existingData = mapOf(
                "make" to make,
                "model" to model,
                "generation" to generation,
                "engineVariant" to engineVariant,
                "vin" to vin,
                "year" to year,
                "fuelType" to fuelType,
                "engineSize" to engineSize,
                "power" to power,
                "torque" to torque,
                "color" to color,
                "registrationPlate" to licensePlate,
                "numberOfSeats" to numberOfSeats,
                "numberOfDoors" to numberOfDoors,
                "weight" to weight,
                "engineCode" to engineCode,
                "emissionStandard" to emissionStandard,
                "gearboxType" to gearboxType,
                "drivetrain" to drivetrain,
                "engineLayout" to engineLayout,
                "cylinderLayout" to cylinderLayout,
                "fuelTankCapacity" to fuelTankCapacity,
                "topSpeed" to topSpeed,
                "acceleration0to100" to acceleration0to100,
                "fuelConsumptionCombined" to fuelConsumptionCombined,
                "co2Emissions" to co2Emissions
            ),
            usesMiles = usesMiles,
            consumptionUnit = consumptionUnit,
            onDismiss = { dataToConfirm = null }
        ) { selectedData ->
            if (make.isBlank()) selectedData.make?.let { 
                make = it.lowercase().replaceFirstChar { char -> char.uppercase() } 
            }
                if (model.isBlank()) selectedData.model?.let { model = it }
                if (engineVariant.isBlank()) selectedData.engineVariant?.let { engineVariant = it }
                if (vin.isBlank()) selectedData.vin?.let { vin = it.uppercase() }
                if (year.isBlank()) selectedData.year?.let { year = it.roundToInt().toString() }
                if (fuelType.isBlank()) selectedData.fuelType?.let { if (it in fuelTypes) fuelType = it }
                if (engineSize.isBlank()) selectedData.engineSize?.let { engineSize = it.roundToInt().toString() }
                if (power.isBlank()) selectedData.power?.let { power = it.roundToInt().toString() }
                if (powerUnit.isBlank() || (powerUnit.lowercase() == "hp")) selectedData.powerUnit?.let { powerUnit = it }
                if (torque.isBlank()) selectedData.torque?.let { torque = it.roundToInt().toString() }
                if (color.isBlank()) selectedData.color?.let { color = it }
                if (licensePlate.isBlank()) selectedData.registrationPlate?.let { licensePlate = it.uppercase() }
                if (numberOfSeats.isBlank()) selectedData.numberOfSeats?.let { numberOfSeats = it.roundToInt().toString() }
                if (numberOfDoors.isBlank()) selectedData.numberOfDoors?.let { numberOfDoors = it.roundToInt().toString() }
                if (weight.isBlank()) selectedData.weight?.let { weight = it.roundToInt().toString() }
                if (engineCode.isBlank()) selectedData.engineCode?.let { engineCode = it }
                if (emissionStandard.isBlank()) {
                    selectedData.emissionStandard?.let {
                        if (it in emissionStandards) emissionStandard = it
                        else if (it.contains("Euro", ignoreCase = true)) {
                            emissionStandards.find { s -> it.contains(s.takeLast(1)) }?.let { standard ->
                                emissionStandard = standard
                            }
                        }
                    }
                }
                if (gearboxType.isBlank()) selectedData.gearboxType?.let { if (it in gearboxTypes) gearboxType = it }
                if (drivetrain.isBlank()) selectedData.drivetrain?.let { if (it in drivetrainOptions) drivetrain = it }
                if (engineLayout.isBlank()) selectedData.engineLayout?.let { if (it in engineLayouts) engineLayout = it }
                if (cylinderLayout.isBlank()) selectedData.cylinderLayout?.let { if (it in cylinderLayouts) cylinderLayout = it }
                if (fuelTankCapacity.isBlank()) selectedData.fuelTankCapacity?.let { fuelTankCapacity = it.toString() }
                if (topSpeed.isBlank()) selectedData.topSpeed?.let { topSpeed = it.roundToInt().toString() }
                if (acceleration0to100.isBlank()) selectedData.acceleration0to100?.let { acceleration0to100 = it.toString() }
                if (fuelConsumptionCombined.isBlank()) selectedData.fuelConsumptionCombined?.let { fuelConsumptionCombined = it.toString() }
                if (co2Emissions.isBlank()) selectedData.co2Emissions?.let { co2Emissions = it.roundToInt().toString() }
                
                if (airbags.isBlank()) selectedData.airbags?.let { airbags = it.roundToInt().toString() }

                dataToConfirm = null
            }
    }

    var identityExpanded by remember { mutableStateOf(value = true) }
    var engineExpanded by remember { mutableStateOf(false) }
    var dimensionsExpanded by remember { mutableStateOf(false) }
    var safetyExpanded by remember { mutableStateOf(false) }
    var equipmentExpanded by remember { mutableStateOf(false) }

    var countryExpanded by remember { mutableStateOf(false) }
    var manufacturingCountryExpanded by remember { mutableStateOf(false) }
    var makeExpanded by remember { mutableStateOf(false) }
    var showFullBrandList by remember { mutableStateOf(false) }
    var fuelTypeExpanded by remember { mutableStateOf(false) }
    var engineLayoutExpanded by remember { mutableStateOf(false) }
    var emissionStandardExpanded by remember { mutableStateOf(false) }
    var aspirationExpanded by remember { mutableStateOf(false) }
    var cylinderLayoutExpanded by remember { mutableStateOf(false) }
    var drivetrainExpanded by remember { mutableStateOf(false) }
    var gearboxTypeExpanded by remember { mutableStateOf(false) }
    var modelExpanded by remember { mutableStateOf(false) }
    var showFullModelList by remember { mutableStateOf(false) }
    var frontBrakesExpanded by remember { mutableStateOf(false) }
    var rearBrakesExpanded by remember { mutableStateOf(false) }
    var frontSuspensionExpanded by remember { mutableStateOf(false) }
    var rearSuspensionExpanded by remember { mutableStateOf(false) }
    var vehicleTypeExpanded by remember { mutableStateOf(false) }

    var powerUnitExpanded by remember { mutableStateOf(false) }
    val powerUnits = listOf("hp", "kW")

    var selectedEquipments by remember { mutableStateOf(setOf<String>()) }
    
    val density = LocalDensity.current

    val onToggleEquipment = { id: String ->
        selectedEquipments = if (id in selectedEquipments) {
            selectedEquipments - id
        } else {
            selectedEquipments + id
        }
    }

    val safetyEquipment = remember {
        listOf(
            com.dariusepure.caractivitylog.domain.CarEquipment.ABS to R.string.equip_abs,
            com.dariusepure.caractivitylog.domain.CarEquipment.ESP to R.string.equip_esp,
            com.dariusepure.caractivitylog.domain.CarEquipment.ASR to R.string.equip_asr,
            com.dariusepure.caractivitylog.domain.CarEquipment.ISOFIX to R.string.equip_isofix,
            com.dariusepure.caractivitylog.domain.CarEquipment.LANE_ASSIST to R.string.equip_lane_assist,
            com.dariusepure.caractivitylog.domain.CarEquipment.BLIND_SPOT to R.string.equip_blind_spot,
            com.dariusepure.caractivitylog.domain.CarEquipment.ADAPTIVE_CRUISE to R.string.equip_adaptive_cruise,
            com.dariusepure.caractivitylog.domain.CarEquipment.EMERGENCY_BRAKE to R.string.equip_emergency_brake
        )
    }
    val comfortEquipment = remember {
        listOf(
            com.dariusepure.caractivitylog.domain.CarEquipment.AC to R.string.equip_ac,
            com.dariusepure.caractivitylog.domain.CarEquipment.CLIMATE_CONTROL to R.string.equip_climate_control,
            com.dariusepure.caractivitylog.domain.CarEquipment.HEATED_SEATS to R.string.equip_heated_seats,
            com.dariusepure.caractivitylog.domain.CarEquipment.VENTILATED_SEATS to R.string.equip_ventilated_seats,
            com.dariusepure.caractivitylog.domain.CarEquipment.HEATED_STEERING to R.string.equip_heated_steering,
            com.dariusepure.caractivitylog.domain.CarEquipment.LEATHER_INTERIOR to R.string.equip_leather_interior,
            com.dariusepure.caractivitylog.domain.CarEquipment.ELECTRIC_WINDOWS to R.string.equip_electric_windows,
            com.dariusepure.caractivitylog.domain.CarEquipment.POWER_STEERING to R.string.equip_power_steering,
            com.dariusepure.caractivitylog.domain.CarEquipment.CENTRAL_LOCKING to R.string.equip_central_locking
        )
    }
    val techEquipment = remember {
        listOf(
            com.dariusepure.caractivitylog.domain.CarEquipment.NAVIGATION to R.string.equip_navigation,
            com.dariusepure.caractivitylog.domain.CarEquipment.BLUETOOTH to R.string.equip_bluetooth,
            com.dariusepure.caractivitylog.domain.CarEquipment.CARPLAY_ANDROID_AUTO to R.string.equip_carplay_android_auto,
            com.dariusepure.caractivitylog.domain.CarEquipment.KEYLESS to R.string.equip_keyless,
            com.dariusepure.caractivitylog.domain.CarEquipment.START_STOP to R.string.equip_start_stop
        )
    }
    val exteriorEquipment = remember {
        listOf(
            com.dariusepure.caractivitylog.domain.CarEquipment.SUNROOF to R.string.equip_sunroof,
            com.dariusepure.caractivitylog.domain.CarEquipment.XENON_LED to R.string.equip_xenon_led,
            com.dariusepure.caractivitylog.domain.CarEquipment.FOG_LIGHTS to R.string.equip_fog_lights,
            com.dariusepure.caractivitylog.domain.CarEquipment.ALLOY_WHEELS to R.string.equip_alloy_wheels,
            com.dariusepure.caractivitylog.domain.CarEquipment.RAIN_SENSORS to R.string.equip_rain_sensors,
            com.dariusepure.caractivitylog.domain.CarEquipment.LIGHT_SENSORS to R.string.equip_light_sensors
        )
    }
    val parkingEquipment = remember {
        listOf(
            com.dariusepure.caractivitylog.domain.CarEquipment.PARKING_SENSORS to R.string.equip_parking_sensors,
            com.dariusepure.caractivitylog.domain.CarEquipment.REAR_CAMERA to R.string.equip_rear_camera,
            com.dariusepure.caractivitylog.domain.CarEquipment.CAMERA_360 to R.string.equip_360_camera,
            com.dariusepure.caractivitylog.domain.CarEquipment.PARK_ASSIST to R.string.equip_park_assist
        )
    }

    // Local states for each dropdown width to ensure isolation
    var makeWidth by remember { mutableStateOf(0.dp) }
    var modelWidth by remember { mutableStateOf(0.dp) }
    var colorWidth by remember { mutableStateOf(0.dp) }
    var countryWidth by remember { mutableStateOf(0.dp) }
    var vehicleTypeWidth by remember { mutableStateOf(0.dp) }
    var manufacturingCountryWidth by remember { mutableStateOf(0.dp) }
    var fuelTypeWidth by remember { mutableStateOf(0.dp) }
    var fuelSystemWidth by remember { mutableStateOf(0.dp) }
    var aspirationWidth by remember { mutableStateOf(0.dp) }
    var powerUnitWidth by remember { mutableStateOf(0.dp) }
    var engineLayoutWidth by remember { mutableStateOf(0.dp) }
    var cylinderLayoutWidth by remember { mutableStateOf(0.dp) }
    var emissionStandardWidth by remember { mutableStateOf(0.dp) }
    var gearboxTypeWidth by remember { mutableStateOf(0.dp) }
    var drivetrainWidth by remember { mutableStateOf(0.dp) }
    var frontSuspensionWidth by remember { mutableStateOf(0.dp) }
    var rearSuspensionWidth by remember { mutableStateOf(0.dp) }
    var frontBrakesWidth by remember { mutableStateOf(0.dp) }
    var rearBrakesWidth by remember { mutableStateOf(0.dp) }


    val handleBack = {
        val hasRequiredData = make.isNotBlank() && model.isNotBlank()
        val isVinValid = vin.isEmpty() || (vin.length == 17)
        
        if (hasRequiredData && isVinValid) {
            viewModel.onAddOrUpdateCar(
                licensePlate = licensePlate,
                plateCountry = selectedCountry?.code ?: "",
                make = make,
                model = model,
                generation = generation,
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
                tireDiameter = tireDiameter,
                engineVariant = engineVariant,
                acceleration0to100 = acceleration0to100,
                fuelConsumptionCombined = fuelConsumptionCombined,
                fuelConsumptionUrban = fuelConsumptionUrban,
                fuelConsumptionExtraUrban = fuelConsumptionExtraUrban,
                co2Emissions = co2Emissions,
                airbags = airbags,
                equipments = selectedEquipments.toList()
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
                licensePlate = car.licensePlate
                selectedCountry = europeanCountries.find { it.code == car.plateCountry }
                
                make = car.make
                model = car.model
                generation = car.generation
                engineVariant = car.engineVariant
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
                
                val displayTopSpeed = CarFormatters.fromCanonicalSpeed(car.topSpeed, usesMiles)
                topSpeed = displayTopSpeed.takeIf { it != 0.0 }?.roundToInt()?.toString() ?: ""
                
                acceleration0to100 = car.acceleration0to100.takeIf { it != 0.0 }?.toString() ?: ""
                fuelConsumptionCombined = car.fuelConsumptionCombined.let { if (it == 0.0) "" else String.format(java.util.Locale.US, "%.2f", CarFormatters.fromCanonicalConsumption(it, usesMiles)) }
                fuelConsumptionUrban = car.fuelConsumptionUrban.let { if (it == 0.0) "" else String.format(java.util.Locale.US, "%.2f", CarFormatters.fromCanonicalConsumption(it, usesMiles)) }
                fuelConsumptionExtraUrban = car.fuelConsumptionExtraUrban.let { if (it == 0.0) "" else String.format(java.util.Locale.US, "%.2f", CarFormatters.fromCanonicalConsumption(it, usesMiles)) }
                co2Emissions = car.co2Emissions.takeIf { it != 0 }?.toString() ?: ""

                numberOfCylinders = car.numberOfCylinders.takeIf { it != 0 }?.toString() ?: ""
                valvesPerCylinder = car.valvesPerCylinder.takeIf { it != 0 }?.toString() ?: ""
                
                length = car.length.takeIf { it != 0 }?.toString() ?: ""
                width = car.width.takeIf { it != 0 }?.toString() ?: ""
                height = car.height.takeIf { it != 0 }?.toString() ?: ""
                wheelbase = car.wheelbase.takeIf { it != 0 }?.toString() ?: ""
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

                airbags = car.airbags.takeIf { it != 0 }?.toString() ?: ""
                selectedEquipments = car.equipments.toSet()
            }
        }
    }

    val haptic = LocalHapticFeedback.current

    LaunchedEffect(Unit) {
        viewModel.navigationEvent.collect {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
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
                },
                actions = {
                    val canSave = (make.isNotBlank() && model.isNotBlank()) && state !is AddCarState.Pending
                    if (state is AddCarState.Pending) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(40.dp).padding(end = 16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        TextButton(
                            onClick = {
                                viewModel.onAddOrUpdateCar(
                                    licensePlate = licensePlate,
                                    plateCountry = selectedCountry?.code ?: "",
                                    make = make,
                                    model = model,
                                    generation = generation,
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
                                    engineVariant = engineVariant,
                                    aspiration = aspiration,
                                    frontBrakes = frontBrakes,
                                    rearBrakes = rearBrakes,
                                    acceleration0to100 = acceleration0to100,
                                    fuelConsumptionCombined = fuelConsumptionCombined,
                                    fuelConsumptionUrban = fuelConsumptionUrban,
                                    fuelConsumptionExtraUrban = fuelConsumptionExtraUrban,
                                    co2Emissions = co2Emissions,
                                    airbags = airbags,
                                    equipments = selectedEquipments.toList()
                                )
                            },
                            enabled = canSave
                        ) {
                            Text(
                                stringResource(R.string.common_save),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (canSave) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                            )
                        }
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

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ScanButton(
                        onClick = { photoPicker.launch("image/*") },
                        modifier = Modifier.weight(1f),
                        state = state,
                        label = stringResource(R.string.car_scan_photo)
                    )
                    ScanButton(
                        onClick = { pdfPicker.launch("application/pdf") },
                        modifier = Modifier.weight(1f),
                        state = state,
                        label = stringResource(R.string.car_scan_pdf)
                    )
                }
            }

            // --- 1. IDENTITY ---
            CollapsibleSection(
                title = stringResource(R.string.car_identity_section),
                isExpanded = identityExpanded,
                onToggle = { identityExpanded = !identityExpanded }
            ) {
                val filteredBrands = remember(make, showFullBrandList) {
                    if (showFullBrandList || make.isEmpty()) carBrands.filter { it != "Other" }
                    else carBrands.filter { it.startsWith(make, ignoreCase = true) && it != "Other" }
                }

                ExposedDropdownMenuBox(
                    expanded = makeExpanded,
                    onExpandedChange = { 
                        if (!it) showFullBrandList = false
                        makeExpanded = it 
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = make,
                        onValueChange = { 
                            make = it.lowercase().replaceFirstChar { char -> char.uppercase() } 
                            showFullBrandList = false
                            makeExpanded = make.isNotEmpty() && filteredBrands.isNotEmpty()
                        },
                        label = { Text(stringResource(R.string.car_make_label)) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryEditable, true).fillMaxWidth()
                            .onGloballyPositioned { makeWidth = with(density) { it.size.width.toDp() } },
                        singleLine = true,
                        leadingIcon = {
                            Icon(Icons.Outlined.DirectionsCar, null, modifier = Modifier.size(24.dp))
                        },
                        trailingIcon = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (make.isNotEmpty()) {
                                    IconButton(
                                        onClick = { 
                                            make = "" 
                                            showFullBrandList = true
                                            makeExpanded = true
                                        }
                                    ) {
                                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                                    }
                                }
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = makeExpanded)
                            }
                        },
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Characters
                        ),
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                    )
                    
                    if (filteredBrands.isNotEmpty() && makeExpanded) {
                        Popup(
                            onDismissRequest = { 
                                makeExpanded = false
                                showFullBrandList = false
                            },
                            popupPositionProvider = DropdownPositionProvider(),
                            properties = PopupProperties(focusable = false, clippingEnabled = false)
                        ) {
                            Surface(
                                modifier = Modifier.width(makeWidth).heightIn(max = 300.dp),
                                shape = RoundedCornerShape(4.dp),
                                tonalElevation = 3.dp,
                                shadowElevation = 3.dp
                            ) {
                                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                                    filteredBrands.forEach { brand ->
                                        val logoRes = CarFormatters.getBrandLogoResource(brand)
                                        DropdownMenuItem(
                                            text = { 
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    if (logoRes != null) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(32.dp)
                                                                .clip(CircleShape)
                                                                .background(MaterialTheme.colorScheme.surfaceVariant),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Image(
                                                                painter = painterResource(logoRes),
                                                                contentDescription = null,
                                                                modifier = Modifier
                                                                    .size(32.dp)
                                                                    .padding(4.dp)
                                                                    .clip(CircleShape),
                                                                contentScale = ContentScale.Fit
                                                            )
                                                        }
                                                        Spacer(Modifier.width(12.dp))
                                                    }
                                                    Text(brand)
                                                }
                                            },
                                            onClick = {
                                                make = brand
                                                makeExpanded = false
                                                showFullBrandList = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                val modelsForBrand = remember(make) { carModels[make.uppercase()] ?: emptyList() }
                val filteredModels = remember(model, modelsForBrand, showFullModelList) {
                    if (showFullModelList || model.isEmpty()) modelsForBrand
                    else modelsForBrand.filter { it.startsWith(model, ignoreCase = true) }
                }

                ExposedDropdownMenuBox(
                    expanded = modelExpanded,
                    onExpandedChange = { 
                        if (!it) showFullModelList = false
                        modelExpanded = it 
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = model,
                        onValueChange = { 
                            model = it 
                            showFullModelList = false
                            modelExpanded = model.isNotEmpty() && filteredModels.isNotEmpty()
                        },
                        label = { Text(stringResource(R.string.car_model_label)) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryEditable, true).fillMaxWidth()
                            .onGloballyPositioned { modelWidth = with(density) { it.size.width.toDp() } },
                        singleLine = true,
                        enabled = state !is AddCarState.Pending,
                        trailingIcon = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (model.isNotEmpty()) {
                                    IconButton(
                                        onClick = { 
                                            model = "" 
                                            showFullModelList = true
                                            modelExpanded = true
                                        }
                                    ) {
                                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                                    }
                                }
                                if (modelsForBrand.isNotEmpty()) {
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = modelExpanded)
                                }
                            }
                        },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                    )
                    
                    if (filteredModels.isNotEmpty() && modelExpanded) {
                        Popup(
                            onDismissRequest = { 
                                modelExpanded = false
                                showFullModelList = false
                            },
                            popupPositionProvider = DropdownPositionProvider(),
                            properties = PopupProperties(focusable = false, clippingEnabled = false)
                        ) {
                            Surface(
                                modifier = Modifier.width(modelWidth).heightIn(max = 300.dp),
                                shape = RoundedCornerShape(4.dp),
                                tonalElevation = 3.dp,
                                shadowElevation = 3.dp
                            ) {
                                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                                    filteredModels.forEach { carModel ->
                                        DropdownMenuItem(
                                            text = { Text(carModel) },
                                            onClick = {
                                                model = carModel
                                                modelExpanded = false
                                                showFullModelList = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }


                OutlinedTextField(
                    value = generation,
                    onValueChange = { generation = it },
                    label = { Text(stringResource(R.string.car_generation_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = state !is AddCarState.Pending,
                    trailingIcon = if (generation.isNotEmpty()) {
                        {
                            IconButton(onClick = { generation = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    } else null
                )


                OutlinedTextField(
                    value = engineVariant,
                    onValueChange = { engineVariant = it },
                    label = { Text(stringResource(R.string.car_engine_variant_label)) },
                    placeholder = { Text(stringResource(R.string.car_engine_variant_placeholder)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = state !is AddCarState.Pending,
                    trailingIcon = if (engineVariant.isNotEmpty()) {
                        {
                            IconButton(onClick = { engineVariant = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    } else null
                )


                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = year,
                        onValueChange = { if (it.all { char -> char.isDigit() }) year = it },
                        label = { Text(stringResource(R.string.car_year_label)) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        enabled = state !is AddCarState.Pending,
                        trailingIcon = if (year.isNotEmpty()) {
                            {
                                IconButton(onClick = { year = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                                }
                            }
                        } else null
                    )
                    
                    var colorExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = colorExpanded,
                        onExpandedChange = { colorExpanded = it },
                        modifier = Modifier.weight(1.3f)
                    ) {
                        OutlinedTextField(
                            value = CarTranslations.getColorLabel(context, color),
                            onValueChange = { color = it },
                            label = { Text(stringResource(R.string.car_color_label)) },
                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryEditable, true).fillMaxWidth()
                                .onGloballyPositioned { colorWidth = with(density) { it.size.width.toDp() } },
                            singleLine = true,
                            enabled = state !is AddCarState.Pending,
                            trailingIcon = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (color.isNotEmpty()) {
                                        IconButton(onClick = { color = "" }) {
                                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                                        }
                                    }
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = colorExpanded)
                                }
                            },
                            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                        )
                        if (colorExpanded) {
                            Popup(
                                onDismissRequest = { colorExpanded = false },
                                popupPositionProvider = DropdownPositionProvider(),
                                properties = PopupProperties(focusable = false, clippingEnabled = false)
                            ) {
                                Surface(
                                    modifier = Modifier.width(colorWidth).heightIn(max = 300.dp),
                                    shape = RoundedCornerShape(4.dp),
                                    tonalElevation = 3.dp,
                                    shadowElevation = 3.dp
                                ) {
                                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                                        sortedColors.forEach { c ->
                                            DropdownMenuItem(
                                                text = { Text(CarTranslations.getColorLabel(context, c)) },
                                                onClick = {
                                                    color = c
                                                    colorExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }


                ExposedDropdownMenuBox(
                    expanded = countryExpanded,
                    onExpandedChange = { countryExpanded = it },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = selectedCountry?.let { CarTranslations.getCountryName(context, it.code, it.name) } ?: "",
                        onValueChange = { },
                        readOnly = true,
                        label = { Text(stringResource(R.string.car_plate_country_label)) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, true).fillMaxWidth()
                            .onGloballyPositioned { countryWidth = with(density) { it.size.width.toDp() } },
                        trailingIcon = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (selectedCountry != null) {
                                    IconButton(onClick = { selectedCountry = null }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                                    }
                                }
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = countryExpanded)
                            }
                        },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                    )
                    if (countryExpanded) {
                        Popup(
                            onDismissRequest = { countryExpanded = false },
                            popupPositionProvider = DropdownPositionProvider(),
                            properties = PopupProperties(focusable = false, clippingEnabled = false)
                        ) {
                            Surface(
                                modifier = Modifier.width(countryWidth).heightIn(max = 300.dp),
                                shape = RoundedCornerShape(4.dp),
                                tonalElevation = 3.dp,
                                shadowElevation = 3.dp
                            ) {
                                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                                    sortedCountries.forEach { country ->
                                        DropdownMenuItem(
                                            text = {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(country.flag)
                                                    Spacer(Modifier.width(8.dp))
                                                    Text(CarTranslations.getCountryName(context, country.code, country.name))
                                                }
                                            },
                                            onClick = {
                                                selectedCountry = country
                                                countryExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                if (selectedCountry != null) {
    
                    OutlinedTextField(
                        value = licensePlate,
                        onValueChange = { licensePlate = it.uppercase() },
                        label = { Text(stringResource(R.string.car_license_plate_label)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        enabled = state !is AddCarState.Pending,
                        trailingIcon = if (licensePlate.isNotEmpty()) {
                            {
                                IconButton(onClick = { licensePlate = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                                }
                            }
                        } else null
                    )
                }


                OutlinedTextField(
                    value = vin,
                    onValueChange = { input ->
                        val upperInput = input.uppercase()
                        val hasInvalidChars = upperInput.any { it in listOf('I', 'O', 'Q') }
                        val filtered = upperInput.filter { it.isLetterOrDigit() && it !in listOf('I', 'O', 'Q') }
                        if (hasInvalidChars) {
                            showVinError = true
                            Toast.makeText(context, context.getString(R.string.car_vin_invalid_chars), Toast.LENGTH_SHORT).show()
                        } else if (filtered.length > vin.length) {
                            showVinError = false
                        }
                        if (filtered.length <= 17) vin = filtered
                    },
                    label = { Text(stringResource(R.string.car_vin_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = state !is AddCarState.Pending,
                    trailingIcon = if (vin.isNotEmpty()) {
                        {
                            IconButton(onClick = { vin = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    } else null,
                    supportingText = {
                        if (vin.isNotEmpty()) {
                            Text("${vin.length}/17")
                        }
                    },
                    isError = (vin.isNotEmpty() && vin.length != 17) || showVinError
                )

                ExposedDropdownMenuBox(
                    expanded = vehicleTypeExpanded,
                    onExpandedChange = { vehicleTypeExpanded = it },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = getVehicleTypeLabel(context, vehicleType),
                        onValueChange = { },
                        readOnly = true,
                        label = { Text(stringResource(R.string.car_vehicle_type_label)) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, true).fillMaxWidth()
                            .onGloballyPositioned { vehicleTypeWidth = with(density) { it.size.width.toDp() } },
                        trailingIcon = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (vehicleType.isNotEmpty()) {
                                    IconButton(onClick = { vehicleType = "" }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                                    }
                                }
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = vehicleTypeExpanded)
                            }
                        },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                    )
                    if (vehicleTypeExpanded) {
                        Popup(
                            onDismissRequest = { vehicleTypeExpanded = false },
                            popupPositionProvider = DropdownPositionProvider(),
                            properties = PopupProperties(focusable = false, clippingEnabled = false)
                        ) {
                            Surface(
                                modifier = Modifier.width(vehicleTypeWidth).heightIn(max = 300.dp),
                                shape = RoundedCornerShape(4.dp),
                                tonalElevation = 3.dp,
                                shadowElevation = 3.dp
                            ) {
                                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
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
                        }
                    }
                }


                ExposedDropdownMenuBox(
                    expanded = manufacturingCountryExpanded,
                    onExpandedChange = { manufacturingCountryExpanded = it },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = europeanCountries.find { it.name == manufacturingCountry }?.let { CarTranslations.getCountryName(context, it.code, it.name) } ?: manufacturingCountry,
                        onValueChange = { },
                        readOnly = true,
                        label = { Text(stringResource(R.string.car_manufacturing_country_label)) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, true).fillMaxWidth()
                            .onGloballyPositioned { manufacturingCountryWidth = with(density) { it.size.width.toDp() } },
                        trailingIcon = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (manufacturingCountry.isNotEmpty()) {
                                    IconButton(onClick = { manufacturingCountry = "" }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                                    }
                                }
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = manufacturingCountryExpanded)
                            }
                        },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                    )
                    if (manufacturingCountryExpanded) {
                        Popup(
                            onDismissRequest = { manufacturingCountryExpanded = false },
                            popupPositionProvider = DropdownPositionProvider(),
                            properties = PopupProperties(focusable = false, clippingEnabled = false)
                        ) {
                            Surface(
                                modifier = Modifier.width(manufacturingCountryWidth).heightIn(max = 300.dp),
                                shape = RoundedCornerShape(4.dp),
                                tonalElevation = 3.dp,
                                shadowElevation = 3.dp
                            ) {
                                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                                    sortedCountries.forEach { country ->
                                        DropdownMenuItem(
                                            text = {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(country.flag)
                                                    Spacer(Modifier.width(8.dp))
                                                    Text(CarTranslations.getCountryName(context, country.code, country.name))
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
                    }
                }
            }


            // --- 2. ENGINE & TRANSMISSION ---
            CollapsibleSection(
                title = stringResource(R.string.car_engine_transmission_section),
                isExpanded = engineExpanded,
                onToggle = { engineExpanded = !engineExpanded }
            ) {
                // Engine Capacity (Full width)
                OutlinedTextField(
                    value = engineSize,
                    onValueChange = { engineSize = it },
                    label = { Text(stringResource(R.string.car_engine_size_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    suffix = { Text(if (context.resources.configuration.locales[0].language == "ro") "cmc" else "cc") },
                    trailingIcon = if (engineSize.isNotEmpty()) {
                        {
                            IconButton(onClick = { engineSize = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    } else null
                )


                // Fuel & Injection System
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ExposedDropdownMenuBox(
                        expanded = fuelTypeExpanded,
                        onExpandedChange = { fuelTypeExpanded = it },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = getFuelTypeLabel(context, fuelType),
                            onValueChange = { },
                            readOnly = true,
                            label = { Text(stringResource(R.string.car_fuel_type_label)) },
                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, true).fillMaxWidth()
                                .onGloballyPositioned { fuelTypeWidth = with(density) { it.size.width.toDp() } },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = fuelTypeExpanded) },
                            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                        )
                        if (fuelTypeExpanded) {
                            Popup(
                                onDismissRequest = { fuelTypeExpanded = false },
                                popupPositionProvider = DropdownPositionProvider(),
                                properties = PopupProperties(focusable = false, clippingEnabled = false)
                            ) {
                                Surface(
                                    modifier = Modifier.width(fuelTypeWidth).heightIn(max = 300.dp),
                                    shape = RoundedCornerShape(4.dp),
                                    tonalElevation = 3.dp,
                                    shadowElevation = 3.dp
                                ) {
                                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
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
                            }
                        }
                    }

                    if (fuelType == "Petrol" || fuelType == "LPG" || fuelType == "Diesel") {
                        var fuelSystemExpanded by remember { mutableStateOf(false) }
                        val fuelSystemOptions = when (fuelType) {
                            "Petrol", "LPG" -> listOf("Carburetor", "Multi Point Injection", "Direct Injection")
                            "Diesel" -> listOf("Injection Pump", "Pumpe Duse", "Common Rail")
                            else -> emptyList()
                        }

                        ExposedDropdownMenuBox(
                            expanded = fuelSystemExpanded,
                            onExpandedChange = { fuelSystemExpanded = it },
                            modifier = Modifier.weight(1f)
                        ) {
                            OutlinedTextField(
                                value = getFuelSystemLabel(context, fuelSystem),
                                onValueChange = { },
                                readOnly = true,
                                label = { Text(if (fuelType == "Diesel") stringResource(R.string.car_fuel_system_label) else stringResource(R.string.car_injection_system_label)) },
                                modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, true).fillMaxWidth()
                                    .onGloballyPositioned { fuelSystemWidth = with(density) { it.size.width.toDp() } },
                                trailingIcon = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (fuelSystem.isNotEmpty()) {
                                            IconButton(onClick = { fuelSystem = "" }) {
                                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                                            }
                                        }
                                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = fuelSystemExpanded)
                                    }
                                },
                                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                            )
                            if (fuelSystemExpanded) {
                                Popup(
                                    onDismissRequest = { fuelSystemExpanded = false },
                                    popupPositionProvider = DropdownPositionProvider(),
                                    properties = PopupProperties(focusable = false, clippingEnabled = false)
                                ) {
                                    Surface(
                                        modifier = Modifier.width(fuelSystemWidth).heightIn(max = 300.dp),
                                        shape = RoundedCornerShape(4.dp),
                                        tonalElevation = 3.dp,
                                        shadowElevation = 3.dp
                                    ) {
                                        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
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
                            }
                        }
                    }
                }


                // Aspiration
                ExposedDropdownMenuBox(
                    expanded = aspirationExpanded,
                    onExpandedChange = { aspirationExpanded = it },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = getAspirationLabel(context, aspiration),
                        onValueChange = { },
                        readOnly = true,
                        label = { Text(stringResource(R.string.car_aspiration_label)) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, true).fillMaxWidth()
                            .onGloballyPositioned { aspirationWidth = with(density) { it.size.width.toDp() } },
                        trailingIcon = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (aspiration.isNotEmpty()) {
                                    IconButton(onClick = { aspiration = "" }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                                    }
                                }
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = aspirationExpanded)
                            }
                        },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                    )
                    if (aspirationExpanded) {
                        Popup(
                            onDismissRequest = { aspirationExpanded = false },
                            popupPositionProvider = DropdownPositionProvider(),
                            properties = PopupProperties(focusable = false, clippingEnabled = false)
                        ) {
                            Surface(
                                modifier = Modifier.width(aspirationWidth).heightIn(max = 300.dp),
                                shape = RoundedCornerShape(4.dp),
                                tonalElevation = 3.dp,
                                shadowElevation = 3.dp
                            ) {
                                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
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
                        }
                    }
                }


                // Power & Torque
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = power,
                        onValueChange = { if (it.all { char -> char.isDigit() }) power = it },
                        label = { Text(stringResource(R.string.car_power_label)) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(28.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    Spacer(Modifier.width(8.dp))
                    ExposedDropdownMenuBox(
                        expanded = powerUnitExpanded,
                        onExpandedChange = { powerUnitExpanded = it },
                        modifier = Modifier.width(100.dp)
                    ) {
                        OutlinedTextField(
                            value = getPowerUnitLabel(context, powerUnit),
                            onValueChange = { },
                            readOnly = true,
                            label = { Text(stringResource(R.string.common_unit)) },
                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, true).fillMaxWidth()
                                .onGloballyPositioned { powerUnitWidth = with(density) { it.size.width.toDp() } },
                            trailingIcon = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (powerUnit.isNotEmpty()) {
                                        IconButton(onClick = { powerUnit = "" }) {
                                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                                        }
                                    }
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = powerUnitExpanded)
                                }
                            },
                            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                        )
                        if (powerUnitExpanded) {
                            Popup(
                                onDismissRequest = { powerUnitExpanded = false },
                                popupPositionProvider = DropdownPositionProvider(),
                                properties = PopupProperties(focusable = false, clippingEnabled = false)
                            ) {
                                Surface(
                                    modifier = Modifier.width(powerUnitWidth).heightIn(max = 300.dp),
                                    shape = RoundedCornerShape(4.dp),
                                    tonalElevation = 3.dp,
                                    shadowElevation = 3.dp
                                ) {
                                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                                        powerUnits.forEach { unit ->
                                            DropdownMenuItem(
                                                text = { Text(getPowerUnitLabel(context, unit)) },
                                                onClick = {
                                                    powerUnit = unit
                                                    powerUnitExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }


                OutlinedTextField(
                    value = torque,
                    onValueChange = { if (it.all { char -> char.isDigit() }) torque = it },
                    label = { Text(stringResource(R.string.car_torque_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    suffix = { Text("Nm") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    trailingIcon = if (torque.isNotEmpty()) {
                        {
                            IconButton(onClick = { torque = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    } else null
                )


                // Engine Code
                OutlinedTextField(
                    value = engineCode,
                    onValueChange = { engineCode = it.uppercase() },
                    label = { Text(stringResource(R.string.car_engine_code_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    trailingIcon = if (engineCode.isNotEmpty()) {
                        {
                            IconButton(onClick = { engineCode = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    } else null
                )


                // Engine Layout (Dispunere)
                ExposedDropdownMenuBox(
                    expanded = engineLayoutExpanded,
                    onExpandedChange = { engineLayoutExpanded = it },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = getEngineLayoutLabel(context, engineLayout),
                        onValueChange = { },
                        readOnly = true,
                        label = { Text(stringResource(R.string.car_engine_layout_label)) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, true).fillMaxWidth()
                            .onGloballyPositioned { engineLayoutWidth = with(density) { it.size.width.toDp() } },
                        trailingIcon = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (engineLayout.isNotEmpty()) {
                                    IconButton(onClick = { engineLayout = "" }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                                    }
                                }
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = engineLayoutExpanded)
                            }
                        },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                    )
                    if (engineLayoutExpanded) {
                        Popup(
                            onDismissRequest = { engineLayoutExpanded = false },
                            popupPositionProvider = DropdownPositionProvider(),
                            properties = PopupProperties(focusable = false, clippingEnabled = false)
                        ) {
                            Surface(
                                modifier = Modifier.width(engineLayoutWidth).heightIn(max = 300.dp),
                                shape = RoundedCornerShape(4.dp),
                                tonalElevation = 3.dp,
                                shadowElevation = 3.dp
                            ) {
                                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
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
                        }
                    }
                    }


                // Cylinders & Valves per Cylinder
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = numberOfCylinders,
                        onValueChange = { if (it.all { char -> char.isDigit() }) numberOfCylinders = it },
                        label = { Text(stringResource(R.string.car_cylinders_label)) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        trailingIcon = if (numberOfCylinders.isNotEmpty()) {
                            {
                                IconButton(onClick = { numberOfCylinders = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                                }
                            }
                        } else null
                    )
                    OutlinedTextField(
                        value = valvesPerCylinder,
                        onValueChange = { if (it.all { char -> char.isDigit() }) valvesPerCylinder = it },
                        label = { Text(stringResource(R.string.car_valves_per_cyl_label)) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        trailingIcon = if (valvesPerCylinder.isNotEmpty()) {
                            {
                                IconButton(onClick = { valvesPerCylinder = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                                }
                            }
                        } else null
                    )
                }


                // Cylinder Configuration
                ExposedDropdownMenuBox(
                    expanded = cylinderLayoutExpanded,
                    onExpandedChange = { cylinderLayoutExpanded = it },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = getCylinderLayoutLabel(context, cylinderLayout),
                        onValueChange = { },
                        readOnly = true,
                        label = { Text(stringResource(R.string.car_cylinder_layout_label)) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, true).fillMaxWidth()
                            .onGloballyPositioned { cylinderLayoutWidth = with(density) { it.size.width.toDp() } },
                        trailingIcon = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (cylinderLayout.isNotEmpty()) {
                                    IconButton(onClick = { cylinderLayout = "" }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                                    }
                                }
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = cylinderLayoutExpanded)
                            }
                        },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                    )
                    if (cylinderLayoutExpanded) {
                        Popup(
                            onDismissRequest = { cylinderLayoutExpanded = false },
                            popupPositionProvider = DropdownPositionProvider(),
                            properties = PopupProperties(focusable = false, clippingEnabled = false)
                        ) {
                            Surface(
                                modifier = Modifier.width(cylinderLayoutWidth).heightIn(max = 300.dp),
                                shape = RoundedCornerShape(4.dp),
                                tonalElevation = 3.dp,
                                shadowElevation = 3.dp
                            ) {
                                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
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
                        }
                    }
                    }


                // Performance
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = acceleration0to100,
                        onValueChange = { if (it.all { char -> char.isDigit() || char == '.' }) acceleration0to100 = it },
                        label = { Text(stringResource(R.string.car_acceleration_label)) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        suffix = { Text("sec") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        trailingIcon = if (acceleration0to100.isNotEmpty()) {
                            {
                                IconButton(onClick = { acceleration0to100 = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                                }
                            }
                        } else null
                    )
                    OutlinedTextField(
                        value = topSpeed,
                        onValueChange = { if (it.all { char -> char.isDigit() }) topSpeed = it },
                        label = { Text(stringResource(R.string.car_top_speed_label)) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        suffix = { Text(if (usesMiles) "mph" else "km/h") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        trailingIcon = if (topSpeed.isNotEmpty()) {
                            {
                                IconButton(onClick = { topSpeed = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                                }
                            }
                        } else null
                    )
                }



                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ExposedDropdownMenuBox(
                        expanded = emissionStandardExpanded,
                        onExpandedChange = { emissionStandardExpanded = it },
                        modifier = Modifier.weight(1.3f)
                    ) {
                        OutlinedTextField(
                            value = getEmissionStandardLabel(context, emissionStandard),
                            onValueChange = { },
                            readOnly = true,
                            label = { Text(stringResource(R.string.car_emission_standard_label)) },
                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, true).fillMaxWidth()
                                .onGloballyPositioned { emissionStandardWidth = with(density) { it.size.width.toDp() } },
                            trailingIcon = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (emissionStandard.isNotEmpty()) {
                                        IconButton(onClick = { emissionStandard = "" }) {
                                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                                        }
                                    }
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = emissionStandardExpanded)
                                }
                            },
                            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                        )
                        if (emissionStandardExpanded) {
                            Popup(
                                onDismissRequest = { emissionStandardExpanded = false },
                                popupPositionProvider = DropdownPositionProvider(),
                                properties = PopupProperties(focusable = false, clippingEnabled = false)
                            ) {
                                Surface(
                                    modifier = Modifier.width(emissionStandardWidth).heightIn(max = 300.dp),
                                    shape = RoundedCornerShape(4.dp),
                                    tonalElevation = 3.dp,
                                    shadowElevation = 3.dp
                                ) {
                                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
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
                            }
                        }
                    }

                    OutlinedTextField(
                        value = co2Emissions,
                        onValueChange = { if (it.all { char -> char.isDigit() }) co2Emissions = it },
                        label = { Text(stringResource(R.string.car_co2_label)) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        suffix = { Text("g/km") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        trailingIcon = if (co2Emissions.isNotEmpty()) {
                            {
                                IconButton(onClick = { co2Emissions = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                                }
                            }
                        } else null
                    )
                }


                // Consumption Section
                Text(
                    text = stringResource(R.string.car_fuel_consumption),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = fuelConsumptionUrban,
                        onValueChange = { if (it.all { char -> char.isDigit() || char == '.' }) fuelConsumptionUrban = it },
                        label = { AutoSizeText(text = stringResource(R.string.car_consumption_urban_label), style = MaterialTheme.typography.bodyMedium, minFontSize = 9.sp) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        suffix = { Text(consumptionUnit) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        trailingIcon = if (fuelConsumptionUrban.isNotEmpty()) {
                            {
                                IconButton(onClick = { fuelConsumptionUrban = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                                }
                            }
                        } else null
                    )
                    OutlinedTextField(
                        value = fuelConsumptionExtraUrban,
                        onValueChange = { if (it.all { char -> char.isDigit() || char == '.' }) fuelConsumptionExtraUrban = it },
                        label = { AutoSizeText(text = stringResource(R.string.car_consumption_extra_urban_label), style = MaterialTheme.typography.bodyMedium, minFontSize = 9.sp) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        suffix = { Text(consumptionUnit) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        trailingIcon = if (fuelConsumptionExtraUrban.isNotEmpty()) {
                            {
                                IconButton(onClick = { fuelConsumptionExtraUrban = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                                }
                            }
                        } else null
                    )
                    OutlinedTextField(
                        value = fuelConsumptionCombined,
                        onValueChange = { if (it.all { char -> char.isDigit() || char == '.' }) fuelConsumptionCombined = it },
                        label = { AutoSizeText(text = stringResource(R.string.car_consumption_label), style = MaterialTheme.typography.bodyMedium, minFontSize = 9.sp) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        suffix = { Text(consumptionUnit) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        trailingIcon = if (fuelConsumptionCombined.isNotEmpty()) {
                            {
                                IconButton(onClick = { fuelConsumptionCombined = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                                }
                            }
                        } else null
                    )
                }

    
                // Gearbox & Gears
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ExposedDropdownMenuBox(
                        expanded = gearboxTypeExpanded,
                        onExpandedChange = { gearboxTypeExpanded = it },
                        modifier = Modifier.weight(1.5f)
                    ) {
                        OutlinedTextField(
                            value = getGearboxTypeLabel(context, gearboxType),
                            onValueChange = { },
                            readOnly = true,
                            label = { Text(stringResource(R.string.car_gearbox_type_label)) },
                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, true).fillMaxWidth()
                                .onGloballyPositioned { gearboxTypeWidth = with(density) { it.size.width.toDp() } },
                            trailingIcon = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (gearboxType.isNotEmpty()) {
                                        IconButton(onClick = { gearboxType = "" }) {
                                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                                        }
                                    }
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = gearboxTypeExpanded)
                                }
                            },
                            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                        )
                        if (gearboxTypeExpanded) {
                            Popup(
                                onDismissRequest = { gearboxTypeExpanded = false },
                                popupPositionProvider = DropdownPositionProvider(),
                                properties = PopupProperties(focusable = false, clippingEnabled = false)
                            ) {
                                Surface(
                                    modifier = Modifier.width(gearboxTypeWidth).heightIn(max = 300.dp),
                                    shape = RoundedCornerShape(4.dp),
                                    tonalElevation = 3.dp,
                                    shadowElevation = 3.dp
                                ) {
                                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
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
                            }
                        }
                    }

                    OutlinedTextField(
                        value = gears,
                        onValueChange = { gears = it },
                        label = { Text(stringResource(R.string.car_gears_label)) },
                        modifier = Modifier.weight(0.8f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        trailingIcon = if (gears.isNotEmpty()) {
                            {
                                IconButton(onClick = { gears = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                                }
                            }
                        } else null
                    )
                }


                // Drivetrain
                ExposedDropdownMenuBox(
                    expanded = drivetrainExpanded,
                    onExpandedChange = { drivetrainExpanded = it },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = getDrivetrainLabel(context, drivetrain),
                        onValueChange = { },
                        readOnly = true,
                        label = { Text(stringResource(R.string.car_drivetrain_label)) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, true).fillMaxWidth()
                            .onGloballyPositioned { drivetrainWidth = with(density) { it.size.width.toDp() } },
                        trailingIcon = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (drivetrain.isNotEmpty()) {
                                    IconButton(onClick = { drivetrain = "" }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                                    }
                                }
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = drivetrainExpanded)
                            }
                        },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                    )
                    if (drivetrainExpanded) {
                        Popup(
                            onDismissRequest = { drivetrainExpanded = false },
                            popupPositionProvider = DropdownPositionProvider(),
                            properties = PopupProperties(focusable = false, clippingEnabled = false)
                        ) {
                            Surface(
                                modifier = Modifier.width(drivetrainWidth).heightIn(max = 300.dp),
                                shape = RoundedCornerShape(4.dp),
                                tonalElevation = 3.dp,
                                shadowElevation = 3.dp
                            ) {
                                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
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
                    }
                    }


                // Suspension
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ExposedDropdownMenuBox(
                        expanded = frontSuspensionExpanded,
                        onExpandedChange = { frontSuspensionExpanded = it },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = getSuspensionLabel(context, frontSuspension),
                            onValueChange = { },
                            readOnly = true,
                            label = { Text(stringResource(R.string.car_front_suspension_label)) },
                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, true).fillMaxWidth()
                                .onGloballyPositioned { frontSuspensionWidth = with(density) { it.size.width.toDp() } },
                            trailingIcon = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (frontSuspension.isNotEmpty()) {
                                        IconButton(onClick = { frontSuspension = "" }) {
                                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                                        }
                                    }
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = frontSuspensionExpanded)
                                }
                            },
                            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                        )
                        if (frontSuspensionExpanded) {
                            Popup(
                                onDismissRequest = { frontSuspensionExpanded = false },
                                popupPositionProvider = DropdownPositionProvider(),
                                properties = PopupProperties(focusable = false, clippingEnabled = false)
                            ) {
                                Surface(
                                    modifier = Modifier.width(frontSuspensionWidth).heightIn(max = 300.dp),
                                    shape = RoundedCornerShape(4.dp),
                                    tonalElevation = 3.dp,
                                    shadowElevation = 3.dp
                                ) {
                                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
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
                            }
                        }
                    }
                    ExposedDropdownMenuBox(
                        expanded = rearSuspensionExpanded,
                        onExpandedChange = { rearSuspensionExpanded = it },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = getSuspensionLabel(context, rearSuspension),
                            onValueChange = { },
                            readOnly = true,
                                label = { Text(stringResource(R.string.car_rear_suspension_label)) },
                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, true).fillMaxWidth()
                                .onGloballyPositioned { rearSuspensionWidth = with(density) { it.size.width.toDp() } },
                            trailingIcon = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (rearSuspension.isNotEmpty()) {
                                        IconButton(onClick = { rearSuspension = "" }) {
                                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                                        }
                                    }
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = rearSuspensionExpanded)
                                }
                            },
                            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                        )
                            if (rearSuspensionExpanded) {
                            Popup(
                                onDismissRequest = { rearSuspensionExpanded = false },
                                popupPositionProvider = DropdownPositionProvider(),
                                properties = PopupProperties(focusable = false, clippingEnabled = false)
                            ) {
                                Surface(
                                    modifier = Modifier.width(rearSuspensionWidth).heightIn(max = 300.dp),
                                    shape = RoundedCornerShape(4.dp),
                                    tonalElevation = 3.dp,
                                    shadowElevation = 3.dp
                                ) {
                                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
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
                        }
                    }
                }


                // Brakes
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ExposedDropdownMenuBox(
                        expanded = frontBrakesExpanded,
                        onExpandedChange = { frontBrakesExpanded = it },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = getBrakesLabel(context, frontBrakes),
                            onValueChange = { },
                            readOnly = true,
                                label = { Text(stringResource(R.string.car_front_brakes_label)) },
                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, true).fillMaxWidth()
                                .onGloballyPositioned { frontBrakesWidth = with(density) { it.size.width.toDp() } },
                            trailingIcon = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (frontBrakes.isNotEmpty()) {
                                        IconButton(onClick = { frontBrakes = "" }) {
                                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                                        }
                                    }
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = frontBrakesExpanded)
                                }
                            },
                            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                        )
                            if (frontBrakesExpanded) {
                            Popup(
                                onDismissRequest = { frontBrakesExpanded = false },
                                popupPositionProvider = DropdownPositionProvider(),
                                properties = PopupProperties(focusable = false, clippingEnabled = false)
                            ) {
                                Surface(
                                    modifier = Modifier.width(frontBrakesWidth).heightIn(max = 300.dp),
                                    shape = RoundedCornerShape(4.dp),
                                    tonalElevation = 3.dp,
                                    shadowElevation = 3.dp
                                ) {
                                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
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
                            }
                        }
                    }
                    ExposedDropdownMenuBox(
                        expanded = rearBrakesExpanded,
                        onExpandedChange = { rearBrakesExpanded = it },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = getBrakesLabel(context, rearBrakes),
                            onValueChange = { },
                            readOnly = true,
                                label = { Text(stringResource(R.string.car_rear_brakes_label)) },
                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, true).fillMaxWidth()
                                .onGloballyPositioned { rearBrakesWidth = with(density) { it.size.width.toDp() } },
                            trailingIcon = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (rearBrakes.isNotEmpty()) {
                                        IconButton(onClick = { rearBrakes = "" }) {
                                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                                        }
                                    }
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = rearBrakesExpanded)
                                }
                            },
                            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                        )
                            if (rearBrakesExpanded) {
                            Popup(
                                onDismissRequest = { rearBrakesExpanded = false },
                                popupPositionProvider = DropdownPositionProvider(),
                                properties = PopupProperties(focusable = false, clippingEnabled = false)
                            ) {
                                Surface(
                                    modifier = Modifier.width(rearBrakesWidth).heightIn(max = 300.dp),
                                    shape = RoundedCornerShape(4.dp),
                                    tonalElevation = 3.dp,
                                    shadowElevation = 3.dp
                                ) {
                                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
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
                        }
                    }
                }
            }


            // --- 3. DIMENSIONS & CHASSIS ---
            CollapsibleSection(
                title = stringResource(R.string.car_dimensions_section),
                isExpanded = dimensionsExpanded,
                onToggle = { dimensionsExpanded = !dimensionsExpanded }
            ) {
                Text(
                    text = stringResource(R.string.car_tire_size_label),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = tireWidth,
                        onValueChange = { if (it.all { char -> char.isDigit() }) tireWidth = it },
                        label = { AutoSizeText(text = stringResource(R.string.car_tire_width_label), style = MaterialTheme.typography.bodyMedium, minFontSize = 10.sp) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        suffix = { Text("mm") },
                        trailingIcon = if (tireWidth.isNotEmpty()) {
                            {
                                IconButton(onClick = { tireWidth = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                                }
                            }
                        } else null
                    )
                    OutlinedTextField(
                        value = tireAspectRatio,
                        onValueChange = { if (it.all { char -> char.isDigit() }) tireAspectRatio = it },
                        label = { AutoSizeText(text = stringResource(R.string.car_tire_ratio_label), style = MaterialTheme.typography.bodyMedium, minFontSize = 10.sp) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        suffix = { Text("%") },
                        trailingIcon = if (tireAspectRatio.isNotEmpty()) {
                            {
                                IconButton(onClick = { tireAspectRatio = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                                }
                            }
                        } else null
                    )
                    OutlinedTextField(
                        value = tireDiameter,
                        onValueChange = { if (it.all { char -> char.isDigit() }) tireDiameter = it },
                        label = { AutoSizeText(text = stringResource(R.string.car_tire_diam_label), style = MaterialTheme.typography.bodyMedium, minFontSize = 10.sp) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        suffix = { Text("\"") },
                        trailingIcon = if (tireDiameter.isNotEmpty()) {
                            {
                                IconButton(onClick = { tireDiameter = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                                }
                            }
                        } else null
                    )
                }

                Spacer(Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = length,
                        onValueChange = { if (it.all { char -> char.isDigit() }) length = it },
                        label = { Text(stringResource(R.string.car_length_label)) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        suffix = { Text("mm") },
                        trailingIcon = if (length.isNotEmpty()) {
                            {
                                IconButton(onClick = { length = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                                }
                            }
                        } else null
                    )
                    OutlinedTextField(
                        value = width,
                        onValueChange = { if (it.all { char -> char.isDigit() }) width = it },
                        label = { Text(stringResource(R.string.car_width_label)) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        suffix = { Text("mm") },
                        trailingIcon = if (width.isNotEmpty()) {
                            {
                                IconButton(onClick = { width = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                                }
                            }
                        } else null
                    )
                }


                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = height,
                        onValueChange = { if (it.all { char -> char.isDigit() }) height = it },
                        label = { Text(stringResource(R.string.car_height_label)) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        suffix = { Text("mm") },
                        trailingIcon = if (height.isNotEmpty()) {
                            {
                                IconButton(onClick = { height = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                                }
                            }
                        } else null
                    )
                    OutlinedTextField(
                        value = wheelbase,
                        onValueChange = { if (it.all { char -> char.isDigit() }) wheelbase = it },
                        label = { Text(stringResource(R.string.car_wheelbase_label)) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        suffix = { Text("mm") },
                        trailingIcon = if (wheelbase.isNotEmpty()) {
                            {
                                IconButton(onClick = { wheelbase = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                                }
                            }
                        } else null
                    )
                }


                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = weight,
                        onValueChange = { if (it.all { char -> char.isDigit() }) weight = it },
                        label = { Text(stringResource(R.string.car_weight_label)) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        suffix = { Text("kg") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        trailingIcon = if (weight.isNotEmpty()) {
                            {
                                IconButton(onClick = { weight = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                                }
                            }
                        } else null
                    )
                    OutlinedTextField(
                        value = bootSpace,
                        onValueChange = { if (it.all { char -> char.isDigit() }) bootSpace = it },
                        label = { Text(stringResource(R.string.car_boot_label)) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        suffix = { Text("L") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        trailingIcon = if (bootSpace.isNotEmpty()) {
                            {
                                IconButton(onClick = { bootSpace = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                                }
                            }
                        } else null
                    )
                }


                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = numberOfSeats,
                        onValueChange = { if (it.all { char -> char.isDigit() }) numberOfSeats = it },
                        label = { Text(stringResource(R.string.car_seats_label)) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        trailingIcon = if (numberOfSeats.isNotEmpty()) {
                            {
                                IconButton(onClick = { numberOfSeats = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                                }
                            }
                        } else null
                    )
                    OutlinedTextField(
                        value = numberOfDoors,
                        onValueChange = { if (it.all { char -> char.isDigit() }) numberOfDoors = it },
                        label = { Text(stringResource(R.string.car_doors_label)) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        trailingIcon = if (numberOfDoors.isNotEmpty()) {
                            {
                                IconButton(onClick = { numberOfDoors = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                                }
                            }
                        } else null
                    )
                }


                // Capacities (Fuel Tank / Battery)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (fuelType != "Electric") {
                        OutlinedTextField(
                            value = fuelTankCapacity,
                            onValueChange = { if (it.all { char -> char.isDigit() || char == '.' }) fuelTankCapacity = it },
                            label = { Text(stringResource(R.string.car_fuel_tank_capacity_label)) },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            suffix = { Text("L") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            trailingIcon = if (fuelTankCapacity.isNotEmpty()) {
                                {
                                    IconButton(onClick = { fuelTankCapacity = "" }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                                    }
                                }
                            } else null
                        )
                    }

                    if (fuelType == "Hybrid" || fuelType == "Electric") {
                        OutlinedTextField(
                            value = batteryCapacity,
                            onValueChange = { if (it.all { char -> char.isDigit() || char == '.' }) batteryCapacity = it },
                            label = { Text(stringResource(R.string.car_battery_capacity_label)) },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            suffix = { Text("kWh") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            trailingIcon = if (batteryCapacity.isNotEmpty()) {
                                {
                                    IconButton(onClick = { batteryCapacity = "" }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                                    }
                                }
                            } else null
                        )
                    }
                }
            }


            // --- 4. SAFETY ---
            CollapsibleSection(
                title = stringResource(R.string.equip_cat_safety),
                isExpanded = safetyExpanded,
                onToggle = { safetyExpanded = !safetyExpanded }
            ) {
                // Airbags stays at top of this section as numeric
                OutlinedTextField(
                    value = airbags,
                    onValueChange = { if (it.all { char -> char.isDigit() }) airbags = it },
                    label = { Text(stringResource(R.string.car_airbags_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    trailingIcon = if (airbags.isNotEmpty()) {
                        {
                            IconButton(onClick = { airbags = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    } else null
                )

                Spacer(Modifier.height(8.dp))

                // Safety
                EquipmentGrid(safetyEquipment, selectedEquipments, onToggleEquipment)

                Spacer(Modifier.height(16.dp))

                // Parking
                EquipmentCategoryHeader(stringResource(R.string.equip_cat_parking))
                EquipmentGrid(parkingEquipment, selectedEquipments, onToggleEquipment)
            }

            // --- 5. EQUIPMENTS ---
            CollapsibleSection(
                title = stringResource(R.string.car_equipments_label),
                isExpanded = equipmentExpanded,
                onToggle = { equipmentExpanded = !equipmentExpanded }
            ) {
                // Comfort
                EquipmentCategoryHeader(stringResource(R.string.equip_cat_comfort))
                EquipmentGrid(comfortEquipment, selectedEquipments, onToggleEquipment)

                Spacer(Modifier.height(16.dp))

                // Tech
                EquipmentCategoryHeader(stringResource(R.string.equip_cat_tech))
                EquipmentGrid(techEquipment, selectedEquipments, onToggleEquipment)

                Spacer(Modifier.height(16.dp))

                // Exterior
                EquipmentCategoryHeader(stringResource(R.string.equip_cat_exterior))
                EquipmentGrid(exteriorEquipment, selectedEquipments, onToggleEquipment)
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ScanButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    state: AddCarState,
    label: String,
    loadingLabel: String? = null,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled && state !is AddCarState.Pending && state !is AddCarState.Scanning,
        contentPadding = PaddingValues(horizontal = 8.dp)
    ) {
        if (state is AddCarState.Scanning) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                if (loadingLabel != null) {
                    Spacer(Modifier.width(8.dp))
                    Text(loadingLabel, style = MaterialTheme.typography.labelSmall)
                }
            }
        } else {
            Text(label, textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun ScannedCarDataConfirmationDialog(
    data: ScannedCarData,
    existingData: Map<String, String>,
    usesMiles: Boolean,
    consumptionUnit: String,
    onDismiss: () -> Unit,
    onConfirm: (ScannedCarData) -> Unit
) {
    val context = LocalContext.current
    // We create a map of keys to values and labels for easy display, 
    // but ONLY for fields that are empty in the form.
    val fields = remember(data, existingData, usesMiles, consumptionUnit) {
        val list = mutableListOf<Triple<String, String, String>>() // Label, Value, Key
        
        fun shouldAdd(key: String) = existingData[key].isNullOrBlank()

        if (shouldAdd("make")) data.make?.let { list.add(Triple(context.getString(R.string.car_make_label), it, "make")) }
        if (shouldAdd("model")) data.model?.let { list.add(Triple(context.getString(R.string.car_model_label), it, "model")) }
        if (shouldAdd("vin")) data.vin?.let { list.add(Triple(context.getString(R.string.car_vin_label), it, "vin")) }
        if (shouldAdd("engineVariant")) data.engineVariant?.let { list.add(Triple(context.getString(R.string.car_engine_variant_label), it, "engineVariant")) }
        if (shouldAdd("year")) data.year?.let { list.add(Triple(context.getString(R.string.car_year_label), it.roundToInt().toString(), "year")) }
        if (shouldAdd("fuelType")) data.fuelType?.let { list.add(Triple(context.getString(R.string.car_fuel_type_label), getFuelTypeLabel(context, it), "fuelType")) }
        if (shouldAdd("engineSize")) data.engineSize?.let { list.add(Triple(context.getString(R.string.car_engine_size_label), "${it.roundToInt()} ${if (context.resources.configuration.locales[0].language == "ro") "cmc" else "cc"}", "engineSize")) }
        if (shouldAdd("power")) data.power?.let { list.add(Triple(context.getString(R.string.car_power_label), "${it.roundToInt()} ${data.powerUnit ?: "hp"}", "power")) }
        if (shouldAdd("torque")) data.torque?.let { list.add(Triple(context.getString(R.string.car_torque_label), "${it.roundToInt()} Nm", "torque")) }
        if (shouldAdd("color")) data.color?.let { list.add(Triple(context.getString(R.string.car_color_label), CarTranslations.getColorLabel(context, it), "color")) }
        if (shouldAdd("registrationPlate")) data.registrationPlate?.let { list.add(Triple(context.getString(R.string.car_license_plate_label), it, "registrationPlate")) }
        if (shouldAdd("numberOfSeats")) data.numberOfSeats?.let { list.add(Triple(context.getString(R.string.car_seats_label), it.roundToInt().toString(), "numberOfSeats")) }
        if (shouldAdd("numberOfDoors")) data.numberOfDoors?.let { list.add(Triple(context.getString(R.string.car_doors_label), it.roundToInt().toString(), "numberOfDoors")) }
        if (shouldAdd("weight")) data.weight?.let { list.add(Triple(context.getString(R.string.car_weight_label), "${it.roundToInt()} kg", "weight")) }
        if (shouldAdd("engineCode")) data.engineCode?.let { list.add(Triple(context.getString(R.string.car_engine_code_label), it, "engineCode")) }
        if (shouldAdd("emissionStandard")) data.emissionStandard?.let { list.add(Triple(context.getString(R.string.car_emission_standard_label), getEmissionStandardLabel(context, it), "emissionStandard")) }
        if (shouldAdd("gearboxType")) data.gearboxType?.let { list.add(Triple(context.getString(R.string.car_gearbox_type_label), getGearboxTypeLabel(context, it), "gearboxType")) }
        if (shouldAdd("drivetrain")) data.drivetrain?.let { list.add(Triple(context.getString(R.string.car_drivetrain_label), getDrivetrainLabel(context, it), "drivetrain")) }
        if (shouldAdd("engineLayout")) data.engineLayout?.let { list.add(Triple(context.getString(R.string.car_engine_layout_label), getEngineLayoutLabel(context, it), "engineLayout")) }
        if (shouldAdd("cylinderLayout")) data.cylinderLayout?.let { list.add(Triple(context.getString(R.string.car_cylinder_layout_label), getCylinderLayoutLabel(context, it), "cylinderLayout")) }
        if (shouldAdd("fuelTankCapacity")) data.fuelTankCapacity?.let { list.add(Triple(context.getString(R.string.car_fuel_tank_capacity_label), "$it L", "fuelTankCapacity")) }
        if (shouldAdd("topSpeed")) data.topSpeed?.let { 
            val displaySpeed = CarFormatters.fromCanonicalSpeed(it, usesMiles)
            list.add(Triple(context.getString(R.string.car_top_speed_label), "${displaySpeed.roundToInt()} ${if (usesMiles) "mph" else "km/h"}", "topSpeed")) 
        }
        if (shouldAdd("acceleration0to100")) data.acceleration0to100?.let { list.add(Triple(context.getString(R.string.car_acceleration_label), "$it sec", "acceleration0to100")) }
        if (shouldAdd("fuelConsumptionCombined")) data.fuelConsumptionCombined?.let { 
            val displayCons = CarFormatters.fromCanonicalConsumption(it, usesMiles)
            list.add(Triple(context.getString(R.string.car_consumption_label), String.format(java.util.Locale.US, "%.2f %s", displayCons, consumptionUnit), "fuelConsumptionCombined")) 
        }
        if (shouldAdd("co2Emissions")) data.co2Emissions?.let { list.add(Triple(context.getString(R.string.car_co2_label), "${it.roundToInt()} g/km", "co2Emissions")) }
        if (shouldAdd("hasAbs")) data.hasAbs?.let { list.add(Triple(context.getString(R.string.car_abs_label), if (it) context.getString(R.string.status_ok) else context.getString(R.string.common_none), "hasAbs")) }
        if (shouldAdd("hasEsp")) data.hasEsp?.let { list.add(Triple(context.getString(R.string.car_esp_label), if (it) context.getString(R.string.status_ok) else context.getString(R.string.common_none), "hasEsp")) }
        if (shouldAdd("airbags")) data.airbags?.let { list.add(Triple(context.getString(R.string.car_airbags_label), it.roundToInt().toString(), "airbags")) }
        list
    }

    if (fields.isEmpty()) {
        LaunchedEffect(Unit) {
            onConfirm(data)
        }
        return
    }

    var selectedKeys by remember { mutableStateOf(fields.asSequence().map { it.third }.toSet()) }

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
                        engineVariant = if ("engineVariant" in selectedKeys) data.engineVariant else null,
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
                        acceleration0to100 = if ("acceleration0to100" in selectedKeys) data.acceleration0to100 else null,
                        fuelConsumptionCombined = if ("fuelConsumptionCombined" in selectedKeys) data.fuelConsumptionCombined else null,
                        co2Emissions = if ("co2Emissions" in selectedKeys) data.co2Emissions else null,
                        hasAbs = if ("hasAbs" in selectedKeys) data.hasAbs else null,
                        hasEsp = if ("hasEsp" in selectedKeys) data.hasEsp else null,
                        airbags = if ("airbags" in selectedKeys) data.airbags else null,
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
private fun getPowerUnitLabel(context: android.content.Context, unit: String): String = CarTranslations.getPowerUnitLabel(context, unit)
private fun getVehicleTypeLabel(context: android.content.Context, type: String): String = CarTranslations.getVehicleTypeLabel(context, type)

@Composable
private fun CollapsibleSection(
    title: String,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp, 
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggle() },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isExpanded) stringResource(R.string.common_collapse) else stringResource(R.string.common_expand),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            if (isExpanded) {
                Spacer(Modifier.height(16.dp))
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    content()
                }
            }
        }
    }
}

@Composable
private fun EquipmentCategoryHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.secondary,
        modifier = Modifier.padding(vertical = 4.dp)
    )
}

@Composable
private fun EquipmentGrid(
    equipmentList: List<Pair<String, Int>>,
    selectedEquipments: Set<String>,
    onToggle: (String) -> Unit
) {
    equipmentList.chunked(2).forEach { rowItems ->
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            rowItems.forEach { (id, labelRes) ->
                val isSelected = id in selectedEquipments
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onToggle(id) }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = { onToggle(id) }
                    )
                    Text(
                        text = stringResource(labelRes),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            if (rowItems.size < 2) {
                Spacer(Modifier.weight(1f))
            }
        }
    }
}

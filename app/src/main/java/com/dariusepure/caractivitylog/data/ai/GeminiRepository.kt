package com.dariusepure.caractivitylog.data.ai

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Base64
import android.util.Log
import com.dariusepure.caractivitylog.BuildConfig
import com.dariusepure.caractivitylog.util.DiagnosticUtils
import com.dariusepure.caractivitylog.domain.ScannedCarData
import com.dariusepure.caractivitylog.domain.AiAnalysis
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GeminiRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val remoteConfig: FirebaseRemoteConfig,
    private val httpClient: HttpClient
) {

    init {
        remoteConfig.fetchAndActivate()
    }

    private val modelName: String
        get() = remoteConfig.getString("gemini_model_name").ifBlank { "gemini-1.5-flash" }

    private val temperature: Float
        get() = remoteConfig.getDouble("gemini_temperature").toFloat()

    private val systemPrompt: String
        get() = remoteConfig.getString("gemini_prompt")

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true
        allowSpecialFloatingPointValues = true
    }

    private val TAG = "GeminiRepository"

    private val updateCarTools = listOf(
        Tool(
            functionDeclarations = listOf(
                FunctionDeclaration(
                    name = "update_car_spec",
                    description = "Updates a specific technical specification of the car.",
                    parameters = buildJsonObject {
                        put("type", "object")
                        put("properties", buildJsonObject {
                            put("field", buildJsonObject {
                                put("type", "string")
                                put("description", "The technical field to update. Valid fields: make, model, vin, year, engineSize, fuelType, fuelSystem, color, power, torque, engineCode, engineLayout (Transverse, Longitudinal), cylinderLayout (Inline, V, W, Boxer), length, width, height, wheelbase, trackWidth, emissionStandard, aspiration, fuelTankCapacity, batteryCapacity, drivetrain, gearboxType, gears, frontSuspension (MacPherson, Double Wishbone, Multi-link), rearSuspension (Torsion Beam, Multi-link, Solid Axle), vehicleType, manufacturingCountry, topSpeed, weight, numberOfSeats, numberOfCylinders, valvesPerCylinder, numberOfDoors, bootSpace, tireWidth, tireAspectRatio, tireDiameter.")
                            })
                            put("value", buildJsonObject {
                                put("type", "string")
                                put("description", "The new value for the field. For dropdown fields, you MUST pick one of the standard English values provided in instructions.")
                            })
                        })
                        put("required", buildJsonArray {
                            add("field")
                            add("value")
                        })
                    }
                ),
                FunctionDeclaration(
                    name = "update_car_mileage",
                    description = "Updates the car's current mileage (odometer reading).",
                    parameters = buildJsonObject {
                        put("type", "object")
                        put("properties", buildJsonObject {
                            put("km", buildJsonObject {
                                put("type", "string")
                                put("description", "The current mileage in kilometers.")
                            })
                        })
                        put("required", buildJsonArray {
                            add("km")
                        })
                    }
                )
            )
        )
    )

    private suspend fun postGemini(request: GeminiRequest): GeminiResponse {
        val model = modelName
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=${BuildConfig.GEMINI_API_KEY}"
        val sha1 = DiagnosticUtils.getAppSignatureSha1(context, withColons = true)
        val packageName = context.packageName

        Log.d(TAG, "Requesting Gemini model: $model")
        Log.d(TAG, "Using Package: $packageName")
        Log.d(TAG, "Using real SHA1 (standard): $sha1")

        try {
            val response: io.ktor.client.statement.HttpResponse = httpClient.post(url) {
                contentType(ContentType.Application.Json)
                header("X-Android-Package", packageName)
                header("X-Android-Cert", sha1)
                setBody(request)
            }
            
            if (response.status.value !in 200..299) {
                val errorBody = response.body<String>()
                Log.e(TAG, "Gemini API Error (${response.status}): $errorBody")
                throw Exception("Gemini API Error ${response.status}: $errorBody")
            }

            return response.body()
        } catch (e: Exception) {
            Log.e(TAG, "Gemini Post failed", e)
            throw e
        }
    }

    suspend fun scanRegistrationCertificate(bitmap: Bitmap): Result<List<ScannedCarData>> {
        return try {
            val prompt = """
                Extract technical details from this vehicle document (registration certificate, invoice, insurance, or technical sheet).
                Analyze the document and look for these fields:
                - make, model, vin (MUST be 17 chars), year (4 digits), fuelType, engineSize (cc), power (hp or kW), torque (Nm), color, gears, registrationPlate.
                
                CRITICAL VALIDATION:
                1. Verify VIN format: must be 17 characters, only letters and digits (excluding I, O, Q).
                2. Verify Year: must be a realistic year (e.g., 1900-2026).
                3. Verify Engine Size: must be in cubic centimeters (cc).
                4. Brand (make) MUST be returned in UPPERCASE (e.g., "BMW", "VOLKSWAGEN").
                5. NUMERIC FIELDS (year, engineSize, power, torque, weight, capacity, speed, consumption, emissions, mileage, etc.) MUST contain ONLY the raw number, NO units (e.g., 230 instead of "230 Nm").
                6. If a value is unreadable, illogical, or not found, return null for that field.
                
                Return ONLY a JSON ARRAY containing one object with these keys: 
                make, model, vin, year, fuelType, engineSize, power, powerUnit, torque, color, 
                registrationPlate, numberOfSeats, numberOfDoors, weight, engineCode, 
                emissionStandard, gearboxType, gears, drivetrain, engineLayout, cylinderLayout, 
                fuelTankCapacity, topSpeed, acceleration0to100, fuelConsumptionCombined, co2Emissions,
                mileage,
                mileageHistory (a list of objects with 'km' and 'date' in YYYY-MM-DD format).
                
                Standard fuelType: Petrol, Diesel, Electric, Hybrid, LPG.
                Standard powerUnit: 'hp'. If kW is found, convert to hp (kW * 1.36).
            """.trimIndent()

            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
            val base64Image = Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)

            val request = GeminiRequest(
                contents = listOf(
                    Content(
                        parts = listOf(
                            Part(text = prompt),
                            Part(inlineData = Blob(mimeType = "image/jpeg", data = base64Image))
                        )
                    )
                ),
                generationConfig = GenerationConfig(temperature = temperature)
            )

            val response = postGemini(request)
            val fullText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull { it.text != null }?.text
                ?: throw Exception("Empty response from AI")

            val jsonText = extractJson(fullText)
            val data = if (jsonText.trim().startsWith("[")) {
                json.decodeFromString<List<ScannedCarData>>(jsonText)
            } else {
                listOf(json.decodeFromString<ScannedCarData>(jsonText))
            }
            Result.success(data)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun scanDocument(uri: Uri, mimeType: String): Result<List<ScannedCarData>> {
        return try {
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: throw Exception("Could not read file")
            val base64Data = Base64.encodeToString(bytes, Base64.NO_WRAP)

            val prompt = """
                Extract technical details from this vehicle document (registration certificate, invoice, insurance, or technical sheet).
                The document might have multiple pages or be a complex PDF. Scan all visible text carefully.
                Analyze the document and look for these fields:
                - make, model, vin (MUST be 17 chars), year (4 digits), fuelType, engineSize (cc), power (hp or kW), torque (Nm), color, gears, registrationPlate.
                
                CRITICAL VALIDATION:
                1. Verify VIN format: must be 17 characters, only letters and digits (excluding I, O, Q).
                2. Verify Year: must be a realistic year (e.g., 1900-2026).
                3. Verify Engine Size: must be in cubic centimeters (cc).
                4. Brand (make) MUST be returned in UPPERCASE (e.g., "BMW", "VOLKSWAGEN").
                5. NUMERIC FIELDS (year, engineSize, power, torque, weight, capacity, speed, consumption, emissions, mileage, etc.) MUST contain ONLY the raw number, NO units (e.g., 230 instead of "230 Nm").
                6. If a value is unreadable, illogical, or not found, return null for that field.
                
                Return ONLY a JSON ARRAY containing one object with these keys: 
                make, model, vin, year, fuelType, engineSize, power, powerUnit, torque, color, 
                registrationPlate, numberOfSeats, numberOfDoors, weight, engineCode, 
                emissionStandard, gearboxType, gears, drivetrain, engineLayout, cylinderLayout, 
                fuelTankCapacity, topSpeed, acceleration0to100, fuelConsumptionCombined, co2Emissions,
                mileage,
                mileageHistory (a list of objects with 'km' and 'date' in YYYY-MM-DD format).
                
                Standard fuelType: Petrol, Diesel, Electric, Hybrid, LPG.
                Standard powerUnit: 'hp'. If kW is found, convert to hp (kW * 1.36).
            """.trimIndent()

            val request = GeminiRequest(
                contents = listOf(
                    Content(
                        parts = listOf(
                            Part(text = prompt),
                            Part(inlineData = Blob(mimeType = mimeType, data = base64Data))
                        )
                    )
                ),
                generationConfig = GenerationConfig(temperature = temperature)
            )

            val response = postGemini(request)
            val fullText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull { it.text != null }?.text
                ?: throw Exception("Empty response from AI")

            val jsonText = extractJson(fullText)
            val data = if (jsonText.trim().startsWith("[")) {
                json.decodeFromString<List<ScannedCarData>>(jsonText)
            } else {
                listOf(json.decodeFromString<ScannedCarData>(jsonText))
            }
            Result.success(data)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getDiagnosisResponse(
        prompt: String,
        carContext: String,
        history: List<com.dariusepure.caractivitylog.ui.cars.ChatMessage>,
        language: String = "English"
    ): GeminiResponse {
        val validatedHistory = history
            .dropWhile { !it.isUser }
            .let { h ->
                if (h.isNotEmpty() && h.size % 2 != 0) h.dropLast(1) else h
            }
            .map {
                Content(
                    role = if (it.isUser) "user" else "model",
                    parts = listOf(Part(text = it.text))
                )
            }

        val finalizedPrompt = systemPrompt.replace("{{context}}", carContext)
        val languageInstruction = "\n\nIMPORTANT: Please respond in $language."
        val userContent = Content(
            role = "user",
            parts = listOf(Part(text = "$finalizedPrompt\n\nUser: $prompt$languageInstruction"))
        )

        val request = GeminiRequest(
            contents = validatedHistory + userContent,
            tools = updateCarTools,
            generationConfig = GenerationConfig(temperature = temperature)
        )

        return postGemini(request)
    }

    suspend fun generateHealthAnalysis(
        carContext: String,
        fuelHistory: String,
        maintenanceHistory: String,
        language: String = "English"
    ): Result<AiAnalysis> {
        return try {
            val prompt = """
                Analyze the health and maintenance state of this vehicle.
                
                CAR SPECS:
                $carContext
                
                RECENT FUEL CONSUMPTION (last 5 entries):
                $fuelHistory
                
                RECENT MAINTENANCE/SERVICE HISTORY:
                $maintenanceHistory
                
                INSTRUCTIONS:
                1. Evaluate if the fuel consumption trend is normal or increasing.
                2. Check if any major maintenance (oil, filters, timing belt) is due based on mileage and last service.
                3. Provide a 'healthScore' from 0 to 100.
                4. Provide a 'summary' (max 3 sentences).
                5. Provide a list of 'recommendations' (max 3 items).
                
                CRITICAL: Please provide the 'summary' and 'recommendations' in $language.
                
                Return ONLY a JSON object with these keys:
                summary, healthScore, recommendations.
            """.trimIndent()

            val request = GeminiRequest(
                contents = listOf(
                    Content(role = "user", parts = listOf(Part(text = prompt)))
                ),
                generationConfig = GenerationConfig(temperature = 0.4f) // Lower temperature for more analytical response
            )

            val response = postGemini(request)
            val fullText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull { it.text != null }?.text
                ?: throw Exception("Empty response from AI")

            val jsonText = extractJson(fullText)
            val analysis = json.decodeFromString<AiAnalysis>(jsonText)
            Result.success(analysis)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchTechnicalSpecs(
        make: String,
        model: String,
        year: Int,
        filters: Map<String, String> = emptyMap()
    ): Result<List<ScannedCarData>> {
        return try {
            val filterText = if (filters.isNotEmpty()) {
                "\nADDITIONAL FILTERS/HINTS provided by user:\n" + 
                filters.filterValues { it.isNotBlank() }.map { "${it.key}: ${it.value}" }.joinToString("\n")
            } else ""

            val prompt = """
                Provide detailed technical specifications for this car: $make $model ($year).
                Search and reference reliable sources like autodata.net to find the most accurate details for this specific model year.
                $filterText
                
                MANDATORY CONSTRAINT: 
                If any ADDITIONAL FILTERS/HINTS are provided above, you MUST return specifications for THAT EXACT VARIANT. 
                Do NOT suggest alternatives or generic variants for THOSE SPECIFIC FIELDS.
                
                MULTI-VARIANT SUPPORT:
                If multiple common variants (e.g., different engine power levels for the same displacement, or different gearboxes) match the user's filters, return up to 5 variations in a list.
                
                TECHNICAL STANDARDS (FOR DROPDOWN FIELDS - YOU MUST PICK EXACTLY FROM THESE LISTS):
                - fuelType: [Petrol, Diesel, Electric, Hybrid, LPG]
                - engineLayout: [Transverse, Longitudinal]
                - cylinderLayout: [Inline, V, W, Boxer]
                - aspiration: [Naturally Aspirated, Turbocharged, Supercharged, Twin-Turbo, Quad-Turbo, Electric]
                - emissionStandard: [Non-Euro, Euro 1, Euro 2, Euro 3, Euro 4, Euro 5, Euro 6]
                - gearboxType: [Manual, Automatic, CVT, DCT, AMT]
                - frontBrakes / rearBrakes: [Ventilated Discs, Solid Discs, Drums, Ceramic Discs]
                - frontSuspension: [MacPherson, Double Wishbone, Multi-link]
                - rearSuspension: [Torsion Beam, Multi-link, Solid Axle]
                - drivetrain: [FWD, RWD, AWD, 4WD]
                - vehicleType: [Saloon, Estate, Hatchback, MPV, SUV, Coupe, Convertible, Van, Pickup]
                - fuelSystem (Petrol/LPG): [Carburetor, Multi Point Injection, Direct Injection]
                - fuelSystem (Diesel): [Injection Pump, Pumpe Duse, Common Rail]
                - powerUnit: [hp, kw]
                
                MAPPING RULES:
                - Always map technical descriptions to the CLOSEST standard value from the lists above.
                - Do NOT invent new categories.
                - Example: "tractiune fata" -> value: "FWD".
                - Example: "cutie manuala" -> value: "Manual".
                - Example: "Direct injection" -> value: "Direct Injection".

                Return ONLY a JSON ARRAY containing objects with these keys: 
                make, model, year, fuelType, engineSize, power, powerUnit, torque, 
                numberOfSeats, numberOfDoors, weight, engineCode, 
                emissionStandard, gearboxType, gears, drivetrain, engineLayout, cylinderLayout, 
                aspiration, fuelTankCapacity, topSpeed, acceleration0to100, 
                fuelConsumptionCombined, co2Emissions, length, width, height, wheelbase,
                tireWidth, tireAspectRatio, tireDiameter.
                
                CRITICAL: 
                - Standard fuelType: Petrol, Diesel, Electric, Hybrid, LPG.
                - Standard powerUnit: 'hp'.
                - Dimensions in mm.
                - Consumption in L/100km.
                - Top speed in km/h.
                - Acceleration in seconds.
                - NUMERIC FIELDS (year, engineSize, power, torque, weight, capacity, speed, consumption, emissions, mileage, etc.) MUST contain ONLY the raw number, NO units (e.g., 230 instead of "230 Nm").
                - If specific data is unknown, use typical values for this model generation.
            """.trimIndent()

            val request = GeminiRequest(
                contents = listOf(
                    Content(parts = listOf(Part(text = prompt)))
                ),
                generationConfig = GenerationConfig(temperature = 0.2f)
            )

            val response = postGemini(request)
            val fullText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull { it.text != null }?.text
                ?: throw Exception("Empty response from AI")

            val jsonText = extractJson(fullText)
            val data = if (jsonText.trim().startsWith("[")) {
                json.decodeFromString<List<ScannedCarData>>(jsonText)
            } else {
                listOf(json.decodeFromString<ScannedCarData>(jsonText))
            }
            Result.success(data)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchModelInsights(make: String, model: String, year: Int, language: String = "English"): Result<String> {
        return try {
            val prompt = """
                Provide a technical summary and reliability report for the $make $model ($year).
                Include:
                1. A brief overview of this generation (max 2 sentences).
                2. COMMON ISSUES/FLAWS: List the most frequent mechanical or electrical problems users report (max 3 items).
                3. PRO TIPS: One specific maintenance recommendation for this model.
                
                Language: Respond in $language.
                Format: Plain text with bullet points. No JSON.
            """.trimIndent()

            val request = GeminiRequest(
                contents = listOf(
                    Content(parts = listOf(Part(text = prompt)))
                ),
                generationConfig = GenerationConfig(temperature = 0.5f)
            )

            val response = postGemini(request)
            val fullText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull { it.text != null }?.text
                ?: throw Exception("Empty response from AI")

            Result.success(fullText.trim())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun extractJson(text: String): String {
        val cleanedText = text.replace("```json", "").replace("```", "").trim()
        val startBrace = cleanedText.indexOf('{')
        val startBracket = cleanedText.indexOf('[')
        
        val start = when {
            startBrace != -1 && startBracket != -1 -> minOf(startBrace, startBracket)
            startBrace != -1 -> startBrace
            startBracket != -1 -> startBracket
            else -> -1
        }
        
        val endBrace = cleanedText.lastIndexOf('}')
        val endBracket = cleanedText.lastIndexOf(']')
        
        val end = when {
            endBrace != -1 && endBracket != -1 -> maxOf(endBrace, endBracket)
            endBrace != -1 -> endBrace
            endBracket != -1 -> endBracket
            else -> -1
        }

        if (start != -1 && end != -1 && end > start) {
            return cleanedText.substring(start, end + 1)
        }
        return cleanedText
    }
}

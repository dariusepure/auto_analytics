package com.dariusepure.caractivitylog.data.ai

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.dariusepure.caractivitylog.BuildConfig
import com.dariusepure.caractivitylog.domain.ScannedCarData
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.FunctionDeclaration
import com.google.ai.client.generativeai.type.Schema
import com.google.ai.client.generativeai.type.Tool
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.generationConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GeminiRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val remoteConfig: FirebaseRemoteConfig
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

    private val updateCarTools = listOf(
        Tool(
            listOf(
                FunctionDeclaration(
                    name = "update_car_spec",
                    description = "Updates a specific technical specification of the car.",
                    parameters = listOf(
                        Schema.str("field", "The technical field to update. Valid fields: make, model, vin, year, engineSize, fuelType, fuelSystem, color, power, torque, engineCode, engineLayout (Transverse, Longitudinal), cylinderLayout (Inline, V, W, Boxer), length, width, height, wheelbase, trackWidth, emissionStandard, aspiration, fuelTankCapacity, batteryCapacity, drivetrain, gearboxType, gears, frontSuspension (MacPherson, Double Wishbone, Multi-link), rearSuspension (Torsion Beam, Multi-link, Solid Axle), vehicleType, manufacturingCountry, topSpeed, weight, numberOfSeats, numberOfCylinders, valvesPerCylinder, numberOfDoors, bootSpace, tireWidth, tireAspectRatio, tireDiameter."),
                        Schema.str("value", "The new value for the field. For dropdown fields, you MUST pick one of the standard English values provided in instructions.")
                    ),
                    requiredParameters = listOf("field", "value")
                ),
                FunctionDeclaration(
                    name = "update_car_mileage",
                    description = "Updates the car's current mileage (odometer reading).",
                    parameters = listOf(
                        Schema.str("km", "The current mileage in kilometers.")
                    ),
                    requiredParameters = listOf("km")
                )
            )
        )
    )

    private fun getModel(tools: List<Tool>? = null): GenerativeModel {
        return GenerativeModel(
            modelName = modelName,
            apiKey = BuildConfig.GEMINI_API_KEY,
            generationConfig = generationConfig {
                this.temperature = this@GeminiRepository.temperature
            },
            tools = tools,
            systemInstruction = if (systemPrompt.isNotEmpty()) content { text(systemPrompt) } else null
        )
    }

    suspend fun scanRegistrationCertificate(bitmap: Bitmap): Result<ScannedCarData> {
        return try {
            val prompt = """
                Extract technical details from this vehicle document (registration certificate, invoice, insurance, or technical sheet).
                Analyze the document and look for these fields:
                - make, model, vin (MUST be 17 chars), year (4 digits), fuelType, engineSize (cc), power (hp or kW), torque (Nm), color, gears, registrationPlate.
                
                CRITICAL VALIDATION:
                1. Verify VIN format: must be 17 characters, only letters and digits (excluding I, O, Q).
                2. Verify Year: must be a realistic year (e.g., 1900-2026).
                3. Verify Engine Size: must be in cubic centimeters (cc).
                4. If a value is unreadable, illogical, or not found, return null for that field.
                
                Return ONLY a JSON object with these keys: 
                make, model, vin, year, fuelType, engineSize, power, powerUnit, torque, color, 
                registrationPlate, numberOfSeats, numberOfDoors, weight, engineCode, 
                emissionStandard, gearboxType, gears, drivetrain, engineLayout, cylinderLayout, 
                fuelTankCapacity, topSpeed, mileage,
                mileageHistory (a list of objects with 'km' and 'date' in YYYY-MM-DD format).
                
                Standard fuelType: Petrol, Diesel, Electric, Hybrid, LPG.
                Standard powerUnit: 'hp'. If kW is found, convert to hp (kW * 1.36).
            """.trimIndent()

            val inputContent = content {
                image(bitmap)
                text(prompt)
            }

            val scanModel = getModel()
            val response = scanModel.generateContent(inputContent)
            val fullText = response.text ?: throw Exception("Empty response from AI")
            
            val jsonText = extractJson(fullText)
            val data = json.decodeFromString<ScannedCarData>(jsonText)
            Result.success(data)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun scanDocument(uri: Uri, mimeType: String): Result<ScannedCarData> {
        return try {
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: throw Exception("Could not read file")

            val prompt = """
                Extract technical details from this vehicle document (registration certificate, invoice, insurance, or technical sheet).
                The document might have multiple pages or be a complex PDF. Scan all visible text carefully.
                Analyze the document and look for these fields:
                - make, model, vin (MUST be 17 chars), year (4 digits), fuelType, engineSize (cc), power (hp or kW), torque (Nm), color, gears, registrationPlate.
                
                CRITICAL VALIDATION:
                1. Verify VIN format: must be 17 characters, only letters and digits (excluding I, O, Q).
                2. Verify Year: must be a realistic year (e.g., 1900-2026).
                3. Verify Engine Size: must be in cubic centimeters (cc).
                4. If a value is unreadable, illogical, or not found, return null for that field.
                
                Return ONLY a JSON object with these keys: 
                make, model, vin, year, fuelType, engineSize, power, powerUnit, torque, color, 
                registrationPlate, numberOfSeats, numberOfDoors, weight, engineCode, 
                emissionStandard, gearboxType, gears, drivetrain, engineLayout, cylinderLayout, 
                fuelTankCapacity, topSpeed, mileage,
                mileageHistory (a list of objects with 'km' and 'date' in YYYY-MM-DD format).
                
                Standard fuelType: Petrol, Diesel, Electric, Hybrid, LPG.
                Standard powerUnit: 'hp'. If kW is found, convert to hp (kW * 1.36).
            """.trimIndent()

            val inputContent = content {
                blob(mimeType, bytes)
                text(prompt)
            }

            val scanModel = getModel()
            val response = scanModel.generateContent(inputContent)
            val fullText = response.text ?: throw Exception("Empty response from AI")
            
            val jsonText = extractJson(fullText)
            val data = json.decodeFromString<ScannedCarData>(jsonText)
            Result.success(data)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getDiagnosisResponse(
        prompt: String,
        carContext: String,
        history: List<com.dariusepure.caractivitylog.ui.cars.ChatMessage>
    ): com.google.ai.client.generativeai.type.GenerateContentResponse {
        val diagnosisModel = getModel(tools = updateCarTools)

        val validatedHistory = history
            .dropWhile { !it.isUser }
            .let { h ->
                if (h.isNotEmpty() && h.size % 2 != 0) h.dropLast(1) else h
            }
            .map { 
                content(if (it.isUser) "user" else "model") { text(it.text) }
            }

        val chat = diagnosisModel.startChat(history = validatedHistory)
        
        val finalizedPrompt = systemPrompt.replace("{{context}}", carContext)
        return chat.sendMessage(content("user") { text("$finalizedPrompt\n\nUser: $prompt") })
    }

    private fun extractJson(text: String): String {
        val cleanedText = text.replace("```json", "").replace("```", "").trim()
        val start = cleanedText.indexOf('{')
        val end = cleanedText.lastIndexOf('}')
        if (start != -1 && end != -1 && end > start) {
            return cleanedText.substring(start, end + 1)
        }
        return cleanedText
    }
}

package com.dariusepure.caractivitylog.di

import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.storage
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.remoteConfig
import com.google.firebase.remoteconfig.remoteConfigSettings
import com.dariusepure.caractivitylog.BuildConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.serialization.kotlinx.json.json
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideHttpClient(): HttpClient = HttpClient(CIO) {
        install(HttpTimeout) {
            connectTimeoutMillis = 15000
            socketTimeoutMillis = 15000
            requestTimeoutMillis = 30000
        }
        install(io.ktor.client.plugins.contentnegotiation.ContentNegotiation) {
            json(kotlinx.serialization.json.Json {
                ignoreUnknownKeys = true
                coerceInputValues = true
                isLenient = true
                allowSpecialFloatingPointValues = true
            })
        }
    }

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = Firebase.auth

    @Provides
    @Singleton
    fun provideFirebaseStorage(): FirebaseStorage = Firebase.storage

    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore {
        val firestore = Firebase.firestore
        firestore.firestoreSettings = com.google.firebase.firestore.firestoreSettings {
            setLocalCacheSettings(com.google.firebase.firestore.PersistentCacheSettings.newBuilder().build())
        }
        return firestore
    }

    @Provides
    @Singleton
    fun provideRemoteConfig(): FirebaseRemoteConfig {
        val remoteConfig = Firebase.remoteConfig
        val configSettings = remoteConfigSettings {
            minimumFetchIntervalInSeconds = if (BuildConfig.DEBUG) 0 else 3600
        }
        remoteConfig.setConfigSettingsAsync(configSettings)
        remoteConfig.setDefaultsAsync(mapOf(
            "gemini_model_name" to "gemini-3.5-flash-lite",
            "gemini_timeout_seconds" to 30L,
            "gemini_temperature" to 0.7f,
            "gemini_prompt" to """
                You are an expert car mechanic AI. You have access to the car's current specifications and mileage history.
                CONTEXT: {{context}}
                
                TECHNICAL STANDARDS (MUST pick from these lists for dropdown fields):
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
                - Always map user descriptions to the CLOSEST standard value from the lists above.
                - Do NOT invent new categories for these fields.
                - Example: User says "MPI" -> value: "Multi Point Injection".
                - Example: User says "rampa comuna" -> value: "Common Rail".
                - Example: User says "tractiune fata" -> value: "FWD".
                - Example: User says "cutie manuala" -> value: "Manual".
                
                YOU CAN:
                1. Analyze the car's state based on its specs and mileage.
                2. Suggest maintenance or fixes.
                3. Update car specifications using 'update_car_spec'. FOR DROPDOWN FIELDS, YOU MUST USE ONE OF THE STANDARD VALUES.
                4. Update the car's current mileage using 'update_car_mileage'.
                
                IMPORTANT: When calling a tool, always inform the user what you are changing or adding using the standard English terms.
            """.trimIndent()
        ))
        remoteConfig.fetchAndActivate()
        return remoteConfig
    }
}

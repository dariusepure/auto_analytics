import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// Machine-local, uncommitted config (secrets, signing) is read from
// local.properties. The Web client ID (Credential Manager sign-in) becomes a
// BuildConfig field; the RELEASE_* values configure release signing.
val localProperties = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

fun readWebClientId(): String {
    val fromProps = localProperties.getProperty("WEB_CLIENT_ID", "")
    if (fromProps.isNotEmpty()) return fromProps

    val googleServicesFile = project.file("google-services.json")
    if (googleServicesFile.exists()) {
        val content = googleServicesFile.readText()
        // Extract the client_id associated with client_type 3 (Web Client)
        val regex = "\"client_id\":\\s*\"([^\"]+)\",\\s*\"client_type\":\\s*3".toRegex()
        return regex.find(content)?.groupValues?.get(1) ?: ""
    }
    return ""
}

val webClientId: String = readWebClientId()

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics)
    alias(libs.plugins.firebase.appdistribution)
}

android {
    namespace = "com.dariusepure.caractivitylog"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.dariusepure.caractivitylog"
        minSdk = 24
        targetSdk = 35
        versionCode = 10
        versionName = "2.0"

        multiDexEnabled = true

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }
        buildConfigField("String", "WEB_CLIENT_ID", "\"$webClientId\"")
        
        val geminiKey: String = localProperties.getProperty("gemini.api.key", "")
        buildConfigField("String", "GEMINI_API_KEY", "\"$geminiKey\"")
    }

    signingConfigs {
        val keystorePath = localProperties.getProperty("RELEASE_KEYSTORE_PATH")
        val keystorePassword = localProperties.getProperty("RELEASE_KEYSTORE_PASSWORD")
        val keyAlias = localProperties.getProperty("RELEASE_KEYSTORE_ALIAS")
        val keyPassword = localProperties.getProperty("RELEASE_ALIAS_PASSWORD")

        val isSigningConfigured = keystorePath != null &&
                keystorePassword != null &&
                keyAlias != null &&
                keyPassword != null &&
                file(keystorePath).exists()

        if (isSigningConfigured) {
            create("release") {
                storeFile = file(keystorePath)
                storePassword = keystorePassword
                this.keyAlias = keyAlias
                this.keyPassword = keyPassword
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = if (signingConfigs.findByName("release") != null) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
            firebaseAppDistribution {
                artifactType = "APK"
                groups = "internal-testers"
                releaseNotes = "Car Activity Log"
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
        resValues = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    // AndroidX core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.auth.credentials)
    implementation(libs.androidx.auth.credentials.play)

    // Compose BOM ensures all compose libs use compatible versions
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.fonts)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.googleid)

    implementation(libs.androidx.work.runtime.ktx)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)

    // Other
    implementation(libs.kotlinx.serialization)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)

    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.storage)
    implementation(libs.google.generativeai)
    implementation(libs.firebase.appcheck.playintegrity)
    debugImplementation(libs.firebase.appcheck.debug)
    implementation(libs.firebase.crashlytics)
    implementation(libs.firebase.config)

    // Ktor Dependencies (Forțează importul corect al modulelor)
    val ktorVersion = "2.3.12"
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-cio:$ktorVersion")
    implementation("io.ktor:ktor-client-android:$ktorVersion")
    implementation("io.ktor:ktor-client-okhttp:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-client-logging:$ktorVersion")
    implementation("io.ktor:ktor-client-auth:$ktorVersion")
    implementation("io.ktor:ktor-client-encoding:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")

    // Image loading
    implementation(libs.coil.compose)

    // Testing (minimal for the starter, expanded later if needed)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}

// Rezolvare conflict metadate compilator Kotlin cu Hilt / KSP și forțare versiune Ktor
configurations.all {
    resolutionStrategy {
        eachDependency {
            if (requested.group == "io.ktor") {
                // Forțează utilizarea versiunii stabile compatibile cu Gemini
                useVersion("2.3.12")
            }
        }
    }
}

val buildDirProvider = layout.buildDirectory
tasks.matching { it.name.contains("GoogleServices") }.configureEach {
    val googleKeyVal = localProperties.getProperty("google.api.key", "")
    val bDir = buildDirProvider.get().asFile
    doLast {
        if (googleKeyVal.isNotEmpty()) {
            val generatedResDir = File(bDir, "generated/res/google-services")
            if (generatedResDir.exists()) {
                generatedResDir.walkTopDown().forEach { file ->
                    if (file.name == "values.xml") {
                        val content = file.readText()
                        if (content.contains("YOUR_GOOGLE_API_KEY_HERE")) {
                            file.writeText(content.replace("YOUR_GOOGLE_API_KEY_HERE", googleKeyVal))
                        }
                    }
                }
            }
        }
    }
}

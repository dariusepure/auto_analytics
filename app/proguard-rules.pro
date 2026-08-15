# Standard Android ProGuard rules
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes InnerClasses
-keepattributes EnclosingMethod

# Kotlin Serialization
-keepclassmembers class ** {
    @kotlinx.serialization.Serializable *;
}
-keepclassmembers class com.dariusepure.caractivitylog.data.** { *; }
-keepclassmembers class com.dariusepure.caractivitylog.domain.** { *; }

# Firebase
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**

# Hilt / Dagger
-keep class dagger.hilt.** { *; }
-keep class androidx.hilt.** { *; }

# Coil
-keep class coil.** { *; }

# Vico Charts
-keep class com.patrykandpatrick.vico.** { *; }

# SLF4J (used by Ktor)
-dontwarn org.slf4j.**

# Ktor
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**

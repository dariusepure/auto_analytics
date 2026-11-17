package com.dariusepure.caractivitylog.core.data.local

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull

object FlexibleDoubleSerializer : KSerializer<Double?> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("FlexibleDouble", PrimitiveKind.DOUBLE)

    @OptIn(ExperimentalSerializationApi::class)
    override fun serialize(encoder: Encoder, value: Double?) {
        if (value == null) {
            encoder.encodeNull()
        } else {
            encoder.encodeDouble(value)
        }
    }

    override fun deserialize(decoder: Decoder): Double? {
        val input = decoder as? JsonDecoder ?: return try { decoder.decodeDouble() } catch (e: Exception) { null }
        val element = input.decodeJsonElement()
        if (element is JsonPrimitive) {
            if (element.isString) {
                val content = element.content
                if (content.isBlank() || content == "null") return null
                // Strip non-numeric characters except decimal point and minus sign
                val numericString = content.replace(Regex("[^0-9.-]"), "")
                return numericString.toDoubleOrNull()
            }
            return element.doubleOrNull
        }
        return null
    }
}

object FlexibleBooleanSerializer : KSerializer<Boolean?> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("FlexibleBoolean", PrimitiveKind.BOOLEAN)

    @OptIn(ExperimentalSerializationApi::class)
    override fun serialize(encoder: Encoder, value: Boolean?) {
        if (value == null) {
            encoder.encodeNull()
        } else {
            encoder.encodeBoolean(value)
        }
    }

    override fun deserialize(decoder: Decoder): Boolean? {
        val input = decoder as? JsonDecoder ?: return try { decoder.decodeBoolean() } catch (e: Exception) { null }
        val element = input.decodeJsonElement()
        if (element is JsonPrimitive) {
            if (element.isString) {
                val content = element.content.trim().lowercase()
                return when (content) {
                    "true", "yes", "da", "1" -> true
                    "false", "no", "nu", "0" -> false
                    else -> null
                }
            }
            return try { element.booleanOrNull } catch (e: Exception) { null }
        }
        return null
    }
}

object FlexibleStringSerializer : KSerializer<String?> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("FlexibleString", PrimitiveKind.STRING)

    @OptIn(ExperimentalSerializationApi::class)
    override fun serialize(encoder: Encoder, value: String?) {
        if (value == null) {
            encoder.encodeNull()
        } else {
            encoder.encodeString(value)
        }
    }

    override fun deserialize(decoder: Decoder): String? {
        val input = decoder as? JsonDecoder ?: return try { decoder.decodeString() } catch (e: Exception) { null }
        val element = input.decodeJsonElement()
        if (element is JsonPrimitive) {
            val content = element.content.trim()
            if (content.isBlank() || content == "null") return null
            return content
        }
        return element.toString()
    }
}


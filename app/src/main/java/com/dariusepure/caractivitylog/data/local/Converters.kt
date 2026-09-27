package com.dariusepure.caractivitylog.data.local

import androidx.room.TypeConverter
import com.dariusepure.caractivitylog.domain.InspectionDurationUnit
import com.dariusepure.caractivitylog.domain.TireSeason
import java.util.Date

class Converters {
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? = value?.let { Date(it) }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? = date?.time

    @TypeConverter
    fun fromStringList(value: String?): List<String> {
        if (value.isNullOrBlank()) return emptyList()
        return value.split("||")
    }

    @TypeConverter
    fun toStringList(list: List<String>?): String {
        return list?.joinToString("||") ?: ""
    }

    @TypeConverter
    fun fromInspectionDurationUnit(unit: InspectionDurationUnit?): String = unit?.name ?: InspectionDurationUnit.YEARS.name

    @TypeConverter
    fun toInspectionDurationUnit(value: String?): InspectionDurationUnit = try {
        InspectionDurationUnit.valueOf(value ?: "YEARS")
    } catch (e: Exception) {
        InspectionDurationUnit.YEARS
    }

    @TypeConverter
    fun fromTireSeason(season: TireSeason?): String = season?.name ?: TireSeason.SUMMER.name

    @TypeConverter
    fun toTireSeason(value: String?): TireSeason = try {
        TireSeason.valueOf(value ?: "SUMMER")
    } catch (e: Exception) {
        TireSeason.SUMMER
    }
}

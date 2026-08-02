package com.dariusepure.caractivitylog.ui.cars

import android.content.Context

object BrandHelper {
    /**
     * Logo lookup is disabled to avoid copyright/trademark issues.
     * Always returns 0 (no resource).
     */
    @Suppress("UNUSED_PARAMETER")
    fun getLogoResource(context: Context, make: String): Int {
        return 0
    }

    /**
     * Returns the representative brand color as a Hex Long (0xAARRGGBB).
     */
    fun getBrandColor(make: String): Long {
        return when (make.uppercase().trim()) {
            "ABARTH" -> 0xFFE30613
            "ACURA" -> 0xFFCE1126
            "ALFA ROMEO" -> 0xFFB10B1B
            "ALPINE" -> 0xFF005BA3
            "ASTON MARTIN" -> 0xFF006B3D
            "AUDI" -> 0xFFE30613
            "BENTLEY" -> 0xFF004225
            "BMW" -> 0xFF1C69D4
            "BUGATTI" -> 0xFF002244
            "BUICK" -> 0xFF0038A8
            "BYD" -> 0xFF00A3E0
            "CADILLAC" -> 0xFFFFD700
            "CATERHAM" -> 0xFF004225
            "CHEVROLET" -> 0xFFEAAA00
            "CHRYSLER" -> 0xFF003399
            "CITROËN" -> 0xFFFF2D00
            "CUPRA" -> 0xFFD19F73
            "DACIA" -> 0xFF4E5B31
            "DAEWOO" -> 0xFF003399
            "DAIHATSU" -> 0xFFEE1111
            "DODGE" -> 0xFFFF0000
            "DS" -> 0xFF5A1E1E
            "FERRARI" -> 0xFFFF2800
            "FIAT" -> 0xFFCD1A1A
            "FORD" -> 0xFF003399
            "GENESIS" -> 0xFFB87333
            "GMC" -> 0xFFE31837
            "HONDA" -> 0xFFFF0000
            "HUMMER" -> 0xFF795548
            "HYUNDAI" -> 0xFF002C5F
            "INFINITI" -> 0xFF002C5F
            "ISUZU" -> 0xFFED1C24
            "JAGUAR" -> 0xFF006B3D
            "JEEP" -> 0xFF5B673A
            "KIA" -> 0xFFBB162B
            "KOENIGSEGG" -> 0xFF00A3E0
            "LAMBORGHINI" -> 0xFFFFCC00
            "LANCIA" -> 0xFF003399
            "LAND ROVER" -> 0xFF004225
            "LEXUS" -> 0xFF003399
            "LINCOLN" -> 0xFF001E32
            "LOTUS" -> 0xFF004225
            "LUCID" -> 0xFFD19F73
            "MASERATI" -> 0xFF003399
            "MAYBACH" -> 0xFF4B3621
            "MAZDA" -> 0xFF9E0B0E
            "MCLAREN" -> 0xFFFF8000
            "MERCEDES-BENZ" -> 0xFF00ADEF
            "MG" -> 0xFFBA0C2F
            "MINI" -> 0xFFF58025
            "MITSUBISHI" -> 0xFFEE1111
            "MORGAN" -> 0xFF004225
            "NISSAN" -> 0xFFC11B17
            "OPEL" -> 0xFFFFD700
            "PAGANI" -> 0xFF002244
            "PEUGEOT" -> 0xFF001E32
            "POLESTAR" -> 0xFFE1B924
            "PORSCHE" -> 0xFFD5001C
            "RAM" -> 0xFFE31837
            "RENAULT" -> 0xFFFFCC00
            "RIMAC" -> 0xFF00A3E0
            "ROLLS-ROYCE" -> 0xFF602F6B
            "ROVER" -> 0xFFB10B1B
            "SAAB" -> 0xFF004A99
            "SEAT" -> 0xFFE31837
            "SKODA" -> 0xFF008B4B
            "SMART" -> 0xFFFFD700
            "SSANGYONG" -> 0xFF0038A8
            "SUBARU" -> 0xFF003399
            "SUZUKI" -> 0xFFEE1111
            "TESLA" -> 0xFFCC0000
            "TOYOTA" -> 0xFFEB0A1E
            "TRIUMPH" -> 0xFF004225
            "TVR" -> 0xFF6A0DAD
            "VAUXHALL" -> 0xFFE31837
            "VOLKSWAGEN" -> 0xFF001E50
            "VOLVO" -> 0xFF003057
            else -> 0xFF2196F3 // Default Blue
        }
    }
}

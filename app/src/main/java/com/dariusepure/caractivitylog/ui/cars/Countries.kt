/*
 * Copyright (C) 2026 Darius Epure (Darius DevWorks)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.dariusepure.caractivitylog.ui.cars

data class Country(
    val name: String,
    val code: String,
    val flag: String,
    val usesMiles: Boolean = false,
    val plateRegex: String? = null,
    val plateHint: String? = null
)

val europeanCountries = listOf(
    Country("Albania", "AL", "🇦🇱", plateRegex = "^[A-Z]{2}\\d{3}[A-Z]{2}$", plateHint = "AA123AA"),
    Country("Andorra", "AD", "🇦🇩", plateRegex = "^[A-Z]\\d{4}$", plateHint = "A1234"),
    Country("Austria", "AT", "🇦🇹", plateRegex = "^[A-Z]{1,2}\\d{1,5}[A-Z]{1,2}$", plateHint = "W12345A"),
    Country("Belarus", "BY", "🇧🇾", plateRegex = "^\\d{4}[A-Z]{2}-\\d$", plateHint = "1234AB-7"),
    Country("Belgium", "BE", "🇧🇪", plateRegex = "^\\d[A-Z]{3}\\d{3}$", plateHint = "1ABC123"),
    Country("Bosnia and Herzegovina", "BA", "🇧🇦", plateRegex = "^[A-Z]\\d{2}[A-Z]\\d{3}$", plateHint = "A12M345"),
    Country("Bulgaria", "BG", "🇧🇬", plateRegex = "^[A-Z]{1,2}\\d{4}[A-Z]{1,2}$", plateHint = "CB1234AB"),
    Country("Croatia", "HR", "🇭🇷", plateRegex = "^[A-Z]{2}\\d{3,4}[A-Z]{1,2}$", plateHint = "ZG1234A"),
    Country("Cyprus", "CY", "🇨🇾", plateRegex = "^[A-Z]{3}\\d{3}$", plateHint = "ABC123"),
    Country("Czech Republic", "CZ", "🇨🇿", plateRegex = "^\\d[A-Z]\\d\\d{4}$", plateHint = "1A23456"),
    Country("Denmark", "DK", "🇩🇰", plateRegex = "^[A-Z]{2}\\d{5}$", plateHint = "AB12345"),
    Country("Estonia", "EE", "🇪🇪", plateRegex = "^\\d{3}[A-Z]{3}$", plateHint = "123ABC"),
    Country("Finland", "FI", "🇫🇮", plateRegex = "^[A-Z]{3}\\d{3}$", plateHint = "ABC123"),
    Country("France", "FR", "🇫🇷", plateRegex = "^[A-Z]{2}\\d{3}[A-Z]{2}$", plateHint = "AA123AA"),
    Country("Germany", "DE", "🇩🇪", plateRegex = "^[A-ZÄÖÜ]{1,3}[A-Z]{1,2}\\d{1,4}[EH]?$", plateHint = "BMW1234"),
    Country("Greece", "GR", "🇬🇷", plateRegex = "^[A-Z]{3}\\d{4}$", plateHint = "ABC1234"),
    Country("Hungary", "HU", "🇭🇺", plateRegex = "^[A-Z]{2}[A-Z]{2}\\d{3}$", plateHint = "AAAA123"),
    Country("Iceland", "IS", "🇮🇸", plateRegex = "^[A-Z]{2}[A-Z0-9]{3}$", plateHint = "AB123"),
    Country("Ireland", "IE", "🇮🇪", plateRegex = "^\\d{2,3}[A-Z]{1,2}\\d{1,6}$", plateHint = "211D123"),
    Country("Italy", "IT", "🇮🇹", plateRegex = "^[A-Z]{2}\\d{3}[A-Z]{2}$", plateHint = "AB123CD"),
    Country("Latvia", "LV", "🇱🇻", plateRegex = "^[A-Z]{2}\\d{1,4}$", plateHint = "AB1234"),
    Country("Liechtenstein", "LI", "🇱🇮", plateRegex = "^FL\\d{1,5}$", plateHint = "FL12345"),
    Country("Lithuania", "LT", "🇱🇹", plateRegex = "^[A-Z]{3}\\d{3}$", plateHint = "ABC123"),
    Country("Luxembourg", "LU", "🇱🇺", plateRegex = "^[A-Z]{2}\\d{4}$", plateHint = "AB1234"),
    Country("Malta", "MT", "🇲🇹", plateRegex = "^[A-Z]{3}\\d{3}$", plateHint = "ABC123"),
    Country("Moldova", "MD", "🇲🇩", plateRegex = "^[A-Z]{3}\\d{1,3}$", plateHint = "ABC1"),
    Country("Monaco", "MC", "🇲🇨", plateRegex = "^[A-Z0-9]\\d{3}$", plateHint = "A123"),
    Country("Montenegro", "ME", "🇲🇪", plateRegex = "^[A-Z]{2}[A-Z]{2}\\d{3}$", plateHint = "PGAB123"),
    Country("Netherlands", "NL", "🇳🇱", plateRegex = "^[A-Z0-9]{2}[A-Z0-9]{3}[A-Z0-9]{1,2}$", plateHint = "12ABC3"),
    Country("North Macedonia", "MK", "🇲🇰", plateRegex = "^[A-Z]{2}\\d{4}[A-Z]{2}$", plateHint = "SK1234AB"),
    Country("Norway", "NO", "🇳🇴", plateRegex = "^[A-Z]{2}\\d{5}$", plateHint = "AB12345"),
    Country("Poland", "PL", "🇵🇱", plateRegex = "^[A-Z]{2,3}[A-Z0-9]{4,5}$", plateHint = "WA12345"),
    Country("Portugal", "PT", "🇵🇹", plateRegex = "^[A-Z]{2}\\d{2}[A-Z]{2}$", plateHint = "AA00AA"),
    Country("Romania", "RO", "🇷🇴", plateRegex = "^[A-Z]{1,2}\\d{2,3}[A-Z]{3}$", plateHint = "B123ABC"),
    Country("San Marino", "SM", "🇸🇲", plateRegex = "^[A-Z]\\d{4}$", plateHint = "A1234"),
    Country("Serbia", "RS", "🇷🇸", plateRegex = "^[A-Z]{2}\\d{3,5}[A-Z]{2}$", plateHint = "BG123AA"),
    Country("Slovakia", "SK", "🇸🇰", plateRegex = "^[A-Z]{2}\\d{3}[A-Z]{2}$", plateHint = "BA123AB"),
    Country("Slovenia", "SI", "🇸🇮", plateRegex = "^[A-Z]{2}[A-Z0-9]{2}\\d{3}$", plateHint = "LJAB123"),
    Country("Spain", "ES", "🇪🇸", plateRegex = "^\\d{4}[B-DF-HJ-NP-TV-Z]{3}$", plateHint = "1234BBB"),
    Country("Sweden", "SE", "🇸🇪", plateRegex = "^[A-Z]{3}\\d{2}[A-Z0-9]$", plateHint = "ABC12A"),
    Country("Switzerland", "CH", "🇨🇭", plateRegex = "^[A-Z]{2}\\d{1,6}$", plateHint = "ZH123456"),
    Country("Ukraine", "UA", "🇺🇦", plateRegex = "^[A-Z]{2}\\d{4}[A-Z]{2}$", plateHint = "AA1234AA"),
    Country("United Kingdom", "GB", "🇬🇧", plateRegex = "^[A-Z]{2}\\d{2}[A-Z]{3}$", plateHint = "AB12ABC", usesMiles = true),
    Country("Vatican City", "VA", "🇻🇦", plateRegex = "^SCV\\d{1,5}$", plateHint = "SCV1"),
    // Non-European
    Country("Japan", "JP", "🇯🇵"),
    Country("USA", "US", "🇺🇸", usesMiles = true),
    Country("South Korea", "KR", "🇰🇷"),
    Country("China", "CN", "🇨🇳"),
    Country("India", "IN", "🇮🇳"),
    Country("Mexico", "MX", "🇲🇽"),
    Country("Brazil", "BR", "🇧🇷"),
    Country("Canada", "CA", "🇨🇦"),
    Country("Australia", "AU", "🇦🇺")
).sortedBy { it.name }


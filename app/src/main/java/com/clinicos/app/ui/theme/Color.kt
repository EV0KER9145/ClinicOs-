package com.clinicos.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Light Theme Palette - Professional Healthcare Teal
val PrimaryLight = Color(0xFF006A60)
val OnPrimaryLight = Color(0xFFFFFFFF)
val PrimaryContainerLight = Color(0xFF9CF1E4)
val OnPrimaryContainerLight = Color(0xFF00201C)

val SecondaryLight = Color(0xFF4A635F)
val OnSecondaryLight = Color(0xFFFFFFFF)
val SecondaryContainerLight = Color(0xFFCCE8E2)
val OnSecondaryContainerLight = Color(0xFF05201C)

val TertiaryLight = Color(0xFF456179)
val OnTertiaryLight = Color(0xFFFFFFFF)
val TertiaryContainerLight = Color(0xFFCCE5FF)
val OnTertiaryContainerLight = Color(0xFF001E31)

val ErrorLight = Color(0xFFBA1A1A)
val OnErrorLight = Color(0xFFFFFFFF)
val ErrorContainerLight = Color(0xFFFFDAD6)
val OnErrorContainerLight = Color(0xFF410002)

val BackgroundLight = Color(0xFFF6FBF9)
val OnBackgroundLight = Color(0xFF161D1C)
val SurfaceLight = Color(0xFFFFFFFF)
val OnSurfaceLight = Color(0xFF161D1C)
val SurfaceVariantLight = Color(0xFFDAE5E2)
val OnSurfaceVariantLight = Color(0xFF3F4947)
val OutlineLight = Color(0xFF6F7976)
val OutlineVariantLight = Color(0xFFBEC9C6)

// Dark Theme Palette
val PrimaryDark = Color(0xFF80D5CB)
val OnPrimaryDark = Color(0xFF003732)
val PrimaryContainerDark = Color(0xFF005048)
val OnPrimaryContainerDark = Color(0xFF9CF1E4)

val SecondaryDark = Color(0xFFB0CCC6)
val OnSecondaryDark = Color(0xFF1B3531)
val SecondaryContainerDark = Color(0xFF324B47)
val OnSecondaryContainerDark = Color(0xFFCCE8E2)

val TertiaryDark = Color(0xFFACCAE5)
val OnTertiaryDark = Color(0xFF133348)
val TertiaryContainerDark = Color(0xFF2D4960)
val OnTertiaryContainerDark = Color(0xFFCCE5FF)

val ErrorDark = Color(0xFFFFB4AB)
val OnErrorDark = Color(0xFF690005)
val ErrorContainerDark = Color(0xFF93000A)
val OnErrorContainerDark = Color(0xFFFFDAD6)

val BackgroundDark = Color(0xFF0E1514)
val OnBackgroundDark = Color(0xFFDEE4E2)
val SurfaceDark = Color(0xFF111B1A)
val OnSurfaceDark = Color(0xFFDEE4E2)
val SurfaceVariantDark = Color(0xFF2B3533)
val OnSurfaceVariantDark = Color(0xFFBEC9C6)
val OutlineDark = Color(0xFF889390)
val OutlineVariantDark = Color(0xFF3F4947)

// Dynamic Theme-Aware Status and Alert Color Helpers
data class StatusColorPair(val container: Color, val content: Color)

@Composable
fun getAlertAmberColors(darkTheme: Boolean = isSystemInDarkTheme()): StatusColorPair {
    return if (darkTheme) {
        StatusColorPair(container = Color(0xFF3B2500), content = Color(0xFFFFB85D))
    } else {
        StatusColorPair(container = Color(0xFFFFF4E5), content = Color(0xFFB76E00))
    }
}

@Composable
fun getAlertRedColors(darkTheme: Boolean = isSystemInDarkTheme()): StatusColorPair {
    return if (darkTheme) {
        StatusColorPair(container = Color(0xFF410002), content = Color(0xFFFFB4AB))
    } else {
        StatusColorPair(container = Color(0xFFFDE8E8), content = Color(0xFF9B1C1C))
    }
}

@Composable
fun getStatusGreenColors(darkTheme: Boolean = isSystemInDarkTheme()): StatusColorPair {
    return if (darkTheme) {
        StatusColorPair(container = Color(0xFF003828), content = Color(0xFF82F6CE))
    } else {
        StatusColorPair(container = Color(0xFFDEF7EC), content = Color(0xFF03543F))
    }
}

@Composable
fun getStatusBlueColors(darkTheme: Boolean = isSystemInDarkTheme()): StatusColorPair {
    return if (darkTheme) {
        StatusColorPair(container = Color(0xFF002F6C), content = Color(0xFFA8C8FF))
    } else {
        StatusColorPair(container = Color(0xFFE1EFFE), content = Color(0xFF1E429F))
    }
}

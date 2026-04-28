package com.slocator.fleetdriver.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.sp
import com.slocator.fleetdriver.R

// Downloadable Google Fonts pair: Tajawal (Arabic) + Inter (English).
// Compose's downloadable fonts API picks the right family per locale automatically
// based on subset coverage, but we explicitly construct one family per script
// so we can also tune weights independently.

private val provider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

private val tajawal = GoogleFont("Tajawal")
private val cairo = GoogleFont("Cairo")
private val inter = GoogleFont("Inter")

private val ArabicFamily = FontFamily(
    androidx.compose.ui.text.googlefonts.Font(googleFont = tajawal, fontProvider = provider, weight = FontWeight.Normal),
    androidx.compose.ui.text.googlefonts.Font(googleFont = tajawal, fontProvider = provider, weight = FontWeight.Medium),
    androidx.compose.ui.text.googlefonts.Font(googleFont = tajawal, fontProvider = provider, weight = FontWeight.SemiBold),
    androidx.compose.ui.text.googlefonts.Font(googleFont = tajawal, fontProvider = provider, weight = FontWeight.Bold),
    androidx.compose.ui.text.googlefonts.Font(googleFont = tajawal, fontProvider = provider, weight = FontWeight.ExtraBold),
    androidx.compose.ui.text.googlefonts.Font(googleFont = cairo, fontProvider = provider, weight = FontWeight.SemiBold)
)

private val LatinFamily = FontFamily(
    androidx.compose.ui.text.googlefonts.Font(googleFont = inter, fontProvider = provider, weight = FontWeight.Normal),
    androidx.compose.ui.text.googlefonts.Font(googleFont = inter, fontProvider = provider, weight = FontWeight.Medium),
    androidx.compose.ui.text.googlefonts.Font(googleFont = inter, fontProvider = provider, weight = FontWeight.SemiBold),
    androidx.compose.ui.text.googlefonts.Font(googleFont = inter, fontProvider = provider, weight = FontWeight.Bold),
    androidx.compose.ui.text.googlefonts.Font(googleFont = inter, fontProvider = provider, weight = FontWeight.ExtraBold)
)

@Composable
fun appTypography(): Typography {
    val cfg = LocalConfiguration.current
    val isArabic = remember(cfg) { cfg.locales.get(0)?.language == "ar" }
    val family = if (isArabic) ArabicFamily else LatinFamily

    // Slightly larger Arabic baseline because Latin Inter looks denser at the same size.
    val scale = if (isArabic) 1.0f else 1.0f

    return Typography(
        // Massive date header — the "in big" requirement.
        displayLarge = TextStyle(
            fontFamily = family,
            fontWeight = FontWeight.ExtraBold,
            fontSize = (44 * scale).sp,
            lineHeight = (52 * scale).sp,
            letterSpacing = (-0.5).sp
        ),
        displayMedium = TextStyle(
            fontFamily = family,
            fontWeight = FontWeight.Bold,
            fontSize = (32 * scale).sp,
            lineHeight = (40 * scale).sp
        ),
        headlineLarge = TextStyle(
            fontFamily = family,
            fontWeight = FontWeight.Bold,
            fontSize = 26.sp,
            lineHeight = 32.sp
        ),
        headlineMedium = TextStyle(
            fontFamily = family,
            fontWeight = FontWeight.SemiBold,
            fontSize = 22.sp,
            lineHeight = 28.sp
        ),
        titleLarge = TextStyle(
            fontFamily = family,
            fontWeight = FontWeight.SemiBold,
            fontSize = 20.sp,
            lineHeight = 26.sp
        ),
        titleMedium = TextStyle(
            fontFamily = family,
            fontWeight = FontWeight.Medium,
            fontSize = 16.sp,
            lineHeight = 22.sp
        ),
        bodyLarge = TextStyle(
            fontFamily = family,
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp,
            lineHeight = 24.sp
        ),
        bodyMedium = TextStyle(
            fontFamily = family,
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp,
            lineHeight = 20.sp
        ),
        labelLarge = TextStyle(
            fontFamily = family,
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.2.sp
        ),
        labelMedium = TextStyle(
            fontFamily = family,
            fontWeight = FontWeight.Medium,
            fontSize = 13.sp,
            lineHeight = 18.sp
        )
    )
}

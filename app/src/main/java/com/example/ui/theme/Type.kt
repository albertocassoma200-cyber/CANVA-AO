package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R

val PlusJakartaSansFamily = FontFamily(
    Font(R.font.plus_jakarta_sans, FontWeight.Normal),
    Font(R.font.plus_jakarta_sans, FontWeight.Medium),
    Font(R.font.plus_jakarta_sans, FontWeight.SemiBold),
    Font(R.font.plus_jakarta_sans, FontWeight.Bold),
    Font(R.font.plus_jakarta_sans, FontWeight.ExtraBold)
)

val InterFamily = FontFamily(
    Font(R.font.inter, FontWeight.Normal),
    Font(R.font.inter, FontWeight.Medium),
    Font(R.font.inter, FontWeight.SemiBold),
    Font(R.font.inter, FontWeight.Bold)
)

val MontserratFamily = FontFamily(Font(R.font.montserrat, FontWeight.Bold))
val PlayfairDisplayFamily = FontFamily(Font(R.font.playfair_display, FontWeight.Bold))
val OswaldFamily = FontFamily(Font(R.font.oswald, FontWeight.Bold))
val PacificoFamily = FontFamily(Font(R.font.pacifico, FontWeight.Normal))

fun editorFontFamily(key: String): FontFamily = when (key.lowercase()) {
    "plus_jakarta_sans" -> PlusJakartaSansFamily
    "inter" -> InterFamily
    "montserrat" -> MontserratFamily
    "playfair_display" -> PlayfairDisplayFamily
    "oswald" -> OswaldFamily
    "pacifico" -> PacificoFamily
    else -> PlusJakartaSansFamily
}

fun editorFontResId(key: String): Int = when (key.lowercase()) {
    "plus_jakarta_sans" -> R.font.plus_jakarta_sans
    "inter" -> R.font.inter
    "montserrat" -> R.font.montserrat
    "playfair_display" -> R.font.playfair_display
    "oswald" -> R.font.oswald
    "pacifico" -> R.font.pacifico
    else -> R.font.plus_jakarta_sans
}

val CanvaAoTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = PlusJakartaSansFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 32.sp,
        lineHeight = 38.sp,
        letterSpacing = (-0.5).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = PlusJakartaSansFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
        letterSpacing = (-0.25).sp
    ),
    titleLarge = TextStyle(
        fontFamily = PlusJakartaSansFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 26.sp
    ),
    titleMedium = TextStyle(
        fontFamily = PlusJakartaSansFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = InterFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = InterFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 19.sp
    ),
    labelLarge = TextStyle(
        fontFamily = PlusJakartaSansFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 18.sp
    ),
    labelSmall = TextStyle(
        fontFamily = InterFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 14.sp
    )
)

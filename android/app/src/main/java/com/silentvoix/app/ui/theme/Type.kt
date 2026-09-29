package com.silentvoix.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.silentvoix.app.R

/**
 * Be Vietnam Pro (SIL OFL, licence in assets/licenses): drawn for Vietnamese, so stacked
 * diacritics (ệ, ẫ, ở) keep their shape and spacing instead of being squeezed into Latin metrics.
 */
val BeVietnamPro = FontFamily(
    Font(R.font.be_vietnam_pro_regular, FontWeight.Normal),
    Font(R.font.be_vietnam_pro_medium, FontWeight.Medium),
    Font(R.font.be_vietnam_pro_semibold, FontWeight.SemiBold),
    Font(R.font.be_vietnam_pro_bold, FontWeight.Bold),
)

private fun style(size: Int, line: Int, weight: FontWeight, tracking: Double = 0.0) = TextStyle(
    fontFamily = BeVietnamPro,
    fontSize = size.sp,
    lineHeight = line.sp,
    fontWeight = weight,
    letterSpacing = tracking.sp,
)

/**
 * Heavy, tight headlines against roomy body text: structure comes from type, not borders. Line
 * heights are generous because Vietnamese stacks marks above and below the x-height; body sizes sit
 * above the Material defaults so the copy reads at arm's length.
 */
internal val SilentVoixTypography = Typography(
    displayLarge = style(52, 62, FontWeight.Bold, -1.2),
    displayMedium = style(42, 52, FontWeight.Bold, -1.0),
    displaySmall = style(34, 44, FontWeight.Bold, -0.6),
    headlineLarge = style(30, 40, FontWeight.Bold, -0.6),
    headlineMedium = style(26, 36, FontWeight.Bold, -0.4),
    headlineSmall = style(22, 32, FontWeight.SemiBold, -0.2),
    titleLarge = style(20, 30, FontWeight.SemiBold, -0.1),
    titleMedium = style(17, 26, FontWeight.SemiBold),
    titleSmall = style(15, 22, FontWeight.SemiBold),
    bodyLarge = style(17, 27, FontWeight.Normal),
    bodyMedium = style(15, 23, FontWeight.Normal),
    bodySmall = style(13, 19, FontWeight.Normal),
    labelLarge = style(15, 21, FontWeight.SemiBold),
    labelMedium = style(13, 18, FontWeight.Medium),
    labelSmall = style(12, 16, FontWeight.Medium),
)

/** Small all-caps eyebrow used above section content. */
internal val EyebrowStyle = style(12, 16, FontWeight.Bold, 1.2)

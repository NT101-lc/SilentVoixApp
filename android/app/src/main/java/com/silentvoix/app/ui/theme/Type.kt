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

/**
 * Lora (SIL OFL, licence in assets/licenses) for display and headline text: a soft, bookish serif
 * that gives titles a storybook voice. Its static weights cover every Vietnamese diacritic.
 */
val Lora = FontFamily(
    Font(R.font.lora_medium, FontWeight.Medium),
    Font(R.font.lora_semibold, FontWeight.SemiBold),
    Font(R.font.lora_bold, FontWeight.Bold),
)

private fun style(size: Int, line: Int, weight: FontWeight, tracking: Double = 0.0, family: FontFamily = BeVietnamPro) = TextStyle(
    fontFamily = family,
    fontSize = size.sp,
    lineHeight = line.sp,
    fontWeight = weight,
    letterSpacing = tracking.sp,
)

/**
 * Serif headlines (Lora) over a roomy sans body (Be Vietnam Pro): titles read like a picture book,
 * everything you act on stays plain. Line heights are generous because Vietnamese stacks marks
 * above and below the x-height; body sizes sit above the Material defaults so the copy reads at
 * arm's length.
 */
internal val SilentVoixTypography = Typography(
    displayLarge = style(52, 64, FontWeight.SemiBold, -0.8, Lora),
    displayMedium = style(42, 54, FontWeight.SemiBold, -0.6, Lora),
    displaySmall = style(34, 46, FontWeight.SemiBold, -0.4, Lora),
    headlineLarge = style(30, 42, FontWeight.SemiBold, -0.3, Lora),
    headlineMedium = style(26, 36, FontWeight.SemiBold, -0.2, Lora),
    headlineSmall = style(22, 32, FontWeight.SemiBold, -0.1, Lora),
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

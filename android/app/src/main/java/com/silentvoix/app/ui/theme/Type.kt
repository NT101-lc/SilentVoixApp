package com.silentvoix.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Tighter, heavier headlines against roomy body text: the screens lean on type rather than on
 * borders and cards for structure. Body and label sizes stay above the Material defaults so the
 * Vietnamese copy is comfortable to read at arm's length.
 */
internal val SilentVoixTypography = Typography().let { base ->
    base.copy(
        displayMedium = base.displayMedium.copy(
            fontWeight = FontWeight.Bold,
            letterSpacing = (-1).sp,
        ),
        displaySmall = base.displaySmall.copy(
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.5).sp,
        ),
        headlineLarge = base.headlineLarge.copy(
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.5).sp,
        ),
        headlineMedium = base.headlineMedium.copy(
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.4).sp,
        ),
        headlineSmall = base.headlineSmall.copy(
            fontWeight = FontWeight.SemiBold,
            letterSpacing = (-0.2).sp,
        ),
        titleLarge = base.titleLarge.copy(fontWeight = FontWeight.SemiBold),
        titleMedium = base.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        bodyLarge = base.bodyLarge.copy(fontSize = 17.sp, lineHeight = 26.sp),
        bodyMedium = base.bodyMedium.copy(fontSize = 15.sp, lineHeight = 23.sp),
        labelLarge = base.labelLarge.copy(fontSize = 15.sp, lineHeight = 21.sp, fontWeight = FontWeight.SemiBold),
    )
}

/** Small all-caps eyebrow used above section content. */
internal val EyebrowStyle = TextStyle(
    fontSize = 12.sp,
    lineHeight = 16.sp,
    fontWeight = FontWeight.Bold,
    letterSpacing = 1.4.sp,
)

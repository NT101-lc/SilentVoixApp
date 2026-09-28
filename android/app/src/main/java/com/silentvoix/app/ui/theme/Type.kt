package com.silentvoix.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.unit.sp

// Material defaults with slightly larger body and label text for readability.
internal val SilentVoixTypography = Typography().let { base ->
    base.copy(
        bodyLarge = base.bodyLarge.copy(fontSize = 18.sp, lineHeight = 26.sp),
        bodyMedium = base.bodyMedium.copy(fontSize = 16.sp, lineHeight = 24.sp),
        labelLarge = base.labelLarge.copy(fontSize = 16.sp, lineHeight = 22.sp),
    )
}

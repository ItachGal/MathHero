package io.github.galitach.mathhero.data

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes

data class Recommendation(
    @param:DrawableRes val iconRes: Int,
    @param:StringRes val titleRes: Int,
    val description: String
)
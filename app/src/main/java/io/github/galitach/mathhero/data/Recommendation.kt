package io.github.galitach.mathhero.data

import android.os.Parcelable
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import kotlinx.parcelize.Parcelize

@Parcelize
data class Recommendation(
    val id: String,
    @DrawableRes val iconRes: Int,
    @StringRes val titleRes: Int,
    val description: String,
    @StringRes val detailTitleRes: Int,
    val detailDescription: String
) : Parcelable
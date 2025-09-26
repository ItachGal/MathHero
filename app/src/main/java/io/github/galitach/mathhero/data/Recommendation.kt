package io.github.galitach.mathhero.data

import android.os.Parcelable
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import kotlinx.parcelize.Parcelize

@Parcelize
data class Recommendation(
    val id: String,
    @param:DrawableRes val iconRes: Int,
    @param:StringRes val titleRes: Int,
    val description: String,
    @param:StringRes val detailTitleRes: Int,
    val detailDescription: String
) : Parcelable
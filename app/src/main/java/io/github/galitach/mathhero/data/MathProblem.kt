package io.github.galitach.mathhero.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class MathProblem(
    val id: Int,
    val question: String,
    val answer: String,
    val distractor1: String,
    val distractor2: String,
    val difficulty: Int,
    val explanation: String?,
    val num1: Int,
    val num2: Int,
    val operator: String
) : Parcelable
package io.github.galitach.mathhero.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ProblemResult(
    val timestamp: Long,
    val operation: Operation,
    val wasCorrect: Boolean,
    val num1: Int,
    val num2: Int,
    val answer: Int
) : Parcelable
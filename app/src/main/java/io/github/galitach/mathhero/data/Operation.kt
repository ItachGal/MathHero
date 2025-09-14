package io.github.galitach.mathhero.data

import androidx.annotation.StringRes
import io.github.galitach.mathhero.R

enum class Operation(val symbol: String, @StringRes val stringRes: Int) {
    ADDITION("+", R.string.operation_addition),
    SUBTRACTION("-", R.string.operation_subtraction),
    MULTIPLICATION("×", R.string.operation_multiplication),
    DIVISION("÷", R.string.operation_division)
}
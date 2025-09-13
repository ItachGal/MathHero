package io.github.galitach.mathhero.data

import android.os.Parcelable
import io.github.galitach.mathhero.R
import kotlinx.parcelize.Parcelize

@Parcelize
data class DifficultySettings(
    val operations: Set<Operation>,
    val maxNumber: Int
) : Parcelable

enum class DifficultyLevel(
    val titleRes: Int,
    val descriptionRes: Int,
    val settings: DifficultySettings
) {
    NOVICE(
        R.string.difficulty_novice,
        R.string.difficulty_novice_desc,
        DifficultySettings(setOf(Operation.ADDITION), 10)
    ),
    APPRENTICE(
        R.string.difficulty_apprentice,
        R.string.difficulty_apprentice_desc,
        DifficultySettings(setOf(Operation.ADDITION, Operation.SUBTRACTION), 20)
    ),
    ADEPT(
        R.string.difficulty_adept,
        R.string.difficulty_adept_desc,
        DifficultySettings(setOf(Operation.ADDITION, Operation.SUBTRACTION), 50)
    ),
    EXPERT(
        R.string.difficulty_expert,
        R.string.difficulty_expert_desc,
        DifficultySettings(setOf(Operation.ADDITION, Operation.SUBTRACTION, Operation.MULTIPLICATION), 20) // smaller numbers for multiplication
    ),
    MASTER(
        R.string.difficulty_master,
        R.string.difficulty_master_desc,
        DifficultySettings(setOf(Operation.ADDITION, Operation.SUBTRACTION, Operation.MULTIPLICATION, Operation.DIVISION), 100)
    );

    companion object {
        fun fromSettings(settings: DifficultySettings): DifficultyLevel? {
            return entries.find { it.settings == settings }
        }
    }
}
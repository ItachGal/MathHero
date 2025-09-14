package io.github.galitach.mathhero.data

import android.annotation.SuppressLint
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import io.github.galitach.mathhero.R

data class Rank(
    val level: Int,
    @param:StringRes val nameRes: Int,
    val requiredStreak: Int,
    @param:DrawableRes val imageRes: Int
) {
    @SuppressLint("ResourceType")
    companion object {
        val allRanks by lazy {
            listOf(
                Rank(1, R.string.rank_novice, -1, R.raw.mathhero_1),
                Rank(2, R.string.rank_apprentice, 2, R.raw.mathhero_2),
                Rank(3, R.string.rank_adept, 3, R.raw.mathhero_3),
                Rank(4, R.string.rank_specialist, 4, R.raw.mathhero_4),
                Rank(5, R.string.rank_expert, 5, R.raw.mathhero_5),
                Rank(6, R.string.rank_master, 6, R.raw.mathhero_6),
                Rank(7, R.string.rank_grandmaster, 7, R.raw.mathhero_7),
                Rank(8, R.string.rank_legend, 8, R.raw.mathhero_8),
                Rank(9, R.string.rank_mythic, 9, R.raw.mathhero_9),
                Rank(10, R.string.rank_titan, 10, R.raw.mathhero_10)
            )
        }

        fun getRankForStreak(streak: Int): Rank {
            return allRanks.last { streak >= it.requiredStreak }
        }
    }
}
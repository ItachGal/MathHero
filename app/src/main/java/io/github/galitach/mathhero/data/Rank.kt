package io.github.galitach.mathhero.data

import android.annotation.SuppressLint
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import io.github.galitach.mathhero.R

data class Rank(
    val level: Int,
    @param:StringRes val nameRes: Int,
    val requiredStreak: Int,
    @param:DrawableRes val imageResA: Int,
    @param:DrawableRes val imageResB: Int
) {
    fun getImageForType(type: HeroType): Int {
        return when (type) {
            HeroType.A -> imageResA
            HeroType.B -> imageResB
        }
    }

    @SuppressLint("ResourceType")
    companion object {
        val allRanks by lazy {
            listOf(
                Rank(1, R.string.rank_novice, 0, R.raw.mathhero_1, R.raw.mathhero_1f),
                Rank(2, R.string.rank_apprentice, 10, R.raw.mathhero_2, R.raw.mathhero_2f),
                Rank(3, R.string.rank_adept, 25, R.raw.mathhero_3, R.raw.mathhero_3f),
                Rank(4, R.string.rank_specialist, 50, R.raw.mathhero_4, R.raw.mathhero_4f),
                Rank(5, R.string.rank_expert, 100, R.raw.mathhero_5, R.raw.mathhero_5f),
                Rank(6, R.string.rank_master, 150, R.raw.mathhero_6, R.raw.mathhero_6f),
                Rank(7, R.string.rank_grandmaster, 250, R.raw.mathhero_7, R.raw.mathhero_7f),
                Rank(8, R.string.rank_legend, 400, R.raw.mathhero_8, R.raw.mathhero_8f),
                Rank(9, R.string.rank_mythic, 600, R.raw.mathhero_9, R.raw.mathhero_9f),
                Rank(10, R.string.rank_titan, 1000, R.raw.mathhero_10, R.raw.mathhero_10f)
            )
        }

        fun getRankForStreak(streak: Int): Rank {
            return allRanks.last { streak >= it.requiredStreak }
        }
    }
}
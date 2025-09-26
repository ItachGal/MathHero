package io.github.galitach.mathhero.ui.onboarding

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import io.github.galitach.mathhero.R

enum class OnboardingPage(
    @param:DrawableRes val imageRes: Int,
    @param:StringRes val titleRes: Int,
    @param:StringRes val descriptionRes: Int
) {
    WELCOME(
        R.drawable.ic_onboarding_welcome,
        R.string.onboarding_welcome_title,
        R.string.onboarding_welcome_desc
    ),
    STREAK(
        R.drawable.ic_onboarding_streak,
        R.string.onboarding_streak_title,
        R.string.onboarding_streak_desc
    ),
    DIFFICULTY(
        R.drawable.ic_onboarding_difficulty,
        R.string.onboarding_difficulty_title,
        R.string.onboarding_difficulty_desc
    )
}
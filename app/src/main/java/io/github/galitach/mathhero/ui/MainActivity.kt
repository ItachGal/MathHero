package io.github.galitach.mathhero.ui

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.SharedPreferences
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.children
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.gms.oss.licenses.OssLicensesMenuActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import io.github.galitach.mathhero.R
import io.github.galitach.mathhero.data.MathProblem
import io.github.galitach.mathhero.data.SharedPreferencesManager
import io.github.galitach.mathhero.databinding.ActivityMainBinding
import io.github.galitach.mathhero.notifications.NotificationScheduler
import io.github.galitach.mathhero.ui.archive.ArchiveDialogFragment
import io.github.galitach.mathhero.ui.ranks.RanksDialogFragment
import io.github.galitach.mathhero.ui.settings.SettingsDialogFragment
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import nl.dionsegijn.konfetti.core.Party
import nl.dionsegijn.konfetti.core.Position
import nl.dionsegijn.konfetti.core.emitter.Emitter
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import androidx.core.view.isEmpty

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels { MainViewModelFactory }

    private lateinit var consentInformation: ConsentInformation
    private val isMobileAdsInitializeCalled = AtomicBoolean(false)

    private var bonusProblemRewardedAd: RewardedAd? = null
    private var saveStreakRewardedAd: RewardedAd? = null

    private lateinit var prefs: SharedPreferences
    private var mediaPlayer: MediaPlayer? = null

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                enableNotifications(true)
            } else {
                enableNotifications(false)
                Toast.makeText(this, getString(R.string.notifications_disabled_toast), Toast.LENGTH_LONG).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        setupClickListeners()
        prefs = getSharedPreferences("main_prefs", MODE_PRIVATE)
        mediaPlayer = MediaPlayer.create(this, R.raw.riddle_complete)


        handleFirstLaunch()
        setupConsentAndAds()
        observeUiState()
        animateContentIn()
    }

    private fun animateContentIn() {
        binding.mainContent.alpha = 0f
        binding.mainContent.translationY = 50f
        binding.mainContent.animate()
            .alpha(1f)
            .translationY(0f)
            .setStartDelay(300)
            .setDuration(500)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()
    }

    private fun setupConsentAndAds() {
        val params = ConsentRequestParameters.Builder().build()
        consentInformation = UserMessagingPlatform.getConsentInformation(this)
        consentInformation.requestConsentInfoUpdate(
            this,
            params,
            { UserMessagingPlatform.loadAndShowConsentFormIfRequired(this) { _ ->
                if (consentInformation.canRequestAds()) initializeMobileAds()
            }
            },
            { }
        )
        if (consentInformation.canRequestAds()) initializeMobileAds()
    }

    private fun initializeMobileAds() {
        if (isMobileAdsInitializeCalled.getAndSet(true)) return
        MobileAds.initialize(this) {
            loadBannerAd()
            loadBonusProblemRewardedAd()
            loadSaveStreakRewardedAd()
        }
    }

    private fun handleFirstLaunch() {
        val isFirstLaunch = prefs.getBoolean("isFirstLaunch", true)
        if (isFirstLaunch) {
            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.disclaimer_title)
                .setMessage(R.string.disclaimer_message)
                .setPositiveButton(android.R.string.ok) { dialog, _ ->
                    prefs.edit { putBoolean("isFirstLaunch", false) }
                    dialog.dismiss()
                    showNotificationPrimerDialog()
                }
                .setCancelable(false)
                .show()
        }
    }

    private fun showNotificationPrimerDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.notification_primer_title)
            .setMessage(R.string.notification_primer_message)
            .setPositiveButton(R.string.notification_primer_positive) { dialog, _ ->
                dialog.dismiss()
                requestNotificationPermission()
            }
            .setNegativeButton(R.string.notification_primer_negative) { dialog, _ ->
                enableNotifications(false)
                dialog.dismiss()
            }
            .setCancelable(false)
            .show()
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            enableNotifications(true)
        }
    }

    private fun enableNotifications(enabled: Boolean) {
        SharedPreferencesManager.setNotificationsEnabled(enabled)
        if (enabled) {
            NotificationScheduler.scheduleDailyNotification(this)
        } else {
            NotificationScheduler.cancelDailyNotification(this)
        }
    }

    private fun setupClickListeners() {
        val openRanksDialog = { view: View ->
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            val dialog = RanksDialogFragment.newInstance(viewModel.uiState.value.highestStreakCount)
            dialog.show(supportFragmentManager, RanksDialogFragment.TAG)
        }
        binding.heroImage.setOnClickListener(openRanksDialog)
        binding.viewRanksButton.setOnClickListener(openRanksDialog)

        binding.buttonHint.setOnClickListener { it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP); viewModel.onHintClicked() }
        binding.buttonConfirmAnswer.setOnClickListener { it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP); viewModel.onConfirmAnswerClicked() }
        binding.buttonShare.setOnClickListener { it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP); shareProblem() }
        binding.bonusRiddleButton.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            if (viewModel.uiState.value.bonusProblemsRemaining > 0) {
                viewModel.onBonusProblemRequested()
            } else {
                showBonusProblemRewardedAd()
            }
        }
        binding.buttonInfo.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            showExplanation()
        }
        binding.multipleChoiceGroup.addOnButtonCheckedListener { group, checkedId, isChecked ->
            if (isChecked) {
                group.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                val checkedButton = group.findViewById<MaterialButton>(checkedId)
                viewModel.onMultipleChoiceAnswerSelected(checkedButton.text.toString())
            } else {
                if (group.checkedButtonId == View.NO_ID) {
                    viewModel.onAnswerSelectionCleared()
                }
            }
        }
    }
    private fun showExplanation() {
        viewModel.uiState.value.problem?.explanation?.let { explanation ->
            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.explanation_title)
                .setMessage(explanation)
                .setPositiveButton(android.R.string.ok, null)
                .show()
        }
    }
    private fun observeUiState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.uiState.map { it.problem?.id }.distinctUntilChanged().collect {
                        resetMultipleChoiceButtonStates()
                        binding.hintGrid.visibility = View.GONE
                        binding.hintGrid.removeAllViews()
                    }
                }

                launch {
                    viewModel.uiState.map { it.currentRank }.distinctUntilChanged().collect { rank ->
                        rank?.let {
                            binding.heroImage.setImageResource(it.imageRes)
                            binding.rankNameText.setText(it.nameRes)
                        }
                    }
                }

                launch {
                    viewModel.uiState
                        .map { it.showSaveStreakDialog }
                        .distinctUntilChanged()
                        .collect { showDialog ->
                            if (showDialog) {
                                showSaveStreakDialog()
                            }
                        }
                }

                viewModel.uiState.collect { state ->
                    binding.problemText.text = state.problem?.question ?: getString(R.string.no_problem_available)
                    binding.difficultyRating.rating = state.problem?.difficulty?.toFloat() ?: 0f

                    updateVisibility(binding.preAnswerActionsContainer, !state.isAnswerRevealed)
                    updateVisibility(binding.postAnswerActionsContainer, state.isAnswerRevealed)
                    updateVisibility(binding.buttonInfo, state.isAnswerRevealed && !state.problem?.explanation.isNullOrEmpty())

                    val isAnswerSelected = state.selectedAnswer != null
                    binding.hintButtonContainer.visibility = if (isAnswerSelected) View.INVISIBLE else View.VISIBLE
                    binding.buttonConfirmAnswer.visibility = if (isAnswerSelected) View.VISIBLE else View.GONE
                    binding.buttonHint.isEnabled = !state.showVisualHint

                    if (state.showVisualHint && binding.hintGrid.isEmpty()) {
                        renderVisualHint(state.problem)
                        binding.hintGrid.visibility = View.VISIBLE
                    }

                    updateBonusButton(state.bonusProblemsRemaining, state.isBonusRewardedAdLoaded)

                    if (state.streakCount > 0) {
                        binding.streakCounter.text = state.streakCount.toString()
                        binding.streakCounter.visibility = View.VISIBLE
                        binding.streakIcon.visibility = View.VISIBLE
                    } else {
                        binding.streakCounter.visibility = View.GONE
                        binding.streakIcon.visibility = View.GONE
                    }

                    if (state.triggerWinAnimation) {
                        triggerWinEffects()
                        viewModel.onWinAnimationComplete()
                    }

                    if (state.triggerRankUpAnimation) {
                        triggerRankUpEffects()
                        viewModel.onRankUpAnimationComplete()
                    }

                    if (!state.isAnswerRevealed) {
                        binding.multipleChoiceGroup.children.filterIsInstance<MaterialButton>()
                            .forEachIndexed { index, button ->
                                button.text = state.shuffledAnswers.getOrNull(index) ?: ""
                            }
                    }

                    if (state.isAnswerRevealed) {
                        highlightMultipleChoiceAnswers(state)
                    } else {
                        updateMultipleChoiceSelection(state)
                    }
                }
            }
        }
    }

    private fun renderVisualHint(problem: MathProblem?) {
        problem ?: return
        binding.hintGrid.removeAllViews()

        val totalItems = problem.num1.coerceAtMost(30) // Cap to avoid performance issues
        val answer = problem.answer.toIntOrNull() ?: 0

        when (problem.operator) {
            "+" -> {
                binding.hintGrid.columnCount = 10
                repeat(problem.num1) { addStarToHint(R.drawable.ic_star_filled) }
                repeat(problem.num2) { addStarToHint(R.drawable.ic_star_secondary) }
            }
            "-" -> {
                binding.hintGrid.columnCount = 10
                repeat(totalItems) { index ->
                    val drawable = if (index < answer) R.drawable.ic_star_filled else R.drawable.ic_star_gray
                    addStarToHint(drawable)
                }
            }
            "×" -> {
                binding.hintGrid.columnCount = problem.num1
                repeat(problem.num1 * problem.num2) { addStarToHint(R.drawable.ic_star_filled) }
            }
            "÷" -> {
                binding.hintGrid.columnCount = answer
                repeat(totalItems) { addStarToHint(R.drawable.ic_star_filled) }
            }
        }
    }

    private fun addStarToHint(drawableRes: Int) {
        val star = ImageView(this).apply {
            layoutParams =
                android.widget.GridLayout.LayoutParams().apply {
                    width = 0
                    height = android.widget.GridLayout.LayoutParams.WRAP_CONTENT
                    columnSpec = android.widget.GridLayout.spec(android.widget.GridLayout.UNDEFINED, 1f)
                    setMargins(4, 4, 4, 4)
                }
            setImageDrawable(ContextCompat.getDrawable(this@MainActivity, drawableRes))
        }
        binding.hintGrid.addView(star)
    }

    private fun updateBonusButton(remaining: Int, isAdLoaded: Boolean) {
        if (remaining > 0) {
            binding.bonusRiddleButton.text = resources.getQuantityString(R.plurals.bonus_problem_remaining, remaining, remaining)
            binding.bonusRiddleButton.icon = null
            binding.bonusRiddleButton.isEnabled = true
        } else {
            binding.bonusRiddleButton.setText(R.string.bonus_problem_ad)
            binding.bonusRiddleButton.setIconResource(R.drawable.ic_bonus_riddle)
            binding.bonusRiddleButton.isEnabled = isAdLoaded
        }
    }

    private fun triggerWinEffects() {
        mediaPlayer?.start()
        val party = Party(
            speed = 0f,
            maxSpeed = 30f,
            damping = 0.9f,
            spread = 360,
            colors = listOf(0xfab042, 0xf4d36b, 0x5e4200),
            emitter = Emitter(duration = 100, TimeUnit.MILLISECONDS).max(100),
            position = Position.Relative(0.5, 0.3)
        )
        binding.konfettiView.start(party)
    }

    private fun triggerRankUpEffects() {
        mediaPlayer?.start()
        val party = Party(
            speed = 10f,
            maxSpeed = 50f,
            damping = 0.9f,
            spread = 360,
            colors = listOf(0xfab042, 0xf4d36b, 0x5e4200, 0xffffff),
            emitter = Emitter(duration = 2, TimeUnit.SECONDS).perSecond(300),
            position = Position.Relative(0.5, -0.1)
        )
        binding.konfettiView.start(party)

        viewModel.uiState.value.currentRank?.let { rank ->
            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.rank_up_title)
                .setMessage(getString(R.string.rank_up_message, getString(rank.nameRes)))
                .setPositiveButton(android.R.string.ok, null)
                .show()
        }
    }

    private fun highlightMultipleChoiceAnswers(state: UiState) {
        if (!state.isAnswerRevealed) return
        val correctAnswer = state.problem?.answer
        binding.multipleChoiceGroup.children.filterIsInstance<MaterialButton>().forEach { button ->
            button.isEnabled = false
            when {
                button.text == correctAnswer -> {
                    button.setStrokeColorResource(R.color.correct_green)
                    button.strokeWidth = 4
                }
                button.text == state.selectedAnswer && state.selectedAnswer != correctAnswer -> {
                    button.setStrokeColorResource(R.color.incorrect_red)
                    button.strokeWidth = 4
                }
                else -> {
                    button.strokeWidth = 2
                    button.alpha = 0.5f
                }
            }
        }
    }

    private fun updateMultipleChoiceSelection(state: UiState) {
        if (state.isAnswerRevealed) return
        binding.multipleChoiceGroup.isEnabled = true
        val selectedButton = binding.multipleChoiceGroup.children
            .filterIsInstance<MaterialButton>()
            .find { it.text == state.selectedAnswer }

        if (selectedButton != null && binding.multipleChoiceGroup.checkedButtonId != selectedButton.id) {
            binding.multipleChoiceGroup.check(selectedButton.id)
        } else if (state.selectedAnswer == null && binding.multipleChoiceGroup.checkedButtonId != View.NO_ID) {
            binding.multipleChoiceGroup.clearChecked()
        }
    }

    private fun resetMultipleChoiceButtonStates() {
        binding.multipleChoiceGroup.clearChecked()
        binding.multipleChoiceGroup.children.filterIsInstance<MaterialButton>().forEach { button ->
            button.strokeWidth = 2
            button.setStrokeColorResource(R.color.colorOutline)
            button.alpha = 1.0f
        }
    }

    private fun updateVisibility(view: View, isVisible: Boolean) {
        val currentVisibility = view.isVisible
        if (currentVisibility == isVisible) return

        if (isVisible) {
            view.alpha = 0f
            view.visibility = View.VISIBLE
            view.animate().alpha(1f).setDuration(300).start()
        } else {
            view.animate().alpha(0f).setDuration(300).withEndAction {
                view.visibility = View.GONE
            }.start()
        }
    }

    private fun shareProblem() {
        viewModel.uiState.value.problem?.question?.let { problemText ->
            val appName = getString(R.string.app_name)
            val playStoreUrl = "https://play.google.com/store/apps/details?id=$packageName"
            val shareText = if (viewModel.uiState.value.isAnswerRevealed) {
                getString(R.string.share_solved_format, problemText, appName, playStoreUrl)
            } else {
                getString(R.string.share_unsolved_format, appName, problemText, playStoreUrl)
            }

            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, shareText)
                type = "text/plain"
            }
            startActivity(Intent.createChooser(shareIntent, getString(R.string.share_problem_title)))
        }
    }

    private fun loadBannerAd() = binding.adView.loadAd(AdRequest.Builder().build())

    private fun loadBonusProblemRewardedAd() {
        viewModel.setBonusRewardedAdLoaded(false)
        RewardedAd.load(this, "ca-app-pub-9478542207288731/1649009341", AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    bonusProblemRewardedAd = ad
                    viewModel.setBonusRewardedAdLoaded(true)
                }
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    bonusProblemRewardedAd = null
                    viewModel.setBonusRewardedAdLoaded(false)
                }
            })
    }

    private fun showBonusProblemRewardedAd() {
        bonusProblemRewardedAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                bonusProblemRewardedAd = null
                loadBonusProblemRewardedAd()
            }
        }
        bonusProblemRewardedAd?.show(this) {
            viewModel.onBonusAdRewardEarned()
        } ?: Toast.makeText(this, getString(R.string.bonus_problem_not_available), Toast.LENGTH_SHORT).show()
    }

    private fun loadSaveStreakRewardedAd() {
        viewModel.setSaveStreakAdLoaded(false)
        RewardedAd.load(this, "ca-app-pub-9478542207288731/1649009341", AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    saveStreakRewardedAd = ad
                    viewModel.setSaveStreakAdLoaded(true)
                }
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    saveStreakRewardedAd = null
                    viewModel.setSaveStreakAdLoaded(false)
                }
            })
    }

    private fun showSaveStreakRewardedAd() {
        saveStreakRewardedAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                saveStreakRewardedAd = null
                loadSaveStreakRewardedAd()
            }
        }
        saveStreakRewardedAd?.show(this) {
            viewModel.onStreakSavedWithAd()
        } ?: Toast.makeText(this, getString(R.string.bonus_problem_not_available), Toast.LENGTH_SHORT).show()
    }

    private fun showSaveStreakDialog() {
        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle(R.string.save_streak_dialog_title)
            .setMessage(R.string.save_streak_dialog_message)
            .setNegativeButton(R.string.save_streak_dialog_negative_button) { _, _ ->
                viewModel.onStreakResetConfirmed()
            }
            .setPositiveButton(R.string.save_streak_dialog_positive_button) { _, _ ->
                showSaveStreakRewardedAd()
            }
            .setCancelable(false)
            .create()

        dialog.setOnShowListener {
            val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            positiveButton.isEnabled = viewModel.uiState.value.isSaveStreakAdLoaded
        }
        dialog.show()
    }

    private fun openStoreForRating() {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, "market://details?id=$packageName".toUri()))
        } catch (_: ActivityNotFoundException) {
            startActivity(Intent(Intent.ACTION_VIEW,
                "https://play.google.com/store/apps/details?id=$packageName".toUri()))
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_archive -> {
                val dialog = ArchiveDialogFragment.newInstance(viewModel.uiState.value.archivedProblems)
                dialog.show(supportFragmentManager, ArchiveDialogFragment.TAG)
                true
            }
            R.id.action_rate_app -> {
                openStoreForRating()
                true
            }
            R.id.action_settings -> {
                SettingsDialogFragment().show(supportFragmentManager, SettingsDialogFragment.TAG)
                true
            }
            R.id.action_licenses -> {
                startActivity(Intent(this, OssLicensesMenuActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        mediaPlayer = null
    }
}
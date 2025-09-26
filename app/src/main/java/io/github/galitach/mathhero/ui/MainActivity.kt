package io.github.galitach.mathhero.ui

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.HapticFeedbackConstants
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.CycleInterpolator
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
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
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.android.play.core.ktx.isImmediateUpdateAllowed
import com.google.android.play.core.ktx.requestAppUpdateInfo
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import io.github.galitach.mathhero.MathHeroApplication
import io.github.galitach.mathhero.R
import io.github.galitach.mathhero.billing.BillingManager
import io.github.galitach.mathhero.data.SharedPreferencesManager
import io.github.galitach.mathhero.databinding.ActivityMainBinding
import io.github.galitach.mathhero.notifications.NotificationScheduler
import io.github.galitach.mathhero.ui.archive.ArchiveDialogFragment
import io.github.galitach.mathhero.ui.difficulty.DifficultySelectionDialogFragment
import io.github.galitach.mathhero.ui.hint.HintBottomSheetFragment
import io.github.galitach.mathhero.ui.onboarding.OnboardingActivity
import io.github.galitach.mathhero.ui.progress.ProgressDialogFragment
import io.github.galitach.mathhero.ui.ranks.RanksDialogFragment
import io.github.galitach.mathhero.ui.settings.SettingsDialogFragment
import io.github.galitach.mathhero.ui.upgrade.UpgradeDialogFragment
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import nl.dionsegijn.konfetti.core.Party
import nl.dionsegijn.konfetti.core.Position
import nl.dionsegijn.konfetti.core.emitter.Emitter

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels { MainViewModelFactory }
    private lateinit var billingManager: BillingManager

    private lateinit var consentInformation: ConsentInformation
    private val isMobileAdsInitializeCalled = AtomicBoolean(false)

    private var saveStreakRewardedAd: RewardedAd? = null

    private lateinit var soundPool: SoundPool
    private var correctSoundId: Int = 0
    private val incorrectSoundIds = mutableListOf<Int>()
    private var soundsLoaded = false

    private lateinit var appUpdateManager: AppUpdateManager
    private val appUpdateResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode != Activity.RESULT_OK) {
            Log.w("MainActivity", "Update flow failed! Result code: " + result.resultCode)
        }
    }

    private val onboardingLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            showNotificationPrimerDialog()
        }
    }

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

        billingManager = (application as MathHeroApplication).billingManager
        appUpdateManager = AppUpdateManagerFactory.create(applicationContext)
        checkForAppUpdates()

        setupSoundPool()
        setupClickListeners()
        setupConsentAndAds()

        handleOnboardingIfNeeded()
        observeUiState()
        animateContentIn()
    }

    private fun setupSoundPool() {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(3)
            .setAudioAttributes(audioAttributes)
            .build()

        var soundsToLoad = 4 // 1 correct, 3 incorrect
        soundPool.setOnLoadCompleteListener { _, _, status ->
            if (status == 0) {
                soundsToLoad--
                if (soundsToLoad == 0) {
                    soundsLoaded = true
                }
            }
        }

        correctSoundId = soundPool.load(this, R.raw.riddle_complete, 1)
        incorrectSoundIds.add(soundPool.load(this, R.raw.incorrect_answer_1, 1))
        incorrectSoundIds.add(soundPool.load(this, R.raw.incorrect_answer_2, 1))
        incorrectSoundIds.add(soundPool.load(this, R.raw.incorrect_answer_3, 1))
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
        if (SharedPreferencesManager.isProUser()) {
            binding.adViewContainer.visibility = View.GONE
            return
        }

        val params = ConsentRequestParameters.Builder().build()
        consentInformation = UserMessagingPlatform.getConsentInformation(this)
        consentInformation.requestConsentInfoUpdate(
            this,
            params,
            { UserMessagingPlatform.loadAndShowConsentFormIfRequired(this) { _ ->
                if (consentInformation.canRequestAds()) initializeMobileAds()
            }
            },
            { formError ->
                Log.w("MainActivity", "Consent form error: ${formError.message}")
            }
        )
        if (consentInformation.canRequestAds()) initializeMobileAds()
    }

    private fun initializeMobileAds() {
        if (SharedPreferencesManager.isProUser()) return
        if (isMobileAdsInitializeCalled.getAndSet(true)) return
        MobileAds.initialize(this) {
            loadBannerAd()
            loadSaveStreakRewardedAd()
        }
    }

    private fun handleOnboardingIfNeeded() {
        if (!SharedPreferencesManager.isOnboardingCompleted()) {
            onboardingLauncher.launch(Intent(this, OnboardingActivity::class.java))
        }
    }

    private fun showNotificationPrimerDialog() {
        val onFinishOnboarding = {
            if (viewModel.uiState.value.needsDifficultySelection) {
                showDifficultySelectionDialog()
            }
        }

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.notification_primer_title)
            .setMessage(R.string.notification_primer_message)
            .setPositiveButton(R.string.notification_primer_positive) { dialog, _ ->
                dialog.dismiss()
                requestNotificationPermission()
                onFinishOnboarding()
            }
            .setNegativeButton(R.string.notification_primer_negative) { dialog, _ ->
                enableNotifications(false)
                dialog.dismiss()
                onFinishOnboarding()
            }
            .setCancelable(false)
            .show()
    }

    private fun showDifficultySelectionDialog() {
        if (supportFragmentManager.findFragmentByTag(DifficultySelectionDialogFragment.TAG) == null) {
            DifficultySelectionDialogFragment.newInstance(isFirstTime = true)
                .show(supportFragmentManager, DifficultySelectionDialogFragment.TAG)
        }
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
        binding.heroCard.setOnClickListener { view ->
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            val dialog = RanksDialogFragment.newInstance(viewModel.uiState.value.highestStreakCount)
            dialog.show(supportFragmentManager, RanksDialogFragment.TAG)
        }

        binding.buttonHint.setOnClickListener { it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP); viewModel.onHintClicked() }
        binding.buttonConfirmAnswer.setOnClickListener { it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP); viewModel.onConfirmAnswerClicked() }
        binding.buttonShare.setOnClickListener { it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP); shareProblem() }
        binding.nextProblemButton.setOnClickListener { view ->
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            viewModel.onNextProblemRequested()
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
                viewModel.uiState.collect { state ->
                    updateProblemUI(state)
                    updateGamificationUI(state)
                    updateButtonStates(state)
                    updateAnswerUI(state)

                    if (state.needsDifficultySelection && SharedPreferencesManager.isOnboardingCompleted()) {
                        showDifficultySelectionDialog()
                    }
                }
            }
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.oneTimeEvent.onEach { event ->
                    when (event) {
                        is OneTimeEvent.LaunchPurchaseFlow -> billingManager.launchPurchaseFlow(this@MainActivity)
                        is OneTimeEvent.ShowHintDialog -> {
                            if (supportFragmentManager.findFragmentByTag(HintBottomSheetFragment.TAG) == null) {
                                HintBottomSheetFragment.newInstance(event.problem)
                                    .show(supportFragmentManager, HintBottomSheetFragment.TAG)
                            }
                        }
                        is OneTimeEvent.ShowSaveStreakDialog -> showSaveStreakDialog(event.brokenStreakValue)
                        is OneTimeEvent.ShowSuggestLowerDifficultyDialog -> showSuggestLowerDifficultyDialog()
                        is OneTimeEvent.TriggerWinAnimation -> {
                            triggerWinEffects()
                            animateStreakCounter()
                            animateHeroImage()
                        }
                        is OneTimeEvent.TriggerRankUpAnimation -> {
                            triggerRankUpEffects()
                            animateStreakCounter()
                            animateHeroImage()
                        }
                    }
                }.launchIn(this)

                viewModel.uiState.map { it.playSoundEvent }.distinctUntilChanged().collect { event ->
                    event?.let {
                        playSound(it)
                        viewModel.onSoundEventHandled()
                    }
                }
                viewModel.uiState.map { it.isPro }.distinctUntilChanged().collect { isPro ->
                    handleProStatusChange(isPro)
                }
                viewModel.uiState.map { it.showStreakSavedToast }.distinctUntilChanged().collect { show ->
                    if (show) {
                        Toast.makeText(this@MainActivity, R.string.streak_saved_pro, Toast.LENGTH_SHORT).show()
                        viewModel.onStreakSaveToastShown()
                    }
                }
                viewModel.uiState.map { it.isLoadingNextProblem }.distinctUntilChanged().collect { isLoading ->
                    binding.clickBlockerView.isVisible = isLoading
                }
            }
        }
    }

    private fun handleProStatusChange(isPro: Boolean) {
        if (isPro) {
            binding.adViewContainer.visibility = View.GONE
            saveStreakRewardedAd = null
        } else {
            binding.adViewContainer.visibility = View.VISIBLE
            initializeMobileAds()
        }
        updateNextProblemButton(viewModel.uiState.value)
    }

    private fun updateProblemUI(state: UiState) {
        binding.mainContent.isVisible = state.problem != null
        binding.problemText.text = state.problem?.question ?: getString(R.string.no_problem_available)
        binding.difficultyRating.rating = state.problem?.difficulty?.toFloat() ?: 0f
    }

    private fun updateGamificationUI(state: UiState) {
        state.currentRank?.let {
            binding.heroImage.setImageResource(it.imageRes)
            binding.rankNameText.setText(it.nameRes)
        }
        state.difficultyDescription?.let {
            binding.difficultyText.text = it
            binding.statusContainer.visibility = View.VISIBLE
        } ?: run {
            binding.statusContainer.visibility = View.GONE
        }

        if (state.streakCount > 0) {
            binding.streakCounter.visibility = View.VISIBLE
            binding.streakIcon.visibility = View.VISIBLE
        } else {
            binding.streakCounter.visibility = View.GONE
            binding.streakIcon.visibility = View.GONE
        }
        // The text is now set here. The animation will only "pop" the view.
        binding.streakCounter.text = state.streakCount.toString()
    }

    private fun updateButtonStates(state: UiState) {
        binding.preAnswerActionsContainer.isVisible = !state.isAnswerRevealed
        binding.postAnswerActionsContainer.isVisible = state.isAnswerRevealed
        binding.buttonInfo.isVisible = state.isAnswerRevealed && !state.problem?.explanation.isNullOrEmpty()

        val isAnswerSelected = state.selectedAnswer != null
        binding.buttonHint.visibility = if (isAnswerSelected) View.GONE else View.VISIBLE
        binding.buttonConfirmAnswer.visibility = if (isAnswerSelected) View.VISIBLE else View.GONE

        updateNextProblemButton(state)
    }

    private fun updateAnswerUI(state: UiState) {
        if (state.isAnswerRevealed) {
            highlightMultipleChoiceAnswers(state)
        } else {
            resetMultipleChoiceButtonStates()
            binding.multipleChoiceGroup.children.filterIsInstance<MaterialButton>()
                .forEachIndexed { index, button ->
                    button.text = state.shuffledAnswers.getOrNull(index) ?: ""
                }
            updateMultipleChoiceSelection(state)
        }
    }

    private fun playSound(event: SoundEvent) {
        if (!SharedPreferencesManager.isSoundEnabled() || !soundsLoaded) return
        val volume = 1.0f
        when (event) {
            is SoundEvent.Correct -> soundPool.play(correctSoundId, volume, volume, 1, 0, 1.0f)
            is SoundEvent.Incorrect -> {
                if (incorrectSoundIds.isNotEmpty()) {
                    soundPool.play(incorrectSoundIds.random(), volume, volume, 1, 0, 1.0f)
                }
            }
        }
    }

    private fun updateNextProblemButton(state: UiState) {
        binding.nextProblemButton.isEnabled = state.isAnswerRevealed && !state.isLoadingNextProblem
    }

    private fun animateStreakCounter() {
        val popViews = listOf(binding.streakCounter, binding.streakIcon)
        popViews.forEach { view ->
            view.animate()
                .scaleX(1.3f)
                .scaleY(1.3f)
                .setDuration(200)
                .setInterpolator(AccelerateDecelerateInterpolator())
                .withEndAction {
                    view.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(200)
                        .setInterpolator(AccelerateDecelerateInterpolator())
                        .start()
                }.start()
        }
    }

    private fun animateHeroImage() {
        binding.heroImage.animate()
            .rotation(10f)
            .scaleX(1.1f)
            .scaleY(1.1f)
            .setInterpolator(CycleInterpolator(1f))
            .setDuration(400)
            .withEndAction {
                binding.heroImage.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(200)
                    .start()
            }
            .start()
    }

    private fun triggerWinEffects() {
        binding.konfettiView.bringToFront()
        binding.konfettiView.start(WIN_PARTY)
    }

    private fun triggerRankUpEffects() {
        binding.konfettiView.bringToFront()
        binding.konfettiView.start(RANK_UP_PARTY)

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
                    button.icon = ContextCompat.getDrawable(this, R.drawable.ic_check_feedback)
                }
                button.text == state.selectedAnswer && state.selectedAnswer != correctAnswer -> {
                    button.setStrokeColorResource(R.color.incorrect_red)
                    button.strokeWidth = 4
                    button.icon = ContextCompat.getDrawable(this, R.drawable.ic_close_feedback)
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
            button.isEnabled = true
            button.strokeWidth = 2
            button.setStrokeColorResource(R.color.colorOutline)
            button.alpha = 1.0f
            button.icon = null
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

    private fun loadSaveStreakRewardedAd() {
        if (viewModel.uiState.value.saveStreakAdState == AdLoadState.LOADED || viewModel.uiState.value.saveStreakAdState == AdLoadState.LOADING) {
            return
        }
        viewModel.setSaveStreakAdState(AdLoadState.LOADING)
        RewardedAd.load(this, "ca-app-pub-9478542207288731/1649009341", AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    saveStreakRewardedAd = ad
                    viewModel.setSaveStreakAdState(AdLoadState.LOADED)
                }
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    saveStreakRewardedAd = null
                    viewModel.setSaveStreakAdState(AdLoadState.FAILED)
                }
            })
    }

    private fun showSaveStreakRewardedAd(brokenStreak: Int) {
        saveStreakRewardedAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                saveStreakRewardedAd = null
                viewModel.setSaveStreakAdState(AdLoadState.IDLE)
                loadSaveStreakRewardedAd()
            }
        }
        saveStreakRewardedAd?.show(this) {
            viewModel.onStreakSaveCompleted(brokenStreak)
        } ?: Toast.makeText(this, getString(R.string.bonus_problem_not_available), Toast.LENGTH_SHORT).show()
    }

    private fun showSaveStreakDialog(brokenStreak: Int) {
        val isPro = viewModel.uiState.value.isPro
        val builder = MaterialAlertDialogBuilder(this)
            .setTitle(R.string.save_streak_dialog_title)
            .setMessage(R.string.save_streak_dialog_message)
            .setNegativeButton(R.string.save_streak_dialog_negative_button) { _, _ ->
                viewModel.onStreakResetConfirmed()
            }
            .setCancelable(false)

        if (isPro) {
            builder.setPositiveButton(R.string.save_streak_dialog_positive_button_pro) { _, _ ->
                viewModel.onStreakSaveCompleted(brokenStreak)
            }
        } else {
            builder.setPositiveButton(R.string.save_streak_dialog_positive_button) { _, _ ->
                showSaveStreakRewardedAd(brokenStreak)
            }
        }

        val dialog = builder.create()
        var adStateObserverJob: Job? = null

        dialog.setOnShowListener {
            val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            if (!isPro) {
                if (viewModel.uiState.value.saveStreakAdState == AdLoadState.IDLE) {
                    loadSaveStreakRewardedAd()
                }

                adStateObserverJob = lifecycleScope.launch {
                    viewModel.uiState.map { it.saveStreakAdState }.distinctUntilChanged().collect { state ->
                        when (state) {
                            AdLoadState.LOADED -> {
                                positiveButton.isEnabled = true
                                positiveButton.setText(R.string.save_streak_dialog_positive_button)
                            }
                            AdLoadState.LOADING, AdLoadState.IDLE -> {
                                positiveButton.isEnabled = false
                                positiveButton.setText(R.string.save_streak_dialog_loading_ad)
                            }
                            AdLoadState.FAILED -> {
                                positiveButton.isEnabled = false
                                positiveButton.setText(R.string.save_streak_dialog_ad_failed)
                            }
                        }
                    }
                }
            }
        }
        dialog.setOnDismissListener {
            adStateObserverJob?.cancel()
        }
        dialog.show()
    }

    private fun showSuggestLowerDifficultyDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.suggest_lower_difficulty_title)
            .setMessage(R.string.suggest_lower_difficulty_message)
            .setPositiveButton(R.string.change_difficulty) { _, _ ->
                viewModel.onDifficultyChangeFromSuggestion()
                DifficultySelectionDialogFragment.newInstance()
                    .show(supportFragmentManager, DifficultySelectionDialogFragment.TAG)
            }
            .setNegativeButton(R.string.keep_going) { _, _ ->
                viewModel.onSuggestLowerDifficultyDismissed()
            }
            .setCancelable(false)
            .show()
    }

    private fun openStoreForRating() {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, "market://details?id=$packageName".toUri()))
        } catch (_: ActivityNotFoundException) {
            startActivity(Intent(Intent.ACTION_VIEW,
                "https://play.google.com/store/apps/details?id=$packageName".toUri()))
        }
    }

    private fun checkForAppUpdates() {
        lifecycleScope.launch {
            try {
                val appUpdateInfo = appUpdateManager.requestAppUpdateInfo()
                if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE &&
                    appUpdateInfo.isImmediateUpdateAllowed
                ) {
                    appUpdateManager.startUpdateFlowForResult(
                        appUpdateInfo,
                        appUpdateResultLauncher,
                        AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE).build()
                    )
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Failed to check for app update.", e)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            try {
                val appUpdateInfo = appUpdateManager.requestAppUpdateInfo()
                if (appUpdateInfo.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
                    appUpdateManager.startUpdateFlowForResult(
                        appUpdateInfo,
                        appUpdateResultLauncher,
                        AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE).build()
                    )
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Failed to check for app update in onResume.", e)
            }
        }
        binding.animatedBackgroundView.start()
    }

    override fun onPause() {
        super.onPause()
        binding.animatedBackgroundView.stop()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        lifecycleScope.launch {
            viewModel.uiState.map { it.isPro }.distinctUntilChanged().collect {
                invalidateOptionsMenu()
            }
        }
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        val upgradeItem = menu.findItem(R.id.action_upgrade_pro)
        upgradeItem?.isVisible = !viewModel.uiState.value.isPro
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_archive -> {
                val dialog = ArchiveDialogFragment.newInstance(viewModel.uiState.value.archivedProblems)
                dialog.show(supportFragmentManager, ArchiveDialogFragment.TAG)
                true
            }
            R.id.action_difficulty -> {
                DifficultySelectionDialogFragment.newInstance()
                    .show(supportFragmentManager, DifficultySelectionDialogFragment.TAG)
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
            R.id.action_progress -> {
                ProgressDialogFragment.newInstance().show(supportFragmentManager, ProgressDialogFragment.TAG)
                true
            }
            R.id.action_upgrade_pro -> {
                UpgradeDialogFragment.newInstance().show(supportFragmentManager, UpgradeDialogFragment.TAG)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        soundPool.release()
    }

    companion object {
        private val WIN_PARTY = Party(
            speed = 10f,
            maxSpeed = 30f,
            damping = 0.9f,
            spread = 60,
            angle = 270,
            colors = listOf(0x0091EA, 0xFF9100, 0x00C853, 0xFFD600),
            emitter = Emitter(duration = 400, TimeUnit.MILLISECONDS).perSecond(50),
            position = Position.Relative(0.5, 0.9)
        )

        private val RANK_UP_PARTY = Party(
            speed = 10f,
            maxSpeed = 40f,
            damping = 0.9f,
            spread = 360,
            colors = listOf(0x0091EA, 0xFF9100, 0x00C853, 0xFFD600, 0xFFFFFF),
            emitter = Emitter(duration = 2500, TimeUnit.MILLISECONDS).perSecond(200),
            position = Position.Relative(0.5, -0.1)
        )
    }
}
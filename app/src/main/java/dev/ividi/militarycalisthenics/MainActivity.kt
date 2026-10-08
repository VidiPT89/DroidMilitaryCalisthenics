package dev.ividi.militarycalisthenics

import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import dev.ividi.militarycalisthenics.notifications.ReminderScheduler
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import dev.ividi.militarycalisthenics.ui.t
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.ividi.militarycalisthenics.data.PlanRepository
import dev.ividi.militarycalisthenics.model.isComplete
import dev.ividi.militarycalisthenics.model.next
import dev.ividi.militarycalisthenics.ui.ProvideLang
import dev.ividi.militarycalisthenics.ui.screens.OnboardingScreen
import dev.ividi.militarycalisthenics.ui.screens.PlanScreen
import dev.ividi.militarycalisthenics.ui.screens.ProgressScreen
import dev.ividi.militarycalisthenics.ui.screens.SettingsScreen
import dev.ividi.militarycalisthenics.ui.screens.SplashScreen
import dev.ividi.militarycalisthenics.ui.screens.WorkoutSessionScreen
import dev.ividi.militarycalisthenics.ui.theme.BgBase
import dev.ividi.militarycalisthenics.ui.theme.MilitaryCalisthenicsTheme
import dev.ividi.militarycalisthenics.viewmodel.MainViewModel
import dev.ividi.militarycalisthenics.viewmodel.MainViewModelFactory

private enum class Screen { SPLASH, ONBOARDING, PLAN, SETTINGS, PROGRESS, SESSION }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val repository = PlanRepository(applicationContext)

        setContent {
            val viewModel: MainViewModel = viewModel(factory = MainViewModelFactory(repository))
            val storedState by viewModel.state.collectAsState()
            val themeMode = storedState.themeMode

            MilitaryCalisthenicsTheme(themeMode = themeMode) {
                val isLoaded by viewModel.isLoaded.collectAsState()
                val errorKey by viewModel.errorKey.collectAsState()
                val plan = storedState.plan
                val lang = storedState.lang
                val weightHistory = storedState.weightHistory
                val remindersEnabled = storedState.remindersEnabled
                val reminderHour = storedState.reminderHour
                var screen by rememberSaveable { mutableStateOf(Screen.SPLASH) }
                var sessionDayIndex by rememberSaveable { mutableStateOf<Int?>(null) }
                var splashFinished by rememberSaveable { mutableStateOf(false) }
                var selectedWeekIndex by rememberSaveable { mutableStateOf(0) }
                var sessionWeekIndex by rememberSaveable { mutableStateOf(0) }

                BackHandler(screen == Screen.SETTINGS || screen == Screen.PROGRESS || (screen == Screen.ONBOARDING && plan != null)) {
                    screen = if (screen == Screen.PROGRESS) Screen.SETTINGS else Screen.PLAN
                }

                LaunchedEffect(isLoaded, remindersEnabled, reminderHour, lang) {
                    if (isLoaded) {
                        if (remindersEnabled) ReminderScheduler.schedule(applicationContext, reminderHour, lang = lang)
                        else ReminderScheduler.cancel(applicationContext)
                    }
                }

                LaunchedEffect(splashFinished, isLoaded) {
                    if (splashFinished && isLoaded && screen == Screen.SPLASH) {
                        screen = if (plan != null) Screen.PLAN else Screen.ONBOARDING
                    }
                }

                ProvideLang(lang) {
                    if (errorKey != null && isLoaded) {
                        AlertDialog(
                            onDismissRequest = viewModel::dismissError,
                            title = { Text(t("storage_error_title", lang)) },
                            text = { Text(t(errorKey!!, lang)) },
                            confirmButton = { TextButton(onClick = viewModel::dismissError) { Text(t("close", lang)) } }
                        )
                    }
                    if (!isLoaded && errorKey != null) {
                        Column(
                            modifier = Modifier.fillMaxSize().background(BgBase).safeDrawingPadding().padding(24.dp),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(t("storage_error_title", lang))
                            Text(t(errorKey!!, lang))
                            TextButton(onClick = viewModel::retryLoad) { Text(t("retry", lang)) }
                        }
                    } else {
                        AnimatedContent(
                            targetState = if (isLoaded) screen else Screen.SPLASH,
                            transitionSpec = {
                                (slideInHorizontally(tween(350)) { it / 4 } + fadeIn(tween(350))) togetherWith
                                    (slideOutHorizontally(tween(200)) { -it / 4 } + fadeOut(tween(200)))
                            },
                            modifier = Modifier.fillMaxSize().background(BgBase).safeDrawingPadding()
                                .wrapContentSize(Alignment.TopCenter).widthIn(max = 760.dp),
                            label = "screenTransition"
                        ) { current ->
                            when (current) {
                                Screen.SPLASH -> {
                                    SplashScreen {
                                        splashFinished = true
                                    }
                                }
                                Screen.ONBOARDING -> {
                                    OnboardingScreen(
                                        lang = lang, initialProfile = plan?.profile,
                                        onCancel = if (plan != null) ({ screen = Screen.PLAN }) else null
                                    ) { profile ->
                                        viewModel.generatePlan(profile) {
                                            selectedWeekIndex = 0
                                            screen = Screen.PLAN
                                        }
                                    }
                                }
                                Screen.PLAN -> {
                                    val currentPlan = plan
                                    if (currentPlan != null) {
                                        val planCompletionAcknowledged = storedState.completionAcknowledged
                                        PlanScreen(
                                            plan = currentPlan,
                                            lang = lang,
                                            selectedWeek = selectedWeekIndex,
                                            onSelectWeek = { selectedWeekIndex = it },
                                            shouldShowPlanComplete = currentPlan.isComplete && !planCompletionAcknowledged,
                                            nextLevel = currentPlan.profile.level.next,
                                            onToggleCompleted = viewModel::toggleWorkoutCompleted,
                                            onRepeatPlan = { viewModel.regeneratePlan(); selectedWeekIndex = 0 },
                                            onLevelUp = { viewModel.levelUp(); selectedWeekIndex = 0 },
                                            onDismissPlanComplete = viewModel::acknowledgePlanComplete,
                                            onOpenSettings = { screen = Screen.SETTINGS },
                                            onStartWorkout = { weekIndex, day ->
                                                sessionWeekIndex = weekIndex
                                                sessionDayIndex = day.dayIndex
                                                screen = Screen.SESSION
                                            }
                                        )
                                    } else {
                                        screen = Screen.ONBOARDING
                                    }
                                }
                                Screen.SESSION -> {
                                    val day = plan?.weeks?.find { it.weekIndex == sessionWeekIndex }
                                        ?.workouts?.find { it.dayIndex == sessionDayIndex }
                                    if (day != null) {
                                        WorkoutSessionScreen(
                                            lang = lang,
                                            day = day,
                                            onExit = {
                                                sessionDayIndex = null
                                                screen = Screen.PLAN
                                            },
                                            onFinish = {
                                                if (!day.completed) {
                                                    viewModel.markWorkoutComplete(sessionWeekIndex, day.dayIndex)
                                                }
                                                sessionDayIndex = null
                                                screen = Screen.PLAN
                                            }
                                        )
                                    } else {
                                        screen = Screen.PLAN
                                    }
                                }
                                Screen.SETTINGS -> {
                                    SettingsScreen(
                                        lang = lang,
                                        onLangChange = viewModel::setLang,
                                        themeMode = themeMode,
                                        onThemeModeChange = viewModel::setThemeMode,
                                        onEditProfile = {
                                            screen = Screen.ONBOARDING
                                        },
                                        onRegeneratePlan = {
                                            viewModel.regeneratePlan {
                                                selectedWeekIndex = 0
                                                screen = Screen.PLAN
                                            }
                                        },
                                        onOpenProgress = { screen = Screen.PROGRESS },
                                        onBack = { screen = Screen.PLAN },
                                        remindersEnabled = remindersEnabled,
                                        reminderHour = reminderHour,
                                        onRemindersChange = viewModel::setReminders
                                    )
                                }
                                Screen.PROGRESS -> {
                                    ProgressScreen(
                                        lang = lang,
                                        weightHistory = weightHistory,
                                        currentWeight = plan?.profile?.weightKg ?: 75.0,
                                        onLogWeight = { weight, saved -> viewModel.logWeight(weight, saved) },
                                        onDeleteWeightEntry = viewModel::deleteWeightEntry,
                                        onBack = { screen = Screen.SETTINGS }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

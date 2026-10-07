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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.ividi.militarycalisthenics.data.PlanRepository
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
            val themeMode by viewModel.themeMode.collectAsState()

            MilitaryCalisthenicsTheme(themeMode = themeMode) {
                val isLoaded by viewModel.isLoaded.collectAsState()
                val plan by viewModel.plan.collectAsState()
                val lang by viewModel.lang.collectAsState()
                val weightHistory by viewModel.weightHistory.collectAsState()
                val remindersEnabled by viewModel.remindersEnabled.collectAsState()
                val reminderHour by viewModel.reminderHour.collectAsState()
                var screen by rememberSaveable { mutableStateOf(Screen.SPLASH) }
                var sessionDayIndex by rememberSaveable { mutableStateOf<Int?>(null) }
                var splashFinished by rememberSaveable { mutableStateOf(false) }
                var sessionWeekIndex by rememberSaveable { mutableStateOf(0) }

                BackHandler(screen == Screen.SETTINGS || screen == Screen.PROGRESS) {
                    screen = if (screen == Screen.PROGRESS) Screen.SETTINGS else Screen.PLAN
                }

                LaunchedEffect(splashFinished, isLoaded) {
                    if (splashFinished && isLoaded && screen == Screen.SPLASH) {
                        screen = if (plan != null) Screen.PLAN else Screen.ONBOARDING
                    }
                }

                ProvideLang(lang) {
                    AnimatedContent(
                        targetState = if (isLoaded) screen else Screen.SPLASH,
                        transitionSpec = {
                            (slideInHorizontally(tween(350)) { it / 4 } + fadeIn(tween(350))) togetherWith
                                (slideOutHorizontally(tween(200)) { -it / 4 } + fadeOut(tween(200)))
                        },
                        modifier = Modifier.fillMaxSize().background(BgBase),
                        label = "screenTransition"
                    ) { current ->
                        when (current) {
                            Screen.SPLASH -> {
                                SplashScreen {
                                    splashFinished = true
                                }
                            }
                            Screen.ONBOARDING -> {
                                OnboardingScreen(lang = lang) { profile ->
                                    viewModel.generatePlan(profile)
                                    screen = Screen.PLAN
                                }
                            }
                            Screen.PLAN -> {
                                val currentPlan = plan
                                if (currentPlan != null) {
                                    val planCompletionAcknowledged by viewModel.planCompletionAcknowledged.collectAsState()
                                    val lastWeek = currentPlan.weeks.maxByOrNull { it.weekIndex }
                                    val isPlanComplete = lastWeek != null &&
                                        lastWeek.workouts.isNotEmpty() &&
                                        lastWeek.workouts.all { it.completed }
                                    PlanScreen(
                                        plan = currentPlan,
                                        lang = lang,
                                        shouldShowPlanComplete = isPlanComplete && !planCompletionAcknowledged,
                                        nextLevel = currentPlan.profile.level.next,
                                        onToggleCompleted = viewModel::toggleWorkoutCompleted,
                                        onRepeatPlan = viewModel::regeneratePlan,
                                        onLevelUp = viewModel::levelUp,
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
                                                viewModel.toggleWorkoutCompleted(sessionWeekIndex, day.dayIndex)
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
                                    onResetProfile = {
                                        viewModel.resetProfile()
                                        screen = Screen.ONBOARDING
                                    },
                                    onRegeneratePlan = {
                                        viewModel.regeneratePlan()
                                        screen = Screen.PLAN
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
                                    onLogWeight = viewModel::logWeight,
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

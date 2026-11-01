package com.measurepulse.ar.ui.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.measurepulse.ar.data.UserPreferencesRepository
import com.measurepulse.ar.ui.components.TwilightGradient
import com.measurepulse.ar.ui.components.MonetizationHost
import com.measurepulse.ar.data.local.ScanCategory
import com.measurepulse.ar.ui.screens.advisor.AiRoomAdvisorScreen
import com.measurepulse.ar.ui.screens.projects.SavedProjectsScreen
import com.measurepulse.ar.ui.screens.settings.GlassWidgetsAndProScreen
import androidx.compose.runtime.LaunchedEffect
import com.measurepulse.ar.ui.screens.artape.ArTapeMeasureScreen
import com.measurepulse.ar.ui.screens.blueprint.RoomBlueprintDetailsScreen
import com.measurepulse.ar.ui.screens.caliper.FrostedCaliperScreen
import com.measurepulse.ar.ui.screens.level.FrostedBubbleLevelScreen
import com.measurepulse.ar.ui.screens.rangefinder.AiRangefinderScreen
import com.measurepulse.ar.ui.screens.home.HomeScreen
import com.measurepulse.ar.ui.screens.onboarding.OnboardingScreen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import androidx.compose.foundation.background
import javax.inject.Inject

object Routes {
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
    const val AR_TAPE = "ar_tape"
    const val BLUEPRINT = "blueprint/{scanId}"
    fun blueprint(scanId: Long) = "blueprint/$scanId"
    const val RANGEFINDER = "rangefinder"
    const val CALIPER = "caliper"
    const val LEVEL = "level"
    const val AI_ADVISOR_BASE = "ai_advisor"
    const val AI_ADVISOR = "ai_advisor?scanId={scanId}"
    fun aiAdvisor(scanId: Long? = null) = if (scanId == null) AI_ADVISOR_BASE else "ai_advisor?scanId=$scanId"
    const val PROJECTS = "projects"
    const val PRO = "pro"
}

@HiltViewModel
class StartViewModel @Inject constructor(prefs: UserPreferencesRepository) : ViewModel() {
    /** null while DataStore loads (a single frame); then the first screen to show. */
    val startRoute = prefs.preferences
        .map { if (it.onboardingComplete) Routes.HOME else Routes.ONBOARDING }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)
}

@Composable
fun MeasurePulseNavHost(startViewModel: StartViewModel = hiltViewModel()) {
    val start by startViewModel.startRoute.collectAsStateWithLifecycle()
    val startRoute = start
    if (startRoute == null) {
        Box(Modifier.fillMaxSize().background(TwilightGradient))
        return
    }
    val nav = rememberNavController()
    // Widget taps arrive as deep-link routes.
    val pendingLink by DeepLinks.pending.collectAsStateWithLifecycle()
    LaunchedEffect(pendingLink) {
        val route = pendingLink ?: return@LaunchedEffect
        DeepLinks.consume()
        runCatching { nav.navigate(route) { launchSingleTop = true } }
    }
    Box(Modifier.fillMaxSize()) {
    NavHost(
        navController = nav,
        startDestination = startRoute,
        enterTransition = { fadeIn(tween(280)) },
        exitTransition = { fadeOut(tween(200)) },
        popEnterTransition = { fadeIn(tween(280)) },
        popExitTransition = { fadeOut(tween(200)) },
    ) {
        composable(Routes.ONBOARDING) {
            OnboardingScreen(
                onFinished = {
                    nav.navigate(Routes.HOME) { popUpTo(Routes.ONBOARDING) { inclusive = true } }
                },
            )
        }
        composable(Routes.HOME) {
            HomeScreen(
                onOpenArTape = { nav.navigate(Routes.AR_TAPE) },
                onOpenScan = { nav.navigate(Routes.blueprint(it)) },
                onOpenRangefinder = { nav.navigate(Routes.RANGEFINDER) },
                onOpenCaliper = { nav.navigate(Routes.CALIPER) },
                onOpenLevel = { nav.navigate(Routes.LEVEL) },
                onOpenAiAdvisor = { nav.navigate(Routes.aiAdvisor()) },
                onOpenProjects = { nav.navigate(Routes.PROJECTS) },
                onOpenPro = { nav.navigate(Routes.PRO) },
            )
        }
        // Screens 3–10 are built in Prompts 3–7; these placeholders keep every Home button working until then.
        composable(Routes.AR_TAPE) {
            ArTapeMeasureScreen(
                onBack = { nav.popBackStack() },
                onOpenBlueprint = { id ->
                    nav.navigate(Routes.blueprint(id)) { popUpTo(Routes.AR_TAPE) { inclusive = true } }
                },
            )
        }
        composable(Routes.BLUEPRINT, arguments = listOf(navArgument("scanId") { type = NavType.LongType })) {
            val id = it.arguments?.getLong("scanId")
            RoomBlueprintDetailsScreen(
                onBack = { nav.popBackStack() },
                onOpenAdvisor = { nav.navigate(Routes.aiAdvisor(id)) },
            )
        }
        composable(Routes.RANGEFINDER) { AiRangefinderScreen(onBack = { nav.popBackStack() }) }
        composable(Routes.CALIPER) { FrostedCaliperScreen(onBack = { nav.popBackStack() }) }
        composable(Routes.LEVEL) { FrostedBubbleLevelScreen(onBack = { nav.popBackStack() }) }
        composable(Routes.AI_ADVISOR, arguments = listOf(navArgument("scanId") { type = NavType.LongType; defaultValue = -1L })) {
            AiRoomAdvisorScreen(onBack = { nav.popBackStack() })
        }
        composable(Routes.PROJECTS) {
            SavedProjectsScreen(
                onBack = { nav.popBackStack() },
                onOpenScan = { scan ->
                    nav.navigate(
                        when (scan.category) {
                            ScanCategory.LEVEL -> Routes.LEVEL
                            ScanCategory.CALIPER -> Routes.CALIPER
                            ScanCategory.RANGEFINDER -> Routes.RANGEFINDER
                            else -> Routes.blueprint(scan.id)
                        }
                    )
                },
                onNewScan = { nav.navigate(Routes.AR_TAPE) },
                onHome = { nav.popBackStack(Routes.HOME, inclusive = false) },
                onAi = { nav.navigate(Routes.aiAdvisor()) },
            )
        }
        composable(Routes.PRO) { GlassWidgetsAndProScreen(onBack = { nav.popBackStack() }) }
    }
    // Rewarded-export offer dialog + its toasts, shared by every screen.
    MonetizationHost(onOpenPro = { nav.navigate(Routes.PRO) { launchSingleTop = true } })
    }
}

package com.openjlpt.app.ui

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.openjlpt.app.data.AppContainer
import com.openjlpt.app.ui.history.HistoryScreen
import com.openjlpt.app.ui.home.HomeScreen
import com.openjlpt.app.ui.mock.MockIntroScreen
import com.openjlpt.app.ui.practice.PracticeSetupScreen
import com.openjlpt.app.ui.quiz.QuizScreen
import com.openjlpt.app.ui.result.ResultScreen
import com.openjlpt.app.ui.settings.SettingsScreen
import com.openjlpt.core.model.JlptLevel
import com.openjlpt.core.model.QuestionType
import com.openjlpt.core.model.Section
import com.openjlpt.core.model.TestMode

object Routes {
    const val HOME = "home"
    const val PRACTICE = "practice/{level}/{section}"
    const val MOCK_INTRO = "mock/{level}"
    const val QUIZ = "quiz/{mode}/{level}?section={section}&type={type}&count={count}"
    const val RESULT = "result/{attemptId}"
    const val HISTORY = "history/{level}"
    const val SETTINGS = "settings"

    fun practice(level: JlptLevel, section: Section) = "practice/${level.name}/${section.name}"
    fun mockIntro(level: JlptLevel) = "mock/${level.name}"
    fun quiz(mode: TestMode, level: JlptLevel, section: Section? = null, type: QuestionType? = null, count: Int = 0) =
        "quiz/${mode.name}/${level.name}?section=${section?.name.orEmpty()}&type=${type?.name.orEmpty()}&count=$count"
    fun result(attemptId: Long) = "result/$attemptId"
    fun history(level: JlptLevel) = "history/${level.name}"
}

/** Creates a ViewModel with a plain factory lambda (no DI framework). */
@Composable
inline fun <reified VM : ViewModel> appViewModel(crossinline create: CreationExtras.() -> VM): VM =
    viewModel(factory = viewModelFactory { initializer { create() } })

@Composable
fun AppNavHost(container: AppContainer) {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                container = container,
                onPractice = { level, section -> nav.navigate(Routes.practice(level, section)) },
                onMock = { level -> nav.navigate(Routes.mockIntro(level)) },
                onReview = { level -> nav.navigate(Routes.quiz(TestMode.REVIEW, level)) },
                onHistory = { level -> nav.navigate(Routes.history(level)) },
                onSettings = { nav.navigate(Routes.SETTINGS) },
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(container = container, onBack = { nav.popBackStack() })
        }
        composable(
            Routes.PRACTICE,
            arguments = listOf(
                navArgument("level") { type = NavType.StringType },
                navArgument("section") { type = NavType.StringType },
            ),
        ) { entry ->
            val level = JlptLevel.valueOf(entry.arguments?.getString("level") ?: "N5")
            val section = Section.valueOf(entry.arguments?.getString("section") ?: Section.VOCABULARY.name)
            PracticeSetupScreen(
                container = container,
                level = level,
                section = section,
                onBack = { nav.popBackStack() },
                onStart = { type, count -> nav.navigate(Routes.quiz(TestMode.PRACTICE, level, section, type, count)) },
            )
        }
        composable(
            Routes.MOCK_INTRO,
            arguments = listOf(navArgument("level") { type = NavType.StringType }),
        ) { entry ->
            val level = JlptLevel.valueOf(entry.arguments?.getString("level") ?: "N5")
            MockIntroScreen(
                container = container,
                level = level,
                onBack = { nav.popBackStack() },
                onStart = {
                    nav.navigate(Routes.quiz(TestMode.MOCK, level)) {
                        popUpTo(Routes.MOCK_INTRO) { inclusive = true }
                    }
                },
            )
        }
        composable(
            Routes.QUIZ,
            arguments = listOf(
                navArgument("mode") { type = NavType.StringType },
                navArgument("level") { type = NavType.StringType },
                navArgument("section") { type = NavType.StringType; defaultValue = "" },
                navArgument("type") { type = NavType.StringType; defaultValue = "" },
                navArgument("count") { type = NavType.IntType; defaultValue = 0 },
            ),
        ) {
            QuizScreen(
                container = container,
                onExit = { nav.popBackStack() },
                onFinished = { attemptId ->
                    nav.navigate(Routes.result(attemptId)) {
                        popUpTo(Routes.QUIZ) { inclusive = true }
                    }
                },
            )
        }
        composable(
            Routes.RESULT,
            arguments = listOf(navArgument("attemptId") { type = NavType.LongType }),
        ) { entry ->
            ResultScreen(
                container = container,
                attemptId = entry.arguments?.getLong("attemptId") ?: 0L,
                onBack = { nav.popBackStack() },
            )
        }
        composable(
            Routes.HISTORY,
            arguments = listOf(navArgument("level") { type = NavType.StringType }),
        ) { entry ->
            val level = JlptLevel.valueOf(entry.arguments?.getString("level") ?: "N5")
            HistoryScreen(
                container = container,
                level = level,
                onBack = { nav.popBackStack() },
                onOpenAttempt = { id -> nav.navigate(Routes.result(id)) },
            )
        }
    }
}

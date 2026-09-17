package dev.brown.wordy.ui.navigation

import kotlinx.serialization.Serializable

@Serializable object OnboardingRoute
@Serializable object HomeRoute
@Serializable object BrowseRoute
@Serializable object StatsRoute
@Serializable object SettingsRoute
/**
 * 학습 세션. [quizCount] 가 0보다 크면 새 단어 없이 문제만 푸는 퀴즈 전용 모드.
 * null 대신 0 을 기본값으로 둔 이유는 Navigation 의 기본 인자 직렬화가
 * nullable Int 보다 단순해서다.
 */
@Serializable data class SessionRoute(val extraNew: Int = 0, val quizCount: Int = 0)
@Serializable data class UnitRoute(val unitId: String, val title: String)
@Serializable data class WordRoute(val wordId: String)
@Serializable data class PracticeRoute(val wordId: String)

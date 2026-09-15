package dev.brown.wordy.ui.navigation

import kotlinx.serialization.Serializable

@Serializable object OnboardingRoute
@Serializable object HomeRoute
@Serializable object BrowseRoute
@Serializable object SettingsRoute
@Serializable data class SessionRoute(val extraNew: Int = 0)
@Serializable data class UnitRoute(val unitId: String, val title: String)
@Serializable data class CoreTrackRoute(val dummy: Int = 0)
@Serializable data class WordRoute(val wordId: String)
@Serializable data class PracticeRoute(val wordId: String)
@Serializable object SearchRoute

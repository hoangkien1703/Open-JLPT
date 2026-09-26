package com.openjlpt.app.data

import android.content.Context
import com.openjlpt.app.data.db.AppDatabase

/** Hand-rolled dependency container, created once by the Application. */
class AppContainer(context: Context) {
    private val database = AppDatabase.create(context)
    val questions = QuestionRepository(context.assets)
    val history = HistoryRepository(database.historyDao())
    val settings = SettingsStore(context)
}

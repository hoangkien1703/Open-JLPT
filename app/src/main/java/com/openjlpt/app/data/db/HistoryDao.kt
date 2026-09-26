package com.openjlpt.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
abstract class HistoryDao {
    @Insert
    abstract suspend fun insertAttempt(attempt: AttemptEntity): Long

    @Insert
    abstract suspend fun insertAnswers(answers: List<AnswerEntity>)

    @Transaction
    open suspend fun insertAttemptWithAnswers(attempt: AttemptEntity, answers: List<AnswerEntity>): Long {
        val id = insertAttempt(attempt)
        insertAnswers(answers.map { it.copy(attemptId = id) })
        return id
    }

    @Query("SELECT * FROM attempts WHERE level = :level ORDER BY finishedAt DESC")
    abstract fun attempts(level: String): Flow<List<AttemptEntity>>

    @Query("SELECT * FROM attempts WHERE id = :id")
    abstract suspend fun attempt(id: Long): AttemptEntity?

    @Query("SELECT * FROM answers WHERE attemptId = :attemptId ORDER BY position")
    abstract suspend fun answers(attemptId: Long): List<AnswerEntity>

    @Query(
        "SELECT section AS section, COUNT(*) AS total, SUM(correct) AS correct " +
            "FROM answers WHERE level = :level GROUP BY section",
    )
    abstract fun sectionStats(level: String): Flow<List<SectionStat>>

    /** Questions whose most recent answer was wrong or missing. */
    @Query(
        "SELECT questionId FROM answers WHERE level = :level AND correct = 0 AND id IN " +
            "(SELECT MAX(id) FROM answers WHERE level = :level GROUP BY questionId) " +
            "ORDER BY answeredAt DESC",
    )
    abstract fun mistakeIds(level: String): Flow<List<String>>

    @Query("DELETE FROM attempts WHERE level = :level")
    abstract suspend fun clear(level: String)
}

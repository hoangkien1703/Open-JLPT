package com.openjlpt.app.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "attempts", indices = [Index("level")])
data class AttemptEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val level: String,
    /** TestMode name: PRACTICE, MOCK or REVIEW. */
    val mode: String,
    val title: String,
    /** Section name for practice, null for mock tests and reviews. */
    val section: String?,
    val startedAt: Long,
    val finishedAt: Long,
    val correct: Int,
    val total: Int,
    /** Estimated JLPT score out of [maxScore]; mock tests only. */
    val score: Int?,
    val maxScore: Int?,
    val passed: Boolean?,
)

@Entity(
    tableName = "answers",
    foreignKeys = [
        ForeignKey(
            entity = AttemptEntity::class,
            parentColumns = ["id"],
            childColumns = ["attemptId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("attemptId"), Index(value = ["level", "questionId"])],
)
data class AnswerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val attemptId: Long,
    val level: String,
    val questionId: String,
    val section: String,
    /** Order of the question within the attempt. */
    val position: Int,
    /** Index into the question's authored choices, or -1 if unanswered. */
    val selected: Int,
    @ColumnInfo(name = "correct") val isCorrect: Boolean,
    val answeredAt: Long,
)

data class SectionStat(
    val section: String,
    val total: Int,
    val correct: Int,
)

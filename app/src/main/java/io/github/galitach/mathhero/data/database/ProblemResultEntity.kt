package io.github.galitach.mathhero.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import io.github.galitach.mathhero.data.Operation

@Entity(tableName = "progress_history")
data class ProblemResultEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long,
    val operation: Operation,
    val wasCorrect: Boolean,
    val num1: Int,
    val num2: Int,
    val answer: Int
)
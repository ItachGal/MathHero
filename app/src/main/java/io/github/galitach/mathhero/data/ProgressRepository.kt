package io.github.galitach.mathhero.data

import io.github.galitach.mathhero.data.database.ProblemResultDao
import io.github.galitach.mathhero.data.database.ProblemResultEntity

class ProgressRepository(private val dao: ProblemResultDao) {

    companion object {
        private const val MAX_PROGRESS_ENTRIES = 500 // Increased limit with database
    }

    suspend fun getProgressHistory(): List<ProblemResult> {
        return dao.getAllOnce().map { it.toDomainModel() }
    }

    suspend fun logProblemResult(problem: MathProblem, wasCorrect: Boolean) {
        val operation = try {
            Operation.entries.first { it.symbol == problem.operator }
        } catch (_: NoSuchElementException) {
            return
        }

        val result = ProblemResultEntity(
            timestamp = System.currentTimeMillis(),
            operation = operation,
            wasCorrect = wasCorrect,
            num1 = problem.num1,
            num2 = problem.num2,
            answer = problem.answer.toIntOrNull() ?: 0
        )
        dao.insert(result)

        // Prune the database if it exceeds the max size
        val count = dao.getCount()
        if (count > MAX_PROGRESS_ENTRIES) {
            dao.deleteOldest(count - MAX_PROGRESS_ENTRIES)
        }
    }

    private fun ProblemResultEntity.toDomainModel(): ProblemResult {
        return ProblemResult(
            timestamp = this.timestamp,
            operation = this.operation,
            wasCorrect = this.wasCorrect,
            num1 = this.num1,
            num2 = this.num2,
            answer = this.answer
        )
    }
}
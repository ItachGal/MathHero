package io.github.galitach.mathhero.data

import android.content.Context
import io.github.galitach.mathhero.R
import kotlin.math.max
import kotlin.random.Random

object MathProblemGenerator {

    fun generateProblem(context: Context, settings: DifficultySettings, streak: Int, seed: Long): MathProblem {
        val random = Random(seed)
        val operation = selectOperationByDifficulty(settings.operations, streak, random)

        // Progressive difficulty: increase number range based on streak
        val maxNumber = settings.maxNumber
        val minNumber = if (operation == Operation.MULTIPLICATION || operation == Operation.DIVISION) 2 else 1
        val rangeStart = max(minNumber, (maxNumber * (streak / 50.0)).toInt())
        val rangeEnd = max(rangeStart + 1, (maxNumber * (0.5 + streak / 100.0)).toInt()).coerceAtMost(maxNumber)

        val (question: String, answer: Int, num1: Int, num2: Int) = when (operation) {
            Operation.ADDITION -> generateAddition(random, rangeStart, rangeEnd)
            Operation.SUBTRACTION -> generateSubtraction(random, rangeStart, rangeEnd)
            Operation.MULTIPLICATION -> {
                val (multStart, multEnd) = getProgressiveRangeForCappedOps(minNumber, 12, streak)
                generateMultiplication(random, multStart, multEnd)
            }
            Operation.DIVISION -> {
                val (divStart, divEnd) = getProgressiveRangeForCappedOps(minNumber, 12, streak)
                generateDivision(random, divStart, divEnd)
            }
        }

        val difficulty = calculateDifficulty(num1, num2, operation)
        val distractors = generateDistractors(answer, difficulty, random)
        val explanation = generateExplanation(context, answer, num1, num2, operation, random)

        return MathProblem(
            id = seed.toInt(),
            question = context.getString(R.string.question_format, question),
            answer = answer.toString(),
            distractor1 = distractors.first.toString(),
            distractor2 = distractors.second.toString(),
            difficulty = difficulty,
            explanation = explanation,
            num1 = num1,
            num2 = num2,
            operator = operation.symbol
        )
    }

    private fun getProgressiveRangeForCappedOps(minNumber: Int, cap: Int, streak: Int): Pair<Int, Int> {
        val rangeStart = max(minNumber, (cap * (streak / 50.0)).toInt())
        // Ensure rangeEnd is always greater than rangeStart
        val rangeEnd = max(rangeStart + 1, (cap * (0.5 + streak / 100.0)).toInt()).coerceAtMost(cap)
        return Pair(rangeStart.coerceAtMost(rangeEnd -1), rangeEnd)
    }

    private fun selectOperationByDifficulty(operations: Set<Operation>, streak: Int, random: Random): Operation {
        if (operations.size <= 1) return operations.firstOrNull() ?: Operation.ADDITION

        val easyOps = operations.filter { it == Operation.ADDITION || it == Operation.SUBTRACTION }
        val hardOps = operations.filter { it == Operation.MULTIPLICATION || it == Operation.DIVISION }

        // If streak is low, prefer easy operations if they are available.
        val streakThreshold = 20
        if (streak < streakThreshold && easyOps.isNotEmpty()) {
            // 80% chance for an easy operation, 20% for a hard one (if available)
            return if (random.nextInt(10) < 8) {
                easyOps.random(random)
            } else {
                hardOps.randomOrNull(random) ?: easyOps.random(random)
            }
        }

        // Otherwise, pick any operation randomly.
        return operations.random(random)
    }


    private fun calculateDifficulty(num1: Int, num2: Int, operation: Operation): Int {
        val numberMagnitude = (num1 + num2) / 2.0
        val operationMultiplier = when (operation) {
            Operation.ADDITION -> 1.0
            Operation.SUBTRACTION -> 1.2
            Operation.MULTIPLICATION -> 2.0
            Operation.DIVISION -> 2.5
        }
        return (numberMagnitude / 10.0 * operationMultiplier).coerceIn(1.0, 10.0).toInt()
    }

    private fun generateAddition(random: Random, min: Int, max: Int): Quadruple<String, Int, Int, Int> {
        val a = random.nextInt(min, max + 1)
        val b = random.nextInt(min, max + 1)
        return Quadruple("$a + $b", a + b, a, b)
    }

    private fun generateSubtraction(random: Random, min: Int, max: Int): Quadruple<String, Int, Int, Int> {
        val a = random.nextInt(min, max + 1)
        val b = random.nextInt(min, a + 1) // Ensure positive result
        return Quadruple("$a - $b", a - b, a, b)
    }

    private fun generateMultiplication(random: Random, min: Int, max: Int): Quadruple<String, Int, Int, Int> {
        val a = random.nextInt(min, max + 1)
        val b = random.nextInt(min, max + 1)
        return Quadruple("$a × $b", a * b, a, b)
    }

    private fun generateDivision(random: Random, min: Int, max: Int): Quadruple<String, Int, Int, Int> {
        val b = random.nextInt(min, max + 1)
        val answer = random.nextInt(min, max + 1)
        val a = answer * b
        return Quadruple("$a ÷ $b", answer, a, b)
    }

    private fun generateDistractors(answer: Int, difficulty: Int, random: Random): Pair<Int, Int> {
        val distractors = mutableSetOf<Int>()
        val range = when(difficulty) {
            in 1..2 -> 3
            in 3..5 -> 5
            in 6..8 -> 10
            else -> 15
        }

        while (distractors.size < 2) {
            val offset = random.nextInt(-range, range + 1)
            if (offset == 0) continue
            val distractor = answer + offset
            if (distractor >= 0 && distractor != answer) {
                distractors.add(distractor)
            }
        }
        return distractors.first() to distractors.last()
    }

    private fun generateExplanation(context: Context, answer: Int, num1: Int, num2: Int, operation: Operation, random: Random): String {
        val explanationTemplates = when (operation) {
            Operation.ADDITION -> context.resources.getStringArray(R.array.addition_explanations)
            Operation.SUBTRACTION -> context.resources.getStringArray(R.array.subtraction_explanations)
            Operation.MULTIPLICATION -> context.resources.getStringArray(R.array.multiplication_explanations)
            Operation.DIVISION -> context.resources.getStringArray(R.array.division_explanations)
        }

        val randomTemplate = explanationTemplates.random(random)
        return String.format(randomTemplate, num1, num2, answer)
    }
}

// Helper class to return multiple values
data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
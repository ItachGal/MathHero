package io.github.galitach.mathhero.data

import android.content.Context
import io.github.galitach.mathhero.R
import kotlin.math.max
import kotlin.random.Random

object MathProblemGenerator {

    fun generateProblem(context: Context, settings: DifficultySettings, streak: Int, seed: Long): MathProblem {
        val random = Random(seed)
        val operation = settings.operations.random(random)

        // Progressive difficulty: increase number range based on streak
        val maxNumber = settings.maxNumber
        val minNumber = if (operation == Operation.MULTIPLICATION || operation == Operation.DIVISION) 2 else 1
        val rangeStart = max(minNumber, (maxNumber * (streak / 50.0)).toInt())
        val rangeEnd = max(rangeStart + 1, (maxNumber * (0.5 + streak / 100.0)).toInt()).coerceAtMost(maxNumber)

        val (question: String, answer: Int, num1: Int, num2: Int) = when (operation) {
            Operation.ADDITION -> generateAddition(random, rangeStart, rangeEnd)
            Operation.SUBTRACTION -> generateSubtraction(random, rangeStart, rangeEnd)
            Operation.MULTIPLICATION -> generateMultiplication(random, rangeStart, rangeEnd.coerceAtMost(12)) // Keep multiplication manageable
            Operation.DIVISION -> generateDivision(random, rangeStart, rangeEnd.coerceAtMost(12))
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
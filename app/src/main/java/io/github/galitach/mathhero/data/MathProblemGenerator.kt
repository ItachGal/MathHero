package io.github.galitach.mathhero.data

import android.content.Context
import io.github.galitach.mathhero.R
import kotlin.random.Random

object MathProblemGenerator {

    fun generateProblem(context: Context, seed: Long): MathProblem {
        val random = Random(seed)
        val difficulty = random.nextInt(1, 11)

        val (question: String, answer: Int, num1: Int, num2: Int, operator: String) = when (difficulty) {
            in 1..2 -> generateAddition(random, 1, 10)
            in 3..4 -> generateSubtraction(random, 1, 20)
            in 5..6 -> generateAddition(random, 10, 50)
            in 7..8 -> generateMultiplication(random, 2, 10)
            else -> generateDivision(random, 2, 10)
        }

        val distractors = generateDistractors(answer, difficulty, random)
        val explanation = generateExplanation(context, question, answer, random)

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
            operator = operator
        )
    }

    private fun generateAddition(random: Random, min: Int, max: Int): Quadruple<String, Int, Int, Int, String> {
        val a = random.nextInt(min, max + 1)
        val b = random.nextInt(min, max + 1)
        return Quadruple("$a + $b", a + b, a, b, "+")
    }

    private fun generateSubtraction(random: Random, min: Int, max: Int): Quadruple<String, Int, Int, Int, String> {
        val a = random.nextInt(min, max + 1)
        val b = random.nextInt(min, a + 1)
        return Quadruple("$a - $b", a - b, a, b, "-")
    }

    private fun generateMultiplication(random: Random, min: Int, max: Int): Quadruple<String, Int, Int, Int, String> {
        val a = random.nextInt(min, max + 1)
        val b = random.nextInt(min, max + 1)
        return Quadruple("$a × $b", a * b, a, b, "×")
    }

    private fun generateDivision(random: Random, min: Int, max: Int): Quadruple<String, Int, Int, Int, String> {
        val b = random.nextInt(min, max + 1)
        val answer = random.nextInt(min, max + 1)
        val a = answer * b
        return Quadruple("$a ÷ $b", answer, a, b, "÷")
    }

    private fun generateDistractors(answer: Int, difficulty: Int, random: Random): Pair<Int, Int> {
        val distractors = mutableSetOf<Int>()
        val range = when(difficulty) {
            in 1..4 -> 5
            in 5..8 -> 10
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

    private fun generateExplanation(context: Context, question: String, answer: Int, random: Random): String {
        val parts = question.replace(" ", "").split(Regex("[+\\-×÷]"))
        val num1 = parts[0].toInt()
        val num2 = parts[1].toInt()

        val explanationTemplates = when {
            question.contains('+') -> context.resources.getStringArray(R.array.addition_explanations)
            question.contains('-') -> context.resources.getStringArray(R.array.subtraction_explanations)
            question.contains('×') -> context.resources.getStringArray(R.array.multiplication_explanations)
            question.contains('÷') -> context.resources.getStringArray(R.array.division_explanations)
            else -> return context.getString(R.string.default_explanation, answer)
        }

        val randomTemplate = explanationTemplates.random(random)
        return String.format(randomTemplate, num1, num2, answer)
    }
}

// Helper class to return multiple values
data class Quadruple<A, B, C, D, E>(val first: A, val second: B, val third: C, val fourth: D, val fifth: E)
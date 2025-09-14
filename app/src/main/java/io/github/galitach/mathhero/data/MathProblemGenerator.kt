package io.github.galitach.mathhero.data

import android.content.Context
import android.util.Log
import io.github.galitach.mathhero.R
import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.round
import kotlin.random.Random

/**
 * A deterministic, progressive, and performant math problem generator.
 *
 * This object creates math problems based on difficulty settings and a user's streak.
 * Key principles:
 * - **Deterministic:** Given the same seed, it will always produce the exact same problem.
 * - **Progressive Difficulty:** Problems get harder as the streak increases by using a "sliding window"
 *   for number selection and weighting operations.
 * - **Performant:** All generation logic, including for division, is designed to be fast and
 *   avoid expensive computations or unbounded memory usage.
 * - **Robust:** It handles invalid settings gracefully and is architecturally thread-safe.
 */
object MathProblemGenerator {

    // === CONFIGURABLE CONSTANTS ===
    private const val STREAK_CAP = 100.0
    private const val MIN_WINDOW_FRACTION = 0.4   // At max streak, window is 40% of total range
    private const val MIN_WINDOW_SIZE = 3         // Never shrink below this many values
    private const val MIN_DIVISOR = 2             // Avoid trivial ÷1 problems
    private const val MIN_QUOTIENT = 2            // Avoid trivial answers of 1
    private const val PRACTICAL_MAX_DIVISOR = 12  // Keep divisors in a mentally-solvable range
    private const val WEIGHT_RAMP = 14.0          // Controls how fast hard ops become common
    private const val MAX_DISTRACTOR_DISTANCE_FACTOR = 3
    private const val MAX_DISTRACTOR_DISTANCE_CAP = 25

    fun generateProblem(
        context: Context,
        settings: DifficultySettings,
        streak: Int,
        seed: Long
    ): MathProblem {
        val random = Random(seed)

        // Validate settings
        val requestedMax = settings.maxNumber
        val operation = selectWeightedOperation(settings.operations, streak, random)
        val minNumber = if (operation == Operation.MULTIPLICATION || operation == Operation.DIVISION) MIN_DIVISOR else 1
        val safeMaxNumber = max(requestedMax, minNumber)
        if (safeMaxNumber != requestedMax) {
            Log.w("MathProblemGenerator", "Invalid maxNumber=$requestedMax. Using $safeMaxNumber instead.")
        }

        // Progressive operand range
        val (rangeStart, rangeEnd) = getProgressiveRange(minNumber, safeMaxNumber, streak)

        // Generate problem
        val (question, answer, num1, num2) = when (operation) {
            Operation.ADDITION -> generateAddition(random, rangeStart, rangeEnd)
            Operation.SUBTRACTION -> generateSubtraction(random, rangeStart, rangeEnd)
            Operation.MULTIPLICATION -> generateMultiplication(random, rangeStart, rangeEnd)
            Operation.DIVISION -> generateDivision(random, rangeStart, rangeEnd)
        }

        val difficulty = calculateDifficulty(num1, num2, answer, operation)
        val (d1, d2) = generateDistractors(answer, difficulty, random)
        val explanation = generateExplanation(context, answer, num1, num2, operation, random)

        return MathProblem(
            id = stableId(seed, operation, num1, num2, answer),
            question = context.getString(R.string.question_format, question),
            answer = answer.toString(),
            distractor1 = d1.toString(),
            distractor2 = d2.toString(),
            difficulty = difficulty,
            explanation = explanation,
            num1 = num1,
            num2 = num2,
            operator = operation.symbol
        )
    }

    // === RANGE PROGRESSION ===
    private fun getProgressiveRange(minNumber: Int, maxNumber: Int, streak: Int): Pair<Int, Int> {
        if (minNumber >= maxNumber) return minNumber to maxNumber

        val prog = (streak / STREAK_CAP).coerceIn(0.0, 1.0)
        val totalRange = maxNumber - minNumber + 1

        // Window shrinks as progression increases, but never below a minimum size.
        val minWindow = min(MIN_WINDOW_SIZE, totalRange)
        val targetWindowSize = totalRange * (1.0 - (1.0 - MIN_WINDOW_FRACTION) * prog)
        val windowSize = round(targetWindowSize).toInt().coerceIn(minWindow, totalRange)

        // The start of the window slides up towards the max number with progression.
        val maxPossibleStart = maxNumber - windowSize + 1
        val start = (minNumber + (maxPossibleStart - minNumber) * prog).toInt().coerceIn(minNumber, maxPossibleStart)
        val end = (start + windowSize - 1)

        return start to end
    }

    // === OPERATION SELECTION ===
    private fun selectWeightedOperation(ops: Set<Operation>, streak: Int, random: Random): Operation {
        if (ops.isEmpty()) return Operation.ADDITION
        if (ops.size == 1) return ops.first()
        val prog = (streak / STREAK_CAP).coerceIn(0.0, 1.0)
        val weights = ops.associateWith { op ->
            when (op) {
                Operation.ADDITION -> 10.0
                Operation.SUBTRACTION -> 10.0
                Operation.MULTIPLICATION -> 1.0 + WEIGHT_RAMP * prog
                Operation.DIVISION -> 1.0 + WEIGHT_RAMP * prog
            }
        }.entries.shuffled(random)
        val total = weights.sumOf { it.value }
        var r = random.nextDouble() * total
        for ((op, w) in weights) {
            if (r < w) return op
            r -= w
        }
        return weights.last().key
    }

    // === PROBLEM GENERATORS ===
    private fun generateAddition(r: Random, min: Int, max: Int): Quadruple<String, Int, Int, Int> {
        val a = r.nextInt(min, max + 1)
        val b = r.nextInt(min, max + 1)
        return Quadruple("$a + $b", a + b, a, b)
    }

    private fun generateSubtraction(r: Random, min: Int, max: Int): Quadruple<String, Int, Int, Int> {
        val a = r.nextInt(min, max + 1)
        val b = r.nextInt(min, a + 1)
        return Quadruple("$a - $b", a - b, a, b)
    }

    private fun generateMultiplication(r: Random, min: Int, max: Int): Quadruple<String, Int, Int, Int> {
        val a = r.nextInt(min, max + 1)
        val b = r.nextInt(min, max + 1)
        return Quadruple("$a × $b", a * b, a, b)
    }

    private fun generateDivision(r: Random, aRangeStart: Int, aRangeEnd: Int): Quadruple<String, Int, Int, Int> {
        val attempts = 50
        for (i in 0 until attempts) {
            // 1. Pick a potential dividend 'a' from the progressive range.
            val a = r.nextInt(aRangeStart, aRangeEnd + 1)

            // 2. Find all valid divisors for 'a'.
            val divisors = (MIN_DIVISOR..PRACTICAL_MAX_DIVISOR).filter { b ->
                if (a % b == 0) {
                    val q = a / b
                    q >= MIN_QUOTIENT // Ensure the answer isn't trivial
                } else {
                    false
                }
            }

            // 3. If we found any, pick one and we're done.
            if (divisors.isNotEmpty()) {
                val b = divisors.random(r)
                val q = a / b
                return Quadruple("$a ÷ $b", q, a, b)
            }
        }

        // 4. Fallback if we couldn't find a suitable problem after many attempts.
        // This might happen if the progressive range is full of small prime numbers.
        // Let's just construct a simple, valid problem.
        val b = r.nextInt(MIN_DIVISOR, 5) // small divisor
        val q = r.nextInt(MIN_QUOTIENT, 10) // small quotient
        val a = b * q
        return Quadruple("$a ÷ $b", q, a, b)
    }


    // === DIFFICULTY ===
    private fun calculateDifficulty(num1: Int, num2: Int, answer: Int, op: Operation): Int {
        val magnitude = when (op) {
            Operation.ADDITION, Operation.SUBTRACTION -> max(num1, num2).toDouble()
            Operation.MULTIPLICATION -> answer.toDouble()
            Operation.DIVISION -> num1.toDouble()
        }.coerceAtLeast(1.0)
        val base = log10(magnitude) * 3.0
        val opBonus = when (op) {
            Operation.ADDITION -> 0.0
            Operation.SUBTRACTION -> 0.3
            Operation.MULTIPLICATION -> 1.8
            Operation.DIVISION -> 2.1
        }
        val spread = abs(num1 - num2) / 50.0
        return round((base + opBonus + spread).coerceIn(0.5, 10.0)).toInt().coerceIn(1, 10)
    }

    // === DISTRACTORS ===
    private fun generateDistractors(answer: Int, difficulty: Int, r: Random): Pair<Int, Int> {
        val near = max(1, min(5, difficulty))
        val far = max(3, min(MAX_DISTRACTOR_DISTANCE_CAP, difficulty * MAX_DISTRACTOR_DISTANCE_FACTOR))
        val offsets = mutableListOf<Int>()

        // Strategy 1: A close distractor
        offsets += (1 + r.nextInt(near)) * if (r.nextBoolean()) 1 else -1

        // Strategy 2: A farther, more distinct distractor
        var off2 = (near + r.nextInt(far - near + 1)) * if (r.nextBoolean()) 1 else -1
        if (off2 == 0 || off2 == offsets[0]) off2 += if (off2 >= 0) 1 else -1
        offsets += off2

        // Convert offsets to actual distractor values, filtering invalids
        val distractors = mutableListOf<Int>()
        for (off in offsets) {
            val candidate = answer + off
            if (candidate >= 0 && candidate != answer && candidate !in distractors) {
                distractors.add(candidate)
            }
        }

        // Fallback fill if strategies produced duplicates or invalid numbers
        var step = 1
        while (distractors.size < 2) {
            val up = answer + step
            val down = answer - step
            if (up >= 0 && up != answer && up !in distractors) distractors.add(up)
            if (distractors.size < 2 && down >= 0 && down != answer && down !in distractors) distractors.add(down)
            step++
        }

        distractors.sort() // Sort for deterministic ordering (d1 < d2)
        return distractors[0] to distractors[1]
    }

    // === EXPLANATION ===
    private fun generateExplanation(
        context: Context, answer: Int, num1: Int, num2: Int, op: Operation, random: Random
    ): String {
        val templates = when (op) {
            Operation.ADDITION -> context.resources.getStringArray(R.array.addition_explanations)
            Operation.SUBTRACTION -> context.resources.getStringArray(R.array.subtraction_explanations)
            Operation.MULTIPLICATION -> context.resources.getStringArray(R.array.multiplication_explanations)
            Operation.DIVISION -> context.resources.getStringArray(R.array.division_explanations)
        }
        return try {
            String.format(templates.random(random), num1, num2, answer)
        } catch (_: Exception) {
            context.getString(R.string.default_explanation, answer)
        }
    }

    // === STABLE ID ===
    private fun stableId(seed: Long, op: Operation, a: Int, b: Int, ans: Int): Int {
        var h = 1125899906842597L // large prime
        h = 31L * h + seed
        h = 31L * h + op.ordinal
        h = 31L * h + a
        h = 31L * h + b
        h = 31L * h + ans
        return (h xor (h ushr 32)).toInt() and 0x7FFFFFFF
    }
}

// Helper class to return multiple values
data class Quadruple<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)
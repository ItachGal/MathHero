package io.github.galitach.mathhero.ui.views

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import io.github.galitach.mathhero.R
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

class AnimatedBackgroundView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val particles = mutableListOf<Particle>()
    private val path = Path()
    private var animator: ValueAnimator? = null
    private val themeColors: List<Int>

    init {
        val typedArray = context.theme.obtainStyledAttributes(attrs, R.styleable.AnimatedBackgroundView, 0, 0)
        try {
            themeColors = listOf(
                typedArray.getColor(R.styleable.AnimatedBackgroundView_pastelColor1, 0),
                typedArray.getColor(R.styleable.AnimatedBackgroundView_pastelColor2, 0),
                typedArray.getColor(R.styleable.AnimatedBackgroundView_pastelColor3, 0),
                typedArray.getColor(R.styleable.AnimatedBackgroundView_pastelColor4, 0)
            ).filter { it != 0 }
        } finally {
            typedArray.recycle()
        }
    }

    private class Particle(
        var x: Float,
        var y: Float,
        val baseRadius: Float,
        var currentRadius: Float,
        var vx: Float,
        var vy: Float,
        val points: List<MorphPoint>,
        val mass: Float,
        var squeezeFactor: Float = 1f,
        val paint: Paint,
        val matrix: Matrix = Matrix(),
        val anchorPointsX: FloatArray,
        val anchorPointsY: FloatArray,
        val midPointsX: FloatArray,
        val midPointsY: FloatArray
    )

    private class MorphPoint(
        val baseAngle: Float,
        var currentAngle: Float,
        val speed: Float,
        val magnitude: Float
    )

    fun start() {
        if (animator?.isStarted != true && particles.isNotEmpty()) {
            startAnimation()
        }
    }

    fun stop() {
        animator?.cancel()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w > 50 && h > 50 && particles.isEmpty()) {
            createParticles(w, h)
            startAnimation()
        }
    }

    private fun createParticles(width: Int, height: Int) {
        if (themeColors.isEmpty()) return

        val random = Random(System.currentTimeMillis())
        val numParticles = 5
        val minRadius = width / 7f
        val maxRadius = width / 3f
        val pointsPerParticle = 8

        repeat(numParticles) {
            val baseRadius = minRadius + random.nextFloat() * (maxRadius - minRadius)
            val morphPoints = List(pointsPerParticle) { i ->
                val angle = (2 * PI.toFloat() / pointsPerParticle) * i
                MorphPoint(
                    baseAngle = angle,
                    currentAngle = random.nextFloat() * 2 * PI.toFloat(),
                    speed = 0.005f + random.nextFloat() * 0.01f,
                    magnitude = baseRadius * (0.1f + random.nextFloat() * 0.2f)
                )
            }

            val padding = baseRadius
            val spawnX = padding + random.nextFloat() * (width - 2 * padding)
            val spawnY = padding + random.nextFloat() * (height - 2 * padding)
            val color = themeColors.random(random)

            val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            val centerColor = Color.argb(
                random.nextInt(60, 120),
                Color.red(color),
                Color.green(color),
                Color.blue(color)
            )
            paint.shader = RadialGradient(
                0f, 0f, 1f,
                centerColor,
                Color.TRANSPARENT,
                Shader.TileMode.CLAMP
            )

            particles.add(
                Particle(
                    x = spawnX,
                    y = spawnY,
                    baseRadius = baseRadius,
                    currentRadius = baseRadius,
                    vx = (random.nextFloat() - 0.5f) * 0.8f,
                    vy = (random.nextFloat() - 0.5f) * 0.8f,
                    points = morphPoints,
                    mass = baseRadius * baseRadius,
                    paint = paint,
                    anchorPointsX = FloatArray(pointsPerParticle),
                    anchorPointsY = FloatArray(pointsPerParticle),
                    midPointsX = FloatArray(pointsPerParticle),
                    midPointsY = FloatArray(pointsPerParticle)
                )
            )
        }
    }

    private fun startAnimation() {
        animator?.cancel()
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = Long.MAX_VALUE
            interpolator = LinearInterpolator()
            addUpdateListener {
                updateParticles()
                invalidate()
            }
            start()
        }
    }

    private fun updateParticles() {
        particles.forEach { p ->
            p.x += p.vx
            p.y += p.vy

            p.points.forEach { point -> point.currentAngle += point.speed }
            p.squeezeFactor += (1f - p.squeezeFactor) * 0.1f

            val pulse = sin(p.points.first().currentAngle) * p.points.first().magnitude
            p.currentRadius = (p.baseRadius + pulse) * p.squeezeFactor

            if (p.x - p.baseRadius < 0) {
                p.x = p.baseRadius
                p.vx *= -1
            } else if (p.x + p.baseRadius > width) {
                p.x = width - p.baseRadius
                p.vx *= -1
            }
            if (p.y - p.baseRadius < 0) {
                p.y = p.baseRadius
                p.vy *= -1
            } else if (p.y + p.baseRadius > height) {
                p.y = height - p.baseRadius
                p.vy *= -1
            }
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        particles.forEach { p ->
            p.matrix.reset()
            p.matrix.postScale(p.currentRadius * 1.5f, p.currentRadius * 1.5f)
            p.matrix.postTranslate(p.x, p.y)
            p.paint.shader.setLocalMatrix(p.matrix)

            drawJellyShape(canvas, p)
        }
    }

    private fun drawJellyShape(canvas: Canvas, particle: Particle) {
        path.rewind()
        val numPoints = particle.points.size
        if (numPoints < 2) return

        for (i in 0 until numPoints) {
            val point = particle.points[i]
            val radius = particle.currentRadius + sin(point.currentAngle) * point.magnitude
            particle.anchorPointsX[i] = particle.x + cos(point.baseAngle) * radius
            particle.anchorPointsY[i] = particle.y + sin(point.baseAngle) * radius
        }

        for (i in 0 until numPoints) {
            val p1x = particle.anchorPointsX[i]
            val p1y = particle.anchorPointsY[i]
            val p2x = particle.anchorPointsX[(i + 1) % numPoints]
            val p2y = particle.anchorPointsY[(i + 1) % numPoints]
            particle.midPointsX[i] = (p1x + p2x) / 2
            particle.midPointsY[i] = (p1y + p2y) / 2
        }

        path.moveTo(particle.midPointsX[numPoints - 1], particle.midPointsY[numPoints - 1])
        for (i in 0 until numPoints) {
            path.quadTo(
                particle.anchorPointsX[i], particle.anchorPointsY[i],
                particle.midPointsX[i], particle.midPointsY[i]
            )
        }
        path.close()
        canvas.drawPath(path, particle.paint)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (particles.isNotEmpty()) {
            startAnimation()
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stop()
    }
}
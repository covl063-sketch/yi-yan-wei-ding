package com.yiyanweiding.app.model

import android.graphics.*
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.PI
import kotlin.random.Random

object WeatherBackgroundRenderer {

    /**
     * Generate a weather-themed background bitmap.
     * Overload that takes WeatherType enum.
     */
    fun render(
        widthDp: Int,
        heightDp: Int,
        weatherType: WeatherManager.WeatherType,
        isNight: Boolean,
        density: Float
    ): Bitmap {
        return render(widthDp, heightDp, weatherType.displayName, isNight, density)
    }


    private val random = Random(System.currentTimeMillis())

    /**
     * Generate a weather-themed background bitmap.
     * @param width Width in dp (will be scaled to pixels)
     * @param height Height in dp
     * @param weatherCode wttr.in style code or OWM ID
     * @param isNight Whether it's night time
     * @param density Screen density for dp->px conversion
     */
    fun render(
        widthDp: Int,
        heightDp: Int,
        weatherCode: String,
        isNight: Boolean,
        density: Float
    ): Bitmap {
        val w = (widthDp * density).toInt()
        val h = (heightDp * density).toInt()
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Determine weather type from code
        val weatherType = classifyWeather(weatherCode)

        // Draw background gradient
        drawBackground(canvas, w, h, weatherType, isNight)

        // Draw weather effects
        when (weatherType) {
            "sunny" -> drawSunnyEffect(canvas, w, h)
            "cloudy" -> drawCloudyEffect(canvas, w, h)
            "overcast" -> drawOvercastEffect(canvas, w, h)
            "rainy" -> drawRainEffect(canvas, w, h)
            "snowy" -> drawSnowEffect(canvas, w, h)
            "foggy" -> drawFoggyEffect(canvas, w, h)
            "night_clear" -> drawNightClearEffect(canvas, w, h)
            else -> drawCloudyEffect(canvas, w, h)
        }

        return bitmap
    }

    private fun classifyWeather(code: String): String {
        return when {
            // wttr.in codes
            code.startsWith("Sunny") || code.startsWith("Clear") -> "sunny"
            code.startsWith("Partly cloudy") -> "cloudy"
            code.startsWith("Cloudy") || code.startsWith("Overcast") -> "overcast"
            code.startsWith("Rain") || code.startsWith("Light rain") ||
                code.startsWith("Heavy rain") || code.startsWith("Drizzle") ||
                code.startsWith("Thunder") -> "rainy"
            code.startsWith("Snow") || code.startsWith("Light snow") ||
                code.startsWith("Heavy snow") || code.startsWith("Sleet") ||
                code.startsWith("Blizzard") -> "snowy"
            code.startsWith("Fog") || code.startsWith("Mist") ||
                code.startsWith("Haze") -> "foggy"
            // OWM weather IDs (as string)
            code.startsWith("800") -> "sunny"
            code.startsWith("801") -> "cloudy"
            code.startsWith("80") -> "overcast" // 802-804
            code.startsWith("5") -> "rainy" // 50x drizzle, 5xx rain
            code.startsWith("2") -> "rainy" // thunderstorm
            code.startsWith("6") -> "snowy" // snow
            code.startsWith("7") -> "foggy" // atmosphere
            else -> "cloudy"
        }
    }

    private fun drawBackground(canvas: Canvas, w: Int, h: Int, weatherType: String, isNight: Boolean) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val colors: IntArray
        val positions: FloatArray

        when {
            isNight -> {
                colors = intArrayOf(
                    Color.argb(180, 10, 10, 40),
                    Color.argb(160, 20, 15, 50),
                    Color.argb(140, 30, 20, 60)
                )
                positions = floatArrayOf(0f, 0.5f, 1f)
            }
            weatherType == "sunny" -> {
                val start = Color.argb(200, 255, 200, 80)
                val mid = Color.argb(180, 255, 220, 150)
                val end = Color.argb(160, 200, 230, 255)
                colors = intArrayOf(start, mid, end)
                positions = floatArrayOf(0f, 0.4f, 1f)
            }
            weatherType == "cloudy" -> {
                colors = intArrayOf(
                    Color.argb(160, 180, 190, 200),
                    Color.argb(140, 160, 170, 185),
                    Color.argb(120, 140, 150, 170)
                )
                positions = floatArrayOf(0f, 0.5f, 1f)
            }
            weatherType == "overcast" -> {
                colors = intArrayOf(
                    Color.argb(170, 120, 125, 135),
                    Color.argb(150, 100, 105, 115),
                    Color.argb(130, 80, 85, 95)
                )
                positions = floatArrayOf(0f, 0.5f, 1f)
            }
            weatherType == "rainy" -> {
                colors = intArrayOf(
                    Color.argb(190, 50, 60, 90),
                    Color.argb(170, 40, 50, 80),
                    Color.argb(150, 30, 40, 70)
                )
                positions = floatArrayOf(0f, 0.5f, 1f)
            }
            weatherType == "snowy" -> {
                colors = intArrayOf(
                    Color.argb(180, 200, 210, 220),
                    Color.argb(160, 220, 225, 230),
                    Color.argb(140, 240, 240, 245)
                )
                positions = floatArrayOf(0f, 0.5f, 1f)
            }
            weatherType == "foggy" -> {
                colors = intArrayOf(
                    Color.argb(170, 160, 155, 140),
                    Color.argb(150, 140, 135, 120),
                    Color.argb(130, 120, 115, 100)
                )
                positions = floatArrayOf(0f, 0.5f, 1f)
            }
            else -> {
                colors = intArrayOf(
                    Color.argb(160, 180, 190, 200),
                    Color.argb(140, 160, 170, 185),
                    Color.argb(120, 140, 150, 170)
                )
                positions = floatArrayOf(0f, 0.5f, 1f)
            }
        }

        val shader = LinearGradient(0f, 0f, 0f, h.toFloat(), colors, positions, Shader.TileMode.CLAMP)
        paint.shader = shader
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), paint)
    }

    private fun drawSunnyEffect(canvas: Canvas, w: Int, h: Int) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f * (h / 200f)
        val cx = w * 0.8f
        val cy = h * 0.15f
        val baseRadius = minOf(w, h) * 0.15f

        // Sun glow - multiple translucent circles
        paint.style = Paint.Style.FILL
        paint.color = Color.argb(60, 255, 220, 100)
        canvas.drawCircle(cx, cy, baseRadius * 2f, paint)
        paint.color = Color.argb(40, 255, 230, 150)
        canvas.drawCircle(cx, cy, baseRadius * 3f, paint)
        paint.color = Color.argb(20, 255, 240, 200)
        canvas.drawCircle(cx, cy, baseRadius * 5f, paint)

        // Sun rays
        paint.style = Paint.Style.STROKE
        paint.color = Color.argb(60, 255, 220, 100)
        paint.strokeWidth = 2f
        val rayCount = 12
        for (i in 0 until rayCount) {
            val angle = (i.toFloat() / rayCount) * 2f * PI.toFloat()
            val inner = baseRadius * 1.3f
            val outer = baseRadius * 2.5f
            canvas.drawLine(
                cx + cos(angle) * inner, cy + sin(angle) * inner,
                cx + cos(angle) * outer, cy + sin(angle) * outer,
                paint
            )
        }

        // Center sun
        paint.style = Paint.Style.FILL
        paint.color = Color.argb(200, 255, 200, 50)
        canvas.drawCircle(cx, cy, baseRadius * 1.1f, paint)
        paint.color = Color.argb(255, 255, 220, 80)
        canvas.drawCircle(cx, cy, baseRadius * 0.8f, paint)
        paint.color = Color.argb(255, 255, 240, 150)
        canvas.drawCircle(cx, cy, baseRadius * 0.4f, paint)
    }

    private fun drawCloudyEffect(canvas: Canvas, w: Int, h: Int) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.style = Paint.Style.FILL

        // Diffuse glow behind clouds
        paint.color = Color.argb(30, 255, 220, 100)
        canvas.drawCircle(w * 0.75f, h * 0.2f, minOf(w, h) * 0.2f, paint)

        // Cloud layers
        val cloudColor = Color.argb(80, 220, 220, 230)
        paint.color = cloudColor

        drawCloud(canvas, paint, w * 0.7f, h * 0.2f, minOf(w, h) * 0.18f)
        drawCloud(canvas, paint, w * 0.3f, h * 0.25f, minOf(w, h) * 0.15f)
        drawCloud(canvas, paint, w * 0.5f, h * 0.3f, minOf(w, h) * 0.12f)
    }

    private fun drawOvercastEffect(canvas: Canvas, w: Int, h: Int) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.style = Paint.Style.FILL
        val cloudColor = Color.argb(100, 140, 145, 155)
        paint.color = cloudColor

        // Dense cloud cover
        drawCloud(canvas, paint, w * 0.5f, h * 0.15f, minOf(w, h) * 0.22f)
        drawCloud(canvas, paint, w * 0.2f, h * 0.22f, minOf(w, h) * 0.2f)
        drawCloud(canvas, paint, w * 0.8f, h * 0.18f, minOf(w, h) * 0.2f)
        drawCloud(canvas, paint, w * 0.35f, h * 0.32f, minOf(w, h) * 0.18f)
        drawCloud(canvas, paint, w * 0.65f, h * 0.35f, minOf(w, h) * 0.16f)
    }

    private fun drawRainEffect(canvas: Canvas, w: Int, h: Int) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.style = Paint.Style.STROKE
        paint.color = Color.argb(120, 180, 200, 255)
        paint.strokeWidth = 2f
        paint.strokeCap = Paint.Cap.ROUND

        // Cloud layer at top
        paint.style = Paint.Style.FILL
        paint.color = Color.argb(80, 60, 70, 100)
        drawCloud(canvas, paint, w * 0.4f, h * 0.1f, minOf(w, h) * 0.2f)
        drawCloud(canvas, paint, w * 0.7f, h * 0.08f, minOf(w, h) * 0.18f)
        drawCloud(canvas, paint, w * 0.2f, h * 0.12f, minOf(w, h) * 0.17f)

        // Rain drops
        paint.style = Paint.Style.STROKE
        paint.color = Color.argb(100, 180, 200, 255)
        paint.strokeWidth = 1.5f
        val dropCount = (w * h) / 12000
        for (i in 0 until dropCount) {
            val x = random.nextFloat() * w
            val y = random.nextFloat() * h
            val len = 8f + random.nextFloat() * 12f
            canvas.drawLine(x, y, x - len * 0.15f, y + len, paint)
        }

        // Slightly thicker drops
        paint.color = Color.argb(60, 200, 220, 255)
        paint.strokeWidth = 1f
        for (i in 0 until dropCount / 2) {
            val x = random.nextFloat() * w
            val y = random.nextFloat() * h
            canvas.drawLine(x, y - 5f, x - 2f, y + 10f, paint)
        }
    }

    private fun drawSnowEffect(canvas: Canvas, w: Int, h: Int) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.style = Paint.Style.FILL

        // Light cloud layer
        paint.color = Color.argb(60, 200, 210, 220)
        drawCloud(canvas, paint, w * 0.5f, h * 0.08f, minOf(w, h) * 0.18f)
        drawCloud(canvas, paint, w * 0.3f, h * 0.12f, minOf(w, h) * 0.15f)
        drawCloud(canvas, paint, w * 0.7f, h * 0.1f, minOf(w, h) * 0.16f)

        // Snowflakes
        paint.color = Color.argb(200, 255, 255, 255)
        val flakeCount = (w * h) / 8000
        for (i in 0 until flakeCount) {
            val x = random.nextFloat() * w
            val y = random.nextFloat() * h
            val r = 2f + random.nextFloat() * 4f
            canvas.drawCircle(x, y, r, paint)
        }

        // Glow on some flakes
        paint.color = Color.argb(80, 255, 255, 255)
        for (i in 0 until flakeCount / 3) {
            val x = random.nextFloat() * w
            val y = random.nextFloat() * h
            val r = 4f + random.nextFloat() * 4f
            canvas.drawCircle(x, y, r, paint)
        }
    }

    private fun drawFoggyEffect(canvas: Canvas, w: Int, h: Int) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.style = Paint.Style.FILL

        // Multiple horizontal mist layers
        for (i in 0..6) {
            val alpha = 20 + random.nextInt(25)
            paint.color = Color.argb(alpha, 200, 200, 210)
            val y = h * (0.1f + i * 0.12f) + random.nextFloat() * h * 0.05f
            val thickness = 8f + random.nextFloat() * 12f
            canvas.drawRoundRect(
                0f, y - thickness / 2f, w.toFloat(), y + thickness / 2f,
                thickness, thickness, paint
            )
        }
    }

    private fun drawNightClearEffect(canvas: Canvas, w: Int, h: Int) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.style = Paint.Style.FILL

        // Moon
        val mx = w * 0.8f
        val my = h * 0.18f
        val mr = minOf(w, h) * 0.08f

        // Moon glow
        paint.color = Color.argb(40, 200, 200, 255)
        canvas.drawCircle(mx, my, mr * 2.5f, paint)
        paint.color = Color.argb(20, 180, 180, 255)
        canvas.drawCircle(mx, my, mr * 4f, paint)

        // Moon crescent
        paint.color = Color.argb(200, 230, 230, 255)
        canvas.drawCircle(mx, my, mr, paint)
        paint.color = Color.argb(200, 10, 10, 40)
        canvas.drawCircle(mx + mr * 0.3f, my - mr * 0.2f, mr * 0.85f, paint)

        // Stars
        paint.color = Color.argb(180, 255, 255, 255)
        val starCount = (w * h) / 8000
        for (i in 0 until starCount) {
            val x = random.nextFloat() * w
            val y = random.nextFloat() * h * 0.6f
            val r = 1f + random.nextFloat() * 2f
            canvas.drawCircle(x, y, r, paint)
        }

        // Twinkling stars
        paint.color = Color.argb(100, 255, 255, 200)
        for (i in 0 until starCount / 3) {
            val x = random.nextFloat() * w
            val y = random.nextFloat() * h * 0.4f
            canvas.drawCircle(x, y, 2.5f + random.nextFloat() * 1.5f, paint)
        }
    }

    private fun drawCloud(canvas: Canvas, paint: Paint, cx: Float, cy: Float, radius: Float) {
        canvas.drawCircle(cx, cy, radius, paint)
        canvas.drawCircle(cx - radius * 0.8f, cy + radius * 0.1f, radius * 0.7f, paint)
        canvas.drawCircle(cx + radius * 0.8f, cy + radius * 0.1f, radius * 0.7f, paint)
        canvas.drawCircle(cx - radius * 0.3f, cy - radius * 0.3f, radius * 0.6f, paint)
        canvas.drawCircle(cx + radius * 0.3f, cy - radius * 0.2f, radius * 0.6f, paint)
    }
}

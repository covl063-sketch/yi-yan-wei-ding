     1|package com.yiyanweiding.app.model
     2|
     3|import android.graphics.*
     4|import kotlin.math.cos
     5|import kotlin.math.sin
     6|import kotlin.math.PI
     7|import kotlin.random.Random
     8|
    27|object WeatherBackgroundRenderer {

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

    28|
    29|    private val random = Random(System.currentTimeMillis())
    30|
    31|    /**
    32|     * Generate a weather-themed background bitmap.
    33|     * @param width Width in dp (will be scaled to pixels)
    34|     * @param height Height in dp
    35|     * @param weatherCode wttr.in style code or OWM ID
    36|     * @param isNight Whether it's night time
    37|     * @param density Screen density for dp->px conversion
    38|     */
    39|    fun render(
    40|        widthDp: Int,
    41|        heightDp: Int,
    42|        weatherCode: String,
    43|        isNight: Boolean,
    44|        density: Float
    45|    ): Bitmap {
    46|        val w = (widthDp * density).toInt()
    47|        val h = (heightDp * density).toInt()
    48|        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    49|        val canvas = Canvas(bitmap)
    50|
    51|        // Determine weather type from code
    52|        val weatherType = classifyWeather(weatherCode)
    53|
    54|        // Draw background gradient
    55|        drawBackground(canvas, w, h, weatherType, isNight)
    56|
    57|        // Draw weather effects
    58|        when (weatherType) {
    59|            "sunny" -> drawSunnyEffect(canvas, w, h)
    60|            "cloudy" -> drawCloudyEffect(canvas, w, h)
    61|            "overcast" -> drawOvercastEffect(canvas, w, h)
    62|            "rainy" -> drawRainEffect(canvas, w, h)
    63|            "snowy" -> drawSnowEffect(canvas, w, h)
    64|            "foggy" -> drawFoggyEffect(canvas, w, h)
    65|            "night_clear" -> drawNightClearEffect(canvas, w, h)
    66|            else -> drawCloudyEffect(canvas, w, h)
    67|        }
    68|
    69|        return bitmap
    70|    }
    71|
    72|    private fun classifyWeather(code: String): String {
    73|        return when {
    74|            // wttr.in codes
    75|            code.startsWith("Sunny") || code.startsWith("Clear") -> "sunny"
    76|            code.startsWith("Partly cloudy") -> "cloudy"
    77|            code.startsWith("Cloudy") || code.startsWith("Overcast") -> "overcast"
    78|            code.startsWith("Rain") || code.startsWith("Light rain") ||
    79|                code.startsWith("Heavy rain") || code.startsWith("Drizzle") ||
    80|                code.startsWith("Thunder") -> "rainy"
    81|            code.startsWith("Snow") || code.startsWith("Light snow") ||
    82|                code.startsWith("Heavy snow") || code.startsWith("Sleet") ||
    83|                code.startsWith("Blizzard") -> "snowy"
    84|            code.startsWith("Fog") || code.startsWith("Mist") ||
    85|                code.startsWith("Haze") -> "foggy"
    86|            // OWM weather IDs (as string)
    87|            code.startsWith("800") -> "sunny"
    88|            code.startsWith("801") -> "cloudy"
    89|            code.startsWith("80") -> "overcast" // 802-804
    90|            code.startsWith("5") -> "rainy" // 50x drizzle, 5xx rain
    91|            code.startsWith("2") -> "rainy" // thunderstorm
    92|            code.startsWith("6") -> "snowy" // snow
    93|            code.startsWith("7") -> "foggy" // atmosphere
    94|            else -> "cloudy"
    95|        }
    96|    }
    97|
    98|    private fun drawBackground(canvas: Canvas, w: Int, h: Int, weatherType: String, isNight: Boolean) {
    99|        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
   100|        val colors: IntArray
   101|        val positions: FloatArray
   102|
   103|        when {
   104|            isNight -> {
   105|                colors = intArrayOf(
   106|                    Color.argb(180, 10, 10, 40),
   107|                    Color.argb(160, 20, 15, 50),
   108|                    Color.argb(140, 30, 20, 60)
   109|                )
   110|                positions = floatArrayOf(0f, 0.5f, 1f)
   111|            }
   112|            weatherType == "sunny" -> {
   113|                val start = Color.argb(200, 255, 200, 80)
   114|                val mid = Color.argb(180, 255, 220, 150)
   115|                val end = Color.argb(160, 200, 230, 255)
   116|                colors = intArrayOf(start, mid, end)
   117|                positions = floatArrayOf(0f, 0.4f, 1f)
   118|            }
   119|            weatherType == "cloudy" -> {
   120|                colors = intArrayOf(
   121|                    Color.argb(160, 180, 190, 200),
   122|                    Color.argb(140, 160, 170, 185),
   123|                    Color.argb(120, 140, 150, 170)
   124|                )
   125|                positions = floatArrayOf(0f, 0.5f, 1f)
   126|            }
   127|            weatherType == "overcast" -> {
   128|                colors = intArrayOf(
   129|                    Color.argb(170, 120, 125, 135),
   130|                    Color.argb(150, 100, 105, 115),
   131|                    Color.argb(130, 80, 85, 95)
   132|                )
   133|                positions = floatArrayOf(0f, 0.5f, 1f)
   134|            }
   135|            weatherType == "rainy" -> {
   136|                colors = intArrayOf(
   137|                    Color.argb(190, 50, 60, 90),
   138|                    Color.argb(170, 40, 50, 80),
   139|                    Color.argb(150, 30, 40, 70)
   140|                )
   141|                positions = floatArrayOf(0f, 0.5f, 1f)
   142|            }
   143|            weatherType == "snowy" -> {
   144|                colors = intArrayOf(
   145|                    Color.argb(180, 200, 210, 220),
   146|                    Color.argb(160, 220, 225, 230),
   147|                    Color.argb(140, 240, 240, 245)
   148|                )
   149|                positions = floatArrayOf(0f, 0.5f, 1f)
   150|            }
   151|            weatherType == "foggy" -> {
   152|                colors = intArrayOf(
   153|                    Color.argb(170, 160, 155, 140),
   154|                    Color.argb(150, 140, 135, 120),
   155|                    Color.argb(130, 120, 115, 100)
   156|                )
   157|                positions = floatArrayOf(0f, 0.5f, 1f)
   158|            }
   159|            else -> {
   160|                colors = intArrayOf(
   161|                    Color.argb(160, 180, 190, 200),
   162|                    Color.argb(140, 160, 170, 185),
   163|                    Color.argb(120, 140, 150, 170)
   164|                )
   165|                positions = floatArrayOf(0f, 0.5f, 1f)
   166|            }
   167|        }
   168|
   169|        val shader = LinearGradient(0f, 0f, 0f, h.toFloat(), colors, positions, Shader.TileMode.CLAMP)
   170|        paint.shader = shader
   171|        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), paint)
   172|    }
   173|
   174|    private fun drawSunnyEffect(canvas: Canvas, w: Int, h: Int) {
   175|        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
   176|        paint.style = Paint.Style.STROKE
   177|        paint.strokeWidth = 2f * (h / 200f)
   178|        val cx = w * 0.8f
   179|        val cy = h * 0.15f
   180|        val baseRadius = minOf(w, h) * 0.15f
   181|
   182|        // Sun glow - multiple translucent circles
   183|        paint.style = Paint.Style.FILL
   184|        paint.color = Color.argb(60, 255, 220, 100)
   185|        canvas.drawCircle(cx, cy, baseRadius * 2f, paint)
   186|        paint.color = Color.argb(40, 255, 230, 150)
   187|        canvas.drawCircle(cx, cy, baseRadius * 3f, paint)
   188|        paint.color = Color.argb(20, 255, 240, 200)
   189|        canvas.drawCircle(cx, cy, baseRadius * 5f, paint)
   190|
   191|        // Sun rays
   192|        paint.style = Paint.Style.STROKE
   193|        paint.color = Color.argb(60, 255, 220, 100)
   194|        paint.strokeWidth = 2f
   195|        val rayCount = 12
   196|        for (i in 0 until rayCount) {
   197|            val angle = (i.toFloat() / rayCount) * 2f * PI.toFloat()
   198|            val inner = baseRadius * 1.3f
   199|            val outer = baseRadius * 2.5f
   200|            canvas.drawLine(
   201|                cx + cos(angle) * inner, cy + sin(angle) * inner,
   202|                cx + cos(angle) * outer, cy + sin(angle) * outer,
   203|                paint
   204|            )
   205|        }
   206|
   207|        // Center sun
   208|        paint.style = Paint.Style.FILL
   209|        paint.color = Color.argb(200, 255, 200, 50)
   210|        canvas.drawCircle(cx, cy, baseRadius * 1.1f, paint)
   211|        paint.color = Color.argb(255, 255, 220, 80)
   212|        canvas.drawCircle(cx, cy, baseRadius * 0.8f, paint)
   213|        paint.color = Color.argb(255, 255, 240, 150)
   214|        canvas.drawCircle(cx, cy, baseRadius * 0.4f, paint)
   215|    }
   216|
   217|    private fun drawCloudyEffect(canvas: Canvas, w: Int, h: Int) {
   218|        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
   219|        paint.style = Paint.Style.FILL
   220|
   221|        // Diffuse glow behind clouds
   222|        paint.color = Color.argb(30, 255, 220, 100)
   223|        canvas.drawCircle(w * 0.75f, h * 0.2f, minOf(w, h) * 0.2f, paint)
   224|
   225|        // Cloud layers
   226|        val cloudColor = Color.argb(80, 220, 220, 230)
   227|        paint.color = cloudColor
   228|
   229|        drawCloud(canvas, paint, w * 0.7f, h * 0.2f, minOf(w, h) * 0.18f)
   230|        drawCloud(canvas, paint, w * 0.3f, h * 0.25f, minOf(w, h) * 0.15f)
   231|        drawCloud(canvas, paint, w * 0.5f, h * 0.3f, minOf(w, h) * 0.12f)
   232|    }
   233|
   234|    private fun drawOvercastEffect(canvas: Canvas, w: Int, h: Int) {
   235|        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
   236|        paint.style = Paint.Style.FILL
   237|        val cloudColor = Color.argb(100, 140, 145, 155)
   238|        paint.color = cloudColor
   239|
   240|        // Dense cloud cover
   241|        drawCloud(canvas, paint, w * 0.5f, h * 0.15f, minOf(w, h) * 0.22f)
   242|        drawCloud(canvas, paint, w * 0.2f, h * 0.22f, minOf(w, h) * 0.2f)
   243|        drawCloud(canvas, paint, w * 0.8f, h * 0.18f, minOf(w, h) * 0.2f)
   244|        drawCloud(canvas, paint, w * 0.35f, h * 0.32f, minOf(w, h) * 0.18f)
   245|        drawCloud(canvas, paint, w * 0.65f, h * 0.35f, minOf(w, h) * 0.16f)
   246|    }
   247|
   248|    private fun drawRainEffect(canvas: Canvas, w: Int, h: Int) {
   249|        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
   250|        paint.style = Paint.Style.STROKE
   251|        paint.color = Color.argb(120, 180, 200, 255)
   252|        paint.strokeWidth = 2f
   253|        paint.strokeCap = Paint.Cap.ROUND
   254|
   255|        // Cloud layer at top
   256|        paint.style = Paint.Style.FILL
   257|        paint.color = Color.argb(80, 60, 70, 100)
   258|        drawCloud(canvas, paint, w * 0.4f, h * 0.1f, minOf(w, h) * 0.2f)
   259|        drawCloud(canvas, paint, w * 0.7f, h * 0.08f, minOf(w, h) * 0.18f)
   260|        drawCloud(canvas, paint, w * 0.2f, h * 0.12f, minOf(w, h) * 0.17f)
   261|
   262|        // Rain drops
   263|        paint.style = Paint.Style.STROKE
   264|        paint.color = Color.argb(100, 180, 200, 255)
   265|        paint.strokeWidth = 1.5f
   266|        val dropCount = (w * h) / 12000
   267|        for (i in 0 until dropCount) {
   268|            val x = random.nextFloat() * w
   269|            val y = random.nextFloat() * h
   270|            val len = 8f + random.nextFloat() * 12f
   271|            canvas.drawLine(x, y, x - len * 0.15f, y + len, paint)
   272|        }
   273|
   274|        // Slightly thicker drops
   275|        paint.color = Color.argb(60, 200, 220, 255)
   276|        paint.strokeWidth = 1f
   277|        for (i in 0 until dropCount / 2) {
   278|            val x = random.nextFloat() * w
   279|            val y = random.nextFloat() * h
   280|            canvas.drawLine(x, y - 5f, x - 2f, y + 10f, paint)
   281|        }
   282|    }
   283|
   284|    private fun drawSnowEffect(canvas: Canvas, w: Int, h: Int) {
   285|        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
   286|        paint.style = Paint.Style.FILL
   287|
   288|        // Light cloud layer
   289|        paint.color = Color.argb(60, 200, 210, 220)
   290|        drawCloud(canvas, paint, w * 0.5f, h * 0.08f, minOf(w, h) * 0.18f)
   291|        drawCloud(canvas, paint, w * 0.3f, h * 0.12f, minOf(w, h) * 0.15f)
   292|        drawCloud(canvas, paint, w * 0.7f, h * 0.1f, minOf(w, h) * 0.16f)
   293|
   294|        // Snowflakes
   295|        paint.color = Color.argb(200, 255, 255, 255)
   296|        val flakeCount = (w * h) / 8000
   297|        for (i in 0 until flakeCount) {
   298|            val x = random.nextFloat() * w
   299|            val y = random.nextFloat() * h
   300|            val r = 2f + random.nextFloat() * 4f
   301|            canvas.drawCircle(x, y, r, paint)
   302|        }
   303|
   304|        // Glow on some flakes
   305|        paint.color = Color.argb(80, 255, 255, 255)
   306|        for (i in 0 until flakeCount / 3) {
   307|            val x = random.nextFloat() * w
   308|            val y = random.nextFloat() * h
   309|            val r = 4f + random.nextFloat() * 4f
   310|            canvas.drawCircle(x, y, r, paint)
   311|        }
   312|    }
   313|
   314|    private fun drawFoggyEffect(canvas: Canvas, w: Int, h: Int) {
   315|        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
   316|        paint.style = Paint.Style.FILL
   317|
   318|        // Multiple horizontal mist layers
   319|        for (i in 0..6) {
   320|            val alpha = 20 + random.nextInt(25)
   321|            paint.color = Color.argb(alpha, 200, 200, 210)
   322|            val y = h * (0.1f + i * 0.12f) + random.nextFloat() * h * 0.05f
   323|            val thickness = 8f + random.nextFloat() * 12f
   324|            canvas.drawRoundRect(
   325|                0f, y - thickness / 2f, w.toFloat(), y + thickness / 2f,
   326|                thickness, thickness, paint
   327|            )
   328|        }
   329|    }
   330|
   331|    private fun drawNightClearEffect(canvas: Canvas, w: Int, h: Int) {
   332|        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
   333|        paint.style = Paint.Style.FILL
   334|
   335|        // Moon
   336|        val mx = w * 0.8f
   337|        val my = h * 0.18f
   338|        val mr = minOf(w, h) * 0.08f
   339|
   340|        // Moon glow
   341|        paint.color = Color.argb(40, 200, 200, 255)
   342|        canvas.drawCircle(mx, my, mr * 2.5f, paint)
   343|        paint.color = Color.argb(20, 180, 180, 255)
   344|        canvas.drawCircle(mx, my, mr * 4f, paint)
   345|
   346|        // Moon crescent
   347|        paint.color = Color.argb(200, 230, 230, 255)
   348|        canvas.drawCircle(mx, my, mr, paint)
   349|        paint.color = Color.argb(200, 10, 10, 40)
   350|        canvas.drawCircle(mx + mr * 0.3f, my - mr * 0.2f, mr * 0.85f, paint)
   351|
   352|        // Stars
   353|        paint.color = Color.argb(180, 255, 255, 255)
   354|        val starCount = (w * h) / 8000
   355|        for (i in 0 until starCount) {
   356|            val x = random.nextFloat() * w
   357|            val y = random.nextFloat() * h * 0.6f
   358|            val r = 1f + random.nextFloat() * 2f
   359|            canvas.drawCircle(x, y, r, paint)
   360|        }
   361|
   362|        // Twinkling stars
   363|        paint.color = Color.argb(100, 255, 255, 200)
   364|        for (i in 0 until starCount / 3) {
   365|            val x = random.nextFloat() * w
   366|            val y = random.nextFloat() * h * 0.4f
   367|            canvas.drawCircle(x, y, 2.5f + random.nextFloat() * 1.5f, paint)
   368|        }
   369|    }
   370|
   371|    private fun drawCloud(canvas: Canvas, paint: Paint, cx: Float, cy: Float, radius: Float) {
   372|        canvas.drawCircle(cx, cy, radius, paint)
   373|        canvas.drawCircle(cx - radius * 0.8f, cy + radius * 0.1f, radius * 0.7f, paint)
   374|        canvas.drawCircle(cx + radius * 0.8f, cy + radius * 0.1f, radius * 0.7f, paint)
   375|        canvas.drawCircle(cx - radius * 0.3f, cy - radius * 0.3f, radius * 0.6f, paint)
   376|        canvas.drawCircle(cx + radius * 0.3f, cy - radius * 0.2f, radius * 0.6f, paint)
   377|    }
   378|}
   379|
     1|package com.yiyanweiding.app
     2|
     3|import android.content.ClipData
     4|import android.content.ClipboardManager
     5|import android.content.Context
     6|import android.os.Bundle
     7|import android.widget.Toast
     8|import androidx.activity.ComponentActivity
     9|import androidx.activity.compose.setContent
    10|import androidx.compose.animation.*
    11|import androidx.compose.foundation.background
    12|import androidx.compose.foundation.clickable
    13|import androidx.compose.foundation.layout.*
    14|import androidx.compose.foundation.lazy.LazyColumn
    15|import androidx.compose.foundation.lazy.items
    16|import androidx.compose.foundation.shape.RoundedCornerShape
    17|import androidx.compose.material.icons.Icons
    18|import androidx.compose.material.icons.filled.*
    19|import androidx.compose.material3.*
    20|import androidx.compose.runtime.*
    21|import androidx.compose.ui.Alignment
    22|import androidx.compose.ui.Modifier
    23|import androidx.compose.ui.draw.clip
    24|import androidx.compose.ui.graphics.Brush
    25|import androidx.compose.ui.graphics.Color
    26|import androidx.compose.ui.platform.LocalContext
    27|import androidx.compose.ui.text.font.FontWeight
    28|import androidx.compose.ui.text.style.TextAlign
    29|import androidx.compose.ui.unit.dp
    30|import androidx.compose.ui.unit.sp
    31|import com.yiyanweiding.app.model.ColorUtils
    32|import com.yiyanweiding.app.model.FavoritesManager
    33|import com.yiyanweiding.app.model.Quote
    34|import com.yiyanweiding.app.model.QuoteDatabase
    35|import java.util.Calendar
    36|
    37|class MainActivity : ComponentActivity() {
    38|    override fun onCreate(savedInstanceState: Bundle?) {
    39|        super.onCreate(savedInstanceState)
    40|        setContent {
    41|            MaterialTheme(
    42|                colorScheme = darkColorScheme()
    43|            ) {
    44|                YiYanApp()
    45|            }
    46|        }
    47|    }
    48|}
    49|
    50|@OptIn(ExperimentalMaterial3Api::class)
    51|@Composable
    52|fun YiYanApp() {
    53|    var currentPage by remember { mutableStateOf(0) }
    54|
    55|    Scaffold(
    56|        topBar = {
    57|            TopAppBar(
    58|                title = {
    59|                    Text(
    60|                        if (currentPage == 0) "一言为定" else "我的收藏",
    61|                        fontWeight = FontWeight.Bold
    62|                    )
    63|                },
    64|                actions = {
    65|                    if (currentPage == 0) {
    66|                        // Settings button
    67|                        var showSettings by remember { mutableStateOf(false) }
    68|                        IconButton(onClick = { showSettings = true }) {
    69|                            Icon(Icons.Default.Settings, contentDescription = "设置")
    70|                        }
    71|                        if (showSettings) {
    72|                            SettingsDialog(onDismiss = { showSettings = false })
    73|                        }
    74|                        IconButton(onClick = { currentPage = 1 }) {
    75|                            Icon(Icons.Default.Favorite, contentDescription = "收藏")
    76|                        }
    77|                    } else {
    78|                        IconButton(onClick = { currentPage = 0 }) {
    79|                            Icon(Icons.Default.Home, contentDescription = "首页")
    80|                        }
    81|                    }
    82|                },
    83|                colors = TopAppBarDefaults.topAppBarColors(
    84|                    containerColor = Color(0xFF1A1A2E),
    85|                    titleContentColor = Color.White,
    86|                    actionIconContentColor = Color.White
    87|                )
    88|            )
    89|        },
    90|        containerColor = Color(0xFF1A1A2E)
    91|    ) { padding ->
    92|        Box(modifier = Modifier.padding(padding)) {
    93|            when (currentPage) {
    94|                0 -> TodayQuotePage()
    95|                1 -> FavoritesPage(onBack = { currentPage = 0 })
    96|            }
    97|        }
    98|    }
    99|}
   100|
   101|@Composable
   102|fun TodayQuotePage() {
   103|    val context = LocalContext.current
   104|    var quoteIndex by remember { mutableStateOf(0) }
   105|    var showCopyToast by remember { mutableStateOf(false) }
   106|
   107|    // Load today's quote
   108|    LaunchedEffect(Unit) {
   109|        val dayOfYear = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
   110|        quoteIndex = dayOfYear % QuoteDatabase.size()
   111|    }
   112|
   113|    val quote = remember(quoteIndex) {
   114|        QuoteDatabase.getQuoteByIndex(quoteIndex)
   115|    }
   116|
   117|    val dayOfYear = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
   118|    val colors = remember { ColorUtils.getGradientColors(dayOfYear) }
   119|    val startColor = Color(colors[0])
   120|    val endColor = Color(colors[1])
   121|    val isFav = remember { mutableStateOf(FavoritesManager.isFavorite(quote.text)) }
   122|
   123|    // Update favorite status
   124|    LaunchedEffect(quote.text) {
   125|        isFav.value = FavoritesManager.isFavorite(quote.text)
   126|    }
   127|
   128|    // Copy toast
   129|    if (showCopyToast) {
   130|        LaunchedEffect(Unit) {
   131|            kotlinx.coroutines.delay(1500)
   132|            showCopyToast = false
   133|        }
   134|    }
   135|
   136|    Box(
   137|        modifier = Modifier
   138|            .fillMaxSize()
   139|            .background(
   140|                Brush.verticalGradient(listOf(startColor, endColor))
   141|            ),
   142|        contentAlignment = Alignment.Center
   143|    ) {
   144|        Column(
   145|            modifier = Modifier
   146|                .fillMaxWidth()
   147|                .padding(32.dp),
   148|            horizontalAlignment = Alignment.CenterHorizontally
   149|        ) {
   150|            // Day counter
   151|            Text(
   152|                text = "Day $dayOfYear",
   153|                color = Color.White.copy(alpha = 0.7f),
   154|                fontSize = 14.sp,
   155|                modifier = Modifier.padding(bottom = 16.dp)
   156|            )
   157|
   158|            // Quote text with animation
   159|            AnimatedContent(
   160|                targetState = quoteIndex,
   161|                transitionSpec = {
   162|                    slideInHorizontally { width -> width } + fadeIn() togetherWith
   163|                            slideOutHorizontally { width -> -width } + fadeOut()
   164|                },
   165|                label = "quote_animation"
   166|            ) { targetIndex ->
   167|                val currentQuote = QuoteDatabase.getQuoteByIndex(targetIndex)
   168|                Card(
   169|                    modifier = Modifier
   170|                        .fillMaxWidth()
   171|                        .clickable {
   172|                            // Copy to clipboard
   173|                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
   174|                            val clip = ClipData.newPlainText("quote", currentQuote.text)
   175|                            clipboard.setPrimaryClip(clip)
   176|                            showCopyToast = true
   177|                            Toast.makeText(context, "已复制到剪贴板", Toast.LENGTH_SHORT).show()
   178|                        },
   179|                    colors = CardDefaults.cardColors(
   180|                        containerColor = Color.White.copy(alpha = 0.15f)
   181|                    ),
   182|                    shape = RoundedCornerShape(16.dp)
   183|                ) {
   184|                    Column(
   185|                        modifier = Modifier
   186|                            .fillMaxWidth()
   187|                            .padding(24.dp),
   188|                        horizontalAlignment = Alignment.CenterHorizontally
   189|                    ) {
   190|                        Text(
   191|                            text = currentQuote.text,
   192|                            color = Color.White,
   193|                            fontSize = 22.sp,
   194|                            lineHeight = 32.sp,
   195|                            textAlign = TextAlign.Center,
   196|                            modifier = Modifier.padding(bottom = 16.dp)
   197|                        )
   198|                        Text(
   199|                            text = "—— ${currentQuote.from}",
   200|                            color = Color.White.copy(alpha = 0.7f),
   201|                            fontSize = 14.sp,
   202|                            textAlign = TextAlign.End,
   203|                            modifier = Modifier.fillMaxWidth()
   204|                        )
   205|                    }
   206|                }
   207|            }
   208|
   209|            Spacer(modifier = Modifier.height(24.dp))
   210|
   211|            // Action buttons
   212|            Row(
   213|                modifier = Modifier.fillMaxWidth(),
   214|                horizontalArrangement = Arrangement.SpaceEvenly
   215|            ) {
   216|                // Next quote button
   217|                FilledTonalButton(
   218|                    onClick = {
   219|                        quoteIndex = (quoteIndex + 1) % QuoteDatabase.size()
   220|                        isFav.value = FavoritesManager.isFavorite(
   221|                            QuoteDatabase.getQuoteByIndex(quoteIndex).text
   222|                        )
   223|                    },
   224|                    colors = ButtonDefaults.filledTonalButtonColors(
   225|                        containerColor = Color.White.copy(alpha = 0.2f),
   226|                        contentColor = Color.White
   227|                    ),
   228|                    shape = RoundedCornerShape(12.dp)
   229|                ) {
   230|                    Icon(Icons.Default.SkipNext, contentDescription = null)
   231|                    Spacer(modifier = Modifier.width(4.dp))
   232|                    Text("下一条")
   233|                }
   234|
   235|                // Favorite toggle
   236|                FilledTonalButton(
   237|                    onClick = {
   238|                        val q = QuoteDatabase.getQuoteByIndex(quoteIndex)
   239|                        val newState = FavoritesManager.toggleFavorite(q)
   240|                        isFav.value = newState
   241|                        Toast.makeText(
   242|                            context,
   243|                            if (newState) "已收藏" else "已取消收藏",
   244|                            Toast.LENGTH_SHORT
   245|                        ).show()
   246|                    },
   247|                    colors = ButtonDefaults.filledTonalButtonColors(
   248|                        containerColor = if (isFav.value)
   249|                            Color(0xFFFF6B6B).copy(alpha = 0.3f)
   250|                        else
   251|                            Color.White.copy(alpha = 0.2f),
   252|                        contentColor = Color.White
   253|                    ),
   254|                    shape = RoundedCornerShape(12.dp)
   255|                ) {
   256|                    Icon(
   257|                        if (isFav.value) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
   258|                        contentDescription = null
   259|                    )
   260|                    Spacer(modifier = Modifier.width(4.dp))
   261|                    Text(if (isFav.value) "已收藏" else "收藏")
   262|                }
   263|            }
   264|
   265|            // Copy hint
   266|            Text(
   267|                text = "点击卡片复制到剪贴板",
   268|                color = Color.White.copy(alpha = 0.5f),
   269|                fontSize = 12.sp,
   270|                modifier = Modifier.padding(top = 24.dp)
   271|            )
   272|        }
   273|    }
   274|}
   275|
   276|@Composable
   277|fun FavoritesPage(onBack: () -> Unit) {
   278|    val context = LocalContext.current
   279|    var favorites by remember { mutableStateOf(FavoritesManager.getFavorites()) }
   280|    var selectedQuote by remember { mutableStateOf<Quote?>(null) }
   281|
   282|    if (selectedQuote != null) {
   283|        // Detail view for a favorite quote
   284|        val quote = selectedQuote!!
   285|        Box(
   286|            modifier = Modifier
   287|                .fillMaxSize()
   288|                .background(Color(0xFF1A1A2E)),
   289|            contentAlignment = Alignment.Center
   290|        ) {
   291|            Column(
   292|                modifier = Modifier
   293|                    .fillMaxWidth()
   294|                    .padding(32.dp),
   295|                horizontalAlignment = Alignment.CenterHorizontally
   296|            ) {
   297|                Card(
   298|                    modifier = Modifier
   299|                        .fillMaxWidth()
   300|                        .clickable {
   301|                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
   302|                            val clip = ClipData.newPlainText("quote", quote.text)
   303|                            clipboard.setPrimaryClip(clip)
   304|                            Toast.makeText(context, "已复制到剪贴板", Toast.LENGTH_SHORT).show()
   305|                        },
   306|                    colors = CardDefaults.cardColors(
   307|                        containerColor = Color(0xFF16213E)
   308|                    ),
   309|                    shape = RoundedCornerShape(16.dp)
   310|                ) {
   311|                    Column(
   312|                        modifier = Modifier
   313|                            .fillMaxWidth()
   314|                            .padding(24.dp),
   315|                        horizontalAlignment = Alignment.CenterHorizontally
   316|                    ) {
   317|                        Text(
   318|                            text = quote.text,
   319|                            color = Color.White,
   320|                            fontSize = 20.sp,
   321|                            lineHeight = 30.sp,
   322|                            textAlign = TextAlign.Center,
   323|                            modifier = Modifier.padding(bottom = 12.dp)
   324|                        )
   325|                        Text(
   326|                            text = "—— ${quote.from}",
   327|                            color = Color.White.copy(alpha = 0.7f),
   328|                            fontSize = 14.sp,
   329|                            textAlign = TextAlign.End,
   330|                            modifier = Modifier.fillMaxWidth()
   331|                        )
   332|                    }
   333|                }
   334|
   335|                Spacer(modifier = Modifier.height(24.dp))
   336|
   337|                Row(
   338|                    modifier = Modifier.fillMaxWidth(),
   339|                    horizontalArrangement = Arrangement.SpaceEvenly
   340|                ) {
   341|                    FilledTonalButton(
   342|                        onClick = {
   343|                            favorites = FavoritesManager.getFavorites()
   344|                            selectedQuote = null
   345|                        },
   346|                        colors = ButtonDefaults.filledTonalButtonColors(
   347|                            containerColor = Color.White.copy(alpha = 0.15f),
   348|                            contentColor = Color.White
   349|                        ),
   350|                        shape = RoundedCornerShape(12.dp)
   351|                    ) {
   352|                        Icon(Icons.Default.ArrowBack, contentDescription = null)
   353|                        Spacer(modifier = Modifier.width(4.dp))
   354|                        Text("返回")
   355|                    }
   356|
   357|                    FilledTonalButton(
   358|                        onClick = {
   359|                            FavoritesManager.removeFavorite(quote.text)
   360|                            favorites = FavoritesManager.getFavorites()
   361|                            selectedQuote = null
   362|                            Toast.makeText(context, "已移除收藏", Toast.LENGTH_SHORT).show()
   363|                        },
   364|                        colors = ButtonDefaults.filledTonalButtonColors(
   365|                            containerColor = Color(0xFFFF6B6B).copy(alpha = 0.3f),
   366|                            contentColor = Color.White
   367|                        ),
   368|                        shape = RoundedCornerShape(12.dp)
   369|                    ) {
   370|                        Icon(Icons.Default.Delete, contentDescription = null)
   371|                        Spacer(modifier = Modifier.width(4.dp))
   372|                        Text("移除")
   373|                    }
   374|                }
   375|            }
   376|        }
   377|    } else {
   378|        // Favorites list
   379|        LazyColumn(
   380|            modifier = Modifier
   381|                .fillMaxSize()
   382|                .background(Color(0xFF1A1A2E))
   383|                .padding(16.dp),
   384|            verticalArrangement = Arrangement.spacedBy(12.dp)
   385|        ) {
   386|            if (favorites.isEmpty()) {
   387|                item {
   388|                    Box(
   389|                        modifier = Modifier
   390|                            .fillMaxWidth()
   391|                            .padding(top = 100.dp),
   392|                        contentAlignment = Alignment.Center
   393|                    ) {
   394|                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
   395|                            Icon(
   396|                                Icons.Default.FavoriteBorder,
   397|                                contentDescription = null,
   398|                                tint = Color.White.copy(alpha = 0.3f),
   399|                                modifier = Modifier.size(64.dp)
   400|                            )
   401|                            Spacer(modifier = Modifier.height(16.dp))
   402|                            Text(
   403|                                text = "暂无收藏",
   404|                                color = Color.White.copy(alpha = 0.5f),
   405|                                fontSize = 18.sp
   406|                            )
   407|                            Text(
   408|                                text = "在大号小组件上点击❤️收藏名言",
   409|                                color = Color.White.copy(alpha = 0.3f),
   410|                                fontSize = 14.sp,
   411|                                modifier = Modifier.padding(top = 8.dp)
   412|                            )
   413|                        }
   414|                    }
   415|                }
   416|            } else {
   417|                items(favorites, key = { it.text }) { quote ->
   418|                    Card(
   419|                        modifier = Modifier
   420|                            .fillMaxWidth()
   421|                            .clickable { selectedQuote = quote },
   422|                        colors = CardDefaults.cardColors(
   423|                            containerColor = Color(0xFF16213E)
   424|                        ),
   425|                        shape = RoundedCornerShape(12.dp)
   426|                    ) {
   427|                        Column(
   428|                            modifier = Modifier
   429|                                .fillMaxWidth()
   430|                                .padding(16.dp)
   431|                        ) {
   432|                            Text(
   433|                                text = quote.text,
   434|                                color = Color.White,
   435|                                fontSize = 16.sp,
   436|                                lineHeight = 24.sp,
   437|                                modifier = Modifier.padding(bottom = 8.dp)
   438|                            )
   439|                            Text(
   440|                                text = "—— ${quote.from}",
   441|                                color = Color.White.copy(alpha = 0.6f),
   442|                                fontSize = 12.sp,
   443|                                textAlign = TextAlign.End,
   444|                                modifier = Modifier.fillMaxWidth()
   445|                            )
   446|                        }
   447|                    }
   448|                }
   449|            }
   450|        }
   451|    }
   452|}
   453|

@Composable
fun SettingsDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    var cityInput by remember {
        mutableStateOf(com.yiyanweiding.app.widget.WidgetUtils.getCity(context).replace("auto", ""))
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("天气设置", color = Color.White)
        },
        text = {
            Column {
                Text(
                    "输入你所在的城市名称（拼音或中文）",
                    color = Color.Gray,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                androidx.compose.material3.OutlinedTextField(
                    value = cityInput,
                    onValueChange = { cityInput = it },
                    label = { Text("城市") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = Color.White,
                        focusedBorderColor = Color(0xFFE94560),
                        unfocusedBorderColor = Color.Gray
                    ),
                    singleLine = true,
                    placeholder = { Text("例如: 北京 / Beijing", color = Color.Gray) }
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "修改城市后，下次 widget 刷新会使用新城市的气温信息。",
                    color = Color.DarkGray,
                    fontSize = 12.sp
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val city = cityInput.trim()
                if (city.isNotEmpty()) {
                    com.yiyanweiding.app.widget.WidgetUtils.setCity(context, city)
                    // Trigger weather refresh
                    val intent = Intent(context, com.yiyanweiding.app.widget.YiYanWidgetProvider::class.java).apply {
                        action = com.yiyanweiding.app.widget.WidgetUtils.ACTION_REFRESH_WEATHER
                        putExtra(com.yiyanweiding.app.widget.WidgetUtils.EXTRA_CITY, city)
                    }
                    context.sendBroadcast(intent)
                    onDismiss()
                }
            }) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        },
        containerColor = Color(0xFF1A1A2E)
    )
}


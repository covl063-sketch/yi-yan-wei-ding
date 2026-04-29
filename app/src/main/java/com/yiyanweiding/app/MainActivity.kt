package com.yiyanweiding.app

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yiyanweiding.app.model.ColorUtils
import com.yiyanweiding.app.model.FavoritesManager
import com.yiyanweiding.app.model.Quote
import com.yiyanweiding.app.model.QuoteDatabase
import com.yiyanweiding.app.model.WeatherManager
import com.yiyanweiding.app.widget.WidgetUtils
import java.util.Calendar

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme()
            ) {
                YiYanApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YiYanApp() {
    var currentPage by remember { mutableStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (currentPage == 0) "一言为定" else "我的收藏",
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    if (currentPage == 0) {
                        IconButton(onClick = { currentPage = 1 }) {
                            Icon(Icons.Default.Favorite, contentDescription = "收藏")
                        }
                    } else {
                        IconButton(onClick = { currentPage = 0 }) {
                            Icon(Icons.Default.Home, contentDescription = "首页")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1A1A2E),
                    titleContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        },
        containerColor = Color(0xFF1A1A2E)
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (currentPage) {
                0 -> TodayQuotePage()
                1 -> FavoritesPage(onBack = { currentPage = 0 })
            }
        }
    }
}

@Composable
fun TodayQuotePage() {
    val context = LocalContext.current
    var quoteIndex by remember { mutableStateOf(0) }
    var showCopyToast by remember { mutableStateOf(false) }

    // Load today's quote
    LaunchedEffect(Unit) {
        val dayOfYear = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
        quoteIndex = dayOfYear % QuoteDatabase.size()
    }

    val quote = remember(quoteIndex) {
        QuoteDatabase.getQuoteByIndex(quoteIndex)
    }

    val dayOfYear = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
    val colors = remember { ColorUtils.getGradientColors(dayOfYear) }
    val startColor = Color(colors[0])
    val endColor = Color(colors[1])
    val isFav = remember { mutableStateOf(FavoritesManager.isFavorite(quote.text)) }

    // Update favorite status
    LaunchedEffect(quote.text) {
        isFav.value = FavoritesManager.isFavorite(quote.text)
    }

    // Copy toast
    if (showCopyToast) {
        LaunchedEffect(Unit) {
            kotlinx.coroutines.delay(1500)
            showCopyToast = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(startColor, endColor))
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Day counter
            Text(
                text = "Day $dayOfYear",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Quote text with animation
            AnimatedContent(
                targetState = quoteIndex,
                transitionSpec = {
                    slideInHorizontally { width -> width } + fadeIn() togetherWith
                            slideOutHorizontally { width -> -width } + fadeOut()
                },
                label = "quote_animation"
            ) { targetIndex ->
                val currentQuote = QuoteDatabase.getQuoteByIndex(targetIndex)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            // Copy to clipboard
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("quote", currentQuote.text)
                            clipboard.setPrimaryClip(clip)
                            showCopyToast = true
                            Toast.makeText(context, "已复制到剪贴板", Toast.LENGTH_SHORT).show()
                        },
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White.copy(alpha = 0.15f)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = currentQuote.text,
                            color = Color.White,
                            fontSize = 22.sp,
                            lineHeight = 32.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        Text(
                            text = "—— ${currentQuote.from}",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 14.sp,
                            textAlign = TextAlign.End,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Next quote button
                FilledTonalButton(
                    onClick = {
                        quoteIndex = (quoteIndex + 1) % QuoteDatabase.size()
                        isFav.value = FavoritesManager.isFavorite(
                            QuoteDatabase.getQuoteByIndex(quoteIndex).text
                        )
                    },
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = Color.White.copy(alpha = 0.2f),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.SkipNext, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("下一条")
                }

                // Favorite toggle
                FilledTonalButton(
                    onClick = {
                        val q = QuoteDatabase.getQuoteByIndex(quoteIndex)
                        val newState = FavoritesManager.toggleFavorite(q)
                        isFav.value = newState
                        Toast.makeText(
                            context,
                            if (newState) "已收藏" else "已取消收藏",
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = if (isFav.value)
                            Color(0xFFFF6B6B).copy(alpha = 0.3f)
                        else
                            Color.White.copy(alpha = 0.2f),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        if (isFav.value) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (isFav.value) "已收藏" else "收藏")
                }
            }

            // Copy hint
            Text(
                text = "点击卡片复制到剪贴板",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 24.dp)
            )
        }
    }
}

@Composable
fun FavoritesPage(onBack: () -> Unit) {
    val context = LocalContext.current
    var favorites by remember { mutableStateOf(FavoritesManager.getFavorites()) }
    var selectedQuote by remember { mutableStateOf<Quote?>(null) }

    if (selectedQuote != null) {
        // Detail view for a favorite quote
        val quote = selectedQuote!!
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF1A1A2E)),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("quote", quote.text)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "已复制到剪贴板", Toast.LENGTH_SHORT).show()
                        },
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF16213E)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = quote.text,
                            color = Color.White,
                            fontSize = 20.sp,
                            lineHeight = 30.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        Text(
                            text = "—— ${quote.from}",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 14.sp,
                            textAlign = TextAlign.End,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    FilledTonalButton(
                        onClick = {
                            favorites = FavoritesManager.getFavorites()
                            selectedQuote = null
                        },
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = Color.White.copy(alpha = 0.15f),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("返回")
                    }

                    FilledTonalButton(
                        onClick = {
                            FavoritesManager.removeFavorite(quote.text)
                            favorites = FavoritesManager.getFavorites()
                            selectedQuote = null
                            Toast.makeText(context, "已移除收藏", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = Color(0xFFFF6B6B).copy(alpha = 0.3f),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("移除")
                    }
                }
            }
        }
    } else {
        // Favorites list
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF1A1A2E))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (favorites.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 100.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.FavoriteBorder,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.3f),
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "暂无收藏",
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 18.sp
                            )
                            Text(
                                text = "在大号小组件上点击❤️收藏名言",
                                color = Color.White.copy(alpha = 0.3f),
                                fontSize = 14.sp,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }
                }
            } else {
                items(favorites, key = { it.text }) { quote ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedQuote = quote },
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF16213E)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text(
                                text = quote.text,
                                color = Color.White,
                                fontSize = 16.sp,
                                lineHeight = 24.sp,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            Text(
                                text = "—— ${quote.from}",
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 12.sp,
                                textAlign = TextAlign.End,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }
}

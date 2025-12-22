package fansirsqi.xposed.sesame.ui.log

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogViewerScreen(
    filePath: String,
    onBackClick: () -> Unit,
    viewModel: LogViewerViewModel = viewModel()
) {
    // 首次加载
    LaunchedEffect(filePath) {
        viewModel.loadLogs(filePath)
    }

    // 1. 监听 ViewModel 中的 uiState
    val state by viewModel.uiState.collectAsState()

    // 2. 监听 ViewModel 中的 fontSize
    val floatValue by viewModel.fontSize.collectAsState()

    // 3. 将数值转换为 sp 单位，方便后面使用
    val currentFontSize = floatValue.sp
    val listState = rememberLazyListState()
    var showFontMenu by remember { mutableStateOf(false) } // 菜单显示状态还是放在 UI 里记着就行

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (state.isLoading) {
                        Text("Loading...", fontSize = 16.sp)
                    } else {
                        Column {
                            Text(File(filePath).name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                "${state.displayLines.size} / ${state.totalCount} lines",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    // 字体按钮
                    Box {
                        IconButton(onClick = { showFontMenu = true }) {
                            Text("Aa", color = Color.White, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                        }
                        DropdownMenu(
                            expanded = showFontMenu,
                            onDismissRequest = { showFontMenu = false }
                        ) {
                            // 4. 点击菜单时，调用 ViewModel 的方法
                            DropdownMenuItem(
                                text = { Text("放大字体 (+)") },
                                onClick = { viewModel.increaseFontSize() }, // 调用 VM
                                leadingIcon = { Icon(Icons.Default.Add, null) }
                            )
                            DropdownMenuItem(
                                text = { Text("缩小字体 (-)") },
                                onClick = { viewModel.decreaseFontSize() }, // 调用 VM
                                leadingIcon = { Icon(Icons.Default.Remove, null) }
                            )
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text("重置 (12sp)") },
                                onClick = { viewModel.resetFontSize() } // 调用 VM
                            )
                        }
                    }
                    // 搜索框逻辑
                    var isSearchActive by remember { mutableStateOf(false) }
                    if (isSearchActive) {
                        TextField(
                            value = state.searchQuery,
                            onValueChange = { viewModel.search(it) },
                            placeholder = { Text("Search log...") },
                            singleLine = true,
                            modifier = Modifier.width(200.dp),
                            trailingIcon = {
                                IconButton(onClick = {
                                    viewModel.search("") // 清除搜索
                                }) {
                                    Icon(Icons.Default.Close, "Close")
                                }
                            }
                        )
                    } else {
                        IconButton(onClick = { isSearchActive = true }) {
                            Icon(Icons.Default.Search, "Search")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFF1E1E1E)) // 深色背景
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                // 允许长按复制
                SelectionContainer {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(8.dp)
                    ) {
                        itemsIndexed(
                            items = state.displayLines,
                            key = { index, _ -> index }
                        ) { index, line ->
                            Row(modifier = Modifier.fillMaxWidth()) {
                                // 行号使用 currentFontSize
                                Text(
                                    text = "${index + 1} ",
                                    color = Color.Gray,
                                    fontSize = (currentFontSize.value * 0.8).sp, // 稍微小一点
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier
                                        .padding(top = 2.dp, end = 4.dp)
                                        .width((currentFontSize.value * 3.5).dp)
                                )

                                // 5. 把 currentFontSize 传给子组件
                                LogLineItem(
                                    line = line,
                                    searchQuery = state.searchQuery,
                                    fontSize = currentFontSize
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LogLineItem(
    line: String,
    searchQuery: String,
    fontSize: TextUnit // 1. 新增参数
) {
    // 高亮逻辑保持不变
    val annotatedString = if (searchQuery.isNotEmpty()) {
        buildAnnotatedString {
            val lowerLine = line.lowercase()
            val lowerQuery = searchQuery.lowercase()
            var startIndex = 0

            while (true) {
                val index = lowerLine.indexOf(lowerQuery, startIndex)
                if (index == -1) {
                    append(line.substring(startIndex))
                    break
                }
                append(line.substring(startIndex, index))
                withStyle(style = SpanStyle(background = Color(0xFF6B5800), color = Color.White)) {
                    append(line.substring(index, index + searchQuery.length))
                }
                startIndex = index + searchQuery.length
            }
        }
    } else {
        buildAnnotatedString { append(line) }
    }

    Text(
        text = annotatedString,
        color = Color(0xFFCCCCCC),

        // 2. 使用传入的字体大小
        fontSize = fontSize,

        fontFamily = FontFamily.Monospace,

        // 3. 动态调整行高 (重要！)
        // 乘以 1.3 或 1.4 是比较舒服的阅读间距，防止字体变大后重叠
        lineHeight = fontSize * 1.3f,

        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
    )
}
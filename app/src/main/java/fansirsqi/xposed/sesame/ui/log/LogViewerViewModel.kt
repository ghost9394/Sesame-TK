package fansirsqi.xposed.sesame.ui.log

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

data class LogUiState(
    val allLines: List<String> = emptyList(),      // 原始数据
    val displayLines: List<String> = emptyList(),  // 用于展示的数据（可能是搜索结果）
    val isLoading: Boolean = true,
    val searchQuery: String = "",
    val totalCount: Int = 0
)

class LogViewerViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(LogUiState())
    private val _fontSize = MutableStateFlow(12f) // 存 float
    val fontSize = _fontSize.asStateFlow()
    val uiState = _uiState.asStateFlow()

    private var searchJob: Job? = null

    fun increaseFontSize() {
        _fontSize.update { current ->
            (current + 2f).coerceAtMost(30f) // 最大限制 30
        }
    }

    fun decreaseFontSize() {
        _fontSize.update { current ->
            (current - 2f).coerceAtLeast(8f) // 最小限制 8
        }
    }

    fun resetFontSize() {
        _fontSize.value = 12f
    }


    // 加载日志文件
    fun loadLogs(path: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val file = File(path)
            if (!file.exists()) {
                _uiState.update { it.copy(isLoading = false, displayLines = listOf("文件不存在")) }
                return@launch
            }

            /// 优化：使用双端队列，只保留最后 200,000 行
            // 这样即使文件有 1GB，内存中也只会有 20w 行，不会 OOM
            val maxLines = 200000
            val buffer = java.util.ArrayDeque<String>(maxLines)

            try {
                file.useLines { sequence ->
                    sequence.forEach { line ->
                        if (buffer.size >= maxLines) {
                            buffer.removeFirst() // 移除最早的一行
                        }
                        buffer.addLast(line)
                    }
                }
            } catch (e: Exception) {
                buffer.add("读取错误: ${e.message}")
            }

            val safeLines = buffer.toList() // 转为不可变 List 供 UI 使用

            withContext(Dispatchers.Main) {
                _uiState.update {
                    it.copy(
                        allLines = safeLines,
                        displayLines = safeLines,
                        totalCount = safeLines.size,
                        isLoading = false
                    )
                }
            }
        }
    }

    // 执行搜索 (直接过滤，不建立索引)
    fun search(query: String) {
        // 取消上一次正在进行的搜索
        searchJob?.cancel()

        _uiState.update { it.copy(searchQuery = query) }

        if (query.isBlank()) {
            _uiState.update { it.copy(displayLines = it.allLines) }
            return
        }

        searchJob = viewModelScope.launch(Dispatchers.Default) {
            // 简单的包含匹配，对于 20w 行数据，现代 CPU 只需要几十毫秒
            val filtered = _uiState.value.allLines.filter {
                it.contains(query, ignoreCase = true)
            }

            _uiState.update { it.copy(displayLines = filtered) }
        }
    }

    fun clearLogs() {
        // 实现清空逻辑...
    }
}
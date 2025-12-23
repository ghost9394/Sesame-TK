package fansirsqi.xposed.sesame.ui.log

import android.content.Context
import android.os.Build
import android.os.FileObserver
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fansirsqi.xposed.sesame.R
import fansirsqi.xposed.sesame.util.Files
import fansirsqi.xposed.sesame.util.Log
import fansirsqi.xposed.sesame.util.ToastUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

data class LogUiState(
    // 现在的 mappingList 存储的是： 列表索引 -> 真实文件行号
    // 如果没有搜索，这就是一个 0, 1, 2... 的序列
    // 如果有搜索，这就是匹配到的行号列表 [5, 12, 100...]
    val mappingList: List<Int> = emptyList(),
    val isLoading: Boolean = true,
    val searchQuery: String = "",
    val totalCount: Int = 0,
    val autoScroll: Boolean = true
)

class LogViewerViewModel : ViewModel() {

    private val tag = "LogViewerViewModel"

    private val _uiState = MutableStateFlow(LogUiState())
    val uiState = _uiState.asStateFlow()

    private val _fontSize = MutableStateFlow(12f)
    val fontSize = _fontSize.asStateFlow()

    private val _scrollEvent = Channel<Int>(Channel.BUFFERED)
    val scrollEvent = _scrollEvent.receiveAsFlow()

    private var logReader: LogFileReader? = null
    private var fileObserver: FileObserver? = null
    private var currentFilePath: String? = null
    private var searchJob: Job? = null

    fun loadLogs(path: String) {
        if (currentFilePath == path) return
        currentFilePath = path

        viewModelScope.launch {
            val file = File(path)
            if (!file.exists()) {
                _uiState.update { it.copy(isLoading = false) }
                return@launch
            }

            // 初始化读取器
            logReader = LogFileReader(file)
            logReader?.buildIndex() // 建立索引

            refreshList()
            startFileObserver(path)
        }
    }

    // 根据当前的搜索状态，刷新 UI 列表
    private suspend fun refreshList() {
        val reader = logReader ?: return
        val totalLines = reader.getLineCount()
        val query = _uiState.value.searchQuery

        val newMapping: List<Int>

        if (query.isEmpty()) {
            // 没有搜索：直接生成 0 到 N-1 的自然数列
            newMapping = List(totalLines) { it }
        } else {
            // 有搜索：遍历读取并匹配
            // 注意：全量搜索 50MB 文件可能耗时，这部分已经在 IO 线程
            newMapping = withContext(Dispatchers.IO) {
                val matches = ArrayList<Int>()
                for (i in 0 until totalLines) {
                    // 读取一行进行匹配
                    val line = reader.readLine(i)
                    if (line.contains(query, true)) {
                        matches.add(i)
                    }
                }
                matches
            }
        }

        _uiState.update {
            it.copy(
                mappingList = newMapping,
                totalCount = newMapping.size,
                isLoading = false
            )
        }

        // 自动滚动
        if (_uiState.value.autoScroll && newMapping.isNotEmpty()) {
            _scrollEvent.send(newMapping.size - 1)
        }
    }

    /**
     * 给 UI 调用的方法：获取指定位置的文本
     * position: 列表中的位置 (0 ~ mappingList.size)
     */
    fun getLineContent(position: Int): String {
        val mapping = _uiState.value.mappingList
        if (position < 0 || position >= mapping.size) return ""

        // 获取真实行号
        val realLineIndex = mapping[position]
        // 从磁盘/缓存读取
        return logReader?.readLine(realLineIndex) ?: ""
    }

    private fun startFileObserver(path: String) {
        val file = File(path)
        val parentPath = file.parent ?: return
        val parentFile = File(parentPath)

        val onFileEvent: (String?) -> Unit = { p ->
            if (p == file.name) {
                viewModelScope.launch {
                    logReader?.updateIndex() // 增量更新索引
                    refreshList() // 刷新列表
                }
            }
        }

        fileObserver = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            object : FileObserver(parentFile, MODIFY) {
                override fun onEvent(event: Int, p: String?) {
                    onFileEvent(p)
                }
            }
        } else {
            @Suppress("DEPRECATION")
            object : FileObserver(parentPath, MODIFY) {
                override fun onEvent(event: Int, p: String?) {
                    onFileEvent(p)
                }
            }
        }

        fileObserver?.startWatching()
    }

    // --- 新增功能：清空日志 ---
    fun clearLogFile(context: Context) { // 增加 context 参数用于弹 Toast
        val path = currentFilePath ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // 调用你已有的工具类
                if (Files.clearFile(File(path))) {
                    // 如果工具类返回 true，说明清空成功
                    // 重建索引（此时为空）
                    logReader?.buildIndex()
                    refreshList()

                    withContext(Dispatchers.Main) {
                        // 强制更新状态为空
                        _uiState.update {
                            it.copy(
                                mappingList = emptyList(),
                                totalCount = 0
                            )
                        }
                        // 弹提示
                        ToastUtil.showToast(context, "文件已清空")
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        ToastUtil.showToast(context, "清空失败")
                    }
                }
            } catch (e: Exception) {
                Log.printStackTrace(tag, "Clear error", e)
                withContext(Dispatchers.Main) {
                    ToastUtil.showToast(context, "清空异常: ${e.message}")
                }
            }
        }
    }

    // --- 导出日志 (使用你的 Files 工具类) ---
    fun exportLogFile(context: Context) {
        val path = currentFilePath ?: return
        try {
            val file = File(path)
            if (!file.exists()) {
                ToastUtil.showToast(context, "源文件不存在")
                return
            }
            // 调用你项目中的工具类进行导出
            val exportFile = Files.exportFile(file, true)
            if (exportFile != null && exportFile.exists()) {
                val msg = "${context.getString(R.string.file_exported)} ${exportFile.path}"
                ToastUtil.showToast(context, msg)
            } else {
                ToastUtil.showToast(context, "导出失败")
            }
        } catch (e: Exception) {
            Log.printStackTrace(tag, "Export error", e)
            ToastUtil.showToast(context, "导出异常: ${e.message}")
        }
    }

    fun search(query: String) {
        searchJob?.cancel()
        _uiState.update { it.copy(searchQuery = query) }
        searchJob = viewModelScope.launch(Dispatchers.Default) {
            refreshList()
        }
    }

    fun increaseFontSize() {
        _fontSize.update { (it + 2f).coerceAtMost(30f) }
    }

    fun decreaseFontSize() {
        _fontSize.update { (it - 2f).coerceAtLeast(8f) }
    }

    fun scaleFontSize(factor: Float) {
        _fontSize.update { (it * factor).coerceIn(8f, 50f) }
    }

    fun resetFontSize() {
        _fontSize.value = 12f
    }

    fun toggleAutoScroll(enabled: Boolean) {
        _uiState.update { it.copy(autoScroll = enabled) }
        if (enabled) viewModelScope.launch {
            val size = _uiState.value.mappingList.size
            if (size > 0) _scrollEvent.send(size - 1)
        }
    }

    override fun onCleared() {
        super.onCleared()
        fileObserver?.stopWatching()
    }
}
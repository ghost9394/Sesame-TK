package fansirsqi.xposed.sesame.ui.log

import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.RandomAccessFile

/**
 * 高性能日志读取器
 * 原理：只在内存中保存每一行的起始位置(Offset)，内容按需从磁盘读取。
 * 支持秒开数百MB的大文件，内存占用极低。
 */
class LogFileReader(val file: File) {

    // 存储每一行的起始字节偏移量
    // 20万行日志只需要约 1.6MB 内存
    private val lineOffsets = ArrayList<Long>()

    // 简单的内存缓存，防止频繁 IO 导致列表滑动卡顿
    // 缓存最近读取的 1000 行字符串
    private val lineCache = object : LruCache<Int, String>(1000) {}

    // 文件总大小，用于检测变动
    var fileSize: Long = 0
        private set

    /**
     * 索引构建（耗时操作，需在后台执行）
     * 扫描整个文件，找到所有的换行符
     */
    suspend fun buildIndex() = withContext(Dispatchers.IO) {
        lineOffsets.clear()
        lineCache.evictAll()
        fileSize = file.length()

        if (!file.exists()) return@withContext

        // 第 0 行从 0 开始
        lineOffsets.add(0L)

        // 使用 BufferedInputStream 高效扫描换行符
        file.inputStream().buffered(1024 * 128).use { fis ->
            var pos = 0L
            var byteData = fis.read()
            while (byteData != -1) {
                pos++
                // 遇到换行符，记录下一行的起始位置
                if (byteData == '\n'.code) {
                    // 如果不是最后一行，记录位置
                    lineOffsets.add(pos)
                }
                byteData = fis.read()
            }
        }
        // 如果最后一行后面没有换行符，索引已经正确，因为我们记录的是"下一行起始位"
        // 但要注意最后一行如果不为空，上面逻辑已覆盖
    }

    /**
     * 获取总行数
     */
    fun getLineCount(): Int = lineOffsets.size

    /**
     * 读取指定行的内容
     * @param index 行号
     */
    fun readLine(index: Int): String {
        if (index < 0 || index >= lineOffsets.size) return ""

        // 1. 先查缓存
        lineCache.get(index)?.let { return it }

        // 2. 缓存未命中，从磁盘读取
        // 注意：RandomAccessFile 读取是同步 IO，但在现代闪存上读一行极快
        return try {
            val start = lineOffsets[index]
            val end = if (index + 1 < lineOffsets.size) {
                lineOffsets[index + 1] - 1 // 减去换行符
            } else {
                fileSize // 最后一行读到文件末尾
            }

            val length = (end - start).toInt()
            if (length <= 0) return ""

            RandomAccessFile(file, "r").use { raf ->
                raf.seek(start)
                val bytes = ByteArray(length)
                raf.readFully(bytes)
                // 假设是 UTF-8，如果需要支持其他编码可在此修改
                val str = String(bytes, Charsets.UTF_8)

                // 3. 放入缓存
                lineCache.put(index, str)
                str
            }
        } catch (e: Exception) {
            "<读取错误: ${e.message}>"
        }
    }

    /**
     * 增量更新（当文件变大时，只扫描新增部分）
     */
    suspend fun updateIndex() = withContext(Dispatchers.IO) {
        val newSize = file.length()
        if (newSize <= fileSize) {
            // 如果文件变小了（被清空），重全量建索引
            if (newSize < fileSize) buildIndex()
            return@withContext
        }

        val startPos = fileSize
        fileSize = newSize

        file.inputStream().use { fis ->
            fis.skip(startPos) // 跳过已知部分
            fis.buffered(1024 * 64).use { bis ->
                var pos = startPos
                var byteData = bis.read()
                while (byteData != -1) {
                    pos++
                    if (byteData == '\n'.code) {
                        lineOffsets.add(pos)
                    }
                    byteData = bis.read()
                }
            }
        }
    }
}
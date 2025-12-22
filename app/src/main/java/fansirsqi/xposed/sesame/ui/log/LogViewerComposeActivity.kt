package fansirsqi.xposed.sesame.ui.log

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme

/**
 * 承载 Compose 日志查看器的 Activity
 */
class LogViewerComposeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 从 Intent 中获取文件路径
        // MainActivity 中传的是: data = fileUri ("file:///storage/...")
        // intent.data?.path 会自动去掉 "file://" 前缀，拿到实际路径
        val path = intent.data?.path ?: ""

        setContent {
            // 使用暗色主题，因为 LogViewerScreen 的背景硬编码为了深色 (0xFF1E1E1E)
            MaterialTheme(colorScheme = darkColorScheme()) {
                LogViewerScreen(
                    filePath = path,
                    onBackClick = {
                        // 点击返回箭头时关闭当前页面
                        finish()
                    }
                )
            }
        }
    }
}
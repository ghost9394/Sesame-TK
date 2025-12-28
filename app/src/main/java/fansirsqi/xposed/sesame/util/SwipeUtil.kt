package fansirsqi.xposed.sesame.util

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.os.RemoteException
import android.util.Log
import androidx.core.net.toUri
import fansirsqi.xposed.sesame.ICallback
import fansirsqi.xposed.sesame.ICommandService
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/**
 * 滑动操作工具类
 * 通过 AIDL 调用 CommandService 执行 Shizuku 命令
 */
object SwipeUtil {

    private const val TAG = "SwipeUtil"

    @JvmStatic
    fun startBySchemeSync(context: Context): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_VIEW, "alipays://platformapi/startapp?appId=".toUri())
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            Log.d(TAG, "Scheme 启动成功")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Scheme 启动失败: ${e.message}")
            false
        }
    }
}
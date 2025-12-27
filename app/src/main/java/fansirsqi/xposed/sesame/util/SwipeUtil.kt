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
    private const val DEFAULT_DURATION = 500L
    private const val TIMEOUT_MS = 10000L
    private const val ACTION_BIND = "fansirsqi.xposed.sesame.action.BIND_COMMAND_SERVICE"
    private const val BIND_TIMEOUT_MS = 2000L

    private var commandService: ICommandService? = null
    private var isBound = false
    private var lastBindAttemptTime = 0L
    private var lastBindSuccess = false

    // 修复 1: 改为 var 并设为可空，每次绑定时重建
    private var connectionDeferred: CompletableDeferred<Unit>? = null

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            Log.d(TAG, "CommandService 已连接")
            commandService = ICommandService.Stub.asInterface(service)
            isBound = true
            // 修复: 只有当 deferred 存在且未完成时才完成它
            connectionDeferred?.let {
                if (it.isActive) it.complete(Unit)
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            Log.d(TAG, "CommandService 已断开")
            commandService = null
            isBound = false
            // 这里不需要处理 deferred，因为再次绑定会创建新的
        }
    }

    /**
     * 绑定服务（同步等待连接完成）
     * 优化：缓存绑定结果，避免重复等待
     */
    private suspend fun bindService(context: Context): Boolean = withContext(Dispatchers.IO) {
        if (isBound && commandService != null) {
            return@withContext true
        }
        
        val currentTime = System.currentTimeMillis()
        if (lastBindAttemptTime > 0 && (currentTime - lastBindAttemptTime) < 30000L && !lastBindSuccess) {
            Log.d(TAG, "30秒内已尝试绑定失败，跳过本次绑定")
            return@withContext false
        }
        
        lastBindAttemptTime = currentTime
        
        try {
            connectionDeferred = CompletableDeferred()
            val intent = Intent(ACTION_BIND)
            intent.setPackage("fansirsqi.xposed.sesame")
            val result = context.applicationContext.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
            Log.d(TAG, "绑定服务结果: $result" + "请启动Sesame-TK")
            if (!result) {
                lastBindSuccess = false
                return@withContext false
            }
            val connected = withTimeoutOrNull(BIND_TIMEOUT_MS) {
                connectionDeferred?.await()
            }
            lastBindSuccess = (connected != null)
            connected != null
        } catch (e: Exception) {
            Log.e(TAG, "绑定服务失败: ${e.message}")
            lastBindSuccess = false
            false
        }
    }



    suspend fun execShizukuCommandOriginal(context: Context, command: String): Boolean = withContext(Dispatchers.IO) {
        Log.d(TAG, "准备执行命令: $command")
        if (!bindService(context)) {
            Log.e(TAG, "绑定服务失败，无法执行命令")
            return@withContext false
        }
        val service = commandService
        if (service == null) {
            Log.e(TAG, "commandService 为 null，无法执行命令")
            return@withContext false
        }
        
        Log.d(TAG, "服务已连接，开始发送命令: $command")
        val deferred = CompletableDeferred<Boolean>()
        val callback = object : ICallback.Stub() {
            override fun onSuccess(output: String) {
                Log.d(TAG, "命令执行成功: $output")
                deferred.complete(true)
            }
            override fun onError(error: String) {
                Log.e(TAG, "命令执行失败: $error")
                deferred.complete(false)
            }
        }
        try {
            service.executeCommand(command, callback)
            Log.d(TAG, "命令已发送，等待结果...")
            val result = withTimeoutOrNull(TIMEOUT_MS) { deferred.await() }
            Log.d(TAG, "命令执行结果: $result")
            result ?: false
        } catch (e: Exception) {
            Log.e(TAG, "执行命令异常: ${e.message}", e)
            false
        }
    }

    /**
     * 执行滑动操作
     * @param context 上下文
     * @param startX 起始X坐标
     * @param startY 起始Y坐标
     * @param endX 结束X坐标
     * @param endY 结束Y坐标
     * @param duration 滑动持续时间
     * @return true表示成功，false表示失败
     */
    suspend fun swipe(context: Context, startX: Int, startY: Int, endX: Int, endY: Int, duration: Long = DEFAULT_DURATION): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "执行滑动: ($startX, $startY) -> ($endX, $endY), 耗时: ${duration}ms")
            val command = "input swipe $startX $startY $endX $endY $duration"
            
            val result = execShizukuCommandOriginal(context, command)
            if (result) {
                Log.d(TAG, "滑动命令发送成功")
                delay(duration + 200)
                true
            } else {
                Log.e(TAG, "滑动操作失败,Sesame-TK后台已关闭")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "滑动操作异常: ${e.message}")
            false
        }
    }

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
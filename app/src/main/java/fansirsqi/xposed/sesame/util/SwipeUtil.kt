package fansirsqi.xposed.sesame.util

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.net.Uri
import android.os.IBinder
import android.os.RemoteException
import android.util.Log
import fansirsqi.xposed.sesame.ICallback
import fansirsqi.xposed.sesame.ICommandService
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/**
 * 滑动操作工具类
 * 通过 AIDL 调用 CommandService 执行 root 命令
 */
object SwipeUtil {

    private const val TAG = "SwipeUtil"

    private const val DEFAULT_DURATION = 500L

    private const val TIMEOUT_MS = 10000L

    private const val ACTION_BIND = "fansirsqi.xposed.sesame.action.BIND_COMMAND_SERVICE"

    private var commandService: ICommandService? = null

    private var isBound = false

    private val connectionDeferred = CompletableDeferred<Unit>()

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            Log.d(TAG, "CommandService 已连接")
            commandService = ICommandService.Stub.asInterface(service)
            isBound = true
            if (!connectionDeferred.isCompleted) {
                connectionDeferred.complete(Unit)
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            Log.d(TAG, "CommandService 已断开")
            commandService = null
            isBound = false
            if (!connectionDeferred.isCompleted) {
                connectionDeferred.completeExceptionally(Exception("服务已断开"))
            }
        }
    }

    /**
     * 绑定服务（同步等待连接完成）
     * @param context 上下文
     */
    private suspend fun bindService(context: Context): Boolean = withContext(Dispatchers.IO) {
        if (isBound && commandService != null) {
            return@withContext true
        }

        try {
            val intent = Intent(ACTION_BIND)
            intent.setPackage("fansirsqi.xposed.sesame")
            val result = context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
            Log.d(TAG, "绑定服务结果: $result")

            if (!result) {
                return@withContext false
            }

            // 等待服务连接完成，最多等待5秒
            val connected = withTimeoutOrNull(5000) {
                connectionDeferred.await()
            }
            connected != null
        } catch (e: Exception) {
            Log.e(TAG, "绑定服务失败: ${e.message}")
            false
        }
    }

    /**
     * 执行 Root 命令（通过 AIDL）
     * @param context 上下文
     * @param command 要执行的命令
     * @return 命令执行结果
     */
    private suspend fun execRootCommand(context: Context, command: String): Boolean = withContext(Dispatchers.IO) {
        if (!bindService(context)) {
            Log.e(TAG, "无法绑定 CommandService")
            return@withContext false
        }

        val service = commandService
        if (service == null) {
            Log.e(TAG, "CommandService 未连接")
            return@withContext false
        }

        val deferred = CompletableDeferred<Boolean>()

        val callback = object : ICallback.Stub() {
            override fun onSuccess(output: String) {
                Log.d(TAG, "命令执行成功: $command")
                deferred.complete(true)
            }

            override fun onError(error: String) {
                Log.e(TAG, "命令执行失败: $command, 错误: $error")
                deferred.complete(false)
            }
        }

        try {
            service.executeCommand(command, callback)
            withTimeoutOrNull(TIMEOUT_MS) {
                deferred.await()
            } ?: false
        } catch (e: RemoteException) {
            Log.e(TAG, "执行命令异常: $command, 错误: ${e.message}")
            false
        } catch (e: Exception) {
            Log.e(TAG, "执行命令超时或异常: $command, 错误: ${e.message}")
            false
        }
    }

    /**
     * 执行滑动操作（使用协程）
     * @param context 上下文
     * @param startX 起始 X 坐标
     * @param startY 起始 Y 坐标
     * @param endX 结束 X 坐标
     * @param endY 结束 Y 坐标
     * @param duration 滑动持续时间（毫秒）
     * @return 是否成功执行
     */
    suspend fun swipe(context: Context, startX: Int, startY: Int, endX: Int, endY: Int, duration: Long = DEFAULT_DURATION): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "执行滑动: ($startX, $startY) -> ($endX, $endY), 耗时: ${duration}ms")

            val command = "input swipe $startX $startY $endX $endY $duration"
            val result = execRootCommand(context, command)
            if (result) {
                Log.d(TAG, "滑动成功")
            } else {
                Log.e(TAG, "滑动失败")
            }
            result
        } catch (e: Exception) {
            Log.e(TAG, "滑动操作失败: ${e.message}")
            e.printStackTrace()
            false
        }
    }

    /**
     * 执行滑动操作（使用 IntArray 参数，使用协程）
     * @param context 上下文
     * @param path 滑动路径数组 [startX, startY, endX, endY]
     * @param duration 滑动持续时间（毫秒）
     * @return 是否成功执行
     */
    suspend fun swipe(context: Context, path: IntArray, duration: Long = DEFAULT_DURATION): Boolean {
        if (path.size < 4) {
            Log.e(TAG, "滑动路径参数错误，需要至少4个坐标值")
            return false
        }
        return swipe(context, path[0], path[1], path[2], path[3], duration)
    }

    /**
     * 执行点击操作（使用协程）
     * @param context 上下文
     * @param x X 坐标
     * @param y Y 坐标
     * @return 是否成功执行
     */
    suspend fun click(context: Context, x: Int, y: Int): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "执行点击: ($x, $y)")

            val command = "input tap $x $y"
            execRootCommand(context, command)
        } catch (e: Exception) {
            Log.e(TAG, "点击操作失败: ${e.message}")
            false
        }
    }

    /**
     * 执行长按操作（使用协程）
     * @param context 上下文
     * @param x X 坐标
     * @param y Y 坐标
     * @param duration 长按持续时间（毫秒）
     * @return 是否成功执行
     */
    suspend fun longPress(context: Context, x: Int, y: Int, duration: Long = 1000): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "执行长按: ($x, $y), 耗时: ${duration}ms")

            val command = "input swipe $x $y $x $y $duration"
            execRootCommand(context, command)
        } catch (e: Exception) {
            Log.e(TAG, "长按操作失败: ${e.message}")
            false
        }
    }

    /**
     * 启动支付宝应用（使用 root 命令）
     * @param context 上下文
     * @return 是否成功执行
     */
    suspend fun startAlipay(context: Context): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "启动支付宝")

            val command = "am start -n com.eg.android.AlipayGphone/com.eg.android.AlipayGphone.AlipayLogin --ez anim_from_item_click true --ei start_reason 3 --ez anim_not_finish false"
            val result = execRootCommand(context, command)
            if (result) {
                Log.d(TAG, "支付宝启动成功")
            } else {
                Log.e(TAG, "支付宝启动失败")
            }
            result
        } catch (e: Exception) {
            Log.e(TAG, "启动支付宝失败: ${e.message}")
            false
        }
    }

    /**
     * 启动支付宝应用（使用 root 命令，非协程版本，供 Java 调用）
     * @param context 上下文
     * @return 是否成功执行
     */
    @JvmStatic
    fun startAlipaySync(context: Context): Boolean {
        try {
            Log.d(TAG, "启动支付宝（同步版本）")
            GlobalThreadPools.execute {
                try {
                    bindService(context)
                    if (commandService == null) {
                        Log.e(TAG, "服务未连接，无法启动支付宝")
                        return@execute
                    }
                    val command = "am start -n com.eg.android.AlipayGphone/com.eg.android.AlipayGphone.AlipayLogin"
                    val deferred = CompletableDeferred<Boolean>()
                    val callback = object : ICallback.Stub() {
                        override fun onSuccess(output: String?) {
                            Log.d(TAG, "支付宝启动成功: $output")
                            deferred.complete(true)
                        }

                        override fun onError(error: String?) {
                            Log.e(TAG, "支付宝启动失败: $error")
                            deferred.complete(false)
                        }
                    }
                    commandService?.executeCommand(command, callback)
                    val result = deferred.await()
                    
                    if (result) {
                        Log.d(TAG, "等待用户完成滑块验证...")
                        Thread.sleep(10000)
                        Log.d(TAG, "滑块验证等待完成")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "启动支付宝异常: ${e.message}")
                }
            }
            return true
        } catch (e: Exception) {
            Log.e(TAG, "启动支付宝失败: ${e.message}")
            return false
        }
    }

    /**
     * 通过 Scheme 启动应用（使用协程，无需 root）
     * @param context 上下文
     * @param scheme URL Scheme（如 alipays://platformapi/startapp?appId=xxx）
     * @return 是否成功执行
     */
    suspend fun startByScheme(context: Context, scheme: String): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "通过 Scheme 启动: $scheme")

            val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(scheme))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            Log.d(TAG, "Scheme 启动成功")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Scheme 启动失败: ${e.message}")
            false
        }
    }

    /**
     * 通过 Scheme 启动应用（非协程版本，供 Java 调用，无需 root）
     * @param context 上下文
     * @param scheme URL Scheme（如 alipays://platformapi/startapp?appId=xxx）
     * @return 是否成功执行
     */
    @JvmStatic
    fun startBySchemeSync(context: Context, scheme: String): Boolean {
        return try {
            Log.d(TAG, "通过 Scheme 启动（同步版本）: $scheme")
            val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(scheme))
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

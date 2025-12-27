package fansirsqi.xposed.sesame.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import fansirsqi.xposed.sesame.ICallback
import fansirsqi.xposed.sesame.ICommandService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout

/**
 * 命令执行服务
 * 使用 cmd-android 库和 ShellManager 执行命令
 */
class CommandService : Service() {

    companion object {
        private const val TAG = "CommandService"

        // 设置命令执行超时时间，例如 15 秒
        private const val COMMAND_TIMEOUT_MS = 15000L
    }

    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    private val commandMutex = Mutex()
    
    // ShellManager 实例
    private var shellManager: ShellManager? = null

    private val binder = object : ICommandService.Stub() {
        override fun executeCommand(command: String, callback: ICallback?) {
            Log.d(TAG, "收到命令执行请求: $command")

            serviceScope.launch {
                commandMutex.withLock {
                    try {
                        // 初始化 ShellManager（如果尚未初始化）
                        if (shellManager == null) {
                            shellManager = ShellManager(applicationContext)
                        }

                        Log.d(TAG, "开始执行命令: $command")

                        // 使用 ShellManager 执行命令，带超时
                        val result = withTimeout(COMMAND_TIMEOUT_MS) {
                            shellManager!!.exec(command)
                        }

                        // 处理执行结果
                        if (result.isSuccess) {
                            Log.d(TAG, "命令执行成功: $command (使用 ${shellManager!!.selectedName})")
                            callback?.onSuccess(result.stdout.trim())
                        } else {
                            Log.e(TAG, "命令执行失败: $command (使用 ${shellManager!!.selectedName}), 退出码: ${result.exitCode}, 错误: ${result.stderr}")
                            callback?.onError("退出码: ${result.exitCode}, 错误: ${result.stderr}")
                        }

                    } catch (e: TimeoutCancellationException) {
                        Log.e(TAG, "命令执行超时 (${COMMAND_TIMEOUT_MS}ms): $command")
                        callback?.onError("命令执行超时")
                    } catch (e: Exception) {
                        Log.e(TAG, "执行命令异常: $command, 错误: ${e.message}")
                        callback?.onError(e.message ?: "未知错误")
                    } finally {
                        Log.d(TAG, "命令执行流程结束: $command")
                    }
                }
            }
        }

        override fun getShellType(): String {
            return shellManager?.selectedName ?: "未初始化"
        }
    }

    override fun onBind(intent: Intent?): IBinder {
        Log.d(TAG, "CommandService onBind 被调用")
        // 初始化 ShellManager 实例
        shellManager = ShellManager(applicationContext)
        Log.i(TAG, "ShellManager 已初始化, 当前 Shell: ${shellManager?.selectedName}")
        return binder
    }

    override fun onUnbind(intent: Intent?): Boolean {
        Log.d(TAG, "CommandService 解绑")
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        // 清理资源
        shellManager = null
        serviceScope.cancel() // 销毁时取消所有协程任务
        Log.d(TAG, "CommandService 销毁")
    }

}
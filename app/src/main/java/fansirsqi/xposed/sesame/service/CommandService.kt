package fansirsqi.xposed.sesame.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.os.RemoteCallbackList
import android.os.RemoteException
import android.util.Log
import fansirsqi.xposed.sesame.ICallback
import fansirsqi.xposed.sesame.ICommandService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.BufferedReader

/**
 * 命令执行服务
 * 在 Xposed 模块进程中运行，执行 root 命令
 */
class CommandService : Service() {

    companion object {
        private const val TAG = "CommandService"
        private const val ACTION_BIND = "fansirsqi.xposed.sesame.action.BIND_COMMAND_SERVICE"
    }

    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    private val commandMutex = Mutex()

    private val binder = object : ICommandService.Stub() {
        override fun executeCommand(command: String, callback: ICallback?) {
            Log.d(TAG, "收到命令执行请求: $command")
            serviceScope.launch {
                commandMutex.withLock {
                    Log.d(TAG, "开始执行命令: $command")
                    var process: Process? = null
                    try {
                        process = Runtime.getRuntime().exec(arrayOf("su", "-c", command))
                        
                        val output = StringBuilder()
                        val error = StringBuilder()
                        
                        val outputJob = launch {
                            process?.inputStream?.bufferedReader()?.use { reader ->
                                reader.forEachLine { line ->
                                    output.append(line).append("\n")
                                }
                            }
                        }
                        
                        val errorJob = launch {
                            process?.errorStream?.bufferedReader()?.use { reader ->
                                reader.forEachLine { line ->
                                    error.append(line).append("\n")
                                }
                            }
                        }
                        
                        outputJob.join()
                        errorJob.join()
                        
                        val exitCode = process?.waitFor() ?: -1
                        
                        if (exitCode == 0) {
                            Log.d(TAG, "命令执行成功: $command")
                            callback?.onSuccess(output.toString())
                        } else {
                            Log.e(TAG, "命令执行失败: $command, 退出码: $exitCode, 错误: $error")
                            callback?.onError("退出码: $exitCode, 错误: $error")
                        }
                    } catch (e: TimeoutCancellationException) {
                        Log.e(TAG, "命令执行超时: $command")
                        process?.destroy()
                        callback?.onError("命令执行超时")
                    } catch (e: Exception) {
                        Log.e(TAG, "执行命令异常: $command, 错误: ${e.message}")
                        callback?.onError(e.message ?: "未知错误")
                    } finally {
                        process?.destroy()
                        Log.d(TAG, "命令执行完成: $command")
                    }
                }
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder {
        Log.d(TAG, "CommandService 绑定")
        return binder
    }

    override fun onUnbind(intent: Intent?): Boolean {
        Log.d(TAG, "CommandService 解绑")
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "CommandService 销毁")
    }
}

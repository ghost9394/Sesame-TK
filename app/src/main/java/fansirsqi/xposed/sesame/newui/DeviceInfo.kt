package fansirsqi.xposed.sesame.newui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import android.os.RemoteException
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fansirsqi.xposed.sesame.BuildConfig
import fansirsqi.xposed.sesame.ICallback
import fansirsqi.xposed.sesame.ICommandService
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

class PreviewDeviceInfoProvider : PreviewParameterProvider<Map<String, String>> {
    override val values: Sequence<Map<String, String>> = sequenceOf(
        mapOf(
            "型号" to "Pixel 6",
            "产品" to "Google Pixel",
            "Android ID" to "abcd1234567890ef",
            "系统" to "Android 13 (33)",
            "构建" to "UQ1A.230105.002 S1B51",
            "OTA" to "OTA-12345",
            "SN" to "SN1234567890",
            "模块版本" to "v1.0.0-release 📦",
            "构建日期" to "2023-10-01 12:00 ⏰"
        )
    )
}


@Composable
fun DeviceInfoCard(info: Map<String, String>) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            info.forEach { (label, value) ->
                when (label) {
                    "Verify ID" -> {
                        var showFull by remember { mutableStateOf(false) }
                        val displayValue = if (showFull) value else "***********"
                        val context = LocalContext.current
                        Text(
                            text = "$label: $displayValue",
                            fontSize = 14.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable { showFull = !showFull }
                                .combinedClickable(
                                    onClick = { showFull = !showFull },
                                    onLongClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        val clip = ClipData.newPlainText("Android ID", value)
                                        clipboard.setPrimaryClip(clip)
                                        Toast.makeText(context, "Verify ID copied", Toast.LENGTH_SHORT).show()
                                    }
                                )
                        )
                    }

                    else -> {
                        Text(text = "$label: $value", fontSize = 14.sp)
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}

object DeviceInfoUtil {

    private const val TAG = "DeviceInfoUtil"

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
    private suspend fun execRootCommand(context: Context, command: String): String = withContext(Dispatchers.IO) {
        if (!bindService(context)) {
            Log.e(TAG, "无法绑定 CommandService")
            return@withContext ""
        }

        val service = commandService
        if (service == null) {
            Log.e(TAG, "CommandService 未连接")
            return@withContext ""
        }

        val deferred = CompletableDeferred<String>()

        val callback = object : ICallback.Stub() {
            override fun onSuccess(output: String) {
              //  Log.d(TAG, "命令执行成功: $command")
                deferred.complete(output)
            }

            override fun onError(error: String) {
                Log.e(TAG, "命令执行失败: $command, 错误: $error")
                deferred.complete("")
            }
        }

        try {
            service.executeCommand(command, callback)
            withTimeoutOrNull(TIMEOUT_MS) {
                deferred.await()
            } ?: ""
        } catch (e: RemoteException) {
            Log.e(TAG, "执行命令异常: $command, 错误: ${e.message}")
            ""
        } catch (e: Exception) {
            Log.e(TAG, "执行命令超时或异常: $command, 错误: ${e.message}")
            ""
        }
    }

    /**
     * 检测 Root 权限（通过 AIDL）
     * @param context 上下文
     * @return 是否有 Root 权限
     */
    private suspend fun checkRootPermission(context: Context): Boolean = withContext(Dispatchers.IO) {
        try {
            val output = execRootCommand(context, "id")
            val success = output.contains("uid=0")

            if (success) {
                Log.d(TAG, "Root 权限检测成功")
            } else {
                Log.e(TAG, "Root 权限检测失败，输出: $output")
            }
            success
        } catch (e: Exception) {
            Log.e(TAG, "Root 权限检测异常: ${e.message}")
            false
        }
    }

    /**
     * 检测 Shizuku 权限
     * @return 是否有 Shizuku 权限
     */
    private fun checkShizukuPermission(): Boolean {
        return try {
            Class.forName("rikka.shizuku.Shizuku")
            val checkPermissionMethod = Class.forName("rikka.shizuku.Shizuku")
                .getMethod("checkSelfPermission", String::class.java)
            val granted = checkPermissionMethod.invoke(null, "rikka.shizuku.permission.REQUEST") as Int
            granted == 0
        } catch (e: Exception) {
            false
        }
    }

    suspend fun showInfo(vid: String, context: Context): Map<String, String> = withContext(Dispatchers.IO) {
        fun getProp(prop: String): String {
            return try {
                val p = Runtime.getRuntime().exec("getprop $prop")
                p.inputStream.bufferedReader().readLine().orEmpty()
            } catch (_: Exception) {
                ""
            }
        }

        fun getDeviceName(): String {
            val candidates = listOf(
                "ro.vendor.oplus.market.enname",
                "ro.vendor.oplus.market.name",
                "ro.product.marketname",
                "ro.vivo.market.name",
                "ro.oppo.market.name",
                "ro.product.odm.device",
                "ro.product.brand"
            )
            for (prop in candidates) {
                val value = getProp(prop)
                if (value.isNotBlank()) return value
            }
            return "${Build.BRAND} ${Build.MODEL}"
        }

        val rootPermission = checkRootPermission(context)
        val shizukuPermission = checkShizukuPermission()
        val permissionStatus = when {
            rootPermission && shizukuPermission -> "Root + Shizuku ✓"
            rootPermission -> "Root ✓"
            shizukuPermission -> "Shizuku ✓"
            else -> "None ✗"
        }

        mapOf(
            "Product" to "${Build.MANUFACTURER} ${Build.PRODUCT}",
            "Device" to getDeviceName(),
            "Android Version" to "${Build.VERSION.RELEASE} SDK (${Build.VERSION.SDK_INT})",
            "OS Build" to "${Build.DISPLAY}",
            "Permission" to permissionStatus,
            "Verify ID" to vid,
            "Module Version" to "v${BuildConfig.VERSION_NAME}.${BuildConfig.BUILD_TYPE} 📦",
            "Module Build" to "${BuildConfig.BUILD_DATE} ${BuildConfig.BUILD_TIME} ⏰"
        )
    }
}

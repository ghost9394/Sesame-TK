package fansirsqi.xposed.sesame.hook.internal

import de.robv.android.xposed.XposedHelpers
import org.json.JSONObject
import java.util.concurrent.Executors

object LocationHelper {

    private const val TAG = "LocationInfoHelper"
    private var classLoader: ClassLoader? = null
    private val executor = Executors.newSingleThreadExecutor()
    private var cachedLocation: JSONObject? = null

    /**
     * 初始化 LocationHelper
     * @param loader 支付宝的 ClassLoader
     */
    fun init(loader: ClassLoader) {
        classLoader = loader
    }

    /**
     * 获取当前位置信息
     * @return 包含经纬度的 JSONObject，如果获取失败则返回状态信息
     */
    @JvmStatic
    fun getLocation(): JSONObject? {
        if (cachedLocation == null) {
            cachedLocation = JSONObject().apply {
                put("status", "等待支付宝初始化中...")
            }
        }
        executor.execute {
            try {
                if (classLoader == null) return@execute
                val lnsctrUtilsClass = XposedHelpers.findClass("com.alipay.mobile.common.lnsctr.LnsctrUtils", classLoader)
                val latitude = XposedHelpers.callStaticMethod(lnsctrUtilsClass, "getLatitude") as? Double
                val longitude = XposedHelpers.callStaticMethod(lnsctrUtilsClass, "getLongitude") as? Double
                val cityCode = XposedHelpers.callStaticMethod(lnsctrUtilsClass, "getAdcode") as? String
                
                cachedLocation = if (latitude != null && longitude != null) {
                    JSONObject().apply {
                        put("latitude", latitude)
                        put("longitude", longitude)
                        put("cityCode", cityCode)
                    }
                } else {
                    JSONObject().apply {
                        put("status", "等待支付宝初始化中...")
                    }
                }
            } catch (e: Throwable) {
                cachedLocation = JSONObject().apply {
                    put("status", "获取经纬度失败: ${e.message}")
                }
            }
        }
        return cachedLocation
    }

}

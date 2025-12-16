package fansirsqi.xposed.sesame.util

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import fansirsqi.xposed.sesame.model.BaseModel.Companion.showToast
import fansirsqi.xposed.sesame.model.BaseModel.Companion.toastOffsetY
import fansirsqi.xposed.sesame.model.BaseModel.Companion.toastPerfix

object ToastUtil {
    private const val TAG = "ToastUtil"
    private var appContext: Context? = null

    /**
     * 初始化全局 Context。建议在 Application 类中调用。
     *
     * @param context 应用上下文
     */
    fun init(context: Context?) {
        if (context != null) {
            appContext = context.applicationContext
        }
    }

    private val context: Context
        /**
         * 获取当前环境的 Context
         *
         * @return Context
         */
        get() {
            checkNotNull(appContext) { "ToastUtil is not initialized. Call ToastUtil.init(context) in Application." }
            return appContext!!
        }

    /**
     * 显示自定义 Toast
     *
     * @param message 显示的消息
     */
    fun showToast(message: String?) {
        showToast(context, message)
    }

    /**
     * 显示自定义 Toast
     *
     * @param context 上下文
     * @param message 显示的消息
     */
    fun showToast(context: Context?, message: String?) {
        var message = message
        val shouldShow = showToast.value
        val perfix = toastPerfix.value
        if (!perfix.isNullOrBlank() || perfix != "null") {
            message = "$perfix:$message"
        }
        Log.runtime(TAG, "showToast::$shouldShow$message")
        if (shouldShow) {
            val toast = Toast.makeText(context, message, Toast.LENGTH_SHORT)
            toast.setGravity(toast.gravity, toast.xOffset, toastOffsetY.value)
            toast.show()
        }
    }

    /**
     * 创建自定义 Toast
     *
     * @param context  上下文
     * @param message  显示的消息
     * @param duration 显示时长
     * @return Toast 对象
     */
    fun makeText(context: Context?, message: String?, duration: Int): Toast {
        var message = message
        val perfix = toastPerfix.value
        if (!perfix.isNullOrBlank()||perfix != "null") {
            message = "$perfix:$message"
        }
        val toast = Toast.makeText(context, message, duration)
        toast.setGravity(toast.gravity, toast.xOffset, toastOffsetY.value)
        return toast
    }


    /**
     * 创建自定义 Toast
     *
     * @param message  显示的消息
     * @param duration 显示时长
     * @return Toast 对象
     */
    fun makeText(message: String?, duration: Int): Toast {
        return makeText(context, message, duration)
    }


    fun showToastWithDelay(context: Context?, message: String?, delayMillis: Int) {
        Handler(Looper.getMainLooper()).postDelayed({ makeText(context, message, Toast.LENGTH_SHORT).show() }, delayMillis.toLong())
    }

    fun showToastWithDelay(message: String?, delayMillis: Int) {
        Handler(Looper.getMainLooper()).postDelayed({ makeText(message, Toast.LENGTH_SHORT).show() }, delayMillis.toLong())
    }
}

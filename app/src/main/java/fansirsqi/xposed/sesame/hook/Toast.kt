package fansirsqi.xposed.sesame.hook

import android.content.Context
import android.os.Handler
import android.os.Looper
import fansirsqi.xposed.sesame.model.BaseModel.Companion.showToast
import fansirsqi.xposed.sesame.model.BaseModel.Companion.toastOffsetY
import fansirsqi.xposed.sesame.model.BaseModel.Companion.toastPerfix
import fansirsqi.xposed.sesame.util.Log

object Toast {
    private val TAG: String = Toast::class.java.getSimpleName()


    /**
     * 显示 Toast 消息
     *
     * @param message 要显示的消息
     */
    @JvmOverloads
    fun show(message: CharSequence?, force: Boolean = false) {
        var message = message
        val context = ApplicationHook.getAppContext()
        if (context == null) {
            Log.runtime(TAG, "Context is null, cannot show toast")
            return
        }
        val shouldShow = force || showToast.value
        val perfix = toastPerfix.value
        if (!perfix.isNullOrBlank() || perfix != "null") {
            message = "$perfix:$message"
        }
        if (shouldShow) {
            displayToast(context.applicationContext, message)
        }
    }

    /**
     * 显示 Toast 消息（确保在主线程中调用）
     *
     * @param context 上下文
     * @param message 要显示的消息
     */
    private fun displayToast(context: Context?, message: CharSequence?) {
        try {
            val mainHandler = Handler(Looper.getMainLooper())
            if (Looper.myLooper() == Looper.getMainLooper()) {
                // 如果当前线程是主线程，直接显示
                createAndShowToast(context, message)
            } else {
                // 在非主线程，通过 Handler 切换到主线程
                mainHandler.post { createAndShowToast(context, message) }
            }
        } catch (t: Throwable) {
            Log.printStackTrace(TAG, "displayToast err:", t)
        }
    }

    /**
     * 创建并显示 Toast
     *
     * @param context 上下文
     * @param message 要显示的消息
     */
    private fun createAndShowToast(context: Context?, message: CharSequence?) {
        try {
            val toast = android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT)
            toast.setGravity(
                toast.gravity,
                toast.xOffset,
                toastOffsetY.value
            )
            toast.show()
        } catch (t: Throwable) {
            Log.printStackTrace(TAG, "createAndShowToast err:", t)
        }
    }
}

package fansirsqi.xposed.sesame.hook

import android.annotation.SuppressLint
import android.app.Activity
import fansirsqi.xposed.sesame.hook.simple.SimpleViewImage
import fansirsqi.xposed.sesame.newutil.DataStore
import fansirsqi.xposed.sesame.util.GlobalThreadPools.sleepCompat
import fansirsqi.xposed.sesame.util.Log
import fansirsqi.xposed.sesame.util.SwipeUtil


/**
 * 验证码处理器基类 - 提取公共逻辑
 * 处理支付宝验证码页面的滑动验证码
 */
abstract class BaseCaptchaHandler {

    companion object {
        private const val TAG = "CaptchaHandler"
    }

    /**
     * 获取滑动路径在 DataStore 中的存储 key
     */
    protected abstract fun getSlidePathKey(): String

    /**
     * 是否需要调试打印所有 TextView
     */
    protected open fun shouldDebugPrintTextViews(): Boolean = false

    /**
     * 处理 Activity 中的验证码
     */
    open suspend fun handleActivity(activity: Activity, root: SimpleViewImage): Boolean {
        try {
            Log.debug(TAG, "Activity: ${activity.javaClass.name}")
            Log.debug(TAG, "Root View: ${root.getType()}")
            
            if (handleSlideCaptcha(activity, root)) {
                return true
            }

        } catch (e: Exception) {
           Log.error(TAG, "处理验证码页面时发生异常: ${e.message}")
        }

        return false
    }

    @SuppressLint("SuspiciousIndentation")
    private suspend fun handleSlideCaptcha(activity: Activity, root: SimpleViewImage): Boolean {
        return try {
           Log.captcha(TAG, "========== 开始处理滑动验证码 ==========")
           
           // 调试：打印所有TextView的文本内容
           if (shouldDebugPrintTextViews()) {
               debugPrintAllTextViews(root, 0)
           }
           
           // 查找"向右滑动验证"文字
           val slideText = root.xpath2One("//TextView[contains(@text,'向右滑动验证')]")
       
           Log.captcha(TAG, "查找结果:")
           Log.captcha(TAG, "  向右滑动验证(text): ${if (slideText != null) "找到" else "未找到"}")

           if (slideText == null) {
                Log.captcha(TAG, "未找到任何滑动验证相关文字")
                return false
            }
                
            Log.captcha(TAG, "发现滑动验证文字: ${slideText.getText()}")
            // 获取父节点位置信息
            val slideTextParent = slideText.parentNode(1)
            if (slideTextParent == null) {
                Log.captcha(TAG, "未找到父节点")
                return false
            }
            val slideRect = slideTextParent.locationOnScreen()
             Log.captcha(TAG, "滑动区域位置: ${slideRect.contentToString()}")
                // 获取当前屏幕信息
                val displayMetrics = activity.resources.displayMetrics
                val screenWidth = displayMetrics.widthPixels
                val screenHeight = displayMetrics.heightPixels
                Log.captcha(TAG, "屏幕尺寸: ${screenWidth}x$screenHeight")
                
                // 重新计算滑动路径
                val startX = slideRect[0] + 50  // 左边偏移50像素
                val centerY = slideRect[1] + 50  // 使用已知Y坐标
                // 滑动到屏幕最右边
                val endX = screenWidth - 100  // 滑动到屏幕右边，留50像素边界
                Log.captcha(TAG, "重新计算的滑动路径: ($startX, $centerY) -> ($endX, $centerY)")

                // 构建滑动路径数组
                // 将滑动路径存储到DataStore（只在路径变化时保存）
                try {
                    val slidePath = intArrayOf(startX, centerY, endX, centerY)
                    val slidePathKey = getSlidePathKey()
                    val existingPath = DataStore.get(slidePathKey, IntArray::class.java)
                    
                    // 只有路径发生变化时才保存
                    if (existingPath == null || !existingPath.contentEquals(slidePath)) {
                        DataStore.put(slidePathKey, slidePath)
                        Log.captcha(TAG, "滑动路径已保存到DataStore: [${slidePath.joinToString(", ")}]")
                    }
                    Log.captcha(TAG, "路径数据: [${slidePath.joinToString(", ")}]")
                    sleepCompat(3000)
                    // 执行滑动操作
                    val swipeSuccess = SwipeUtil.swipe(activity, startX, centerY, endX, centerY, 1000)
                    if (swipeSuccess) {
                        Log.captcha(TAG, "滑动操作执行成功")
                        return true
                    } else {
                        Log.captcha(TAG, "滑动操作执行失败")
                    }
                } catch (e: Exception) {
                    Log.captcha(TAG, "保存滑动路径到DataStore失败: ${e.message}")
                }

            return false
        } catch (e: Exception) {
            Log.captcha(TAG, "处理滑动验证码时发生异常: ${e.message}")
            false
        }
    }
    
    /**
     * 调试：打印所有TextView的文本内容
     */
    private fun debugPrintAllTextViews(view: SimpleViewImage, depth: Int) {
        val indent = "  ".repeat(depth)
        val text = view.getText()
        if (text != null && text.isNotEmpty()) {
            Log.captcha(TAG, "${indent}[${view.getType()}] text=$text")
        }
        for (child in view.children()) {
            debugPrintAllTextViews(child, depth + 1)
        }
    }

}

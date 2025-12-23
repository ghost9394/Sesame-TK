package fansirsqi.xposed.sesame.hook

import android.annotation.SuppressLint
import android.app.Activity
import com.zhenxi.Superappium.PageManager
import com.zhenxi.Superappium.ViewImage
import fansirsqi.xposed.sesame.newutil.DataStore
import fansirsqi.xposed.sesame.util.Log

/**
 * 验证码处理器 - 处理支付宝验证码页面
 * 使用 SuperAppium 框架自动识别和处理各种验证码场景
 */
class Captcha1Handler : PageManager.ActivityFocusHandler {

    override fun handleActivity(activity: Activity, root: ViewImage): Boolean {
        // 记录验证码页面出现
        try {
            // 生活缴费验证码页面
            if (handleSlideCaptcha(activity, root)) {
                return true
            }

        } catch (e: Exception) {
           Log.record("CaptchaHandler", "处理验证码页面时发生异常: ${e.message}")
        }

        // 返回false表示未消费事件，允许其他处理器继续处理
        return false
    }

    /**
     * 处理滑动验证码
     * @param activity 当前活动
     * @param root 根视图
     */
    @SuppressLint("SuspiciousIndentation")
    private fun handleSlideCaptcha(activity: Activity, root: ViewImage): Boolean {
        return try {
           Log.debug("CaptchaHandler", "========== 开始处理滑动验证码 ==========")
            // 先检测是否有"向右滑动验证"文字
            val slideText = root.xpath2One("//android.widget.TextView[contains(@text,'向右滑动验证')]")
                ?: return false
                
            Log.debug("CaptchaHandler", "发现'向右滑动验证'文字: ${slideText.text}")
            // 获取父节点位置信息
            val slideTextParent = slideText.parentNode(1)
            val slideRect = slideTextParent.locationOnScreen()
             Log.debug("CaptchaHandler", "滑动区域位置: ${slideRect.contentToString()}")
                // 获取当前屏幕信息
                val displayMetrics = activity.resources.displayMetrics
                val screenWidth = displayMetrics.widthPixels
                val screenHeight = displayMetrics.heightPixels
                Log.debug("CaptchaHandler", "屏幕尺寸: ${screenWidth}x$screenHeight")
                
                // 重新计算滑动路径
                val startX = slideRect[0] + 50  // 左边偏移50像素
                val centerY = slideRect[1] + 50  // 使用已知Y坐标
                // 滑动到屏幕最右边
                val endX = screenWidth - 100  // 滑动到屏幕右边，留50像素边界
                Log.record("CaptchaHandler", "重新计算的滑动路径: ($startX, $centerY) -> ($endX, $centerY)")

                // 构建滑动路径数组
                // 将滑动路径存储到DataStore（只在路径变化时保存）
                try {
                    val slidePath = intArrayOf(startX, centerY, endX, centerY)
                    val existingPath = DataStore.get("slide_path_Sheng_Huo", IntArray::class.java)
                    
                    // 只有路径发生变化时才保存
                    if (existingPath == null || !existingPath.contentEquals(slidePath)) {
                        DataStore.put("slide_path_Sheng_Huo", slidePath)
                        Log.record("CaptchaHandler", "滑动路径已保存到DataStore: [${slidePath.joinToString(", ")}]")
                    }
                    Log.record("CaptchaHandler", "路径数据: [${slidePath.joinToString(", ")}]")
                } catch (e: Exception) {
                    Log.record("CaptchaHandler", "保存滑动路径到DataStore失败: ${e.message}")
                }

            // 返回滑动路径信息，不执行滑动操作
            return false
        } catch (e: Exception) {
            Log.record("CaptchaHandler", "处理滑动验证码时发生异常: ${e.message}")
            false
        }
    }

}
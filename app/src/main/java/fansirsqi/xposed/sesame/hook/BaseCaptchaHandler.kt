package fansirsqi.xposed.sesame.hook

import android.annotation.SuppressLint
import android.app.Activity
import fansirsqi.xposed.sesame.hook.simple.MotionEventSimulator
import fansirsqi.xposed.sesame.hook.simple.SimplePageManager
import fansirsqi.xposed.sesame.hook.simple.SimpleViewImage
import fansirsqi.xposed.sesame.model.BaseModel
import fansirsqi.xposed.sesame.util.Log
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex

/**
 * 验证码处理程序的基类，提供处理滑动验证码的通用逻辑。
 * 该类专门用于处理支付宝验证页面上的滑动验证码。
 */
abstract class BaseCaptchaHandler {

    companion object {
        private const val TAG = "CaptchaHandler"

        // 滑动位置偏移量和持续时间
        private const val SLIDE_START_OFFSET = 25 // Offset from the left edge of the slider
        private const val SLIDE_END_MARGIN = 50   // Margin from the right edge of the screen
        private const val SLIDE_DURATION = 500L

        // 滑动后延迟检查是否成功
        private const val POST_SLIDE_CHECK_DELAY_MS = 2000L

        // 查找滑动验证文本的 XPath。文本是中文的。
        private const val SLIDE_VERIFY_TEXT_XPATH = "//TextView[contains(@text,'向右滑动验证')]"

        // 并发控制，防止多个处理程序同时运行。
        private val captchaProcessingMutex = Mutex()
    }

    /**
     * 获取在 DataStore 中存储滑动路径的键。
     * @return 用于存储滑动路径的键。
     */
    protected abstract fun getSlidePathKey(): String

    /**
     * 处理当前 Activity 中的验证码。
     * @param activity 当前 Activity 实例。
     * @param root 根视图图像。
     * @return 如果验证码处理成功返回 true，否则返回 false。
     */
    open suspend fun handleActivity(activity: Activity, root: SimpleViewImage): Boolean {
        return try {
            handleSlideCaptcha(activity, root)
        } catch (e: Exception) {
            Log.error(TAG, "处理验证码页面时发生异常: ${e.stackTraceToString()}")
            false
        }
    }

    @SuppressLint("SuspiciousIndentation")
    private suspend fun handleSlideCaptcha(activity: Activity, root: SimpleViewImage): Boolean {
        if (!captchaProcessingMutex.tryLock()) {
            Log.captcha(TAG, "验证码正在处理中，跳过本次调用")
            return true // 返回 true 告知上层已处理，避免重试
        }
        try {
            Log.captcha(TAG, "========== 开始处理滑动验证码 ==========")

            val slideTextInDialog = findSlideTextInDialog() ?: run {
                Log.captcha(TAG, "未找到滑动验证文本，跳过处理")
                return false // 未找到关键视图，返回 false 让其他处理器尝试
            }

            Log.captcha(TAG, "发现滑动验证文本: ${slideTextInDialog.getText()}")

            if (!BaseModel.enableSlide.value) {
                Log.captcha(TAG, "通过 MotionEvent 模拟滑动的功能已禁用，不执行任何操作。")
                // 如果需要，可以在此处添加回退到旧版广播的逻辑
            } else {
                performSlideAndVerify(activity, slideTextInDialog)
            }
            return true // 已决定处理，返回 true 阻止 SimplePageManager 重试
        } catch (e: Exception) {
            Log.captcha(TAG, "处理滑动验证码时发生错误: ${e.stackTraceToString()}")
            return false
        } finally {
            Log.captcha(TAG, "========== 结束处理滑动验证码 ==========")
            captchaProcessingMutex.unlock()
        }
    }

    /**
     * 执行滑动操作并验证结果。
     * @param activity 当前的 Activity。
     * @param slideTextView “向右滑动验证”文本的视图图像，作为查找滑块的锚点。
     * @return 如果验证码成功解除返回 true，否则返回 false。
     */
    private suspend fun performSlideAndVerify(activity: Activity, slideTextView: SimpleViewImage): Boolean {
        Log.captcha(TAG, "========== 正在查找真实滑块并执行滑动 ==========")

        val sliderView = ViewHierarchyAnalyzer.findActualSliderView(slideTextView) ?: run {
            Log.captcha(TAG, "未能找到可操作的滑块视图，滑动无法执行。")
            return false
        }
        // 计算滑动的绝对坐标
        val location = IntArray(2)
        sliderView.getLocationOnScreen(location)
        val viewX = location[0]
        val viewY = location[1]
        val startX = viewX + SLIDE_START_OFFSET.toFloat()
        val startY = viewY + sliderView.height / 2f

        val screenWidth = activity.resources.displayMetrics.widthPixels
        val endX = screenWidth - SLIDE_END_MARGIN.toFloat()

        Log.captcha(TAG, "计算出的滑动路径: ($startX, $startY) -> ($endX, $startY) on view: ${sliderView.javaClass.name}")

        // 执行滑动
        MotionEventSimulator.simulateSwipe(
            view = sliderView,
            startX = startX,
            startY = startY,
            endX = endX,
            endY = startY, // 水平滑动
            duration = SLIDE_DURATION
        )

        Log.captcha(TAG, "滑动模拟已发送，延迟检查结果...")
        delay(POST_SLIDE_CHECK_DELAY_MS)

        return if (checkCaptchaTextGone()) {
            Log.captcha(TAG, "验证码文本已消失，滑动成功。")
            true
        } else {
            Log.captcha(TAG, "验证码文本仍然存在，滑动可能失败。")
            false
        }
    }

    /**
     * 检查验证码验证文本是否已从视图中消失。
     * @return 如果文本已消失返回 true，如果仍然存在返回 false。
     */
    private fun checkCaptchaTextGone(): Boolean {
        val slideTextInDialog = findSlideTextInDialog()
        return if (slideTextInDialog == null) {
            Log.captcha(TAG, "验证码文本已消失 (在对话框中未找到)。")
            true
        } else {
            Log.captcha(TAG, "验证码文本仍然存在 (在对话框中找到)。")
            false
        }
    }

    /**
     * 在对话框视图中查找滑动验证文本。
     * @return 如果找到则返回文本视图的 SimpleViewImage，否则返回 null。
     */
    private fun findSlideTextInDialog(): SimpleViewImage? {
        return try {
            Log.captcha(TAG, "尝试通过 XPath 查找滑动验证文本: $SLIDE_VERIFY_TEXT_XPATH")
            val result = SimplePageManager.tryGetTopView(SLIDE_VERIFY_TEXT_XPATH)
            if (result != null) {
                Log.captcha(TAG, "成功找到滑动验证文本。")
            } else {
                Log.captcha(TAG, "未找到滑动验证文本。")
            }
            result
        } catch (e: Exception) {
            Log.captcha(TAG, "由于异常导致查找验证码文本失败: ${e.stackTraceToString()}")
            null
        }
    }
}

package fansirsqi.xposed.sesame.hook

import android.annotation.SuppressLint
import com.zhenxi.Superappium.PageManager
import com.zhenxi.Superappium.ViewImage
import android.app.Activity

import fansirsqi.xposed.sesame.util.Log

/**
 * 验证码处理器 - 处理支付宝验证码页面
 * 使用 SuperAppium 框架自动识别和处理各种验证码场景
 */
class CaptchaHandler : PageManager.ActivityFocusHandler {
    
    override fun handleActivity(activity: Activity, root: ViewImage): Boolean {
        // 记录验证码页面出现
        try {
            // 直接处理滑动验证码
            if (handleSlideCaptcha(root)) {
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
     */
    @SuppressLint("SuspiciousIndentation")
    private fun handleSlideCaptcha(root: ViewImage): Boolean {
        return try {
           Log.record("CaptchaHandler", "========== 开始处理滑动验证码 ==========")
            // 先检测是否有"向右滑动验证"文字
            val slideText = root.xpath2One("//android.widget.TextView[contains(@text,'向右滑动验证')]")
            if (slideText == null) {
               Log.record("CaptchaHandler", "未找到'向右滑动验证'文字，跳过处理")
                return false
            }

            // 打印验证文字的详细信息
               Log.record("CaptchaHandler", "发现'向右滑动验证'文字: ${slideText.text}")
                
                // 尝试多种方式获取文字位置信息
                var textLocation: IntArray? = null

            try {
                    // 方法1: 尝试locationOnScreen
                    textLocation = slideText.locationOnScreen()
                   Log.record("CaptchaHandler", "locationOnScreen()结果: ${textLocation?.contentToString()}")
                } catch (e: Exception) {
                   Log.record("CaptchaHandler", "locationOnScreen()失败: ${e.message}")
                }
                
                
                
                // 尝试解析bounds字符串 [x1,y1][x2,y2]
                var textLeft = 0
                var textTop = 0
                var textRight = 0
                var textBottom = 0
                
                if (textLocation != null && textLocation.size >= 2) {
                    // 使用locationOnScreen的结果获取左上角坐标
                    textLeft = textLocation[0]
                    textTop = textLocation[1]
                    
                    // 通过originView获取宽度和高度
                    try {
                        val originView = slideText.originView
                        if (originView != null) {
                            textRight = textLeft + originView.width
                            textBottom = textTop + originView.height
                           Log.record("CaptchaHandler", "使用locationOnScreen+originView尺寸获取位置")
                        } else {
                           Log.record("CaptchaHandler", "验证文字originView为null")
                        }
                    } catch (e: Exception) {
                       Log.record("CaptchaHandler", "获取验证文字originView尺寸失败: ${e.message}")
                    }
                }

            val textWidth = textRight - textLeft
                val textHeight = textBottom - textTop
                val textCenterX = textLeft + textWidth / 2
                val textCenterY = textTop + textHeight / 2

                   Log.record("CaptchaHandler", "文字位置: (${textLeft}, ${textTop}, ${textRight}, ${textBottom})")
                   Log.record("CaptchaHandler", "文字宽度: $textWidth, 高度: $textHeight")
                   Log.record("CaptchaHandler", "文字中心: ($textCenterX, $textCenterY)")
                   Log.record("CaptchaHandler", "文字类型: ${slideText.type}")
                    
                    // 获取下一个兄弟节点（滑块）
                       Log.record("CaptchaHandler", "========== 获取下一个兄弟节点 ==========")
                        try {
                            // 首先检查slideText的基本信息
                            val slideTextParent = slideText.parentNode()
                           Log.record("CaptchaHandler", "slideText父节点: $slideTextParent")
                           Log.record("CaptchaHandler", "slideText类型: ${slideText.type}")
                            
                            val nextSibling = slideText.nextSibling()
                            if (nextSibling != null) {
                               Log.record("CaptchaHandler", "找到下一个兄弟节点")
                                
                                try {
                                    val siblingType = nextSibling.type
                                   Log.record("CaptchaHandler", "兄弟节点类型: $siblingType")
                                    
                                    // 检查是否是FrameLayout
                                    if (siblingType == "android.widget.FrameLayout") {
                                       Log.record("CaptchaHandler", "*** 找到FrameLayout滑块 ***")
                                        
                                        // 获取兄弟节点的位置信息
                                        val siblingLocation = nextSibling.locationOnScreen()
                                        if (siblingLocation != null && siblingLocation.size >= 2) {
                                            try {
                                                val siblingOriginView = nextSibling.originView
                                                if (siblingOriginView != null) {
                                                    val siblingLeft = siblingLocation[0]
                                                    val siblingTop = siblingLocation[1]
                                                    val siblingWidth = siblingOriginView.width
                                                    val siblingHeight = siblingOriginView.height
                                                    val siblingRight = siblingLeft + siblingWidth
                                                    val siblingBottom = siblingTop + siblingHeight
                                                    
                                                   Log.record("CaptchaHandler", "最终滑块位置: [$siblingLeft, $siblingTop, $siblingRight, $siblingBottom]")
                                                } else {
                                                   Log.record("CaptchaHandler", "滑块originView为null")
                                                }
                                            } catch (e: Exception) {
                                               Log.record("CaptchaHandler", "处理滑块时异常: ${e.message}")
                                            }
                                        } else {
                                           Log.record("CaptchaHandler", "无法获取滑块位置信息")
                                        }
                                    } else {
                                       Log.record("CaptchaHandler", "下一个兄弟节点不是FrameLayout，继续寻找")
                                        // 如果不是FrameLayout，尝试获取下一个的下一个
                                        val nextNextSibling = nextSibling.nextSibling()
                                        if (nextNextSibling != null) {
                                            val nextNextType = nextNextSibling.type
                                           Log.record("CaptchaHandler", "下下个兄弟节点类型: $nextNextType")
                                            if (nextNextType == "android.widget.FrameLayout") {
                                               Log.record("CaptchaHandler", "*** 找到FrameLayout滑块（下下个）***")
                                                // 这里可以添加对第二个FrameLayout的处理逻辑
                                            }
                                        }
                                    }
                                } catch (e: Exception) {
                                   Log.record("CaptchaHandler", "获取兄弟节点信息时异常: ${e.message}")
                                }
                            } else {
                               Log.record("CaptchaHandler", "未找到下一个兄弟节点")
                            }
                        } catch (e: Exception) {
                           Log.record("CaptchaHandler", "获取下一个兄弟节点时异常: ${e.message}")
                            e.printStackTrace()
                        }
                
                return false
            
        } catch (e: Exception) {
           Log.record("CaptchaHandler", "滑动验证码处理异常: ${e.message}")
            false
        }
    }


}
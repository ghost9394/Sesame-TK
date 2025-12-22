package fansirsqi.xposed.sesame.newutil

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import fansirsqi.xposed.sesame.data.General
import fansirsqi.xposed.sesame.util.ToastUtil

object IconManager {
    // 请确保这两个常量对应 Manifest 中的完整路径
    // 例如: "fansirsqi.xposed.sesame.ui.MainActivityAlias"
    const val COMPONENT_DEFAULT = General.MODULE_PACKAGE_UI_ICON
    const val COMPONENT_CHRISTMAS = General.MODULE_PACKAGE_UI_ICON + "Christmas" // 或者你自己定义的字符串

    /**
     * 核心方法：根据“用户是否想隐藏”和“当前日期”来决定最终状态
     * @param context 上下文
     * @param userWantsHide 用户是否勾选了“隐藏图标”
     */
    fun syncIconState(context: Context, userWantsHide: Boolean) {
        val pm = context.packageManager

        // 1. 如果用户选择隐藏，直接禁用所有图标
        if (userWantsHide) {
            disableComponent(context, pm, COMPONENT_DEFAULT)
            disableComponent(context, pm, COMPONENT_CHRISTMAS)
            return
        }

        // 2. 如果用户选择显示，再判断日期
        if (isChristmasTime()) {
            // 圣诞节：启用圣诞版，禁用默认版
            enableComponent(context, pm, COMPONENT_CHRISTMAS)
            disableComponent(context, pm, COMPONENT_DEFAULT)
            val emoji = listOf("🎅", "🎄", "🎁", "✨", "❄️")
            val randomEmoji = emoji.random()
            ToastUtil.showToast(context, "$randomEmoji 圣诞快乐!")
        } else {
            // 平时：启用默认版，禁用圣诞版
            enableComponent(context, pm, COMPONENT_DEFAULT)
            disableComponent(context, pm, COMPONENT_CHRISTMAS)
        }
    }

    private fun isChristmasTime(): Boolean {
        val calendar = java.util.Calendar.getInstance()
        val month = calendar.get(java.util.Calendar.MONTH) + 1
        val day = calendar.get(java.util.Calendar.DAY_OF_MONTH)
        // 12月24日 - 12月26日
        return month == 12 && (day in 22..31)
    }

    private fun enableComponent(context: Context, pm: PackageManager, className: String) {
        val componentName = ComponentName(context, className)
        if (pm.getComponentEnabledSetting(componentName) != PackageManager.COMPONENT_ENABLED_STATE_ENABLED) {
            pm.setComponentEnabledSetting(
                componentName,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP
            )
        }
    }

    private fun disableComponent(context: Context, pm: PackageManager, className: String) {
        val componentName = ComponentName(context, className)
        if (pm.getComponentEnabledSetting(componentName) != PackageManager.COMPONENT_ENABLED_STATE_DISABLED) {
            pm.setComponentEnabledSetting(
                componentName,
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP
            )
        }
    }
}
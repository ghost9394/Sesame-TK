package fansirsqi.xposed.sesame.task.antForest

import fansirsqi.xposed.sesame.data.Status
import fansirsqi.xposed.sesame.task.antFarm.TaskStatus
import fansirsqi.xposed.sesame.util.GlobalThreadPools.sleepCompat
import fansirsqi.xposed.sesame.util.Log
import fansirsqi.xposed.sesame.util.ResChecker.checkRes
import fansirsqi.xposed.sesame.util.maps.UserMap
import org.json.JSONObject
import java.util.Locale.getDefault
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * 森林抽抽乐任务处理类（每天自动执行，完成后标记）
 *
 * 核心流程：
 * 1. 检查活动有效期
 * 2. 循环处理任务（执行→领取）
 * 3. 执行抽奖
 * 4. 检查完成度并标记
 *
 * 变量说明（Scene 对象）：
 * - s.id = activityId （活动ID，如 "2025101301"）
 * - s.code = sceneCode （场景代码，如 "ANTFOREST_NORMAL_DRAW"）
 * - s.name = sceneName （场景名称，如 "森林抽抽乐普通版"）
 * - s.flag = flagKey （完成标记Key，如 "forest::chouChouLe::normal::completed"）
 * - s.taskCode = "${s.code}_TASK" （任务场景代码，如 "ANTFOREST_NORMAL_DRAW_TASK"）
 */
class ForestChouChouLe {

    companion object {
        private const val TAG = "ForestChouChouLe"
        private const val SOURCE = "task_entry"

        // 屏蔽的任务类型（邀请好友类任务不执行）
        private val BLOCKED_TYPES = setOf("FOREST_NORMAL_DRAW_SHARE",
            "FOREST_ACTIVITY_DRAW_SHARE",
            "FOREST_ACTIVITY_DRAW_XS") //玩游戏得新机会
        private val BLOCKED_NAMES = setOf("玩游戏得", "开宝箱") // 屏蔽的任务名称关键词

        /**
         * 抽奖场景数据类
         * @param id 活动ID（用于RPC调用）
         * @param code 场景代码（用于RPC调用）
         * @param name 场景名称（用于日志显示）
         * @param flag 完成标记Key（用于Status记录）
         */
        private data class Scene(val id: String, val code: String, val name: String, val flag: String) {
            val taskCode get() = "${code}_TASK"  // 任务场景代码
        }

        // 动态获取抽奖场景配置
        private fun getScenes(): List<Scene> {
            return runCatching {
                val scenes = mutableListOf<Scene>()
                // 使用任意场景代码查询可用的抽奖活动
                val response = JSONObject(AntForestRpcCall.enterDrawActivityopengreen("", "ANTFOREST_NORMAL_DRAW", SOURCE))
                if (response.optBoolean("success", false)) {
                    val drawSceneGroups = response.getJSONArray("drawSceneGroups")
                    for (i in 0 until drawSceneGroups.length()) {
                        val sceneGroup = drawSceneGroups.getJSONObject(i)
                        val drawActivity = sceneGroup.getJSONObject("drawActivity")
                        val activityId = drawActivity.getString("activityId")
                        val sceneCode = drawActivity.getString("sceneCode")
                        val name = sceneGroup.getString("name")
                        val flag = when (sceneCode) {
                            "ANTFOREST_NORMAL_DRAW" -> "forest::chouChouLe::normal::completed"
                            "ANTFOREST_ACTIVITY_DRAW" -> "forest::chouChouLe::activity::completed"
                            else -> "forest::chouChouLe::${sceneCode.lowercase(getDefault())}::completed"
                        }
                        scenes.add(Scene(activityId, sceneCode, name, flag))
                    }
                }
                scenes
            }.getOrElse {
                Log.printStackTrace(TAG, "获取抽奖场景配置失败，使用默认配置", it)
                // 失败时返回默认配置
                listOf(
                    Scene("2025112701", "ANTFOREST_NORMAL_DRAW", "森林抽抽乐普通版", "forest::chouChouLe::normal::completed"),
                    Scene("20251024", "ANTFOREST_ACTIVITY_DRAW", "森林抽抽乐活动版", "forest::chouChouLe::activity::completed")
                )
            }
        }
    }

    private val taskTryCount = ConcurrentHashMap<String, AtomicInteger>()

    fun chouChouLe() {
        runCatching {
            val scenes = getScenes()
            if (scenes.all { Status.hasFlagToday(it.flag) }) {
                Log.record("⏭️ 今天所有森林抽抽乐任务已完成，跳过执行")
                return
            }
            Log.record("开始处理森林抽抽乐")
            scenes.forEach { processScene(it); sleepCompat(3000L) }
        }.onFailure { Log.printStackTrace(TAG, "执行异常", it) }
    }

    /**
     * 处理单个抽奖场景
     * @param s 场景对象 (s.id=活动ID, s.code=场景代码, s.name=场景名称, s.flag=完成标记)
     */
    private fun processScene(s: Scene) = runCatching {
        // 检查今天是否已完成
        if (Status.hasFlagToday(s.flag)) {
            Log.record("⏭️ ${s.name} 今天已完成，跳过")
            return@runCatching
        }

        Log.record("开始处理：${s.name} (ActivityId: ${s.id}, SceneCode: ${s.code})")

        // 1. 检查活动有效期
        JSONObject(AntForestRpcCall.enterDrawActivityopengreen(s.id, s.code, SOURCE)).let { resp ->
            if (!checkRes(TAG, resp)) return@runCatching
            val now = System.currentTimeMillis()
            resp.getJSONObject("drawActivity").let { act ->
                if (now !in act.getLong("startTime")..act.getLong("endTime")) {
                    Log.record("${s.name} 活动不在有效期内，跳过")
                    return@runCatching
                }
            }
        }

        // 2. 处理任务（最多循环3次）
        repeat(3) { loop ->
            Log.record("${s.name} 第 ${loop + 1} 轮任务处理开始")
            // 获取任务列表（s.taskCode = 场景任务代码，如 "ANTFOREST_NORMAL_DRAW_TASK"）
            val tasks = JSONObject(AntForestRpcCall.listTaskopengreen(s.taskCode, SOURCE))
            if (!checkRes(TAG, tasks)) return@repeat

            val taskList = tasks.getJSONArray("taskInfoList")
            Log.record("${s.name} 发现 ${taskList.length()} 个任务")
            var hasChange = false  // 是否有任务状态变化

            // 处理每个任务
            for (i in 0 until taskList.length()) {
                if (processTask(s, taskList.getJSONObject(i))) hasChange = true
            }

            // 如果没有任务变化或已是最后一轮，退出
            if (!hasChange || loop >= 2) return@repeat
            Log.record("${s.name} 等待3秒后继续下一轮检查")
            sleepCompat(3000L)
        }

        // 3. 抽奖（s.id=活动ID, s.code=场景代码）
        JSONObject(AntForestRpcCall.enterDrawActivityopengreen(s.id, s.code, SOURCE)).takeIf { checkRes(TAG, it) }?.let { resp ->
            var balance = resp.getJSONObject("drawAsset").optInt("blance", 0)  // 剩余抽奖次数
            Log.record("${s.name} 剩余抽奖次数：$balance/${resp.getJSONObject("drawAsset").optInt("totalTimes", 0)}")

            repeat(50) {
                if (balance <= 0) return@repeat
                Log.record("${s.name} 第 ${it + 1} 次抽奖")
                JSONObject(AntForestRpcCall.drawopengreen(s.id, s.code, SOURCE, UserMap.currentUid)).let { draw ->
                    if (!checkRes(TAG, draw)) return@repeat
                    balance = draw.getJSONObject("drawAsset").getInt("blance")
                    val prize = draw.getJSONObject("prizeVO")
                    Log.forest("${s.name}🎁[领取: ${prize.getString("prizeName")}*${prize.getInt("prizeNum")}] 剩余次数: $balance")
                    if (balance > 0) sleepCompat(2000L)
                }
            }
        }

        // 4. 检查完成度并标记（s.taskCode=任务场景代码, s.flag=完成标记Key）
        Log.record("${s.name} 检查所有任务完成状态")
        JSONObject(AntForestRpcCall.listTaskopengreen(s.taskCode, SOURCE)).takeIf { checkRes(TAG, it) }?.let { resp ->
            val taskList = resp.getJSONArray("taskInfoList")
            var total = 0       // 总任务数（不含屏蔽任务）
            var completed = 0   // 已完成任务数
            var allDone = true  // 是否全部完成

            for (i in 0 until taskList.length()) {
                val task = taskList.getJSONObject(i)
                val baseInfo = task.getJSONObject("taskBaseInfo")
                val taskType = baseInfo.getString("taskType")
                val taskStatus = baseInfo.getString("taskStatus")
                val bizInfo = JSONObject(baseInfo.getString("bizInfo"))
                val taskName = bizInfo.optString("title", taskType)

                // 跳过屏蔽任务（类型和名称都检查）
                if (BLOCKED_TYPES.any { it in taskType } || BLOCKED_NAMES.any { it in taskName }) continue

                total++

                // 判断任务是否完成：状态为 RECEIVED（已领取奖励）
                if (taskStatus == TaskStatus.RECEIVED.name) {
                    completed++
                } else {
                    allDone = false
                    val btnText = bizInfo.optString("completeBtnText", "")
                    Log.record("${s.name} 未完成任务: $taskName [状态: $taskStatus, 按钮: $btnText]")
                }
            }

            Log.record("${s.name} 任务完成度: $completed/$total")
            if (allDone) {
                // 所有任务已完成，标记今天已处理（使用 s.flag）
                Status.setFlagToday(s.flag)
                Log.record("✅ ${s.name} 所有任务已完成，今天不再处理")
            } else {
                Log.record("⚠️ ${s.name} 还有未完成任务，下次运行时会继续处理")
            }
        }
    }.onFailure { Log.printStackTrace(TAG, "${s.name} 处理异常", it) }

    /**
     * 处理单个任务
     * @param s 场景对象（包含活动ID、场景代码等信息）
     * @param task 任务JSON对象
     * @return 是否有任务状态变化
     */
    private fun processTask(s: Scene, task: JSONObject): Boolean {
        val baseInfo = task.getJSONObject("taskBaseInfo")
        val bizInfo = JSONObject(baseInfo.getString("bizInfo"))
        val taskName = bizInfo.getString("title")           // 任务名称
        val taskCode = baseInfo.getString("sceneCode")      // 任务场景代码
        val taskStatus = baseInfo.getString("taskStatus")   // 任务状态：TODO/FINISHED/RECEIVED
        val taskType = baseInfo.getString("taskType")       // 任务类型

        val rights = task.getJSONObject("taskRights")
        val current = rights.getInt("rightsTimes")      // 当前完成次数
        val limit = rights.getInt("rightsTimesLimit")   // 最大可完成次数

        Log.record("${s.name} 任务: $taskName [$taskType] 状态: $taskStatus 进度: $current/$limit")

        // 跳过屏蔽任务（邀请好友类）
        if (BLOCKED_TYPES.any { it in taskType } || BLOCKED_NAMES.any { it in taskName }){
            Log.record("${s.name} 已屏蔽任务，跳过：$taskName (类型: $taskType)")
            return false
        }

        return when {
            // 活力值兑换任务（使用 s.id=活动ID, s.code=场景代码）
            taskType == "NORMAL_DRAW_EXCHANGE_VITALITY" && taskStatus == TaskStatus.TODO.name -> {
                Log.record("${s.name} 处理活力值兑换任务：$taskName")
                val result = AntForestRpcCall.exchangeTimesFromTaskopengreen(s.id, s.code, SOURCE, taskCode, taskType)
                checkRes(TAG, result).also {
                    if (it) Log.forest("${s.name}🧾：$taskName 兑换成功")
                    else Log.error(TAG, "${s.name} 活力值兑换失败: $taskName")
                }
            }

            // 待执行任务
            (taskType.startsWith("FOREST_NORMAL_DRAW") || taskType.startsWith("FOREST_ACTIVITY_DRAW"))
                    && taskStatus == TaskStatus.TODO.name -> {
                Log.record("${s.name} 执行任务延时30S模拟：$taskName")
                sleepCompat(30000L)
                val result = if ("XLIGHT" in taskType)
                    AntForestRpcCall.finishTask4Chouchoule(taskType, taskCode)
                else
                    AntForestRpcCall.finishTaskopengreen(taskType, taskCode)

                checkRes(TAG, result).also {
                    if (it) {
                        Log.forest("${s.name}🧾：$taskName 完成成功")
                    } else {
                        Log.error(TAG, "${s.name} 任务完成失败: $taskName")
                        val tryCount = taskTryCount.computeIfAbsent(taskType) { AtomicInteger(0) }.incrementAndGet()
                        if (tryCount > 3) Log.record("${s.name} 任务 $taskName 多次失败，建议检查")
                    }
                }
            }

            // 领取奖励
            taskStatus == TaskStatus.FINISHED.name -> {
                Log.record("${s.name} 领取奖励延时3S:$taskName")
                sleepCompat(3000L)
                val result = AntForestRpcCall.receiveTaskAwardopengreen(SOURCE, taskCode, taskType)
                checkRes(TAG, result).also {
                    if (it) {
                        Log.forest("${s.name}🧾：$taskName 奖励领取成功")
                    } else {
                        Log.error(TAG, "${s.name} 奖励领取失败: $taskName")
                    }
                } && limit - current > 0
            }

            else -> false
        }
    }
}
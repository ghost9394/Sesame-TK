package fansirsqi.xposed.sesame.task.antForest

import android.annotation.SuppressLint
import fansirsqi.xposed.sesame.util.Log
import fansirsqi.xposed.sesame.util.ResChecker
import kotlinx.coroutines.*
import org.json.JSONObject
import java.util.concurrent.atomic.AtomicLong

/**
 * 6秒拼手速打地鼠（随机间隔防限流版）
 *
 * 核心优化策略：
 * 1. 串行启动：避免并发请求触发服务器限流
 * 2. 随机间隔：启动间隔1000-2000ms随机，击打间隔50-60ms随机，模拟真人操作
 * 3. 智能等待：凑满6秒总时长，确保游戏逻辑完整
 * 4. 限流检测：检测到userBaseInfo=null时增加500ms退避时间
 * 5. 动态模式：支持击打模式（MAX_HITS_PER_GAME>0）和直接结算模式（=0）
 *
 * @author Ghostxx (优化版)
 */
object WhackMole {
    private const val TAG = "WhackMole"
    // ========== 核心配置 ==========
    /** 一次性启动的游戏局数：5局 */
    private const val TOTAL_GAMES = 5
    /** 游戏总时长（毫秒）：严格等待10秒，让所有局完成 */
    private const val GAME_DURATION_MS = 10000L
    /** 全局协程作用域：用于管理所有协程，SupervisorJob确保子协程失败不影响其他 */
    private val globalScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** 记录启动开始时间：用于精确计算已用时长，凑满6秒 */
    private val startTime = AtomicLong(0)

    // ========== 数据类 ==========
    /**
     * 游戏会话：存储单局游戏的关键信息（新规则：只存储token）
     * @param token 本局游戏的唯一凭证
     * @param roundNumber 局号（用于日志显示）
     */
    data class GameSession(
        val token: String,
        val roundNumber: Int
    )

    // ========== 自动入口 ==========
    /**
     * 启动打地鼠游戏的主入口
     * 1. 从配置读取击打次数
     * 2. 串行启动所有局（带随机间隔）
     * 3. 等待凑满6秒
     * 4. 按能量从高到低依次结算
     */
    @SuppressLint("DefaultLocale")
    fun startWhackMole() {
        // 记录新规则到日志
        Log.other(TAG, "打地鼠启动 ${TOTAL_GAMES}局 新规则：只启动存储token，10秒后串行结算")

        // 在IO协程中执行，避免阻塞主线程
        globalScope.launch {
            try {
                // 记录启动时间戳
                startTime.set(System.currentTimeMillis())
                // 串行启动每局游戏（避免并发限流）
                val sessions = mutableListOf<GameSession>()
                for (roundNum in 1..TOTAL_GAMES) {
                    val session = startSingleRound(roundNum)
                    if (session != null) {
                        sessions.add(session)
                    }
                    // 随机间隔1000-2000ms，避免并发请求触发服务器限流
                    if (roundNum < TOTAL_GAMES) {
                        val delayMs = (1000..2000).random()
                        delay(delayMs.toLong())
                    }
                }


                // 计算剩余等待时间，凑满10秒
                val elapsedTime = System.currentTimeMillis() - startTime.get()
                val remainingTime = GAME_DURATION_MS - elapsedTime
                if (remainingTime > 0) {
                    Log.other(TAG, "已启动${sessions.size}局，等待${remainingTime}ms凑满10秒...")
                    delay(remainingTime)
                } else {
                    Log.other(TAG, "已启动${sessions.size}局，已超过10秒，立即结算")
                }

                // 串行结算所有游戏局
                var totalEnergy = 0
                sessions.forEach { session ->
                    totalEnergy += settleBestRound(session)
                }

                // 最终日志：显示成功局数和总能量
                Log.forest("森林能量⚡️[打地鼠${sessions.size}局串行结算 总计${totalEnergy}g]")

            } catch (_: CancellationException) {
                Log.other(TAG, "打地鼠协程被取消")
            } catch (e: Exception) {
                Log.other(TAG, "打地鼠异常: ${e.message}")
            }
        }
    }

    // ========== 单局游戏 ==========
    /**
     * 启动单局游戏（新规则：只启动不击打）
     * 1. 调用startWhackMole获取token
     * 2. 检测userBaseInfo判断服务器是否限流
     * 3. 直接返回token，不进行击打
     */
    private suspend fun startSingleRound(round: Int): GameSession? = withContext(Dispatchers.IO) {
        try {
            // 调用无参的startWhackMole()（source已内联）
            val startResp = JSONObject(AntForestRpcCall.startWhackMole())
            if (!ResChecker.checkRes("$TAG 启动失败:", startResp)) {
                return@withContext null
            }
            // 限流检测：userBaseInfo为null说明服务器限制新开游戏
            val userBaseInfo = startResp.optJSONObject("userBaseInfo")
            if (userBaseInfo == null) {
                Log.other(TAG, "服务器限流：userBaseInfo=null，第${round}局失败")
                delay(500L) // 退避500ms后重试
                return@withContext null
            }
            val token = startResp.optString("token")
            Log.other(TAG, "第${round}局启动成功，token=$token")
            // 新规则：直接返回token，不进行击打
            GameSession(token, round)
        } catch (e: CancellationException) {
            // 协程取消异常需要重新抛出，父协程会处理
            throw e
        } catch (e: Exception) {
            Log.other(TAG, "第${round}局异常: ${e.message}")
            null
        }
    }



    // ========== 结算 ==========
    /**
     * 结算单局游戏
     * 调用单参的settlementWhackMole()，获取最终能量
     * @return 获得的能量数
     */
    private suspend fun settleBestRound(session: GameSession): Int = withContext(Dispatchers.IO) {
        try {
            // 调用单参的settlementWhackMole()（其他参数已内联）
            val resp = JSONObject(AntForestRpcCall.settlementWhackMole(session.token))

            if (ResChecker.checkRes(TAG, resp)) {
                val total = resp.optInt("totalEnergy", 0)
                val provide = resp.optInt("provideDefaultEnergy", 0)
                Log.forest(
                    "森林能量⚡️[第${session.roundNumber}局结算 " +
                            "默认${provide}g 总计${total}g]"
                )
                return@withContext total
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.other(TAG, "结算异常: ${e.message}")
        }
        return@withContext 0
    }
}

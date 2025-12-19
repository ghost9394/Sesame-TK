package fansirsqi.xposed.sesame.task.antForest

import android.annotation.SuppressLint
import fansirsqi.xposed.sesame.util.Log
import fansirsqi.xposed.sesame.util.ResChecker
import kotlinx.coroutines.*
import org.json.JSONObject
import java.util.concurrent.atomic.AtomicInteger

/**
 * 6秒拼手速打地鼠（Kotlin优化版）
 * 
 * 主要优化点：
 * 1. 使用Kotlin协程替代线程池，提高并发性能和资源利用率
 * 2. 使用data class替代record，提供更多功能
 * 3. 使用Kotlin的集合操作和扩展函数简化代码
 * 4. 使用Kotlin的字符串模板简化日志输出
 * 5. 使用scope函数和let/run等提高代码可读性
 * 6. 使用更安全的空值处理
 * 
 * @author Ghostxx (优化版)
 */
object WhackMole {
    private const val TAG = "WhackMole"
    private const val SOURCE = "senlinguangchangdadishu"
    
    // ========== 核心配置 ==========
    /** 一次性启动的游戏局数：5局并发 */
    private const val TOTAL_GAMES = 5
    
    /** 游戏总时长（毫秒）：严格等待6秒，让所有局完成 */
    private const val GAME_DURATION_MS = 6000L
    
    /** 每局最多击打次数：3次（设置为0时直接结算，不进行击打） */
    private var MAX_HITS_PER_GAME = 3
    
    // ========== 统计 ==========
    /** 累计获得能量：所有被结算的局 */
    private val totalEnergyEarned = AtomicInteger(0)
    
    /** 全局协程作用域，用于管理所有协程 */
    private val globalScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    // ========== 数据类 ==========
    /**
     * 游戏会话：存储单局游戏的token、剩余ID、能量和局号
     * 使用data class替代record，提供更多功能如copy、equals等
     */
    data class GameSession(
        val token: String,
        val remainingIds: List<String>,
        val whackedEnergy: Int,
        val roundNumber: Int
    )
    
    // ========== 自动入口 ==========
    /**
     * 启动打地鼠游戏
     * 使用Kotlin协程作用域和协程构建器，更高效地管理并发任务
     */
    @SuppressLint("DefaultLocale")
    fun startWhackMole() {
        // 从AntForest获取自定义击打次数
        MAX_HITS_PER_GAME = AntForest.whackMoleHits?.value ?: 3
        Log.other(TAG, "纯净版打地鼠启动 一次性启动${TOTAL_GAMES}局 每局击打${MAX_HITS_PER_GAME}次")
        // 使用全局协程作用域，确保协程不会被意外取消
        globalScope.launch {
            try {
                // 1. 使用async并发启动所有局
                val deferredSessions = (1..TOTAL_GAMES).map { roundNum ->
                    async { startSingleRound(roundNum) }
                }
                Log.other(TAG, "已启动${TOTAL_GAMES}局游戏，等待6秒...")
                // 2. 使用delay替代Thread.sleep，非阻塞等待
                delay(GAME_DURATION_MS)
                // 3. 使用awaitAll收集所有结果
                val sessions = deferredSessions.awaitAll().filterNotNull()
                if (sessions.isEmpty()) {
                    Log.other(TAG, "所有局都失败了！")
                    return@launch
                }
                // 4. 使用Kotlin的sortedByDescending函数按能量从高到低排序
                val sortedSessions = sessions.sortedByDescending { it.whackedEnergy }
                
                // 5. 依次结算所有局（从最高到最低）
                sortedSessions.forEachIndexed { index, session ->
                    settleBestRound(session)
                    totalEnergyEarned.addAndGet(session.whackedEnergy)
                    
                    // 小间隔，避免结算请求被限流
                    if (index < sortedSessions.size - 1) {
                        delay(100)
                    }
                }
                
                Log.forest("森林能量⚡️[6秒完成${TOTAL_GAMES}局 总计${totalEnergyEarned.get()}g]")
                
            } catch (_: CancellationException) {
                // 协程取消异常，不需要处理日志
                Log.other(TAG, "打地鼠协程被取消")
            } catch (e: Exception) {
                Log.other(TAG, "打地鼠过程中发生异常: ${e.message}")
            }
        }
    }
    
    // ========== 单局游戏 ==========
    /**
     * 启动单局游戏
     * 使用Kotlin的空值安全操作符和扩展函数简化代码
     */
    private suspend fun startSingleRound(round: Int): GameSession? = withContext(Dispatchers.IO) {
        try {
            val startResp = JSONObject(AntForestRpcCall.startWhackMole(SOURCE))
            if (!ResChecker.checkRes("$TAG 启动失败:", startResp)) {
                return@withContext null
            }
            
            // 检测：如果用户基础信息缺失，说明服务器限制新开
            val userBaseInfo = startResp.optJSONObject("userBaseInfo")
            if (userBaseInfo == null) {
                Log.other(TAG, "服务器限制：无法新开游戏，userBaseInfo=null")
                return@withContext null
            }
            
            val token = startResp.optString("token")
            val moleArray = startResp.optJSONArray("moleArray")
                ?: return@withContext null
            
            var totalEnergy = 0
            var hitCount = 0
            
            // 如果MAX_HITS_PER_GAME为0，则直接结算不进行击打
            if (MAX_HITS_PER_GAME == 0) {
                Log.other(TAG, "第${round}局设置为直接结算模式，跳过击打")
            } else {
                // 使用Kotlin的take和map函数提取目标ID
                val targetIds = (0 until moleArray.length())
                    .asSequence()
                    .map { moleArray.getJSONObject(it) }
                    .filter { it.has("bubbleId") }
                    .take(MAX_HITS_PER_GAME)
                    .map { it.getLong("id") }
                    .toList()
                
                // 击打地鼠
                for (moleId in targetIds) {
                    if (hitCount >= MAX_HITS_PER_GAME) break
                    val energy = whackMoleSync(moleId, token)
                    if (energy > 0) {
                        totalEnergy += energy
                        hitCount++
                        Log.other(TAG, "第${round}局 第${hitCount}击 energy=$energy")
                    }
                }
                
                Log.other(TAG, "第${round}局完成 击打${hitCount}次 获得${totalEnergy}g")
            }
            // 使用Kotlin的map函数构建剩余ID列表
            val remainingIds = (0 until moleArray.length())
                .map { moleArray.getJSONObject(it).getString("id") }
            GameSession(token, remainingIds, totalEnergy, round)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.other(TAG, "第${round}局异常: ${e.message}")
            null
        }
    }
    
    // ========== 同步击打 ==========
    /**
     * 同步击打地鼠
     * 使用Kotlin的简洁语法和空值安全操作符
     */
    private suspend fun whackMoleSync(moleId: Long, token: String): Int = withContext(Dispatchers.IO) {
        try {
            val resp = JSONObject(AntForestRpcCall.whackMole(moleId, token, SOURCE))
            if (resp.optBoolean("success")) resp.optInt("energyAmount", 0) else 0
        } catch (e: CancellationException) {
            // 协程取消异常，重新抛出
            throw e
        } catch (_: Exception) {
            0
        }
    }
    
    // ========== 结算 ==========
    /**
     * 结算最佳回合
     * 使用Kotlin的字符串模板简化输出
     */
    private suspend fun settleBestRound(session: GameSession) = withContext(Dispatchers.IO) {
        try {
            val resp = JSONObject(
                AntForestRpcCall.settlementWhackMole(session.token, session.remainingIds, SOURCE)
            )
            
            if (ResChecker.checkRes(TAG, resp)) {
                val total = resp.optInt("totalEnergy", 0)
                val provide = resp.optInt("provideDefaultEnergy", 0)
                Log.forest(
                    "森林能量⚡️[第${session.roundNumber}局结算 " +
                    "地鼠${total - provide}g 默认${provide}g 总计${total}g]"
                )
            }
        } catch (e: CancellationException) {
            // 协程取消异常，重新抛出
            throw e
        } catch (e: Exception) {
            Log.other(TAG, "结算异常: ${e.message}")
        }
    }
}
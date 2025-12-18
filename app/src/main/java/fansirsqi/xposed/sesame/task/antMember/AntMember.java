package fansirsqi.xposed.sesame.task.antMember;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import fansirsqi.xposed.sesame.data.StatusFlags;
import fansirsqi.xposed.sesame.entity.MemberBenefit;
import fansirsqi.xposed.sesame.hook.SecurityBodyHelper;
import fansirsqi.xposed.sesame.model.BaseModel;
import fansirsqi.xposed.sesame.model.ModelFields;
import fansirsqi.xposed.sesame.model.ModelGroup;
import fansirsqi.xposed.sesame.model.modelFieldExt.BooleanModelField;
import fansirsqi.xposed.sesame.model.modelFieldExt.SelectModelField;
import fansirsqi.xposed.sesame.task.ModelTask;
import fansirsqi.xposed.sesame.task.TaskCommon;
import fansirsqi.xposed.sesame.task.antOrchard.AntOrchardRpcCall;
import fansirsqi.xposed.sesame.util.GlobalThreadPools;
import fansirsqi.xposed.sesame.util.Log;
import fansirsqi.xposed.sesame.util.maps.IdMapManager;
import fansirsqi.xposed.sesame.util.maps.MemberBenefitsMap;
import fansirsqi.xposed.sesame.util.maps.UserMap;
import fansirsqi.xposed.sesame.data.Status;
import fansirsqi.xposed.sesame.util.ResChecker;
import fansirsqi.xposed.sesame.util.TimeUtil;

public class AntMember extends ModelTask {
  private static final String TAG = AntMember.class.getSimpleName();
  @Override
  public String getName() {
    return "会员";
  }
  @Override
  public ModelGroup getGroup() {
    return ModelGroup.MEMBER;
  }
  @Override
  public String getIcon() {
    return "AntMember.png";
  }
  private BooleanModelField memberSign;
  private BooleanModelField memberTask;
  private BooleanModelField memberPointExchangeBenefit;
  private SelectModelField memberPointExchangeBenefitList;
  private BooleanModelField collectSesame;
  private BooleanModelField collectSesameWithOneClick;
  private BooleanModelField sesameTask;
  private BooleanModelField collectInsuredGold;
  private BooleanModelField enableGameCenter;
  private BooleanModelField merchantSign;
  private BooleanModelField merchantKmdk;
  private BooleanModelField merchantMoreTask;
  private BooleanModelField beanSignIn;
  private BooleanModelField beanExchangeBubbleBoost;
  // 芝麻炼金
  private BooleanModelField sesameAlchemy;
  // 芝麻树
  private BooleanModelField enableZhimaTree;
  //年度回顾
  private BooleanModelField AnnualReview;
  // 黄金票配置 - 签到
  private BooleanModelField enableGoldTicket;
  // 黄金票配置 - 提取/兑换
  private BooleanModelField enableGoldTicketConsume;

  @Override
  public ModelFields getFields() {
    ModelFields modelFields = new ModelFields();
    modelFields.addField(memberSign = new BooleanModelField("memberSign", "会员签到", false));
    modelFields.addField(memberTask = new BooleanModelField("memberTask", "会员任务", false));
    modelFields.addField(memberPointExchangeBenefit = new BooleanModelField("memberPointExchangeBenefit", "会员积分 | 兑换权益", false));
    modelFields.addField(memberPointExchangeBenefitList = new SelectModelField("memberPointExchangeBenefitList", "会员积分 | 权益列表", new LinkedHashSet<>(), MemberBenefit.Companion.getList()));
    modelFields.addField(sesameTask = new BooleanModelField("sesameTask", "芝麻信用|芝麻粒信用任务", false));
    modelFields.addField(collectSesame = new BooleanModelField("collectSesame", "芝麻信用|芝麻粒领取", false));
    modelFields.addField(collectSesameWithOneClick = new BooleanModelField("collectSesameWithOneClick", "芝麻信用|芝麻粒领取使用一键收取", false));
    // 芝麻炼金
    modelFields.addField(sesameAlchemy = new BooleanModelField("sesameAlchemy", "芝麻炼金", false));
    // 芝麻树
    modelFields.addField(enableZhimaTree = new BooleanModelField("enableZhimaTree", "芝麻信用|芝麻树", false));
    modelFields.addField(collectInsuredGold = new BooleanModelField("collectInsuredGold", "蚂蚁保|保障金领取", false));
    // 黄金票配置
    modelFields.addField(enableGoldTicket = new BooleanModelField("enableGoldTicket", "黄金票签到", false));
    modelFields.addField(enableGoldTicketConsume = new BooleanModelField("enableGoldTicketConsume", "黄金票提取(兑换黄金)", false));
    modelFields.addField(enableGameCenter = new BooleanModelField("enableGameCenter", "游戏中心签到", false));
    modelFields.addField(merchantSign = new BooleanModelField("merchantSign", "商家服务|签到", false));
    modelFields.addField(merchantKmdk = new BooleanModelField("merchantKmdk", "商家服务|开门打卡", false));
    modelFields.addField(merchantMoreTask = new BooleanModelField("merchantMoreTask", "商家服务|积分任务", false));
    modelFields.addField(beanSignIn = new BooleanModelField("beanSignIn", "安心豆签到", false));
    modelFields.addField(beanExchangeBubbleBoost = new BooleanModelField("beanExchangeBubbleBoost", "安心豆兑换时光加速器", false));
    modelFields.addField(AnnualReview = new BooleanModelField("AnnualReview", "年度回顾", false));

    return modelFields;
  }

  @Override
  public Boolean check() {
    if (TaskCommon.IS_ENERGY_TIME){
      Log.record(TAG,"⏸ 当前为只收能量时间【"+ BaseModel.Companion.getEnergyTime().getValue() +"】，停止执行" + getName() + "任务！");
      return false;
    }else if (TaskCommon.IS_MODULE_SLEEP_TIME) {
      Log.record(TAG,"💤 模块休眠时间【"+ BaseModel.Companion.getModelSleepTime().getValue() +"】停止执行" + getName() + "任务！");
      return false;
    } else {
      return true;
    }
  }

  @Override
  protected void runJava() {
    try {
      Log.record(TAG,"执行开始-" + getName());
      if (memberSign.getValue()) {
        doMemberSign();
      }
      if (memberTask.getValue()) {
        doAllMemberAvailableTask();

      }
      if (memberPointExchangeBenefit.getValue()) {
        memberPointExchangeBenefit();
      }
      // 芝麻信用相关检测
      boolean isSesameOpened = checkSesameCanRun();

      if ((sesameTask.getValue() || collectSesame.getValue()) && isSesameOpened) {
        // 芝麻粒福利签到
        doSesameZmlCheckIn();
        if (Status.hasFlagToday(StatusFlags.FLAG_AntMember_doAllAvailableSesameTask)) {
          Log.record(TAG, "⏭️ 今天已完成过芝麻信用任务，跳过执行");
        } else {
          // 芝麻信用任务（今日首次）
          Log.record(TAG, "🎮 开始执行芝麻信用任务（今日首次）");
          doAllAvailableSesameTask();
          handleGrowthGuideTasks();
          queryAndCollect();//做完任务领取球
          Log.record(TAG, "✅ 芝麻信用任务已完成，今天不再执行");
        }
        if (collectSesame.getValue()) {
          collectSesame(collectSesameWithOneClick.getValue());
        }
      }
      if (collectInsuredGold.getValue()) {
        collectInsuredGold();
      }
      // 【更新】执行黄金票任务，替换旧的 goldTicket()
      if (enableGoldTicket.getValue() || enableGoldTicketConsume.getValue()) {
        // 传入签到和提取的开关值
        doGoldTicketTask(enableGoldTicket.getValue(), enableGoldTicketConsume.getValue());
      }
      if (enableGameCenter.getValue()) {
        enableGameCenter();
      }
      if (beanSignIn.getValue()) {
        beanSignIn();
      }
      if (AnnualReview.getValue()) {
        doAnnualReview();
      }
      if (beanExchangeBubbleBoost.getValue()) {
        beanExchangeBubbleBoost();
      }
      // 芝麻炼金
      if (sesameAlchemy.getValue() && isSesameOpened) {
        doSesameAlchemy();
        // ===== 次日奖励：只有今天还没领过才执行 =====
        if (!Status.hasFlagToday(StatusFlags.FLAG_ZMXY_ALCHEMY_NEXT_DAY_AWARD)) {
          doSesameAlchemyNextDayAward();

        }else Log.record(TAG, "✅ 芝麻粒次日奖励已领取，今天不再执行");
      }
      // 芝麻树
      if (enableZhimaTree.getValue() && isSesameOpened) {
        doZhimaTree();
      }

      if (merchantSign.getValue() || merchantKmdk.getValue() || merchantMoreTask.getValue()) {
        JSONObject jo = new JSONObject(AntMemberRpcCall.transcodeCheck());
        if (!ResChecker.checkRes(TAG, jo)) {
          return;
        }
        JSONObject data = jo.getJSONObject("data");
        if (!data.optBoolean("isOpened")) {
          Log.record(TAG,"商家服务👪未开通");
          return;
        }
        if (merchantKmdk.getValue()) {
          if (TimeUtil.isNowAfterTimeStr("0600") && TimeUtil.isNowBeforeTimeStr("1200")) {
            kmdkSignIn();
          }
          kmdkSignUp();
        }
        if (merchantSign.getValue()) {
          doMerchantSign();
        }
        if (merchantMoreTask.getValue()) {
          doMerchantMoreTask();
        }
      }
    } catch (Throwable t) {
      Log.printStackTrace(TAG, t);
    }finally {
      Log.record(TAG,"执行结束-" + getName());
    }
  }

  private void handleGrowthGuideTasks() {
    try {
      Log.record(TAG + ".", "开始执行信誉任务领取");
      String resp = null;
      try {
        resp = AntMemberRpcCall.Zmxy.queryGrowthGuideToDoList("yuebao_7d", "1.0.2025.10.27");
      } catch (Throwable e) {
        Log.printStackTrace(TAG + ".handleGrowthGuideTasks.queryGrowthGuideToDoList", e);
        return;
      }

      if (resp.isEmpty()) {
        Log.record(TAG + ".handleGrowthGuideTasks", "信誉任务列表返回空");
        return;
      }

      JSONObject root;
      try {
        root = new JSONObject(resp);
      } catch (Throwable e) {
        Log.printStackTrace(TAG + ".handleGrowthGuideTasks.parseRootJson", e);
        return;
      }

      if (!ResChecker.checkRes(TAG, root)) {
        Log.record(TAG + ".handleGrowthGuideTasks", "信誉任务列表获取失败: " + root.optString("resultView", resp));
        return;
      }
      // 成长引导列表（不会用，只做计数）
      JSONArray growthGuideList = root.optJSONArray("growthGuideList");
      int guideCount = growthGuideList != null ? growthGuideList.length() : 0;

      // 待处理任务列表
      JSONArray toDoList = root.optJSONArray("toDoList");
      int toDoCount = toDoList != null ? toDoList.length() : 0;
      if (toDoList == null || toDoCount == 0) {
        return;
      }

      for (int i = 0; i < toDoList.length(); i++) {
        JSONObject task = null;
        try {
          task = toDoList.optJSONObject(i);
        } catch (Throwable ignored) {
        }

        if (task == null)
          continue;

        String behaviorId = task.optString("behaviorId", "");
        String title = task.optString("title", "");
        String status = task.optString("status", "");
        String subTitle = task.optString("subTitle", "");

        // ===== 2.1 公益类任务 =====
        if ("wait_receive".equals(status)) {
          String openResp;
          try {
            openResp = AntMemberRpcCall.Zmxy.openBehaviorCollect(behaviorId);
          } catch (Throwable e) {
            Log.printStackTrace(TAG + ".handleGrowthGuideTasks.openBehaviorCollect", e);
            continue;
          }

          try {
            JSONObject openJo = new JSONObject(openResp);
            if (ResChecker.checkRes(TAG, openJo)) {
              Log.other(TAG, "信誉任务[领取成功] " + title);
            } else {
              Log.record(TAG + ".handleGrowthGuideTasks", "信誉任务[领取失败] behaviorId="
                      + behaviorId + " title=" + title + " resp=" + openResp);
            }
          } catch (Throwable e) {
            Log.printStackTrace(TAG + ".handleGrowthGuideTasks.parseOpenBehaviorCollect", e);
          }
          continue;
        }

        // ===== 2.2 每日问答 =====
        if ("meiriwenda".equals(behaviorId)&&"wait_doing".equals(status) ) {//如果等待去做才执行，一般不会进入下面的今日已参与判断

          if (subTitle.contains("今日已参与")) {
            Log.other(TAG, "信誉任务[每日问答] " + subTitle + "（跳过答题）");
            continue;
          }

          try {
            // ① 查询题目
            String quizResp = AntMemberRpcCall.Zmxy.queryDailyQuiz(behaviorId);
            JSONObject quizJo;
            try {
              quizJo = new JSONObject(quizResp);
            } catch (Throwable e) {
              Log.error(TAG + ".handleGrowthGuideTasks", "每日问答[解析失败] resp=" + quizResp);
              continue;
            }

            if (!ResChecker.checkRes(TAG, quizJo)) {
              Log.error(TAG + ".handleGrowthGuideTasks", "每日问答[查询失败] resp=" + quizResp);
              continue;
            }

            JSONObject data = quizJo.optJSONObject("data");
            if (data == null) {
              Log.error(TAG + ".handleGrowthGuideTasks", "每日问答[返回缺少data]");
              continue;
            }

            JSONObject qVo = data.optJSONObject("questionVo");
            if (qVo == null) {
              Log.error(TAG + ".handleGrowthGuideTasks", "每日问答[缺少questionVo]");
              continue;
            }

            JSONObject rightAnswer = qVo.optJSONObject("rightAnswer");
            if (rightAnswer == null) {
              Log.error(TAG + ".handleGrowthGuideTasks", "每日问答[缺少rightAnswer]");
              continue;
            }

            long bizDate = data.optLong("bizDate", 0L);
            String questionId = qVo.optString("questionId", "");
            String questionContent = qVo.optString("questionContent", "");
            String answerId = rightAnswer.optString("answerId", "");
            String answerContent = rightAnswer.optString("answerContent", "");

            if (bizDate <= 0 || questionId.isEmpty() || answerId.isEmpty()) {
              Log.error(TAG + ".handleGrowthGuideTasks", "每日问答[关键字段缺失]");
              continue;
            }

            // ② 提交答案
            String pushResp = AntMemberRpcCall.Zmxy.pushDailyTask(
                    behaviorId, bizDate, answerId, questionId, "RIGHT");

            JSONObject pushJo;
            try {
              pushJo = new JSONObject(pushResp);
            } catch (Throwable e) {
              Log.error(TAG + ".handleGrowthGuideTasks", "每日问答[提交解析失败] resp=" + pushResp);
              continue;
            }

            if (ResChecker.checkRes(TAG, pushJo)) {
              Log.other(TAG, "信誉任务[每日答题成功] " + questionContent
                      + " | 答案=" + answerContent + "(" + answerId + ")"
                      + (subTitle.isEmpty() ? "" : " | " + subTitle));
            } else {
              Log.error(TAG + ".handleGrowthGuideTasks", "每日问答[提交失败] resp=" + pushResp);
            }

          } catch (Throwable e) {
            Log.printStackTrace(TAG + ".handleGrowthGuideTasks.meiriwenda", e);
          }
        }

        // ===== 2.3 视频问答 =====
        if ("shipingwenda".equals(behaviorId) && "wait_doing".equals(status)) {

          long bizDate = System.currentTimeMillis();
          String questionId = "question3";
          String answerId = "A";
          String answerType = "RIGHT";

          String pushResp = AntMemberRpcCall.Zmxy.pushDailyTask(
                  behaviorId,
                  bizDate,
                  answerId,
                  questionId,
                  answerType
          );

          JSONObject jo;
          try {
            jo = new JSONObject(pushResp);
          } catch (Throwable e) {
            Log.error(TAG + ".handleGrowthGuideTasks", "视频问答[解析失败] resp=" + pushResp);
            continue;   // 改为continue，避免return影响循环
          }

          if (ResChecker.checkRes(TAG, jo)) {
            Log.other(TAG, "信誉任务[视频问答提交成功] → ");
          } else {
            Log.error(TAG + ".handleGrowthGuideTasks", "视频问答[提交失败] → " + pushResp);
          }
        }

        // ===== 2.4 芭芭农场施肥 =====
        if ("babanongchang_7d".equals(behaviorId) && "wait_doing".equals(status)) {
          try {
            // 假设getWua()方法存在，返回wua（为空即可）
            String wua = SecurityBodyHelper.INSTANCE.getSecurityBodyData(4); // 传入空字符串
            String source = "DNHZ_NC_zhimajingnangSF"; // 从buttonUrl提取的source
            Log.record(TAG, "set Wua " + wua);

            String spreadManureDataStr = AntOrchardRpcCall.orchardSpreadManure(Objects.requireNonNull(wua), source);
            JSONObject spreadManureData;
            try {
              spreadManureData = new JSONObject(spreadManureDataStr);
            } catch (Throwable e) {
              Log.error(TAG + ".handleGrowthGuideTasks", "芭芭农场[解析失败] resp=" + spreadManureDataStr);
              continue;
            }

            if (!"100".equals(spreadManureData.optString("resultCode"))) {
              Log.record(TAG, "农场 orchardSpreadManure 错误：" + spreadManureData.optString("resultDesc"));
              Log.runtime(TAG, "农场 orchardSpreadManure 错误：" + spreadManureData.toString());
              continue;
            }

            String taobaoDataStr = spreadManureData.optString("taobaoData", "");
            if (taobaoDataStr.isEmpty()) {
              Log.error(TAG + ".handleGrowthGuideTasks", "芭芭农场[缺少taobaoData]");
              continue;
            }

            JSONObject spreadTaobaoData;
            try {
              spreadTaobaoData = new JSONObject(taobaoDataStr);
            } catch (Throwable e) {
              Log.error(TAG + ".handleGrowthGuideTasks", "芭芭农场[taobaoData解析失败]");
              continue;
            }

            JSONObject currentStage = spreadTaobaoData.optJSONObject("currentStage");
            if (currentStage == null) {
              Log.error(TAG + ".handleGrowthGuideTasks", "芭芭农场[缺少currentStage]");
              continue;
            }

            String stageText = currentStage.optString("stageText", "");
            JSONObject statistics = spreadTaobaoData.optJSONObject("statistics");
            int dailyAppWateringCount = statistics != null ? statistics.optInt("dailyAppWateringCount", 0) : 0;

            Log.forest("今日农场已施肥💩 " + dailyAppWateringCount + " 次 [" + stageText + "]");

            Log.other(TAG, "信誉任务[芭芭农场施肥成功] " + title + " | 已施肥 " + dailyAppWateringCount + " 次");

          } catch (Throwable e) {
            Log.printStackTrace(TAG + ".handleGrowthGuideTasks.babanongchang", e);
          }
        }

      }
    } catch (Throwable e) {
      Log.printStackTrace(TAG + ".handleGrowthGuideTasks.Fatal", e);
    }
  }

  /**
   * 查询 + 自动领取可领取球（精简一行输出领取信息）
   */
  public static void queryAndCollect() {
    try {
      // 1. 查询进度球状态
      String queryResp = AntMemberRpcCall.Zmxy.queryScoreProgress();
      if (queryResp == null || queryResp.isEmpty()) return;

      JSONObject json = new JSONObject(queryResp);

      // 检查 success
      if (!ResChecker.checkRes(TAG, json)) return;

      JSONObject totalWait = json.optJSONObject("totalWaitProcessVO");
      if (totalWait == null) return;

      JSONArray idList = totalWait.optJSONArray("totalProgressIdList");
      if (idList == null || idList.length() == 0) return;

      // 直接传 JSONArray
      String collectResp = AntMemberRpcCall.Zmxy.collectProgressBall(idList);
      if (collectResp == null) return;

      JSONObject collectJson = new JSONObject(collectResp);

      Log.other(
              TAG,
              String.format(
                      "领取完成 → 本次加速进度: %d, 当前加速倍率: %.2f",
                      collectJson.optInt("collectedAccelerateProgress", -1),
                      collectJson.optDouble("currentAccelerateValue", -1)
              )
      );

    } catch (Exception e) {
      e.printStackTrace();
    }
  }





  /**
   * 年度回顾任务：通过 programInvoke 查询并自动完成任务
   *
   *
   * 1) alipay.imasp.program.programInvoke + ..._task_reward_query 查询 playTaskOrderInfoList
   * 2) 对于 taskStatus = "init" 的任务，使用 ..._task_reward_apply(code) 领取，得到 recordNo
   * 3) 使用 ..._task_reward_process(code, recordNo) 上报完成，服务端自动发放成长值奖励
   */
  private void doAnnualReview () {
    try {
      Log.record(TAG + ".doAnnualReview", "年度回顾🎞[开始执行]");

      String resp = AntMemberRpcCall.annualReviewQueryTasks();
      if (resp == null || resp.isEmpty()) {
        Log.record(TAG + ".doAnnualReview", "年度回顾[查询返回空]");
        return;
      }

      JSONObject root;
      try {
        root = new JSONObject(resp);
      } catch (Throwable e) {
        Log.printStackTrace(TAG + ".doAnnualReview.parseRoot", e);
        return;
      }

      if (!root.optBoolean("isSuccess", false)) {
        Log.record(TAG + ".doAnnualReview", "年度回顾[查询失败]#" + resp);
        return;
      }

      JSONObject components = root.optJSONObject("components");
      if (components == null || components.length() == 0) {
        Log.record(TAG + ".doAnnualReview", "年度回顾[components 为空]");
        return;
      }

      JSONObject queryComp = components.optJSONObject(AntMemberRpcCall.ANNUAL_REVIEW_QUERY_COMPONENT);
      if (queryComp == null) {
        // 兜底：取第一个组件
        try {
          java.util.Iterator<String> it = components.keys();
          if (it.hasNext()) {
            queryComp = components.optJSONObject(it.next());
          }
        } catch (Throwable ignored) {
        }
      }
      if (queryComp == null) {
        Log.record(TAG + ".doAnnualReview", "年度回顾[未找到查询组件]");
        return;
      }
      if (!queryComp.optBoolean("isSuccess", true)) {
        Log.record(TAG + ".doAnnualReview", "年度回顾[查询组件返回失败]");
        return;
      }

      JSONObject content = queryComp.optJSONObject("content");
      if (content == null) {
        Log.record(TAG + ".doAnnualReview", "年度回顾[content 为空]");
        return;
      }

      JSONArray taskList = content.optJSONArray("playTaskOrderInfoList");
      if (taskList == null || taskList.length() == 0) {
        Log.record(TAG + ".doAnnualReview", "年度回顾[当前无可处理任务]");
        return;
      }

      int candidate = 0;
      int applied = 0;
      int processed = 0;
      int failed = 0;

      for (int i = 0; i < taskList.length(); i++) {
        JSONObject task = taskList.optJSONObject(i);
        if (task == null) {
          continue;
        }

        String taskStatus = task.optString("taskStatus", "");
        if (!"init".equals(taskStatus)) {
          // 已完成/已领奖等状态直接跳过
          continue;
        }
        candidate++;

        String code = task.optString("code", "");
        if (code.isEmpty()) {
          JSONObject extInfo = task.optJSONObject("extInfo");
          if (extInfo != null) {
            code = extInfo.optString("taskId", "");
          }
        }
        if (code.isEmpty()) {
          failed++;
          continue;
        }

        String taskName = code;
        JSONObject displayInfo = task.optJSONObject("displayInfo");
        if (displayInfo != null) {
          String name = displayInfo.optString("taskName",
                  displayInfo.optString("activityName", code));
          if (!name.isEmpty()) {
            taskName = name;
          }
        }

        // ========== Step 1: 领取任务 (apply) ==========
        String applyResp = AntMemberRpcCall.annualReviewApplyTask(code);
        if (applyResp == null || applyResp.isEmpty()) {
          Log.record(TAG + ".doAnnualReview", "年度回顾[领任务失败]" + taskName + "#响应为空");
          failed++;
          continue;
        }

        JSONObject applyRoot;
        try {
          applyRoot = new JSONObject(applyResp);
        } catch (Throwable e) {
          Log.printStackTrace(TAG + ".doAnnualReview.parseApply", e);
          failed++;
          continue;
        }
        if (!applyRoot.optBoolean("isSuccess", false)) {
          Log.record(TAG + ".doAnnualReview", "年度回顾[领任务失败]" + taskName + "#" + applyResp);
          failed++;
          continue;
        }
        JSONObject applyComps = applyRoot.optJSONObject("components");
        if (applyComps == null) {
          failed++;
          continue;
        }
        JSONObject applyComp = applyComps.optJSONObject(AntMemberRpcCall.ANNUAL_REVIEW_APPLY_COMPONENT);
        if (applyComp == null) {
          try {
            java.util.Iterator<String> it2 = applyComps.keys();
            if (it2.hasNext()) {
              applyComp = applyComps.optJSONObject(it2.next());
            }
          } catch (Throwable ignored) {
          }
        }
        if (applyComp == null || !applyComp.optBoolean("isSuccess", true)) {
          failed++;
          continue;
        }
        JSONObject applyContent = applyComp.optJSONObject("content");
        if (applyContent == null) {
          failed++;
          continue;
        }
        JSONObject claimedTask = applyContent.optJSONObject("claimedTask");
        if (claimedTask == null) {
          failed++;
          continue;
        }
        String recordNo = claimedTask.optString("recordNo", "");
        if (recordNo.isEmpty()) {
          failed++;
          continue;
        }
        applied++;

        GlobalThreadPools.sleepCompat(500);

        // ========== Step 2: 提交任务完成 (process) ==========
        String processResp = AntMemberRpcCall.annualReviewProcessTask(code, recordNo);
        if (processResp == null || processResp.isEmpty()) {
          Log.record(TAG + ".doAnnualReview", "年度回顾[提交任务失败]" + taskName + "#响应为空");
          failed++;
          continue;
        }

        JSONObject processRoot;
        try {
          processRoot = new JSONObject(processResp);
        } catch (Throwable e) {
          Log.printStackTrace(TAG + ".doAnnualReview.parseProcess", e);
          failed++;
          continue;
        }
        if (!processRoot.optBoolean("isSuccess", false)) {
          Log.record(TAG + ".doAnnualReview", "年度回顾[提交任务失败]" + taskName + "#" + processResp);
          failed++;
          continue;
        }
        JSONObject processComps = processRoot.optJSONObject("components");
        if (processComps == null) {
          failed++;
          continue;
        }
        JSONObject processComp = processComps.optJSONObject(AntMemberRpcCall.ANNUAL_REVIEW_PROCESS_COMPONENT);
        if (processComp == null) {
          try {
            java.util.Iterator<String> it3 = processComps.keys();
            if (it3.hasNext()) {
              processComp = processComps.optJSONObject(it3.next());
            }
          } catch (Throwable ignored) {
          }
        }
        if (processComp == null || !processComp.optBoolean("isSuccess", true)) {
          failed++;
          continue;
        }
        JSONObject processContent = processComp.optJSONObject("content");
        if (processContent == null) {
          failed++;
          continue;
        }
        JSONObject processedTask = processContent.optJSONObject("processedTask");
        if (processedTask == null) {
          failed++;
          continue;
        }
        String newStatus = processedTask.optString("taskStatus", "");
        String rewardStatus = processedTask.optString("rewardStatus", "");

        // ========== Step 3: 如仍未发奖，则调用 get_reward 领取奖励 ==========
        if (!"success".equalsIgnoreCase(rewardStatus)) {
          try {
            String rewardResp = AntMemberRpcCall.annualReviewGetReward(code, recordNo);
            if (rewardResp != null && !rewardResp.isEmpty()) {
              JSONObject rewardRoot = new JSONObject(rewardResp);
              if (rewardRoot.optBoolean("isSuccess", false)) {
                JSONObject rewardComps = rewardRoot.optJSONObject("components");
                if (rewardComps != null) {
                  JSONObject rewardComp = rewardComps.optJSONObject(AntMemberRpcCall.ANNUAL_REVIEW_GET_REWARD_COMPONENT);
                  if (rewardComp == null) {
                    try {
                      java.util.Iterator<String> it4 = rewardComps.keys();
                      if (it4.hasNext()) {
                        rewardComp = rewardComps.optJSONObject(it4.next());
                      }
                    } catch (Throwable ignored) {
                    }
                  }
                  if (rewardComp != null && rewardComp.optBoolean("isSuccess", true)) {
                    JSONObject rewardContent = rewardComp.optJSONObject("content");
                    if (rewardContent != null) {
                      JSONObject rewardTask = rewardContent.optJSONObject("processedTask");
                      if (rewardTask == null) {
                        rewardTask = rewardContent.optJSONObject("claimedTask");
                      }
                      if (rewardTask != null) {
                        String rs = rewardTask.optString("rewardStatus", "");
                        if (!rs.isEmpty()) {
                          rewardStatus = rs;
                        }
                      }
                    }
                  }
                }
              }
            }
          } catch (Throwable e) {
            Log.printStackTrace(TAG + ".doAnnualReview.getReward", e);
          }
        }

        processed++;
        Log.other("年度回顾🎞[任务完成]" + taskName + "#状态=" + newStatus + " 奖励状态=" + rewardStatus);
      }

      Log.record(TAG + ".doAnnualReview",
              "年度回顾🎞[执行结束] 待处理=" + candidate + " 已领取=" + applied + " 已提交=" + processed + " 失败=" + failed);
    } catch (Throwable t) {
      Log.printStackTrace(TAG + ".doAnnualReview", t);
    }
  }

  /**
   * 会员积分0元兑，权益道具兑换
   */
  private void memberPointExchangeBenefit() {
    try {
      String userId = UserMap.getCurrentUid();
      JSONObject memberInfo = new JSONObject(AntMemberRpcCall.queryMemberInfo());
      if (!ResChecker.checkRes(TAG, memberInfo)) {
        return;
      }
      String pointBalance = memberInfo.getString("pointBalance");
      JSONObject jo = new JSONObject(AntMemberRpcCall.queryShandieEntityList(userId, pointBalance));
      if (!ResChecker.checkRes(TAG, jo)) {
        return;
      }
      if (!jo.has("benefits")) {
        Log.record(TAG,"会员积分[未找到可兑换权益]");
        return;
      }
      JSONArray benefits = jo.getJSONArray("benefits");
      for (int i = 0; i < benefits.length(); i++) {
        JSONObject benefitInfo = benefits.getJSONObject(i);
        JSONObject pricePresentation = benefitInfo.getJSONObject("pricePresentation");
        String name = benefitInfo.getString("name");
        String benefitId = benefitInfo.getString("benefitId");
        IdMapManager.getInstance(MemberBenefitsMap.class).add(benefitId, name);
        if (!Status.canMemberPointExchangeBenefitToday(benefitId)
                || !memberPointExchangeBenefitList.getValue().contains(benefitId)) {
          continue;
        }
        String itemId = benefitInfo.getString("itemId");
        if (exchangeBenefit(benefitId, itemId)) {
          String point = pricePresentation.getString("point");
          Log.other("会员积分🎐兑换[" + name + "]#花费[" + point + "积分]");
        } else {
          Log.other("会员积分🎐兑换[" + name + "]失败！");
        }
      }
      IdMapManager.getInstance(MemberBenefitsMap.class).save(userId);
    } catch (JSONException e) {
      Log.record(TAG,"JSON解析错误: " + e.getMessage());
      Log.printStackTrace(TAG, e);
    } catch (Throwable t) {
      Log.runtime(TAG, "memberPointExchangeBenefit err:");
      Log.printStackTrace(TAG, t);
    }
  }

  private Boolean exchangeBenefit(String benefitId, String itemId) {
    try {
      JSONObject jo = new JSONObject(AntMemberRpcCall.exchangeBenefit(benefitId, itemId));
      if (ResChecker.checkRes(TAG + "会员权益兑换失败:", jo)) {
        Status.memberPointExchangeBenefitToday(benefitId);
        return true;
      }
    } catch (Throwable t) {
      Log.runtime(TAG, "exchangeBenefit err:");
      Log.printStackTrace(TAG, t);
    }
    return false;
  }

  /**
   * 会员签到
   */
  private void doMemberSign() {
    try {
      if (Status.canMemberSignInToday(UserMap.getCurrentUid())) {
        String s = AntMemberRpcCall.queryMemberSigninCalendar();
        GlobalThreadPools.sleepCompat(500);
        JSONObject jo = new JSONObject(s);
        if (ResChecker.checkRes(TAG + "会员签到失败:", jo)) {
          Log.other("会员签到📅[" + jo.getString("signinPoint") + "积分]#已签到" + jo.getString("signinSumDay") + "天");
          Status.memberSignInToday(UserMap.getCurrentUid());
        } else {
          Log.record(jo.getString("resultDesc"));
          Log.runtime(s);
        }
      }
      queryPointCert(1, 8);
    } catch (Throwable t) {
      Log.printStackTrace(TAG, t);
    }
  }

  /**
   * 会员任务-逛一逛
   * 单次执行 1
   */
  private void doAllMemberAvailableTask() {
    try {
      String str = AntMemberRpcCall.queryAllStatusTaskList();
      GlobalThreadPools.sleepCompat(500);
      JSONObject jsonObject = new JSONObject(str);
      if (!ResChecker.checkRes(TAG, jsonObject)) {
        Log.error(TAG + ".doAllMemberAvailableTask", "会员任务响应失败: " + jsonObject.getString("resultDesc"));
        return;
      }
      if (!jsonObject.has("availableTaskList")) {
        return;
      }
      JSONArray taskList = jsonObject.getJSONArray("availableTaskList");
      for (int j = 0; j < taskList.length(); j++) {
        JSONObject task = taskList.getJSONObject(j);
        processTask(task);
      }
    } catch (Throwable t) {
      Log.runtime(TAG, "doAllMemberAvailableTask err:");
      Log.printStackTrace(TAG, t);
    }
  }

  /**
   * 会员积分收取
   * @param page 第几页
   * @param pageSize 每页数据条数
   */
  private static void queryPointCert(int page, int pageSize) {
    try {
      String s = AntMemberRpcCall.queryPointCert(page, pageSize);
      GlobalThreadPools.sleepCompat(500);
      JSONObject jo = new JSONObject(s);
      if (ResChecker.checkRes(TAG + "查询会员积分证书失败:", jo)) {
        boolean hasNextPage = jo.getBoolean("hasNextPage");
        JSONArray jaCertList = jo.getJSONArray("certList");
        for (int i = 0; i < jaCertList.length(); i++) {
          jo = jaCertList.getJSONObject(i);
          String bizTitle = jo.getString("bizTitle");
          String id = jo.getString("id");
          int pointAmount = jo.getInt("pointAmount");
          s = AntMemberRpcCall.receivePointByUser(id);
          jo = new JSONObject(s);
          if (ResChecker.checkRes(TAG + "会员积分领取失败:", jo)) {
            Log.other("会员积分🎖️[领取" + bizTitle + "]#" + pointAmount + "积分");
          } else {
            Log.record(jo.getString("resultDesc"));
            Log.runtime(s);
          }
        }
        if (hasNextPage) {
          queryPointCert(page + 1, pageSize);
        }
      } else {
        Log.record(jo.getString("resultDesc"));
        Log.runtime(s);
      }
    } catch (Throwable t) {
      Log.runtime(TAG, "queryPointCert err:");
      Log.printStackTrace(TAG, t);
    }
  }

  /**
   * 检查是否满足运行芝麻信用任务的条件
   * @return bool
   */
  private static Boolean checkSesameCanRun() {
    try {
      String s = AntMemberRpcCall.queryHome();
      JSONObject jo = new JSONObject(s);
      if (!ResChecker.checkRes(TAG, jo)) {
        Log.other(TAG, "芝麻信用💳[首页响应失败]#" + jo.optString("errorMsg"));
        Log.error(TAG + ".checkSesameCanRun.queryHome", "芝麻信用💳[首页响应失败]#" + s);
        return false;
      }
      JSONObject entrance = jo.getJSONObject("entrance");
      if (!entrance.optBoolean("openApp")) {
        Log.other("芝麻信用💳[未开通芝麻信用]");
        return false;
      }
      return true;
    } catch (Throwable t) {
      Log.printStackTrace(TAG + ".checkSesameCanRun", t);
      return false;
    }
  }

  /**
   * 芝麻信用任务 - 重构版本
   */
  private void doAllAvailableSesameTask() {
    try {
      String s = AntMemberRpcCall.queryAvailableSesameTask();
      GlobalThreadPools.sleepCompat(500);
      JSONObject jo = new JSONObject(s);
      if (jo.has("resData")) {
        jo = jo.getJSONObject("resData");
      }
      if (!ResChecker.checkRes(TAG, jo)) {
        Log.other(TAG, "芝麻信用💳[查询任务响应失败]#" + jo.getString("resultCode"));
        Log.error(TAG + ".doAllAvailableSesameTask.queryAvailableSesameTask", "芝麻信用💳[查询任务响应失败]#" + s);
        return;
      }

      // Log.record(TAG, "芝麻信用💳[查询任务响应]#" + s);

      JSONObject taskObj = jo.getJSONObject("data");
      int totalTasks = 0;
      int completedTasks = 0;
      int skippedTasks = 0;

      // 处理日常任务
      if (taskObj.has("dailyTaskListVO")) {
        JSONObject dailyTaskListVO = taskObj.getJSONObject("dailyTaskListVO");

        if (dailyTaskListVO.has("waitCompleteTaskVOS")) {
          JSONArray waitCompleteTaskVOS = dailyTaskListVO.getJSONArray("waitCompleteTaskVOS");
          totalTasks += waitCompleteTaskVOS.length();
          Log.record(TAG, "芝麻信用💳[待完成任务]#开始处理(" + waitCompleteTaskVOS.length() + "个)");
          int[] results = joinAndFinishSesameTaskWithResult(waitCompleteTaskVOS);
          completedTasks += results[0];
          skippedTasks += results[1];
        }

        if (dailyTaskListVO.has("waitJoinTaskVOS")) {
          JSONArray waitJoinTaskVOS = dailyTaskListVO.getJSONArray("waitJoinTaskVOS");
          totalTasks += waitJoinTaskVOS.length();
          Log.record(TAG, "芝麻信用💳[待加入任务]#开始处理(" + waitJoinTaskVOS.length() + "个)");
          int[] results = joinAndFinishSesameTaskWithResult(waitJoinTaskVOS);
          completedTasks += results[0];
          skippedTasks += results[1];
        }
      }

      // 处理toCompleteVOS任务
      if (taskObj.has("toCompleteVOS")) {
        JSONArray toCompleteVOS = taskObj.getJSONArray("toCompleteVOS");
        totalTasks += toCompleteVOS.length();
        Log.record(TAG, "芝麻信用💳[toCompleteVOS任务]#开始处理(" + toCompleteVOS.length() + "个)");
        int[] results = joinAndFinishSesameTaskWithResult(toCompleteVOS);
        completedTasks += results[0];
        skippedTasks += results[1];
      }

      // 统计结果并决定是否关闭开关
      Log.record(TAG, "芝麻信用💳[任务处理完成]#总任务:" + totalTasks + "个, 完成:" + completedTasks + "个, 跳过:" + skippedTasks + "个");

      // 如果所有任务都已完成或跳过（没有剩余可完成任务），关闭开关
      if (totalTasks > 0 && (completedTasks + skippedTasks) >= totalTasks) {
        Status.setFlagToday(StatusFlags.FLAG_AntMember_doAllAvailableSesameTask);
        Log.record(TAG, "芝麻信用💳[已全部完成任务，临时关闭]");
      }
    } catch (Throwable t) {
      Log.printStackTrace(TAG + ".doAllAvailableSesameTask", t);
    }
  }

  /**
   * 不能完成的任务黑名单（根据title关键词匹配）
   */
  private static final String[] TASK_BLACKLIST = {
          "每日施肥领水果",           // 需要淘宝操作
          "坚持种水果",              // 需要淘宝操作
          "坚持去玩休闲小游戏",       // 需要游戏操作
          "去AQapp提问",            // 需要下载APP
          "去AQ提问",               // 需要下载APP
          "坚持看直播领福利",        // 需要淘宝直播
          "去淘金币逛一逛",          // 需要淘宝操作
          "坚持攒保障金",            // 参数错误：promiseActivityExtCheck
          "芝麻租赁下单得芝麻粒",     // 需要租赁操作
          "去玩小游戏",              // 参数错误：promiseActivityExtCheck
          "浏览租赁商家小程序",       // 需要小程序操作
          "订阅小组件",              // 参数错误：promiseActivityExtCheck
          "租1笔图书",               // 参数错误：promiseActivityExtCheck
          "去订阅芝麻小组件",         // 参数错误：promiseActivityExtCheck
          "坚持攒保障"               // 参数错误：promiseActivityExtCheck（与"坚持攒保障金"类似，防止匹配遗漏）
  };

  /**
   * 检查任务是否在黑名单中
   * @param taskTitle 任务标题
   * @return true表示在黑名单中，应该跳过
   */
  private static boolean isTaskInBlacklist(String taskTitle) {
    if (taskTitle == null) return false;
    for (String blacklistItem : TASK_BLACKLIST) {
      if (taskTitle.contains(blacklistItem)) {
        return true;
      }
    }
    return false;
  }

  /**
   * 芝麻信用-领取并完成任务（带结果统计）
   * @param taskList 任务列表
   * @return int数组 [完成数量, 跳过数量]
   * @throws JSONException JSON解析异常，上抛处理
   */
  private static int[] joinAndFinishSesameTaskWithResult(JSONArray taskList) throws JSONException {
    int completedCount = 0;
    int skippedCount = 0;

    for (int i = 0; i < taskList.length(); i++) {
      JSONObject task = taskList.getJSONObject(i);
      String taskTitle = task.has("title") ? task.getString("title") : "未知任务";

      // 打印任务状态信息用于调试
      boolean finishFlag = task.optBoolean("finishFlag", false);
      String actionText = task.optString("actionText", "");
      //  Log.record(TAG, "芝麻信用💳[任务状态调试]#" + taskTitle + " - finishFlag:" + finishFlag + ", actionText:" + actionText);

      // 检查任务是否已完成
      if (finishFlag || "已完成".equals(actionText)) {
        Log.record(TAG, "芝麻信用💳[跳过已完成任务]#" + taskTitle);
        skippedCount++;
        continue;
      }

      // 检查黑名单
      if (isTaskInBlacklist(taskTitle)) {
        Log.record(TAG, "芝麻信用💳[跳过黑名单任务]#" + taskTitle);
        skippedCount++;
        continue;
      }

      // 添加检查，确保templateId存在
      if (!task.has("templateId")) {
        Log.record(TAG, "芝麻信用💳[跳过缺少templateId任务]#" + taskTitle);
        skippedCount++;
        continue;
      }

      String taskTemplateId = task.getString("templateId");
      int needCompleteNum = task.has("needCompleteNum") ? task.getInt("needCompleteNum") : 1;
      int completedNum = task.optInt("completedNum", 0);
      String s;
      String recordId;
      JSONObject responseObj;


      if (task.has("actionUrl") && task.getString("actionUrl").contains("jumpAction")) {
        // 跳转APP任务 依赖跳转的APP发送请求鉴别任务完成 仅靠hook支付宝无法完成
        Log.record(TAG, "芝麻信用💳[跳过跳转APP任务]#" + taskTitle);
        skippedCount++;
        continue;
      }

      boolean taskCompleted = false;
      if (!task.has("todayFinish")) {
        // 领取任务
        s = AntMemberRpcCall.joinSesameTask(taskTemplateId);
        GlobalThreadPools.sleepCompat(200);
        responseObj = new JSONObject(s);
        if (!ResChecker.checkRes(TAG, responseObj)) {
          Log.other(TAG, "芝麻信用💳[领取任务" + taskTitle + "失败]#" + s);
          skippedCount++;
          continue;
        }
        recordId = responseObj.getJSONObject("data").getString("recordId");
      } else {
        if (!task.has("recordId")) {
          Log.other(TAG, "芝麻信用💳[任务" + taskTitle + "未获取到recordId]#" + task);
          skippedCount++;
          continue;
        }
        recordId = task.getString("recordId");
      }

      // 完成任务
      for (int j = completedNum; j < needCompleteNum; j++) {
        s = AntMemberRpcCall.finishSesameTask(recordId);
        GlobalThreadPools.sleepCompat(200);
        responseObj = new JSONObject(s);
        if (ResChecker.checkRes(TAG, responseObj)) {
          Log.record(TAG, "芝麻信用💳[完成任务" + taskTitle + "]#(" + (j + 1) + "/" + needCompleteNum + "天)");
          taskCompleted = true;
        } else {
          Log.other(TAG, "芝麻信用💳[完成任务" + taskTitle + "失败]#" + s);
          break;
        }
      }

      if (taskCompleted) {
        completedCount++;
      } else {
        skippedCount++;
      }
    }

    return new int[]{completedCount, skippedCount};
  }

  /**
   * 芝麻粒信用福利签到  与芝麻粒炼金的签到方法都一样 alchemyQueryCheckIn 只不过scenecode不一样
   * 基于 HomeV8RpcManager.queryServiceCard 返回的 serviceCardVOList
   * 通过 itemAttrs.checkInModuleVO.currentDateCheckInTaskVO 判断今日是否可签到
   */
  private void doSesameZmlCheckIn() {
    try {

      String checkInRes = AntMemberRpcCall.Zmxy.Alchemy.alchemyQueryCheckIn("zml");
      JSONObject checkInJo = new JSONObject(checkInRes);
      if (ResChecker.checkRes(TAG, checkInJo)) {
        JSONObject data = checkInJo.optJSONObject("data");
        if (data != null) {
          JSONObject currentDay = data.optJSONObject("currentDateCheckInTaskVO");
          if (currentDay != null) {
            String status = currentDay.optString("status");
            String checkInDate = currentDay.optString("checkInDate");
            if ("CAN_COMPLETE".equals(status) && !checkInDate.isEmpty()) {
              // 信誉主页签到
              String completeRes = AntMemberRpcCall.zmCheckInCompleteTask(checkInDate, "zml");
              try {
                JSONObject completeJo = new JSONObject(completeRes);
                if (ResChecker.checkRes(TAG, completeJo)) {
                  JSONObject prize = completeJo.optJSONObject("data");
                  int num = prize != null ? prize.optInt("zmlNum",
                          prize.optJSONObject("prize") != null ?
                                  prize.optJSONObject("prize").optInt("num", 0) : 0) : 0;
                  Log.other("芝麻炼金⚗️[每日签到成功]#获得" + num + "粒");
                } else {
                  Log.runtime(TAG + ".doSesameAlchemy", "炼金签到失败:" + completeRes);
                }
              } catch (Throwable e) {
                Log.printStackTrace(TAG + ".doSesameAlchemy.alchemyCheckInComplete", e);
              }
            } // status 为 COMPLETED 时不再重复签到
          }
        }
      }
    } catch (Throwable t) {
      Log.printStackTrace(TAG + ".doSesameZmlCheckIn", t);
    }
  }


  //z
  private void doSesameAlchemyNextDayAward() {
    try {

      // ===== 调用领取奖励 RPC =====
      String awardRes = AntMemberRpcCall.Zmxy.Alchemy.claimAward();

      JSONObject jo = new JSONObject(awardRes);

      if (!ResChecker.checkRes(TAG, jo)) {
        Log.error("芝麻炼金⚗️[次日奖励失败]：" + awardRes);
        // 即使失败也要设 flag，避免卡死重复调用
        Status.setFlagToday(StatusFlags.FLAG_ZMXY_ALCHEMY_NEXT_DAY_AWARD);
        return;
      }

      JSONObject data = jo.optJSONObject("data");
      int gotNum = 0;

      if (data != null) {
        // 解析奖励数组
        JSONArray arr = data.optJSONArray("alchemyAwardSendResultVOS");
        if (arr != null && arr.length() > 0) {
          JSONObject item = arr.optJSONObject(0);
          if (item != null) {
            gotNum = item.optInt("pointNum", 0);
          }
        }
      }

      if (gotNum > 0) {
        Log.other("芝麻炼金⚗️[次日奖励领取成功]#获得" + gotNum + "粒");
      } else {
        Log.record("芝麻炼金⚗️[次日奖励无奖励] 已领取或无可领奖励");
      }

      // ★★★★★ 不论有无奖励都标记今日完成 ★★★★★
      Status.setFlagToday(StatusFlags.FLAG_ZMXY_ALCHEMY_NEXT_DAY_AWARD);

    } catch (Throwable t) {
      Log.printStackTrace("doSesameAlchemyNextDayAward", t);
      // 异常也要标记，否则会无限尝试
      Status.setFlagToday(StatusFlags.FLAG_ZMXY_ALCHEMY_NEXT_DAY_AWARD);
    }
  }


  /**
   * 芝麻粒收取
   * @param withOneClick 启用一键收取
   */
  private void collectSesame(Boolean withOneClick) {
    try {
      JSONObject jo = new JSONObject(AntMemberRpcCall.queryCreditFeedback());
      GlobalThreadPools.sleepCompat(500);
      if (!ResChecker.checkRes(TAG, jo)) {
        Log.other(TAG, "芝麻信用💳[查询未领取芝麻粒响应失败]#" + jo.getString("resultView"));
        Log.error(TAG + ".collectSesame.queryCreditFeedback", "芝麻信用💳[查询未领取芝麻粒响应失败]#" + jo);
        return;
      }
      JSONArray availableCollectList = jo.getJSONArray("creditFeedbackVOS");
      if (withOneClick) {
        GlobalThreadPools.sleepCompat(2000);
        jo = new JSONObject(AntMemberRpcCall.collectAllCreditFeedback());
        GlobalThreadPools.sleepCompat(2000);
        if (!ResChecker.checkRes(TAG, jo)) {
          Log.other(TAG, "芝麻信用💳[一键收取芝麻粒响应失败]#" + jo);
          Log.error(TAG + ".collectSesame.collectAllCreditFeedback", "芝麻信用💳[一键收取芝麻粒响应失败]#" + jo);
          return;
        }
      }
      for (int i = 0; i < availableCollectList.length(); i++) {
        jo = availableCollectList.getJSONObject(i);
        if (!"UNCLAIMED".equals(jo.getString("status"))) {
          continue;
        }
        String title = jo.getString("title");
        String creditFeedbackId = jo.getString("creditFeedbackId");
        String potentialSize = jo.getString("potentialSize");
        if (!withOneClick) {
          jo = new JSONObject(AntMemberRpcCall.collectCreditFeedback(creditFeedbackId));
          GlobalThreadPools.sleepCompat(2000);
          if (!ResChecker.checkRes(TAG, jo)) {
            Log.other(TAG, "芝麻信用💳[查询未领取芝麻粒响应失败]#" + jo.getString("resultView"));
            Log.error(TAG + ".collectSesame.collectCreditFeedback", "芝麻信用💳[收取芝麻粒响应失败]#" + jo);
            continue;
          }
        }
        Log.other("芝麻信用💳[" + title + "]#" + potentialSize + "粒" + (withOneClick ? "(一键收取)" : ""));
      }
    } catch (Throwable t) {
      Log.printStackTrace(TAG + ".collectSesame", t);
    }
  }

  /**
   * 商家开门打卡签到
   */
  private static void kmdkSignIn() {
    try {
      String s = AntMemberRpcCall.queryActivity();
      JSONObject jo = new JSONObject(s);
      if (ResChecker.checkRes(TAG, jo)) {
        if ("SIGN_IN_ENABLE".equals(jo.getString("signInStatus"))) {
          String activityNo = jo.getString("activityNo");
          JSONObject joSignIn = new JSONObject(AntMemberRpcCall.signIn(activityNo));
          if (ResChecker.checkRes(TAG, joSignIn)) {
            Log.other(TAG,"商家服务🏬[开门打卡签到成功]");
          } else {
            Log.record(TAG,joSignIn.getString("errorMsg"));
            Log.runtime(TAG,joSignIn.toString());
          }
        }
      } else {
        Log.record(TAG,"queryActivity" + " " + s);
      }
    } catch (Throwable t) {

      Log.printStackTrace(TAG, "kmdkSignIn err:",t);
    }
  }

  /**
   * 商家开门打卡报名
   */
  private static void kmdkSignUp() {
    try {
      for (int i = 0; i < 5; i++) {
        JSONObject jo = new JSONObject(AntMemberRpcCall.queryActivity());
        if (ResChecker.checkRes(TAG, jo)) {
          String activityNo = jo.getString("activityNo");
          if (!TimeUtil.getFormatDate().replace("-", "").equals(activityNo.split("_")[2])) {
            break;
          }
          if ("SIGN_UP".equals(jo.getString("signUpStatus"))) {
            break;
          }
          if ("UN_SIGN_UP".equals(jo.getString("signUpStatus"))) {
            String activityPeriodName = jo.getString("activityPeriodName");
            JSONObject joSignUp = new JSONObject(AntMemberRpcCall.signUp(activityNo));
            if (ResChecker.checkRes(TAG, joSignUp)) {
              Log.other(TAG,"商家服务🏬[" + activityPeriodName + "开门打卡报名]");
              return;
            } else {
              Log.record(TAG,joSignUp.getString("errorMsg"));
              Log.runtime(TAG,joSignUp.toString());
            }
          }
        } else {
          Log.record(TAG,"queryActivity");
          Log.runtime(TAG,jo.toString());
        }
        GlobalThreadPools.sleepCompat(500);
      }
    } catch (Throwable t) {
      Log.printStackTrace(TAG,"kmdkSignUp err:", t);
    }
  }

  /**
   * 商家积分签到
   */
  private static void doMerchantSign() {
    try {
      String s = AntMemberRpcCall.merchantSign();
      JSONObject jo = new JSONObject(s);
      if (!ResChecker.checkRes(TAG, jo)) {
        Log.runtime(TAG, "doMerchantSign err:" + s);
        return;
      }
      jo = jo.getJSONObject("data");
      String signResult = jo.getString("signInResult");
      String reward = jo.getString("todayReward");
      if ("SUCCESS".equals(signResult)) {
        Log.other(TAG,"商家服务🏬[每日签到]#获得积分" + reward);
      } else {
        Log.record(TAG,s);
        Log.runtime(TAG,s);
      }
    } catch (Throwable t) {
      Log.runtime(TAG);
      Log.printStackTrace(TAG, "kmdkSignIn err:", t);
    }
  }

  /**
   * 商家积分任务
   */
  private static void doMerchantMoreTask() {
    String s = AntMemberRpcCall.taskListQuery();
    try {
      boolean doubleCheck = false;
      JSONObject jo = new JSONObject(s);
      if (ResChecker.checkRes(TAG, jo)) {
        JSONArray taskList = jo.getJSONObject("data").getJSONArray("taskList");
        for (int i = 0; i < taskList.length(); i++) {
          JSONObject task = taskList.getJSONObject(i);
          if (!task.has("status")) {
            continue;
          }
          String title = task.getString("title");
          String reward = task.getString("reward");
          String taskStatus = task.getString("status");
          if ("NEED_RECEIVE".equals(taskStatus)) {
            if (task.has("pointBallId")) {
              jo = new JSONObject(AntMemberRpcCall.ballReceive(task.getString("pointBallId")));
              if (ResChecker.checkRes(TAG, jo)) {
                Log.other("商家服务🏬[" + title + "]#领取积分" + reward);
              }
            }
          } else if ("PROCESSING".equals(taskStatus) || "UNRECEIVED".equals(taskStatus)) {
            if (task.has("extendLog")) {
              JSONObject bizExtMap = task.getJSONObject("extendLog").getJSONObject("bizExtMap");
              jo = new JSONObject(AntMemberRpcCall.taskFinish(bizExtMap.getString("bizId")));
              if (ResChecker.checkRes(TAG, jo)) {
                Log.other("商家服务🏬[" + title + "]#领取积分" + reward);
              }
              doubleCheck = true;
            } else {
              String taskCode = task.getString("taskCode");
              switch (taskCode) {
                case "SYH_CPC_DYNAMIC":
                  // 逛一逛商品橱窗
                  taskReceive(taskCode, "SYH_CPC_DYNAMIC_VIEWED", title);
                  break;
                case "JFLLRW_TASK":
                  // 逛一逛得缴费红包
                  taskReceive(taskCode, "JFLL_VIEWED", title);
                  break;
                case "ZFBHYLLRW_TASK":
                  // 逛一逛支付宝会员
                  taskReceive(taskCode, "ZFBHYLL_VIEWED", title);
                  break;
                case "QQKLLRW_TASK":
                  // 逛一逛支付宝亲情卡
                  taskReceive(taskCode, "QQKLL_VIEWED", title);
                  break;
                case "SSLLRW_TASK":
                  // 逛逛领优惠得红包
                  taskReceive(taskCode, "SSLL_VIEWED", title);
                  break;
                case "ELMGYLLRW2_TASK":
                  // 去饿了么果园0元领水果
                  taskReceive(taskCode, "ELMGYLL_VIEWED", title);
                  break;
                case "ZMXYLLRW_TASK":
                  // 去逛逛芝麻攒粒攻略
                  taskReceive(taskCode, "ZMXYLL_VIEWED", title);
                  break;
                case "GXYKPDDYH_TASK":
                  // 逛信用卡频道得优惠
                  taskReceive(taskCode, "xykhkzd_VIEWED", title);
                  break;
                case "HHKLLRW_TASK":
                  // 49999元花呗红包集卡抽
                  taskReceive(taskCode, "HHKLLX_VIEWED", title);
                  break;
                case "TBNCLLRW_TASK":
                  // 去淘宝芭芭农场领水果百货
                  taskReceive(taskCode, "TBNCLLRW_TASK_VIEWED", title);
                  break;
              }
            }
          }
        }
        if (doubleCheck) {
          doMerchantMoreTask();
        }
      } else {
        Log.runtime(TAG,"taskListQuery err:" + " " + s);
      }
    } catch (Throwable t) {
      Log.runtime(TAG, "taskListQuery err:");
      Log.printStackTrace(TAG, t);
    } finally {
      try {
        GlobalThreadPools.sleepCompat(1000);
      } catch (Exception e) {
        Log.printStackTrace(e);
      }
    }
  }

  /**
   * 完成商家积分任务
   * @param taskCode 任务代码
   * @param actionCode 行为代码
   * @param title 标题
   */
  private static void taskReceive(String taskCode, String actionCode, String title) {
    try {
      String s = AntMemberRpcCall.taskReceive(taskCode);
      JSONObject jo = new JSONObject(s);
      if (ResChecker.checkRes(TAG, jo)) {
        GlobalThreadPools.sleepCompat(500);
        jo = new JSONObject(AntMemberRpcCall.actioncode(actionCode));
        if (ResChecker.checkRes(TAG, jo)) {
          GlobalThreadPools.sleepCompat(16000);
          jo = new JSONObject(AntMemberRpcCall.produce(actionCode));
          if (ResChecker.checkRes(TAG, jo)) {
            Log.other("商家服务🏬[完成任务" + title + "]");
          }
        }
      } else {
        Log.record(TAG,"taskReceive" + " " + s);
      }
    } catch (Throwable t) {
      Log.runtime(TAG, "taskReceive err:");
      Log.printStackTrace(TAG, t);
    }
  }

  /**
   * 保障金领取
   */
  private void collectInsuredGold() {
    try {
      String s = AntMemberRpcCall.queryAvailableCollectInsuredGold();
      GlobalThreadPools.sleepCompat(200);
      JSONObject jo = new JSONObject(s);
      if (!ResChecker.checkRes(TAG, jo)) {
        Log.other(TAG + ".collectInsuredGold.queryInsuredHome", "保障金🏥[响应失败]#" + s);
        return;
      }
      jo = jo.getJSONObject("data");
      JSONObject signInBall = jo.getJSONObject("signInDTO");
      JSONArray otherBallList = jo.getJSONArray("eventToWaitDTOList");
      if (1 == signInBall.getInt("sendFlowStatus") && 1 == signInBall.getInt("sendType")) {
        s = AntMemberRpcCall.collectInsuredGold(signInBall);
        GlobalThreadPools.sleepCompat(2000);
        jo = new JSONObject(s);
        if (!ResChecker.checkRes(TAG, jo)) {
          Log.other(TAG + ".collectInsuredGold.collectInsuredGold", "保障金🏥[响应失败]#" + s);
          return;
        }
        String gainGold = jo.getJSONObject("data").getString("gainSumInsuredYuan");
        Log.other("保障金🏥[领取保证金]#+" + gainGold + "元");
      }
      for (int i = 0; i <otherBallList.length(); i++) {
        JSONObject anotherBall = otherBallList.getJSONObject(i);
        s = AntMemberRpcCall.collectInsuredGold(anotherBall);
        GlobalThreadPools.sleepCompat(2000);
        jo = new JSONObject(s);
        if (!ResChecker.checkRes(TAG, jo)) {
          Log.other(TAG + ".collectInsuredGold.collectInsuredGold", "保障金🏥[响应失败]#" + s);
          return;
        }
        String gainGold = jo.getJSONObject("data").getJSONObject("gainSumInsuredDTO").getString("gainSumInsuredYuan");
        Log.other("保障金🏥[领取保证金]+" + gainGold + "元");
      }
    } catch (Throwable t) {
      Log.printStackTrace(TAG + ".collectInsuredGold", t);
    }
  }

  /**
   * 执行会员任务 类型1
   * @param task 单个任务对象
   */
  private void processTask(JSONObject task) throws JSONException {
    JSONObject taskConfigInfo = task.getJSONObject("taskConfigInfo");
    String name = taskConfigInfo.getString("name");
    long id = taskConfigInfo.getLong("id");
    String awardParamPoint = taskConfigInfo.getJSONObject("awardParam").getString("awardParamPoint");
    String targetBusiness = taskConfigInfo.getJSONArray("targetBusiness").getString(0);
    String[] targetBusinessArray = targetBusiness.split("#");
    if (targetBusinessArray.length < 3) {
      Log.runtime(TAG, "processTask target param err:" + Arrays.toString(targetBusinessArray));
      return;
    }
    String bizType = targetBusinessArray[0];
    String bizSubType = targetBusinessArray[1];
    String bizParam = targetBusinessArray[2];
    GlobalThreadPools.sleepCompat(16000);
    String str = AntMemberRpcCall.executeTask(bizParam, bizSubType, bizType, id);
    JSONObject jo = new JSONObject(str);
    if (!ResChecker.checkRes(TAG + "执行会员任务失败:", jo)) {
      Log.runtime(TAG, "执行任务失败:" + jo.optString("resultDesc"));
      return;
    }
    if (checkMemberTaskFinished(id)) {
      Log.other("会员任务🎖️[" + name + "]#获得积分" + awardParamPoint);
    }
  }

  /**
   * 查询指定会员任务是否完成
   * @param taskId 任务id
   */
  private boolean checkMemberTaskFinished(long taskId) {
    try {
      String str = AntMemberRpcCall.queryAllStatusTaskList();
      GlobalThreadPools.sleepCompat(500);
      JSONObject jsonObject = new JSONObject(str);
      if (!ResChecker.checkRes(TAG + "查询会员任务状态失败:", jsonObject)) {
        Log.error(TAG + ".checkMemberTaskFinished", "会员任务响应失败: " + jsonObject.getString("resultDesc"));
      }
      if (!jsonObject.has("availableTaskList")) {
        return true;
      }
      JSONArray taskList = jsonObject.getJSONArray("availableTaskList");
      for (int i = 0; i < taskList.length(); i++) {
        JSONObject taskConfigInfo = taskList.getJSONObject(i).getJSONObject("taskConfigInfo");
        long id = taskConfigInfo.getLong("id");
        if (taskId == id) {
          return false;
        }
      }
      return true;
    } catch (JSONException e) {
      return false;
    }
  }

  /**
   * 黄金票任务入口 (整合签到和提取)
   * @param doSignIn 是否执行签到
   * @param doConsume 是否执行提取
   */
  private void doGoldTicketTask(boolean doSignIn, boolean doConsume) {
    try {
      Log.record("开始执行黄金票...");

      // 1. 获取首页数据 (签到需要)
      JSONObject homeResult = null;
      if (doSignIn) {
        String homeRes = AntMemberRpcCall.queryWelfareHome();
        if (homeRes != null) {
          JSONObject homeJson = new JSONObject(homeRes);
          if (ResChecker.checkRes(TAG, homeJson)) {
            homeResult = homeJson.optJSONObject("result");
          }
        }
      }

      // 2. 执行签到
      if (doSignIn && homeResult != null) {
        doGoldTicketSignIn(homeResult);
      }

      // 3. 执行提取 (提取功能独立，总是需要调用 queryConsumeHome 获取最新余额)
      if (doConsume) {
        doGoldTicketConsume();
      }

    } catch (Exception e) {
      Log.printStackTrace(TAG, e);
    }
  }

  /**
   * 黄金票签到逻辑 (使用新接口 welfareCenterTrigger)
   */
  private void doGoldTicketSignIn(JSONObject homeResult) {
    try {
      JSONObject signObj = homeResult.optJSONObject("sign");
      if (signObj != null) {
        boolean todayHasSigned = signObj.optBoolean("todayHasSigned", false);
        if (todayHasSigned) {
          Log.record("黄金票🎫[今日已签到]");
        } else {
          Log.record("黄金票🎫[准备签到]");
          // 调用新接口进行签到
          String signRes = AntMemberRpcCall.welfareCenterTrigger("SIGN");
          JSONObject signJson = new JSONObject(signRes);

          if (ResChecker.checkRes(TAG, signJson)) {
            JSONObject signResult = signJson.optJSONObject("result");
            String amount = "";
            if (signResult != null && signResult.has("prize")) {
              amount = signResult.getJSONObject("prize").optString("amount");
            }
            Log.other("黄金票🎫[签到成功]#获得: " + amount);
          }
        }
      }
    } catch (Exception e) {
      Log.printStackTrace(TAG, e);
    }
  }

  /**
   * 黄金票提取逻辑 (使用新接口 queryConsumeHome 和 submitConsume)
   */
  private void doGoldTicketConsume() {
    try {
      Log.record("黄金票🎫[准备检查余额及提取]");

      // 1. 调用新接口 queryConsumeHome 获取最新的资产信息
      String queryRes = AntMemberRpcCall.queryConsumeHome();
      if (queryRes == null) return;
      JSONObject queryJson = new JSONObject(queryRes);
      if (!ResChecker.checkRes(TAG, queryJson)) return;

      JSONObject result = queryJson.optJSONObject("result");
      if (result == null) return;

      // 2. 获取余额
      JSONObject assetInfo = result.optJSONObject("assetInfo");
      if (assetInfo == null) return;

      int availableAmount = assetInfo.optInt("availableAmount", 0);

      // 3. 计算提取数量 (整百提取逻辑)
      int extractAmount = (availableAmount / 100) * 100;

      if (extractAmount < 100) {
        Log.record("黄金票🎫[余额不足] 当前: " + availableAmount + "，最低需100");
        return;
      }

      // 4. 获取必要参数 productId 和 bonusAmount
      String productId = "";
      JSONObject product = result.optJSONObject("product");
      if (product != null) {
        productId = product.optString("productId");
      } else if (result.has("productList") && result.optJSONArray("productList") != null && result.optJSONArray("productList").length() > 0) {
        productId = result.optJSONArray("productList").optJSONObject(0).optString("productId");
      }

      if (productId == null || productId.isEmpty()) {
        Log.record("黄金票🎫[提取异常] 未找到有效的基金ID");
        return;
      }

      int bonusAmount = 0;
      JSONObject bonusInfo = result.optJSONObject("bonusInfo");
      if (bonusInfo != null) {
        bonusAmount = bonusInfo.optInt("bonusAmount", 0);
      }

      // 5. 提交提取
      Log.record("黄金票🎫[开始提取] 计划: " + extractAmount + " 份 (持有: " + availableAmount + ")");
      String submitRes = AntMemberRpcCall.submitConsume(extractAmount, productId, bonusAmount);

      if (submitRes != null) {
        JSONObject submitJson = new JSONObject(submitRes);
        if (ResChecker.checkRes(TAG, submitJson)) {
          JSONObject submitResult = submitJson.optJSONObject("result");
          String writeOffNo = submitResult != null ? submitResult.optString("writeOffNo") : "";

          if (!writeOffNo.isEmpty()) {
            Log.other("黄金票🎫[提取成功]#消耗: " + extractAmount + " 份");
          } else {
            Log.record("黄金票🎫[提取失败] 未返回核销码");
          }
        }
      }

    } catch (Exception e) {
      Log.printStackTrace(TAG, e);
    }
  }

  private void enableGameCenter() {
    try {
      // 1. 查询签到状态并尝试签到
      try {
        String resp = AntMemberRpcCall.querySignInBall();
        JSONObject root = new JSONObject(resp);
        if (!ResChecker.checkRes(TAG, root)) {
          String msg = root.optString("errorMsg", root.optString("resultView", resp));
          Log.record(TAG + ".enableGameCenter.signIn", "游戏中心🎮[签到查询失败]#" + msg);
        } else {
          JSONObject data = root.optJSONObject("data");

          // 情况1：data 为 null 或 空对象 → 默认已经签到过
          if (data == null || data.length() == 0) {
            Log.record(TAG + ".enableGameCenter.signIn", "游戏中心🎮[今日已签到](data为空)");
            return;
          }
          JSONObject signModule = data != null ? data.optJSONObject("signInBallModule") : null;
          boolean signed = signModule != null && signModule.optBoolean("signInStatus", false);
          if (signed) {
            Log.record(TAG + ".enableGameCenter.signIn", "游戏中心🎮[今日已签到]");
          } else {
            String signResp = AntMemberRpcCall.continueSignIn();
            GlobalThreadPools.sleepCompat(300);
            JSONObject signJo = new JSONObject(signResp);
            if (!ResChecker.checkRes(TAG, signJo)) {
              String msg = signJo.optString("errorMsg", signJo.optString("resultView", signResp));
              Log.record(TAG + ".enableGameCenter.signIn", "游戏中心🎮[签到失败]#" + msg);
            } else {
              JSONObject signData = signJo.optJSONObject("data");
              String title = "";
              String desc = "";
              String type = "";
              if (signData != null) {
                JSONObject toast = signData.optJSONObject("autoSignInToastModule");
                if (toast != null) {
                  title = toast.optString("title", "");
                  desc = toast.optString("desc", "");
                  type = toast.optString("type", "");
                }
              }
              boolean toastSuccess = "SUCCESS".equalsIgnoreCase(type)
                      && !title.contains("失败")
                      && !desc.contains("失败");
              if (toastSuccess) {
                StringBuilder sb = new StringBuilder();
                sb.append("游戏中心🎮[每日签到成功]");
                if (!title.isEmpty()) {
                  sb.append("#").append(title);
                }
                if (!desc.isEmpty()) {
                  sb.append("#").append(desc);
                }
                Log.other(sb.toString());
              } else {
                StringBuilder sb = new StringBuilder();
                if (!title.isEmpty()) {
                  sb.append(title);
                }
                if (!desc.isEmpty()) {
                  if (sb.length() > 0) sb.append(" ");
                  sb.append(desc);
                }
                Log.record(TAG + ".enableGameCenter.signIn", "游戏中心🎮[签到失败]#" + (sb.length() > 0 ? sb.toString() : signResp));
              }
            }
          }
        }
      } catch (Throwable th) {
        Log.runtime(TAG, "enableGameCenter.signIn err:");
        Log.printStackTrace(TAG, th);
      }

      // 2. 查询任务列表,完成平台任务
      try {
        String resp = AntMemberRpcCall.queryGameCenterTaskList();
        JSONObject root = new JSONObject(resp);
        if (!ResChecker.checkRes(TAG, root)) {
          String msg = root.optString("errorMsg", root.optString("resultView", resp));
          Log.record(TAG + ".enableGameCenter.tasks", "游戏中心🎮[任务列表查询失败]#" + msg);
        } else {
          JSONObject data = root.optJSONObject("data");
          if (data != null) {
            JSONObject platformTaskModule = data.optJSONObject("platformTaskModule");
            if (platformTaskModule != null) {
              JSONArray platformTaskList = platformTaskModule.optJSONArray("platformTaskList");
              if (platformTaskList != null && platformTaskList.length() > 0) {
                int total = 0;
                int finished = 0;
                int failed = 0;
                String lastFailedTaskId = "";
                int lastFailedCount = 0;

                for (int i = 0; i < platformTaskList.length(); i++) {
                  JSONObject task = platformTaskList.optJSONObject(i);
                  if (task == null) continue;

                  String taskId = task.optString("taskId");
                  String status = task.optString("taskStatus");

                  if (taskId.isEmpty()) continue;
                  if (!"NOT_DONE".equals(status) && !"SIGNUP_COMPLETE".equals(status)) {
                    continue;
                  }

                  // 如果是上次失败的任务,计数加1
                  if (taskId.equals(lastFailedTaskId)) {
                    lastFailedCount++;
                    if (lastFailedCount >= 2) {
                      Log.record(TAG + ".enableGameCenter.tasks",
                              "游戏中心🎮任务[" + task.optString("title") + "]连续失败2次,跳过");
                      continue;
                    }
                  } else {
                    // 新任务,重置计数
                    lastFailedTaskId = taskId;
                    lastFailedCount = 0;
                  }

                  total++;
                  String title = task.optString("title");
                  String subTitle = task.optString("subTitle");
                  boolean needSignUp = task.optBoolean("needSignUp", false);
                  int pointAmount = task.optInt("pointAmount", 0);

                  try {
                    // needSignUp 为 true 且是首次状态 NOT_DONE:先报名
                    if (needSignUp && "NOT_DONE".equals(status)) {
                      String signUpResp = AntMemberRpcCall.doTaskSignup(taskId);
                      GlobalThreadPools.sleepCompat(300);
                      JSONObject signUpJo = new JSONObject(signUpResp);
                      if (!ResChecker.checkRes(TAG, signUpJo)) {
                        String msg = signUpJo.optString("errorMsg", signUpJo.optString("resultView", signUpResp));
                        Log.record(TAG + ".enableGameCenter.tasks", "游戏中心🎮任务[" + title + "]报名失败#" + msg);
                        failed++;
                        continue;
                      }
                    }

                    // 完成任务
                    String doResp = AntMemberRpcCall.doTaskSend(taskId);
                    GlobalThreadPools.sleepCompat(300);
                    JSONObject doJo = new JSONObject(doResp);

                    if (ResChecker.checkRes(TAG, doJo)) {
                      // 检查返回的任务状态
                      JSONObject doData = doJo.optJSONObject("data");
                      String resultStatus = doData != null ? doData.optString("taskStatus", "") : "";

                      if ("SIGNUP_COMPLETE".equals(resultStatus) || "NOT_DONE".equals(resultStatus)) {
                        // 状态未变更,记为失败
                        Log.record(TAG + ".enableGameCenter.tasks",
                                "游戏中心🎮任务[" + title + "]状态未变更,可能无法完成");
                        failed++;
                      } else {
                        // 真正完成,重置失败计数
                        Log.other("游戏中心🎮任务[" + (subTitle.isEmpty() ? title : subTitle) + "]#完成,奖励" +
                                pointAmount + "玩乐豆" + (needSignUp ? "(签到任务)" : ""));
                        finished++;
                        lastFailedTaskId = "";
                        lastFailedCount = 0;
                      }
                    } else {
                      String msg = doJo.optString("errorMsg", doJo.optString("resultView", doResp));
                      Log.record(TAG + ".enableGameCenter.tasks",
                              "游戏中心🎮任务[" + title + "]完成失败#" + msg);
                      failed++;
                    }
                  } catch (Throwable e) {
                    Log.printStackTrace(TAG + ".enableGameCenter.tasks.doTask", e);
                    failed++;
                  }
                }

                if (total > 0) {
                  Log.record(TAG + ".enableGameCenter.tasks",
                          "游戏中心🎮[平台任务处理完成]#待做:" + total + " 完成:" + finished + " 失败:" + failed);
                } else {
                  Log.record(TAG + ".enableGameCenter.tasks", "游戏中心🎮[无待处理的平台任务]");
                }
              } else {
                Log.record(TAG + ".enableGameCenter.tasks", "游戏中心🎮[平台任务列表为空]");
              }
            }
          }
        }
      } catch (Throwable th) {
        Log.runtime(TAG, "enableGameCenter.tasks err:");
        Log.printStackTrace(TAG, th);
      }

      // 3. 查询待收乐豆并使用一键收取接口
      try {
        String resp = AntMemberRpcCall.queryPointBallList();
        JSONObject root = new JSONObject(resp);
        if (!ResChecker.checkRes(TAG, root)) {
          String msg = root.optString("errorMsg", root.optString("resultView", resp));
          Log.record(TAG + ".enableGameCenter.point", "游戏中心🎮[查询待收乐豆失败]#" + msg);
        } else {
          JSONObject data = root.optJSONObject("data");
          JSONArray pointBallList = data != null ? data.optJSONArray("pointBallList") : null;
          if (pointBallList == null || pointBallList.length() == 0) {
            Log.record(TAG + ".enableGameCenter.point", "游戏中心🎮[暂无可领取乐豆]");
          } else {
            String batchResp = AntMemberRpcCall.batchReceivePointBall();
            GlobalThreadPools.sleepCompat(300);
            JSONObject batchJo = new JSONObject(batchResp);
            if (ResChecker.checkRes(TAG, batchJo)) {
              JSONObject batchData = batchJo.optJSONObject("data");
              int receiveAmount = batchData != null ? batchData.optInt("receiveAmount", 0) : 0;
              int totalAmount = batchData != null ? batchData.optInt("totalAmount", receiveAmount) : receiveAmount;
              if (receiveAmount > 0) {
                Log.other("游戏中心🎮[一键领取乐豆成功]#本次领取" + receiveAmount + " | 当前累计" + totalAmount + "玩乐豆");
              } else {
                Log.record(TAG + ".enableGameCenter.point", "游戏中心🎮[暂无可领取乐豆]");
              }
            } else {
              String msg = batchJo.optString("errorMsg", batchJo.optString("resultView", batchResp));
              Log.record(TAG + ".enableGameCenter.point", "游戏中心🎮[一键领取乐豆失败]#" + msg);
            }
          }
        }
      } catch (Throwable th) {
        Log.runtime(TAG, "enableGameCenter.point err:");
        Log.printStackTrace(TAG, th);
      }

    } catch (Throwable t) {
      Log.printStackTrace(TAG, t);
    }
  }

  private void beanSignIn() {
    try {
      try {
        String signInProcessStr = AntMemberRpcCall.querySignInProcess("AP16242232", "INS_BLUE_BEAN_SIGN");

        JSONObject jo = new JSONObject(signInProcessStr);
        if (!ResChecker.checkRes(TAG, jo)) {
          Log.runtime(jo.toString());
          return;
        }

        if (jo.getJSONObject("result").getBoolean("canPush")) {
          String signInTriggerStr = AntMemberRpcCall.signInTrigger("AP16242232", "INS_BLUE_BEAN_SIGN");

          jo = new JSONObject(signInTriggerStr);
          if (ResChecker.checkRes(TAG, jo)) {
            String prizeName = jo.getJSONObject("result").getJSONArray("prizeSendOrderDTOList").getJSONObject(0).getString("prizeName");
            Log.record(TAG,"安心豆🫘[" + prizeName + "]");
          } else {
            Log.runtime(jo.toString());
          }
        }
      } catch (NullPointerException e) {
        Log.error(TAG, "安心豆🫘[RPC桥接失败]#可能是RpcBridge未初始化");
        Log.printStackTrace(TAG, e);
      }
    } catch (Throwable t) {
      Log.runtime(TAG, "beanSignIn err:");
      Log.printStackTrace(TAG, t);
    }
  }

  private void beanExchangeBubbleBoost() {
    try {
      // 检查RPC调用是否可用
      try {
        String accountInfo = AntMemberRpcCall.queryUserAccountInfo("INS_BLUE_BEAN");

        JSONObject jo = new JSONObject(accountInfo);
        if (!ResChecker.checkRes(TAG, jo)) {
          Log.runtime(jo.toString());
          return;
        }

        int userCurrentPoint = jo.getJSONObject("result").getInt("userCurrentPoint");

        // 检查beanExchangeDetail调用
        String exchangeDetailStr = AntMemberRpcCall.beanExchangeDetail("IT20230214000700069722");

        jo = new JSONObject(exchangeDetailStr);
        if (!ResChecker.checkRes(TAG, jo)) {
          Log.runtime(jo.toString());
          return;
        }

        jo = jo.getJSONObject("result").getJSONObject("rspContext").getJSONObject("params").getJSONObject("exchangeDetail");
        String itemId = jo.getString("itemId");
        String itemName = jo.getString("itemName");
        jo = jo.getJSONObject("itemExchangeConsultDTO");
        int realConsumePointAmount = jo.getInt("realConsumePointAmount");

        if (!jo.getBoolean("canExchange") || realConsumePointAmount > userCurrentPoint) {
          return;
        }

        String exchangeResult = AntMemberRpcCall.beanExchange(itemId, realConsumePointAmount);

        jo = new JSONObject(exchangeResult);
        if (ResChecker.checkRes(TAG, jo)) {
          Log.record(TAG,"安心豆🫘[兑换:" + itemName + "]");
        } else {
          Log.runtime(jo.toString());
        }
      } catch (NullPointerException e) {
        Log.error(TAG, "安心豆🫘[RPC桥接失败]#可能是RpcBridge未初始化");
        Log.printStackTrace(TAG, e);
      }
    } catch (Throwable t) {
      Log.runtime(TAG, "beanExchangeBubbleBoost err:");
      Log.printStackTrace(TAG, t);
    }
  }

  /**
   * 芝麻炼金
   */
  private void doSesameAlchemy() {
    try {
      Log.record(TAG, "开始执行芝麻炼金⚗️");

      // ================= Step 1: 自动炼金 (消耗芝麻粒升级) =================
      String homeRes = AntMemberRpcCall.Zmxy.Alchemy.alchemyQueryHome();
      JSONObject homeJo = new JSONObject(homeRes);
      if (ResChecker.checkRes(TAG, homeJo)) {
        JSONObject data = homeJo.optJSONObject("data");
        if (data != null) {
          int zmlBalance = data.optInt("zmlBalance", 0);      // 当前芝麻粒
          int cost = data.optInt("alchemyCostZml", 5);        // 单次消耗
          boolean capReached = data.optBoolean("capReached", false); // 是否达到上限
          int currentLevel = data.optInt("currentLevel", 0);

          // 循环炼金逻辑
          while (zmlBalance >= cost && !capReached) {
            GlobalThreadPools.sleepCompat(1500);
            String alchemyRes = AntMemberRpcCall.Zmxy.Alchemy.alchemyExecute();
            JSONObject alchemyJo = new JSONObject(alchemyRes);

            if (ResChecker.checkRes(TAG, alchemyJo)) {
              JSONObject alData = alchemyJo.optJSONObject("data");
              if (alData != null) {
                boolean levelUp = alData.optBoolean("levelUp", false);
                boolean levelFull = alData.optBoolean("levelFull", false);
                int goldNum = alData.optInt("goldNum", 0);


                if (levelUp) currentLevel++;
                if (levelFull) capReached = true;

                Log.other(
                        "芝麻炼金⚗️[炼金成功]"
                                + "#消耗" + cost + "粒"
                                + " | 获得" + goldNum + "金"
                                + " | 当前等级Lv." + currentLevel
                                + (levelUp ? "（升级🎉）" : "")
                                + (levelFull ? "（满级🏆）" : "")
                );
                zmlBalance -= cost;
              } else {
                break;
              }
            } else {
              Log.record(TAG, "芝麻炼金失败: " + alchemyJo.optString("resultView"));
              break;
            }
          }
        }
      } else {
        Log.record(TAG, "芝麻炼金首页查询失败");
      }

      // ================= Step 2: 自动签到 & 时段奖励 =================
      String checkInRes = AntMemberRpcCall.Zmxy.Alchemy.alchemyQueryCheckIn("alchemy");
      JSONObject checkInJo = new JSONObject(checkInRes);
      if (ResChecker.checkRes(TAG, checkInJo)) {
        JSONObject data = checkInJo.optJSONObject("data");
        if (data != null) {
          JSONObject currentDay = data.optJSONObject("currentDateCheckInTaskVO");
          if (currentDay != null) {
            String status = currentDay.optString("status");
            String checkInDate = currentDay.optString("checkInDate");
            if ("CAN_COMPLETE".equals(status) && !checkInDate.isEmpty()) {
              // 炼金签到
              String completeRes = AntMemberRpcCall.zmCheckInCompleteTask(checkInDate, "alchemy");
              try {
                JSONObject completeJo = new JSONObject(completeRes);
                if (ResChecker.checkRes(TAG, completeJo)) {
                  JSONObject prize = completeJo.optJSONObject("data");
                  int num = prize != null ? prize.optInt("zmlNum",
                          prize.optJSONObject("prize") != null ?
                                  prize.optJSONObject("prize").optInt("num", 0) : 0) : 0;
                  Log.other("芝麻炼金⚗️[每日签到成功]#获得" + num + "粒");
                } else {
                  Log.runtime(TAG + ".doSesameAlchemy", "炼金签到失败:" + completeRes);
                }
              } catch (Throwable e) {
                Log.printStackTrace(TAG + ".doSesameAlchemy.alchemyCheckInComplete", e);
              }
            } // status 为 COMPLETED 时不再重复签到
          }
        }
      }

      // 1. 查询时段任务
      String queryRespStr = AntMemberRpcCall.Zmxy.Alchemy.alchemyQueryTimeLimitedTask();
      Log.record(TAG, "芝麻炼金⚗️[检查时段奖励]");

      JSONObject queryResp = new JSONObject(queryRespStr);
      if (!ResChecker.checkRes(TAG + "查询时段任务失败:", queryResp)
              || !ResChecker.checkRes(TAG, queryResp)
              || queryResp.optJSONObject("data") == null) {
        Log.error(TAG, "芝麻炼金⚗️[检查时段奖励错误] alchemyQueryTimeLimitedTask raw=" + queryResp);
        return;
      }

      JSONObject timeLimitedTaskVO = queryResp.getJSONObject("data").optJSONObject("timeLimitedTaskVO");
      if (timeLimitedTaskVO == null) {
        Log.record(TAG, "芝麻炼金⚗️[当前没有时段奖励任务]");
        return;
      }

      // 2. 获取任务信息
      String taskName = timeLimitedTaskVO.optString("longTitle", "未知任务");
      String templateId = timeLimitedTaskVO.getString("templateId"); // 动态获取
      int state = timeLimitedTaskVO.optInt("state", 0); // 1: 可领取, 2: 未到时间
      boolean tomorrow = timeLimitedTaskVO.optBoolean("tomorrow", false);
      int rewardAmount = timeLimitedTaskVO.optInt("rewardAmount", 0);

      Log.record(TAG, "芝麻炼金⚗️[任务检查] 任务=" + taskName + " 状态=" + state + " 奖励=" + rewardAmount + " 明天=" + tomorrow);

      // 3. 如果是明天任务，跳过
      if (tomorrow) {
        Log.record(TAG, "芝麻炼金⚗️[任务跳过] 任务=" + taskName + " 是明天的奖励");
        return;
      }

      // 4. 如果状态是可领取，则领取奖励
      if (state == 1) { // 可领取
        Log.record(TAG, "芝麻炼金⚗️[开始领取任务奖励] 任务=" + taskName);

        String collectRespStr = AntMemberRpcCall.Zmxy.Alchemy.alchemyCompleteTimeLimitedTask(templateId);
        JSONObject collectResp = new JSONObject(collectRespStr);

        if (!ResChecker.checkRes(TAG, collectResp) || collectResp.optJSONObject("data") == null) {
          Log.error(TAG, "领取任务奖励失败 raw=" + collectResp);
        } else {
          JSONObject data = collectResp.getJSONObject("data");
          int zmlNum = data.optInt("zmlNum", 0);
          String toast = data.optString("toast", "");
          Log.record(TAG, "芝麻炼金⚗️[领取成功] 获得芝麻粒=" + zmlNum + " 提示=" + toast);
        }
      } else { // 其他状态
        Log.record(TAG, "芝麻炼金⚗️[当前不可领取] 任务=" + taskName);
      }


      // ================= Step 3: 自动做任务 =================
      Log.record(TAG, "芝麻炼金⚗️[开始扫描任务列表]");
      String listRes = AntMemberRpcCall.Zmxy.Alchemy.alchemyQueryListV3();
      JSONObject listJo = new JSONObject(listRes);

      if (ResChecker.checkRes(TAG, listJo)) {
        JSONObject data = listJo.optJSONObject("data");
        if (data != null) {
          JSONArray toComplete = data.optJSONArray("toCompleteVOS");
          if (toComplete != null) {
            processAlchemyTasks(toComplete);
          }
          JSONObject dailyTaskVO = data.optJSONObject("dailyTaskListVO");
          if (dailyTaskVO != null) {
            processAlchemyTasks(dailyTaskVO.optJSONArray("waitJoinTaskVOS"));
            processAlchemyTasks(dailyTaskVO.optJSONArray("waitCompleteTaskVOS"));
          }
        }
      }

      // ================= Step 4: [新增] 任务完成后一键收取芝麻粒 =================
      Log.record(TAG, "芝麻炼金⚗️[任务处理完毕，准备收取芝麻粒]");
      GlobalThreadPools.sleepCompat(2000); // 稍作等待，确保任务奖励到账

      // 4.1 查询是否有可收取的芝麻粒
      String queryFeedbackRes = AntMemberRpcCall.queryCreditFeedback();
      JSONObject feedbackJo = new JSONObject(queryFeedbackRes);
      if (ResChecker.checkRes(TAG, feedbackJo)) {
        JSONArray feedbackList = feedbackJo.optJSONArray("creditFeedbackVOS");
        if (feedbackList != null && feedbackList.length() > 0) {
          Log.record(TAG, "芝麻炼金⚗️[发现" + feedbackList.length() + "个待收取项，执行一键收取]");

          // 4.2 执行一键收取
          String collectRes = AntMemberRpcCall.collectAllCreditFeedback();
          JSONObject collectJo = new JSONObject(collectRes);
          if (ResChecker.checkRes(TAG, collectJo)) {
            Log.other("芝麻炼金⚗️[一键收取成功]#收割完毕");
          } else {
            Log.record(TAG, "芝麻炼金⚗️[一键收取失败]#" + collectJo.optString("resultView"));
          }
        } else {
          Log.record(TAG, "芝麻炼金⚗️[当前无待收取芝麻粒]");
        }
      }

    } catch (Throwable t) {
      Log.printStackTrace(TAG + ".doSesameAlchemy", t);
    }
  }

  private void processAlchemyTasks(JSONArray taskList) throws JSONException {
    if (taskList == null || taskList.length() == 0) return;

    // 黑名单：确实做不了或需要其它 App 配合的任务
    String[] blackList = {
            "每日施肥",
            "芝麻租赁",
            "休闲小游戏",
            "AQApp",
            "订阅炼金",
            "租游戏账号",
            "芝麻大表鸽",
            "坚持签到"
    };

    for (int i = 0; i < taskList.length(); i++) {
      JSONObject task = taskList.getJSONObject(i);
      String title = task.optString("title");
      String templateId = task.optString("templateId");
      boolean finishFlag = task.optBoolean("finishFlag", false);
      String bizType = task.optString("bizType", "");

      if (finishFlag) continue;

      // 黑名单检查
      boolean isBlack = false;
      for (String blackKey : blackList) {
        if (title.contains(blackKey)) {
          isBlack = true;
          break;
        }
      }
      if (isBlack) {
        Log.record(TAG, "跳过黑名单任务: " + title);
        continue;
      }

      // 特殊处理：广告浏览任务（逛15秒商品橱窗 / 浏览15秒视频广告 等）
      // 这类任务没有有效 templateId，需要用 logExtMap.bizId 走 com.alipay.adtask.biz.mobilegw.service.task.finish
      if ("AD_TASK".equals(bizType)) {
        JSONObject logExtMap = task.optJSONObject("logExtMap");
        if (logExtMap == null) {
          Log.record(TAG, "芝麻炼金广告任务缺少logExtMap, 跳过: " + title);
          continue;
        }
        String bizId = logExtMap.optString("bizId", "");
        if (bizId.isEmpty()) {
          Log.record(TAG, "芝麻炼金广告任务缺少bizId, 跳过: " + title);
          continue;
        }

        Log.record(TAG, "芝麻炼金广告任务: " + title + " 准备执行");//(bizId=" + bizId + ")

        int sleepTime = 8000;
        if (title.contains("15秒") || title.contains("15s")) {
          // 抓包规则里写明“每次浏览不少于15秒”
          sleepTime = 10000;
        }
        GlobalThreadPools.sleepCompat(sleepTime);

        try {
          String adFinishRes = AntMemberRpcCall.taskFinish(bizId);
          JSONObject adFinishJo = new JSONObject(adFinishRes);
          // 兼容返回中只有 errCode=0 的情况
          if (ResChecker.checkRes(TAG, adFinishJo) || "0".equals(adFinishJo.optString("errCode"))) {
            int reward = task.optInt("rewardAmount", 0);
            Log.other("芝麻炼金⚗️[广告任务完成: " + title + "]#获得" + reward + "粒");
          } else {
            Log.record(TAG, "芝麻炼金广告任务上报失败: " + title + " - " + adFinishRes);
          }
        } catch (Throwable e) {
          Log.printStackTrace(TAG + ".processAlchemyTasks.adTask", e);
        }
        // 广告任务不再走 templateId / recordId 这套逻辑
        continue;
      }

      // 普通任务：仍然使用模板+recordId 的 Promise 流程
      if (templateId.contains("invite") || templateId.contains("upload")
              || templateId.contains("auth") || templateId.contains("banli")) {
        continue;
      }
      String actionUrl = task.optString("actionUrl", "");
      if (actionUrl.startsWith("alipays://") && !actionUrl.contains("chInfo")) {
        // 需要外部 App，无法仅靠 hook 完成
        continue;
      }

      Log.record(TAG, "芝麻炼金任务: " + title + " 准备执行");

      String recordId = task.optString("recordId", "");

      if (recordId.isEmpty()) {
        // templateId 为空或无效时，直接跳过，避免 "参数[templateId]不是有效的入参"
        if (templateId == null || templateId.trim().isEmpty()) {
          Log.record(TAG, "芝麻炼金任务: 模板为空，跳过 " + title);
          continue;
        }
        String joinRes = AntMemberRpcCall.joinSesameTask(templateId);
        JSONObject joinJo = new JSONObject(joinRes);
        if (ResChecker.checkRes(TAG, joinJo)) {
          JSONObject joinData = joinJo.optJSONObject("data");
          if (joinData != null) {
            recordId = joinData.optString("recordId");
          }
          Log.record(TAG, "任务领取成功: " + title);
          GlobalThreadPools.sleepCompat(1000);
        } else {
          Log.record(TAG, "任务领取失败: " + title + " - " + joinJo.optString("resultView", joinRes));
          continue;
        }
      }

      AntMemberRpcCall.feedBackSesameTask(templateId);

      int sleepTime = 3000;
      if (title.contains("浏览") || title.contains("逛")) {
        sleepTime = 15000;
      }
      GlobalThreadPools.sleepCompat(sleepTime);

      if (!recordId.isEmpty()) {
        String finishRes = AntMemberRpcCall.finishSesameTask(recordId);
        JSONObject finishJo = new JSONObject(finishRes);
        if (ResChecker.checkRes(TAG, finishJo)) {
          int reward = task.optInt("rewardAmount", 0);
          Log.other("芝麻炼金⚗️[任务完成: " + title + "]#获得" + reward + "粒");
        } else {
          Log.record(TAG, "任务提交失败: " + title + " - " + finishJo.optString("resultView", finishRes));
        }
      }
      GlobalThreadPools.sleepCompat(2000);
    }
  }
  private void doZhimaTree() {
    try {
      // 1. 执行首页的所有任务 (包括浏览任务和复访任务)
      doHomeTasks();

      // 2. 执行常规列表任务 (赚净化值列表)
      doRentGreenTasks();

      // 3. 消耗净化值进行净化
      doPurification();

    } catch (Throwable t) {
      Log.printStackTrace(TAG, t);
    }
  }

  /**
   * 处理首页返回的任务 (含浏览任务和状态列表任务)
   */
  private void doHomeTasks() {
    try {
      String res = AntMemberRpcCall.zhimaTreeHomePage();
      if (res == null) return;

      JSONObject json = new JSONObject(res);
      if (ResChecker.checkRes(TAG, json)) {
        JSONObject result = json.optJSONObject("extInfo");
        if (result == null) return;
        JSONObject queryResult = result.optJSONObject("zhimaTreeHomePageQueryResult");
        if (queryResult == null) return;

        // 1. 处理 browseTaskList (如：芝麻树首页每日_浏览任务)
        JSONArray browseList = queryResult.optJSONArray("browseTaskList");
        if (browseList != null) {
          for (int i = 0; i < browseList.length(); i++) {
            processSingleTask(browseList.getJSONObject(i));
          }
        }

        // 2. 处理 taskStatusList (如：芝麻树复访任务70净化值)
        JSONArray statusList = queryResult.optJSONArray("taskStatusList");
        if (statusList != null) {
          for (int i = 0; i < statusList.length(); i++) {
            processSingleTask(statusList.getJSONObject(i));
          }
        }
      }
    } catch (Exception e) {
      Log.printStackTrace(TAG, e);
    }
  }

  /**
   * 处理赚净化值列表任务
   */
  private void doRentGreenTasks() {
    try {
      String res = AntMemberRpcCall.queryRentGreenTaskList();
      if (res == null) return;

      JSONObject json = new JSONObject(res);
      if (ResChecker.checkRes(TAG, json)) {
        JSONObject extInfo = json.optJSONObject("extInfo");
        if (extInfo == null) return;

        JSONObject taskDetailListObj = extInfo.optJSONObject("taskDetailList");
        if (taskDetailListObj == null) return;

        JSONArray tasks = taskDetailListObj.optJSONArray("taskDetailList");
        if (tasks == null) return;

        for (int i = 0; i < tasks.length(); i++) {
          processSingleTask(tasks.getJSONObject(i));
        }
      }
    } catch (Exception e) {
      Log.printStackTrace(TAG, e);
    }
  }

  /**
   * 处理单个任务对象的逻辑
   */
  private void processSingleTask(JSONObject task) {
    try {
      JSONObject taskBaseInfo = task.optJSONObject("taskBaseInfo");
      if (taskBaseInfo == null) return;

      String taskId = taskBaseInfo.optString("appletId");
      // 有些任务ID在taskId字段，有些在appletId，做个兼容
      if (taskId == null || taskId.isEmpty()) {
        taskId = task.optString("taskId");
      }

      String title = taskBaseInfo.optString("appletName");
      if (title.isEmpty()) title = taskBaseInfo.optString("title", taskId);

      String status = task.optString("taskProcessStatus");

      // 过滤掉明显无法自动完成的任务（如包含邀请、下单、开通），但保留复访任务
      if (title.contains("邀请") || title.contains("下单") || title.contains("开通")) {
        return;
      }

      // 解析奖励信息
      String prizeName = getPrizeName(task);

      if ("NOT_DONE".equals(status) || "SIGNUP_COMPLETE".equals(status)) {
        // SIGNUP_COMPLETE 通常表示已报名但未做，或者对于复访任务表示可以去完成
        Log.record("芝麻树🌳[开始任务] " + title + (prizeName.isEmpty() ? "" : " (" + prizeName + ")"));
        if (performTask(taskId, title, prizeName)) {
          // 任务完成
        }
      } else if ("TO_RECEIVE".equals(status)) {
        // 待领取状态
        if (doTaskAction(taskId, "receive")) {
          String logMsg = "芝麻树🌳[领取奖励] " + title + " #" + (prizeName.isEmpty() ? "奖励已领取" : prizeName);
          Log.forest(logMsg); // 输出到 forest
        }
      }
    } catch (Exception e) {
      Log.printStackTrace(TAG, e);
    }
  }

  /**
   * 执行任务动作：去完成 -> 等待 -> 领取
   */
  private boolean performTask(String taskId, String title, String prizeName) {
    try {
      // 发送"去完成"指令
      if (doTaskAction(taskId, "send")) {
        int waitTime = 16000; // 默认等待16秒，覆盖大多数浏览任务

        try {
          Thread.sleep(waitTime);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }

        // 发送"领取"指令
        if (doTaskAction(taskId, "receive")) {
          String logMsg = "芝麻树🌳[完成任务] " + title + " #" + (prizeName.isEmpty() ? "奖励已领取" : prizeName);
          Log.forest(logMsg); // 这里输出到 forest
          return true;
        }
      }
    } catch (Exception e) {
      Log.printStackTrace(TAG, e);
    }
    return false;
  }

  /**
   * 获取任务奖励名称
   */
  private String getPrizeName(JSONObject task) {
    String prizeName = "";
    try {
      JSONArray prizes = task.optJSONArray("validPrizeDetailDTO");
      if (prizes == null || prizes.length() == 0) {
        prizes = task.optJSONArray("prizeDetailDTOList");
      }

      if (prizes != null && prizes.length() > 0) {
        JSONObject prizeBase = prizes.getJSONObject(0).optJSONObject("prizeBaseInfoDTO");
        if (prizeBase != null) {
          String rawName = prizeBase.optString("prizeName", "");

          if (rawName.contains("能量")) {
            Pattern p = Pattern.compile("(森林)?能量(\\d+g?)");
            Matcher m = p.matcher(rawName);
            if (m.find()) {
              prizeName = m.group(0);
            } else {
              prizeName = rawName;
            }
          } else if (rawName.contains("净化值")) {
            Pattern p = Pattern.compile("(\\d+净化值|净化值\\d+)");
            Matcher m = p.matcher(rawName);
            if (m.find()) {
              prizeName = m.group(1);
            } else {
              prizeName = rawName;
            }
          } else {
            prizeName = rawName;
          }
        }
      }

      // 如果没找到 PrizeDTO，尝试从 taskExtProps 解析
      if (prizeName.isEmpty()) {
        JSONObject taskExtProps = task.optJSONObject("taskExtProps");
        if (taskExtProps != null && taskExtProps.has("TASK_MORPHO_DETAIL")) {
          JSONObject detail = new JSONObject(taskExtProps.getString("TASK_MORPHO_DETAIL"));
          String val = detail.optString("finishOneTaskGetPurificationValue", "");
          if (!val.isEmpty() && !"0".equals(val)) {
            prizeName = val + "净化值";
          }
        }
      }
    } catch (Exception ignore) {}
    return prizeName;
  }

  private boolean doTaskAction(String taskId, String stageCode) {
    try {
      String s = AntMemberRpcCall.rentGreenTaskFinish(taskId, stageCode);
      if (s == null) return false;
      JSONObject json = new JSONObject(s);
      return ResChecker.checkRes(TAG, json);
    } catch (Exception e) {
      Log.printStackTrace(TAG, e);
      return false;
    }
  }

  /**
   * 净化逻辑
   */
  private void doPurification() {
    try {
      String homeRes = AntMemberRpcCall.zhimaTreeHomePage();
      if (homeRes == null) return;

      JSONObject homeJson = new JSONObject(homeRes);
      if (!ResChecker.checkRes(TAG, homeJson)) return;

      JSONObject result = homeJson.optJSONObject("extInfo")
              .optJSONObject("zhimaTreeHomePageQueryResult");
      if (result == null) return;

      // 获取净化分数（兼容 currentCleanNum）
      int score = result.optInt("purificationScore", result.optInt("currentCleanNum", 0));
      String treeCode = "ZHIMA_TREE";

      // 尝试获取 remainPurificationClickNum（新逻辑）
      int clicks = score / 100; // 默认兜底：按分数计算
      if (result.has("trees") && result.getJSONArray("trees").length() > 0) {
        JSONObject tree = result.getJSONArray("trees").getJSONObject(0);
        treeCode = tree.optString("treeCode", "ZHIMA_TREE");
        // 若服务端明确提供剩余点击次数，则优先使用
        if (tree.has("remainPurificationClickNum")) {
          clicks = Math.max(0, tree.optInt("remainPurificationClickNum", clicks));
        }
      }

      if (clicks <= 0) {
        Log.record("芝麻树🌳[无需净化] 净化值不足（当前: " + score + "g，可点击: " + clicks + "次）");
        return;
      }

      Log.forest("芝麻树🌳[开始净化] 可点击 " + clicks + " 次");

      for (int i = 0; i < clicks; i++) {
        String res = AntMemberRpcCall.zhimaTreeCleanAndPush(treeCode);
        if (res == null) break;

        JSONObject json = new JSONObject(res);
        if (!ResChecker.checkRes(TAG, json)) break;

        JSONObject ext = json.optJSONObject("extInfo");
        if (ext == null) continue;

        // 优先从标准路径取分数
        int newScore = ext.optJSONObject("zhimaTreeCleanAndPushResult")
                .optInt("purificationScore", -1);
        // 兼容旧结构：直接在 extInfo 顶层
        if (newScore == -1) {
          newScore = ext.optInt("purificationScore", score - (i + 1) * 100);
        }

        int growth = ext.optJSONObject("zhimaTreeCleanAndPushResult")
                .optJSONObject("currentTreeInfo")
                .optInt("scoreSummary", -1);

        String log = "芝麻树🌳[净化成功] 第 " + (i + 1) + " 次 | 剩余: " + newScore + "g";
        if (growth != -1) log += " | 成长值: " + growth;
        Log.forest(log + " ✅");

        Thread.sleep(1500);
      }

    } catch (Exception e) {
      Log.printStackTrace(TAG, e);
    }
  }
}

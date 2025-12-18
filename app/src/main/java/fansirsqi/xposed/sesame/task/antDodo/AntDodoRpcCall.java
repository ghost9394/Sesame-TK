package fansirsqi.xposed.sesame.task.antDodo;

import fansirsqi.xposed.sesame.hook.RequestManager;
import fansirsqi.xposed.sesame.util.RandomUtil;

public class AntDodoRpcCall {
    /* 神奇物种 */
    public static String queryAnimalStatus() {
        return RequestManager.requestString("alipay.antdodo.rpc.h5.queryAnimalStatus",
                "[{\"source\":\"chInfo_ch_appcenter__chsub_9patch\"}]");
    }

    public static String homePage() {
        return RequestManager.requestString("alipay.antdodo.rpc.h5.homePage",
                "[{}]");
    }

    public static String collect() {
        return RequestManager.requestString("alipay.antdodo.rpc.h5.collect",
                "[{}]");
    }

    public static String taskList() {
        return RequestManager.requestString("alipay.antdodo.rpc.h5.taskList",
                "[{}]");
    }

    public static String finishTask(String sceneCode, String taskType) {
        String uniqueId = getUniqueId();
        return RequestManager.requestString("com.alipay.antiep.finishTask",
                "[{\"outBizNo\":\"" + uniqueId + "\",\"requestType\":\"rpc\",\"sceneCode\":\""
                        + sceneCode + "\",\"source\":\"af-biodiversity\",\"taskType\":\""
                        + taskType + "\",\"uniqueId\":\"" + uniqueId + "\"}]");
    }

    private static String getUniqueId() {
        return String.valueOf(System.currentTimeMillis()) + RandomUtil.nextLong();
    }

    public static String receiveTaskAward(String sceneCode, String taskType) {
        return RequestManager.requestString("com.alipay.antiep.receiveTaskAward",
                "[{\"ignoreLimit\":0,\"requestType\":\"rpc\",\"sceneCode\":\"" + sceneCode
                        + "\",\"source\":\"af-biodiversity\",\"taskType\":\"" + taskType
                        + "\"}]");
    }

    public static String propList() {
        return RequestManager.requestString("alipay.antdodo.rpc.h5.propList",
                "[{}]");
    }

    public static String consumeProp(String propId, String propType) {
        return RequestManager.requestString("alipay.antdodo.rpc.h5.consumeProp",
                "[{\"propId\":\"" + propId + "\",\"propType\":\"" + propType + "\"}]");
    }

    public static String queryBookInfo(String bookId) {
        return RequestManager.requestString("alipay.antdodo.rpc.h5.queryBookInfo",
                "[{\"bookId\":\"" + bookId + "\"}]");
    }

    // 送卡片给好友
    public static String social(String targetAnimalId, String targetUserId) {
        return RequestManager.requestString("alipay.antdodo.rpc.h5.social",
                "[{\"actionCode\":\"GIFT_TO_FRIEND\",\"source\":\"GIFT_TO_FRIEND_FROM_CC\",\"targetAnimalId\":\""
                        + targetAnimalId + "\",\"targetUserId\":\"" + targetUserId
                        + "\",\"triggerTime\":\"" + System.currentTimeMillis() + "\"}]");
    }

    public static String queryFriend() {
        return RequestManager.requestString("alipay.antdodo.rpc.h5.queryFriend",
                "[{\"sceneCode\":\"EXCHANGE\"}]");
    }

    public static String collecttarget(String targetUserId) {
        return RequestManager.requestString("alipay.antdodo.rpc.h5.collect",
                "[{\"targetUserId\":" + targetUserId + "}]");
    }

    public static String queryBookList(int pageSize, String pageStart) {
        StringBuilder argsBuilder = new StringBuilder();
        argsBuilder.append("{\"pageSize\":").append(pageSize).append(",\"v2\":\"true\"");

        if (pageStart != null && !pageStart.isEmpty()) {
            argsBuilder.append(",\"pageStart\":\"").append(pageStart).append("\"");
        }

        argsBuilder.append("}");

        // 注意这里是单个 JSON 对象，不再是数组，如果接口需要数组可以包一下 []
        String args = "[" + argsBuilder.toString() + "]";
        return RequestManager.requestString("alipay.antdodo.rpc.h5.queryBookList", args);
    }

    public static String generateBookMedal(String bookId) {
        String args = "[{\"bookId\":\"" + bookId + "\"}]";
        return RequestManager.requestString("alipay.antdodo.rpc.h5.generateBookMedal", args);
    }
}
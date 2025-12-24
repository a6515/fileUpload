package com.seeyon.apps.pdfCatchButton.utils;

import com.seeyon.ctp.common.log.CtpLogFactory;
import org.apache.commons.logging.Log;
import org.json.JSONObject;

public class JsonParseUtil {
    private static final Log LOGGER = CtpLogFactory.getLog(JsonParseUtil.class);

    /**
     * 通用检查：判断接口业务是否成功 (resultcode = SUC0000)
     */
    public static boolean isPass(String result) {
        try {
            JSONObject rootJson = new JSONObject(result);
            if (rootJson.has("response")) {
                JSONObject responseJson = rootJson.getJSONObject("response");
                if (responseJson.has("head")) {
                    JSONObject head = responseJson.getJSONObject("head");
                    String resultCode = head.getString("resultcode");
                    if ("SUC0000".equals(resultCode)) {
                        // LOGGER.info("接口业务处理成功！resultcode 为 SUC0000。"); // 减少日志刷屏，可注释
                    } else {
                        String errorMsg = head.getString("resultmsg");
                        String fullError = "招商接口业务处理失败！错误码: " + resultCode + ", 错误信息: " + errorMsg;
                        LOGGER.error(fullError);
                        return false;
                    }
                } else {
                    LOGGER.error("招商接口返回格式异常，'response'内缺少'head'字段。");
                    return false;
                }
            } else {
                LOGGER.error("招商接口返回异常格式，缺少'response'字段。");
                return false;
            }

        } catch (Exception e) {
            LOGGER.error("JSON校验异常: " + e);
            return false;
        }
        return true;
    }

    /**
     * 回单任务专用：解析 rtndat
     * 针对结构：body -> asycalhdz1 -> rtndat
     */
    public static String getrtndat(String result){
        String rtndat = "";
        try {
            JSONObject rootJson = new JSONObject(result);
            if (rootJson.has("response")) {
                JSONObject responseJson = rootJson.getJSONObject("response");
                if (responseJson.has("body")) {
                    JSONObject bodyJson = responseJson.getJSONObject("body");

                    if (bodyJson.has("asycalhdz1")) {
                        JSONObject dataNode = bodyJson.getJSONObject("asycalhdz1");
                        rtndat = dataNode.optString("rtndat", "");
                        System.out.println(">>> 工具类提取 rtndat: " + rtndat);
                    } else {
                        System.out.println(">>> [getrtndat] 未找到 asycalhdz1 节点，body: " + bodyJson.toString());
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("解析 rtndat 出错: "+e);
        }
        return rtndat;
    }

    /**
     * 【新增】对账单任务专用：解析 printid
     * 针对结构：body -> printid (优先) 或 taskid
     */
    public static String getPrintId(String result) {
        String handleId = "";
        try {
            // 先检查业务状态码
            if (!isPass(result)) {
                return "";
            }

            JSONObject rootJson = new JSONObject(result);
            if (rootJson.has("response")) {
                JSONObject responseJson = rootJson.getJSONObject("response");
                if (responseJson.has("body")) {
                    JSONObject body = responseJson.getJSONObject("body");

                    // 根据报文，对账单接口直接在 body 下返回 printid
                    if (body.has("printid")) {
                        handleId = body.getString("printid");
                        System.out.println(">>> 工具类提取 printid: " + handleId);
                    }
                    // 兼容逻辑：如果找不到 printid，尝试取 taskid
                    else if (body.has("taskid")) {
                        handleId = body.getString("taskid");
                        System.out.println(">>> 工具类提取 taskid (作为备选): " + handleId);
                    } else {
                        System.out.println(">>> [getPrintId] body 中未找到 printid 或 taskid。Body: " + body.toString());
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("解析 printid 异常: ", e);
        }
        return handleId;
    }

    /**
     * 通用：解析下载链接
     * 针对结构：body -> fileurl 或 zipUrl
     */
    public static String getZIPUrl(String result){
        String zipUrl = "";
        try {
            // 先检查业务状态码
            if (!isPass(result)) {
                return "";
            }

            JSONObject rootJson = new JSONObject(result);
            if (rootJson.has("response")) {
                JSONObject responseJson = rootJson.getJSONObject("response");
                if (responseJson.has("body")){
                    JSONObject body = responseJson.getJSONObject("body");
                    zipUrl = body.optString("fileurl", "");
                    if ("".equals(zipUrl)) {
                        zipUrl = body.optString("zipUrl", "");
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("解析下载链接异常: " + result, e);
        }
        return zipUrl;
    }
}
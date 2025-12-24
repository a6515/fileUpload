package com.seeyon.apps.pdfCatchButton.schedulerTask;
import com.seeyon.apps.pdfCatchButton.AllInterFace._13DCAGPPDF;
import com.seeyon.apps.pdfCatchButton.AllInterFace._14DCTASKID;
import com.seeyon.apps.pdfCatchButton.config.CmbConfig;
import com.seeyon.apps.pdfCatchButton.service.PdfCatchService;
import com.seeyon.apps.pdfCatchButton.utils.JsonParseUtil;
import com.seeyon.ctp.common.log.CtpLogFactory;
import org.apache.commons.logging.Log;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.annotation.Schedules;
import org.springframework.stereotype.Component;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 代发代扣对账单抓取-定时任务类
 * [修改说明] 逻辑简化，文件下载/解压/去重/上传均移至 Service 层处理
 */
@Component
public class PdfDetialsTask {

    private static final Log LOGGER = CtpLogFactory.getLog(PdfDetialsTask.class);

    // ==========================================
    // 【配置区】
    // ==========================================
    @Value("${${pdf.mode}.form.regularTime.id}")
    private String targetFormId;

    private static final String[] COMPANY_LIST = {"致远", "搭见"};

    // S-逐笔打印，M-批量打印
    private static final String[] PRINT_MODES = {"S", "M"};


    @Autowired
    private PdfCatchService pdfCatchService;

    /**
     * 定时任务入口
     */
//    @Scheduled(cron = "0 0/5 * * * ?")
    @Schedules({
            @Scheduled(cron = "0 52 9 * * ?"),    // 对应 9:52
            @Scheduled(cron = "0 32 13 * * ?"),  // 对应 13:32
            @Scheduled(cron = "0 22 21 * * ?")   // 对应  21:22
    })
    public void execute() {
        // 获取互斥锁
        synchronized (CmbConfig.TASK_LOCK) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            System.out.println("======互斥锁==========================================>>>" +
                    "对账单任务(PdfDetialsTask)已获取锁，当前时间: " + sdf.format(new Date()));

            // 1. 计算日期：只获取昨天
            SimpleDateFormat dateParamFormat = new SimpleDateFormat("yyyy-MM-dd");
            Date current = new Date();
            java.util.Calendar cal = java.util.Calendar.getInstance();
            cal.setTime(current);
            cal.add(java.util.Calendar.DAY_OF_MONTH, -1);
            String yesterdayDate = dateParamFormat.format(cal.getTime());

            // 2. 双层循环：先公司，后模式
            for (String company : COMPANY_LIST)  {
                for (String mode : PRINT_MODES) {
                    try {
                        String modeName = "S".equals(mode) ? "逐笔" : "批量";
                        LOGGER.info(">>> [对账单] 正在处理: " + company + " [模式: " + mode + "-" + modeName + "] [日期: " + yesterdayDate + "]");
                        processSingleCompany(company, yesterdayDate, yesterdayDate, mode);
                    } catch (Exception e) {
                        LOGGER.error(">>> [对账单] 处理公司 [" + company + "] 模式 [" + mode + "] 失败", e);
                    }
                }
            }
            System.out.println("======互斥锁==========================================>>>" +
                    "对账单任务执行完毕，释放锁。当前时间: " + sdf.format(new Date()));
            System.out.println("\n");
        }
    }

    /**
     * 处理单个公司、单个模式的逻辑
     */
    private void processSingleCompany(String company, String startDate, String endDate, String printMode) {
        try {
            // 1. 调用申请接口
            String response1 = _13DCAGPPDF.idQuery(company, startDate, endDate, printMode);
            String taskHandle = JsonParseUtil.getPrintId(response1);

            if (taskHandle == null || taskHandle.isEmpty()) {
                LOGGER.warn(">>> 公司 [" + company + "] 模式 [" + printMode + "] 未获取到有效ID，跳过。");
                return;
            }

            // 2. 强制等待银行生成文件 (30秒)
            try {
                Thread.sleep(30000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // 3. 调用结果查询接口
            String response2 = _14DCTASKID.pdfGet(taskHandle);
            String zipUrl = JsonParseUtil.getZIPUrl(response2);
            LOGGER.info(">>> [对账单] 获取到下载链接: " + zipUrl);

            if (zipUrl == null || zipUrl.length() == 0 || !zipUrl.startsWith("http")) {
                LOGGER.warn(">>> 公司 [" + company + "] 模式 [" + printMode + "] 未获取到下载链接。");
                return;
            }

            // 4. [核心修改] 调用 Service 新接口
            // 参数说明：formId, zipUrl, type=1 (对账单)
            // Service 内部会自动：下载 -> 解压 -> 查重 -> 上传新文件 -> 清理垃圾
            String resultMsg = pdfCatchService.processImportNew(Long.valueOf(targetFormId), zipUrl, 1);

            System.out.println(">>> 公司 [" + company + "] 模式 [" + printMode + "] 处理完毕: " + resultMsg);
            LOGGER.info(">>> 公司 [" + company + "] 模式 [" + printMode + "] 处理完毕: " + resultMsg);

        } catch (Exception e) {
            throw new RuntimeException("流程异常: " + e.getMessage(), e);
        }
        // [移除] finally 块中的文件删除逻辑已不再需要，Service 层会处理
    }
}
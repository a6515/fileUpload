package com.seeyon.apps.pdfCatchButton.schedulerTask;
import com.seeyon.apps.pdfCatchButton.AllInterFace._17ASYCALHD;
import com.seeyon.apps.pdfCatchButton.AllInterFace._18DCTASKID;
import com.seeyon.apps.pdfCatchButton.config.CmbConfig;
import com.seeyon.apps.pdfCatchButton.service.PdfCatchService;
import com.seeyon.apps.pdfCatchButton.utils.JsonParseUtil;
import com.seeyon.ctp.common.AppContext;
import com.seeyon.ctp.common.log.CtpLogFactory;
import com.seeyon.ctp.organization.bo.V3xOrgMember;
import com.seeyon.ctp.organization.manager.OrgManager;
import org.apache.commons.logging.Log;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 薪资回单抓取-定时任务类 (重试/补漏)
 * [修改说明] 逻辑简化，文件下载/解压/去重/上传均移至 Service 层处理
 */
@Component("pdfRetryTaskAll")
public class PdfRetryTask {

    private static final Log LOGGER = CtpLogFactory.getLog(PdfRetryTask.class);

    // ==========================================
    // 配置区
    // ==========================================
    @Value("${${pdf.mode}.form.regularTime.id}")
    private String targetFormId;

    private static final String[] COMPANY_LIST = {"致远", "搭见"};

    @Value("${${pdf.mode}.admin.login.name}")
    private String adminLoginName;

    @Autowired
    private PdfCatchService pdfCatchService;

    /**
     * 定时任务入口
     */

//    @Scheduled(cron = "0 0/5 * * * ?")
    @Scheduled(cron = "0 52 9 1,15 * ?")
    public void executeRetryTask() {
        // 获取互斥锁
        synchronized (CmbConfig.TASK_LOCK) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            System.out.println("======互斥锁==========================================>>>" +
                    "回单任务(PdfRetryTask)已获取锁，开始执行,当前时间: " + sdf.format(new Date()));

            // 环境自检
            checkAdminExist();

            // 1. 计算日期 (过去30天 ~ 今天)
            SimpleDateFormat dateParamFormat = new SimpleDateFormat("yyyy-MM-dd");
            Date current = new Date();
            String todayDate = dateParamFormat.format(current);

            java.util.Calendar cal = java.util.Calendar.getInstance();
            cal.setTime(current);
            cal.add(java.util.Calendar.DAY_OF_MONTH, -31);
            String yesterdayDate = dateParamFormat.format(cal.getTime());

            // 2. 循环处理每个公司
            for (String company : COMPANY_LIST) {
                try {
                    LOGGER.info(">>> [回单重试] 正在处理: " + company + " [" + yesterdayDate + " ~ " + todayDate + "]");
                    processSingleCompany(company, yesterdayDate, todayDate);
                } catch (Exception e) {
                    LOGGER.error(">>> [回单重试] 处理公司 [" + company + "] 失败", e);
                }
            }
            System.out.println("======互斥锁==========================================>>>" +
                    "回单任务执行完毕，释放锁。当前时间: " + sdf.format(new Date()));
            System.out.println("\n");
        }
    }

    private void checkAdminExist() {
        try {
            OrgManager orgManager = (OrgManager) AppContext.getBean("orgManager");
            V3xOrgMember member = orgManager.getMemberByLoginName(adminLoginName);
            if (member == null) {
                LOGGER.warn("[环境自检警告] 未找到登录名为 '" + adminLoginName + "' 的用户！");
            }
        } catch (Exception e) {
            LOGGER.error("环境自检异常", e);
        }
    }

    private void processSingleCompany(String company, String startDate, String endDate) {
        try {
            // 1. 获取任务ID
            String response1 = _17ASYCALHD.ID_Query(company, startDate, endDate);
            String rtndat = JsonParseUtil.getrtndat(response1);

            if (rtndat == null || rtndat.length() == 0) {
                LOGGER.info(">>> 公司 [" + company + "] 无任务ID，跳过。");
                return;
            }

            // 2. 等待银行生成文件 (15秒)
            try {
                Thread.sleep(15000);
            } catch (InterruptedException e) {}

            // 3. 获取下载链接
            String response2 = _18DCTASKID.GET_PDFUrl(rtndat);
            String zipUrl = JsonParseUtil.getZIPUrl(response2);

            if (zipUrl == null || !zipUrl.startsWith("http")) {
                LOGGER.warn(">>> 公司 [" + company + "] 未获取到有效链接。");
                return;
            }
            LOGGER.info(">>> 获取到链接: " + zipUrl);

            // 4. [核心修改] 调用 Service 新接口
            // 参数说明：formId, zipUrl, type=0 (普通回单)
            // Service 内部会自动：下载 -> 解压 -> 查重 -> 上传新文件 -> 清理垃圾
            String resultMsg = pdfCatchService.processImportNew(Long.valueOf(targetFormId), zipUrl, 0);

            LOGGER.info(">>> [" + company + "] 同步结果: " + resultMsg);
            System.out.println(">>> [" + company + "] 同步结果: " + resultMsg);

        } catch (Exception e) {
            throw new RuntimeException("处理异常: " + e.getMessage(), e);
        }
        // [移除] finally 块中的文件删除逻辑已不再需要
    }
}
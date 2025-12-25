package com.seeyon.apps.pdfCatchButton.service.impl;
import com.seeyon.apps.pdfCatchButton.AllInterFace._17ASYCALHD;
import com.seeyon.apps.pdfCatchButton.AllInterFace._18DCTASKID;
import com.seeyon.apps.pdfCatchButton.dao.PdfCatchDao;
import com.seeyon.apps.pdfCatchButton.service.PdfCatchService;
import com.seeyon.apps.pdfCatchButton.utils.*;
import com.seeyon.apps.pdfCatchButton.vo.TaskProgress;
import com.seeyon.cap4.form.bean.FormBean;
import com.seeyon.cap4.form.bean.FormDataMasterBean;
import com.seeyon.cap4.form.service.CAP4FormManager;
import com.seeyon.ctp.common.AppContext;
import com.seeyon.ctp.common.log.CtpLogFactory;
import com.seeyon.ctp.util.UUIDLong;
import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Consumer;

@Service
public class PdfCatchServiceImpl implements PdfCatchService {
    private static final Log LOGGER = CtpLogFactory.getLog(PdfCatchServiceImpl.class);

    @Autowired
    private AttachmentBindingKit attachmentBindingKit;
    @Autowired
    private PdfCatchDao pdfCatchDao;
    @Autowired
    private ZipProcessUtils zipProcessUtils;
    @Autowired
    private NativeFileUploader nativeFileUploader;
    @Autowired
    private DeduplicationHelper deduplicationHelper;

    @Value("${${pdf.mode}.form.salaryTableName}")
    private String salaryTableName;
    @Value("${${pdf.mode}.form.salarySubTableName}")
    private String salarySubTableName;

    private CAP4FormManager getCap4FormManager() {
        return (CAP4FormManager) AppContext.getBean("cap4FormManager");
    }

    /**
     * [新接口] 异步执行入口，包含完整的业务闭环
     * 专门用于前端按钮触发，带有进度回调
     */
    @Override
    public void executeAsyncImport(String startDate, String endDate, String company, Long formId, Consumer<TaskProgress> reporter) {
        Path tempDir = null;
        try {
            // --- 阶段 1: 申请任务ID ---
            reporter.accept(new TaskProgress("RUNNING", 5, "正在向银行申请任务ID..."));
            // 注意：此处调用的是回单查询接口(_17ASYCALHD)，如果要做对账单需区分逻辑
            String response1 = _17ASYCALHD.ID_Query(company, startDate, endDate);
            String rtndat = JsonParseUtil.getrtndat(response1);

            if (rtndat == null || rtndat.length() == 0) {
                reporter.accept(new TaskProgress("ERROR", 0, "未能获取任务ID，银行响应: " + response1));
                return;
            }

            // --- 阶段 2: 强制等待银行生成文件 (模拟进度条) ---
            // 总共等待 15 秒，每秒更新一次进度
            for (int i = 1; i <= 15; i++) {
                int percent = 5 + (int) ((double) i / 15 * 25); // 进度从 5% 走到 30%
                reporter.accept(new TaskProgress("RUNNING", percent, "银行正在生成文件，请稍候 (" + (15 - i) + "s)..."));
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }

            // --- 阶段 3: 获取下载链接 ---
            reporter.accept(new TaskProgress("RUNNING", 35, "正在获取下载链接..."));
            String response2 = _18DCTASKID.GET_PDFUrl(rtndat);
            String zipUrl = JsonParseUtil.getZIPUrl(response2);

            if (zipUrl == null || !zipUrl.startsWith("http")) {
                reporter.accept(new TaskProgress("ERROR", 0, "银行文件未生成或链接无效。请稍后重试。"));
                return;
            }

            // --- 阶段 4: 下载并解压 ---
            reporter.accept(new TaskProgress("RUNNING", 40, "正在下载文件包..."));
            tempDir = Files.createTempDirectory("seeyon_catch_" + UUID.randomUUID());
            List<File> rawFiles = zipProcessUtils.downloadAndExtract(zipUrl, tempDir);

            if (rawFiles == null || rawFiles.isEmpty()) {
                reporter.accept(new TaskProgress("SUCCESS", 100, "同步完成：该时间段内无回单文件。"));
                return;
            }

            // --- 阶段 5: 解析与去重 ---
            reporter.accept(new TaskProgress("RUNNING", 50, "正在解析并去重 " + rawFiles.size() + " 个文件..."));
            // type=0 代表普通回单
            List<DeduplicationHelper.ValidDataWrapper> validFiles = deduplicationHelper.filterDuplicates(rawFiles, formId, 0);

            int total = validFiles.size();
            int duplicateCount = rawFiles.size() - total;

            if (total == 0) {
                reporter.accept(new TaskProgress("SUCCESS", 100, "同步完成：所有文件(" + duplicateCount + "个)均已存在，无新增。"));
                return;
            }

            // --- 阶段 6: 循环入库 (动态进度条) ---
            FormBean formBean = getCap4FormManager().getForm(formId, false);
            int successCount = 0;
            int errorCount = 0;

            for (int i = 0; i < total; i++) {
                DeduplicationHelper.ValidDataWrapper item = validFiles.get(i);
                // 计算进度: 从 50% 走到 95%
                int currentPercent = 50 + (int) ((double) (i + 1) / total * 45);
                reporter.accept(new TaskProgress("RUNNING", currentPercent,
                        "正在入库 (" + (i + 1) + "/" + total + "): " + item.uniqueKey));

                try {
                    String fileId = nativeFileUploader.uploadFile(item.originFile);
                    saveReceiptFormData(formBean, item, fileId);
                    successCount++;
                } catch (Exception e) {
                    errorCount++;
                    LOGGER.error("入库失败: " + item.uniqueKey, e);
                }
            }

            // --- 阶段 7: 完成 ---
            StringBuilder msg = new StringBuilder();
            msg.append("同步成功！\n");
            msg.append("总文件: ").append(rawFiles.size()).append("，");
            msg.append("新增: ").append(successCount).append("，");
            msg.append("过滤重复: ").append(duplicateCount);
            if (errorCount > 0) msg.append("，失败: ").append(errorCount);

            reporter.accept(new TaskProgress("SUCCESS", 100, msg.toString()));

        } catch (Exception e) {
            LOGGER.error("异步流程严重异常", e);
            reporter.accept(new TaskProgress("ERROR", 0, "流程异常: " + e.getMessage()));
        } finally {
            // 清理临时文件
            if (tempDir != null) {
                try {
                    FileUtils.deleteDirectory(tempDir.toFile());
                } catch (Exception e) {
                    LOGGER.warn("临时目录删除失败", e);
                }
            }
        }
    }


    /**
     * [旧接口] 统一导入入口 (保留，供定时任务使用)
     * 兼容性方法，不带进度回调
     *
     * @param formId 表单ID
     * @param zipUrl 下载URL
     * @param type   0:普通回单, 1:对账单
     * @return 结果消息
     */
    @Override
    public String processImportNew(Long formId, String zipUrl, int type) {
        Path tempDir = null;
        try {
            // 1. 创建临时目录
            tempDir = Files.createTempDirectory("seeyon_catch_" + UUID.randomUUID());
            LOGGER.info("创建临时工作目录: " + tempDir.toString());

            // 2. 下载并解压
            List<File> rawFiles = zipProcessUtils.downloadAndExtract(zipUrl, tempDir);
            int totalCount = rawFiles.size(); // 总文件数

            // 3. 调用去重工具
            List<DeduplicationHelper.ValidDataWrapper> validFiles =
                    deduplicationHelper.filterDuplicates(rawFiles, formId, type);

            // 【计算逻辑】重复数 = 总数 - 有效数
            int duplicateCount = totalCount - validFiles.size();

            if (validFiles.isEmpty()) {
                return "本次同步未发现新数据，重复文件已自动过滤 " + duplicateCount + " 条。";
            }

            // 4. 仅对有效文件进行上传和入库
            int successCount = 0;
            int errorCount = 0;

            FormBean formBean = getCap4FormManager().getForm(formId, false);

            for (DeduplicationHelper.ValidDataWrapper item : validFiles) {
                try {
                    // A. 上传文件 (生成 ctp_file)
                    String fileId = nativeFileUploader.uploadFile(item.originFile);

                    // B. 生成表单数据
                    if (type == 0) {
                        saveReceiptFormData(formBean, item, fileId);
                    } else {
                        saveStatementFormData(formBean, item, fileId);
                    }
                    successCount++;
                } catch (Exception e) {
                    errorCount++;
                    LOGGER.error("入库失败 Key=" + item.uniqueKey, e);
                }
            }

            // 5. 构建清晰的统计消息
            StringBuilder sb = new StringBuilder();
            sb.append("同步完成：");
            sb.append("收到文件 ").append(totalCount).append(" 条，");
            sb.append("成功入库 ").append(successCount).append(" 条，");
            sb.append("过滤重复 ").append(duplicateCount).append(" 条");

            if (errorCount > 0) {
                sb.append("，处理失败 ").append(errorCount).append(" 条(详情请查看后台日志)");
            } else {
                sb.append("。");
            }

            return sb.toString();

        } catch (Exception e) {
            LOGGER.error("导入流程异常", e);
            return "导入异常: " + e.getMessage();
        } finally {
            // 6. [扫尾工作] 物理删除临时解压目录
            if (tempDir != null) {
                try {
                    FileUtils.deleteDirectory(tempDir.toFile());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    // --- 私有辅助方法：保存普通回单 ---
    private void saveReceiptFormData(FormBean formBean, DeduplicationHelper.ValidDataWrapper item, String fileId) throws Exception {
        Map<String, String> parseResult = (Map<String, String>) item.parseResult;
        FormDataMasterBean masterBean = FormDataMasterBean.newInstance(formBean);
        masterBean.resetId(UUIDLong.longUUID(), true);

        // 系统字段
        masterBean.addFieldValue("start_member_id", AppContext.currentUserId());
        masterBean.addFieldValue("start_date", new Date());
        masterBean.addFieldValue("state", 1);

        // 业务字段
        masterBean.addFieldValue("field0003", parseResult.get("payerName"));
        if (parseResult.get("tradeDate") != null) {
            masterBean.addFieldValue("field0004", new SimpleDateFormat("yyyy年MM月dd日").parse(parseResult.get("tradeDate")));
        }
        masterBean.addFieldValue("field0005", parseResult.get("businessType"));
        masterBean.addFieldValue("field0006", parseResult.get("businessNo"));
        masterBean.addFieldValue("field0007", parseResult.get("transactionSerial"));
        masterBean.addFieldValue("field0008", parseResult.get("relatedNo"));
        masterBean.addFieldValue("field0009", parseResult.get("customerNo"));
        masterBean.addFieldValue("field0010", parseResult.get("payerAccount"));
        masterBean.addFieldValue("field0011", parseResult.get("payerBank"));
        masterBean.addFieldValue("field0012", parseResult.get("summary"));
        masterBean.addFieldValue("field0013", parseResult.get("amountSmall"));
        masterBean.addFieldValue("field0016", "回单");
        masterBean.addFieldValue("field0014", item.uniqueKey); // 唯一标识

        // 绑定附件
        attachmentBindingKit.bindAttachment(masterBean, "field0001", fileId, item.originFile);

        getCap4FormManager().saveOrUpdateFormData(masterBean, formBean.getId(), true);
    }

    // --- 私有辅助方法：保存对账单 ---
    private void saveStatementFormData(FormBean formBean, DeduplicationHelper.ValidDataWrapper item, String fileId) throws Exception {
        FormDataMasterBean masterBean = FormDataMasterBean.newInstance(formBean);
        masterBean.resetId(UUIDLong.longUUID(), true);

        masterBean.addFieldValue("start_member_id", AppContext.currentUserId());
        masterBean.addFieldValue("start_date", new Date());
        masterBean.addFieldValue("state", 1);

        if ("1".equals(item.flag)) {
            // 总对账单
            List<Map<String, String>> list = (List<Map<String, String>>) item.parseResult;
            Map<String, String> firstRow = list.get(0);
            masterBean.addFieldValue("field0003", firstRow.get("payerName"));
            if (firstRow.get("tradeDate") != null) {
                masterBean.addFieldValue("field0004", new SimpleDateFormat("yyyy-MM-dd").parse(firstRow.get("tradeDate")));
            }
            masterBean.addFieldValue("field0005", firstRow.get("agencyType"));
            masterBean.addFieldValue("field0007", firstRow.get("transactionSerial"));
            masterBean.addFieldValue("field0010", firstRow.get("payerAccount"));
            masterBean.addFieldValue("field0012", firstRow.get("summary"));
            masterBean.addFieldValue("field0013", item.totalMoney);
            masterBean.addFieldValue("field0017", firstRow.get("payerName"));
            masterBean.addFieldValue("field0016", "总对账单");
        } else {
            // 明细对账单
            List<Map<String, String>> list = (List<Map<String, String>>) item.parseResult;
            Map<String, String> row = list.get(0);

            masterBean.addFieldValue("field0003", row.get("payerName"));
            if (row.get("tradeDate") != null) {
                masterBean.addFieldValue("field0004", new SimpleDateFormat("yyyy-MM-dd").parse(row.get("tradeDate")));
            }
            masterBean.addFieldValue("field0005", row.get("agencyType"));
            masterBean.addFieldValue("field0007", row.get("transactionSerial"));
            masterBean.addFieldValue("field0010", row.get("payerAccount"));
            masterBean.addFieldValue("field0012", row.get("summary"));
            masterBean.addFieldValue("field0013", row.get("amount"));
            masterBean.addFieldValue("field0017", row.get("payeeName"));
            masterBean.addFieldValue("field0016", "对账单");

            // 触发薪资反写逻辑
            Long myId = UUIDLong.longUUID();
            String msg = updateSalaryStatus(row.get("businessRefNo"), row.get("payeeName"), myId, Long.valueOf(fileId));
            LOGGER.info("薪资反写结果: " + msg);
        }

        masterBean.addFieldValue("field0015", item.uniqueKey);
        attachmentBindingKit.bindAttachment(masterBean, "field0001", fileId, item.originFile);

        getCap4FormManager().saveOrUpdateFormData(masterBean, formBean.getId(), true);
    }

    // --- 保留原有的 updateSalaryStatus ---
    @Override
    @Transactional(value = "pdfTransactionManager", rollbackFor = Exception.class)
    public String updateSalaryStatus(String yurref, String matchName, Long targetValue, Long fileId) {
        // 1. 读取配置，确定表名
        if (salaryTableName == null || salarySubTableName == null) {
            return "错误：配置文件中未找到环境的表名配置。";
        }

        try {
            // 2. 第一步：根据业务参考号 (yurref) 查主表 ID
            Long mainId = pdfCatchDao.selectMainIdByYurref(salaryTableName, yurref);

            if (mainId == null) {
                LOGGER.warn("未找到主表记录，业务号: " + yurref);
                return "未找到对应的主表记录 (业务号: " + yurref + ")";
            }

            // 3. 第二步：尝试查询子表 ID
            Long subId = pdfCatchDao.selectSubId(salarySubTableName, mainId, matchName);

            if (subId != null) {
                // 先进行文件绑定操作
                Map<String, Object> fileInfo = pdfCatchDao.selectCtpFile(fileId);
                if (fileInfo == null) {
                    throw new RuntimeException("上传后无法查询到文件记录 ID=" + fileId);
                } else {
                    Object dateObj = fileInfo.get("CREATE_DATE");
                    Date createDate = new Date();
                    if (dateObj != null) {
                        if (dateObj instanceof LocalDateTime) {
                            createDate = Timestamp.valueOf((LocalDateTime) dateObj);
                        } else if (dateObj instanceof Date) {
                            createDate = (Date) dateObj;
                        }
                    }

                    pdfCatchDao.insertAttachment(
                            UUIDLong.longUUID(),
                            mainId,
                            targetValue,
                            66,
                            0,
                            (String) fileInfo.get("FILENAME"),
                            fileId,
                            (String) fileInfo.get("MIME_TYPE"),
                            createDate,
                            Long.parseLong(fileInfo.get("FILE_SIZE").toString()),
                            0);
                    LOGGER.info(">>> 附件记录已强行插入 [Ref=" + subId + "]");
                }

                // 4. 如果找到了 ID，执行更新
                int rows = pdfCatchDao.updateSubTableValue(
                        salarySubTableName,
                        targetValue, // 要更新的值
                        subId,      // 主表的子表唯一ID
                        matchName    // 匹配姓名
                );

                if (rows > 0) {
                    String msg = "更新成功！子表ID: " + subId + " (业务号: " + yurref + ", 姓名: " + matchName + ")";
                    LOGGER.info(msg);
                    return msg;
                } else {
                    String msg = "找到子表ID(" + subId + ")但在更新时未影响行数，请检查数据状态。rows的值为:" + rows;
                    LOGGER.warn(msg);
                    return msg;
                }
            } else {
                String msg = "主表已找到(ID:" + mainId + ")，但在子表中未匹配到姓名: " + matchName;
                LOGGER.warn(msg);
                return msg;
            }

        } catch (Exception e) {
            LOGGER.error("更新薪资状态异常", e);
            throw new RuntimeException("更新失败: " + e.getMessage());
        }
    }
}
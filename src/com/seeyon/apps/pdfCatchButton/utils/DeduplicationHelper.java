package com.seeyon.apps.pdfCatchButton.utils;

import com.seeyon.apps.pdfCatchButton.dao.PdfCatchDao;
import com.seeyon.cap4.form.bean.FormBean;
import com.seeyon.cap4.form.service.CAP4FormManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.math.BigDecimal;
import java.util.*;

/**
 * 去重助手：负责在上传文件前进行解析和数据库比对
 */
@Component("deduplicationHelperSalary")
public class DeduplicationHelper {

    @Autowired
    private PdfCatchDao pdfCatchDao;

    @Autowired
    private CAP4FormManager cap4FormManager;

    /**
     * 内部数据封装类
     */
    public static class ValidDataWrapper {
        public File originFile;
        public Object parseResult; // 可能是 Map 或 List<Map>
        public String uniqueKey;
        public Double totalMoney; // 仅对账单用到
        public String flag;       // 仅对账单用到 ("0"或"1")

        public ValidDataWrapper(File f, Object r, String k) {
            this.originFile = f;
            this.parseResult = r;
            this.uniqueKey = k;
        }
    }

    /**
     * 通用去重过滤方法
     * @param rawFiles 解压后的临时文件列表
     * @param formId   表单ID (用于查找数据库表名)
     * @param type     类型枚举：0-普通回单, 1-对账单
     */
    public List<ValidDataWrapper> filterDuplicates(List<File> rawFiles, Long formId, int type) {
        List<ValidDataWrapper> validList = new ArrayList<>();
        List<ValidDataWrapper> pendingList = new ArrayList<>();
        List<String> keysToCheck = new ArrayList<>();

        if (rawFiles == null || rawFiles.isEmpty()) return validList;

        // 1. 获取数据库表名和去重字段
        FormBean formBean = cap4FormManager.getForm(formId, false);
        if (formBean == null) throw new RuntimeException("表单不存在: " + formId);

        String tableName = formBean.getMasterTableBean().getTableName();
        // 根据你的业务：回单唯一值存在 field0014，对账单唯一值存在 field0015
        String dbField = (type == 0) ? "field0014" : "field0015";

        // 2. 预解析 (只在内存中处理，不写库，不上传)
        for (File file : rawFiles) {
            String uniqueKey = null;
            Object resultObj = null;
            ValidDataWrapper wrapper = null;

            try {
                // --- 分支逻辑：普通回单 ---
                if (type == 0) {
                    Map<String, String> res = BankReceiptParser.parse(file);
                    uniqueKey = res.get("receiptNo");
                    if (uniqueKey == null || uniqueKey.isEmpty()) uniqueKey = res.get("businessNo");
                    resultObj = res;
                    wrapper = new ValidDataWrapper(file, resultObj, uniqueKey);
                }
                // --- 分支逻辑：对账单 ---
                else {
                    List<Map<String, String>> resList = BankReceiptParser.parseAgencyBill(file);
                    if (!resList.isEmpty()) {
                        uniqueKey = resList.get(0).get("fileName");
                    }
                    resultObj = resList;
                    wrapper = new ValidDataWrapper(file, resultObj, uniqueKey);

                    // 处理原有逻辑中的 flag 和 totalMoney
                    if (resList.size() > 1) {
                        wrapper.flag = "1";
                        BigDecimal totalMoneyBD = BigDecimal.ZERO;
                        for (Map<String, String> row : resList) {
                            String amountStr = row.get("amount");
                            if (amountStr != null) {
                                totalMoneyBD = totalMoneyBD.add(new BigDecimal(amountStr.replace(",", "")));
                            }
                        }
                        wrapper.totalMoney = totalMoneyBD.setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
                    } else {
                        wrapper.flag = "0";
                    }
                }

                // 收集待检查项
                if (uniqueKey != null && !uniqueKey.trim().isEmpty()) {
                    pendingList.add(wrapper);
                    keysToCheck.add(uniqueKey);
                } else {
                    System.out.println("警告：文件未解析出唯一标识，将被忽略: " + file.getName());
                }
            } catch (Exception e) {
                System.out.println("解析文件异常跳过: " + file.getName());
                e.printStackTrace();
            }
        }

        // 3. 批量查库 (一次 SQL 查询)
        Set<String> existingSet = new HashSet<>();
        if (!keysToCheck.isEmpty()) {
            List<String> dbExistIds = pdfCatchDao.selectExistingIds(tableName, dbField, keysToCheck);
            if (dbExistIds != null) {
                existingSet.addAll(dbExistIds);
            }
        }

        // 4. 过滤结果
        int skipCount = 0;
        for (ValidDataWrapper wrapper : pendingList) {
            if (existingSet.contains(wrapper.uniqueKey)) {
                skipCount++; // 发现重复，直接丢弃
            } else {
                validList.add(wrapper); // 是新数据，加入结果
            }
        }

        System.out.println(">>> [去重工具] 扫描 " + rawFiles.size() + " 个，发现重复 " + skipCount + " 个，有效新文件 " + validList.size() + " 个。");
        return validList;
    }
}
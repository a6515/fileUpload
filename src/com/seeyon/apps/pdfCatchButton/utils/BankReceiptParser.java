package com.seeyon.apps.pdfCatchButton.utils;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 招商银行电子回单/对账单解析工具
 */
public class BankReceiptParser {

    /**
     * 原有的回单解析方法 (保持不变)
     */
    public static Map<String, String> parse(File pdfFile) {
        // ... (此处省略您原有的 parse 方法代码，请保留原样) ...
        // 为了演示方便，这里仅作为占位符，实际使用时请不要删除原来的代码
        Map<String, String> data = new HashMap<>();
        PDDocument document = null;
        try {
            document = PDDocument.load(pdfFile);
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            String fullText = stripper.getText(document);

            // ================= 左侧栏字段 =================
            extract(fullText, "交易日期[:：]\\s*(\\d{4}年\\d{2}月\\d{2}日)", "tradeDate", data);
            extract(fullText, "业务编号[:：]\\s*([A-Za-z0-9]+)", "businessNo", data);
            extract(fullText, "相关编号[:：]\\s*([A-Za-z0-9]+)", "relatedNo", data);
            extract(fullText, "付款账号[:：]\\s*(\\d+)", "payerAccount", data);
            extract(fullText, "付款开户行[:：]\\s*(\\S+)", "payerBank", data);
            extract(fullText, "交易金额\\(小写\\)[:：]\\s*(CNY\\s*[\\d,.]+)", "amountSmall", data);
            extract(fullText, "交易金额\\(大写\\)[:：]\\s*(\\S+)", "amountBig", data);
            extract(fullText, "交易摘要[:：][ \\t\\u00A0]*([^\\n\\r]*)", "summary", data);
            extract(fullText, "业务类型[:：]\\s*(\\S+)", "businessType", data);
            extract(fullText, "交易流水[:：]\\s*([A-Za-z0-9]+)", "transactionSerial", data);
            extract(fullText, "客户编号[:：]\\s*(\\d+)", "customerNo", data);
            extract(fullText, "付款人[:：]\\s*(\\S+)", "payerName", data);
            extract(fullText, "回单编号[:：]\\s*([A-Za-z0-9]+)", "receiptNo", data);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (document != null) {
                try { document.close(); } catch (IOException e) {}
            }
        }
        return data;
    }
    /**
     * 解析招商银行“代发业务明细对账单” (修正：业务参考号跟随交易行变化)
     */
    public static List<Map<String, String>> parseAgencyBill(File pdfFile) {
        List<Map<String, String>> resultList = new ArrayList<>();

        // 1. 提取真正的“页级”公共信息 (付款人、日期范围、验证码)
        Map<String, String> commonData = new HashMap<>();
        PDDocument document = null;
        try {
            document = PDDocument.load(pdfFile);
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            String fullText = stripper.getText(document);

            extract(fullText, "付款账号[:：]\\s*(\\d+)", "payerAccount", commonData);
            extract(fullText, "账户名称[:：]\\s*(\\S+)", "payerName", commonData);
            extract(fullText, "经办日期[:：]\\s*([\\d\\s\\-]+)", "handleDateRange", commonData);
            // 验证码 (8位混合字符)
            extractFirstMatch(fullText, "\\b(?!\\d{8}\\b)([A-Z0-9]{8})\\b", "verifyCode", commonData);
            commonData.put("fileName", pdfFile.getName());

            // 2. 状态机循环扫描
            String[] lines = fullText.split("\\r?\\n");

            // 当前正在处理的交易记录 (用于跨行寻找字段)
            Map<String, String> currentRecord = null;

            for (String line : lines) {
                line = line.trim();

                // === 情况A：发现新的一行交易 (包含日期和金额) ===
                if (line.matches(".*20\\d{2}-\\d{2}-\\d{2}.*") && line.matches(".*\\d+\\.\\d{2}.*")) {

                    // 如果之前已经有一条记录在处理中，先把它保存进列表
                    if (currentRecord != null) {
                        resultList.add(currentRecord);
                    }

                    // 开启新的一条记录，并填入公共信息
                    currentRecord = new HashMap<>(commonData);

                    // --- 提取行内核心数据 ---
                    extractFirstMatch(line, "(20\\d{2}-\\d{2}-\\d{2})", "tradeDate", currentRecord);

                    // =================================================================
                    // 【新增】提取代发种类 (逻辑：位于日期和长数字账号之间)
                    // =================================================================
                    // 解释：匹配 日期 + 空格 + (捕获非空内容) + 空格 + 15位以上数字
                    Pattern typePattern = Pattern.compile("20\\d{2}-\\d{2}-\\d{2}[\\s\\u00A0]+(\\S+)[\\s\\u00A0]+\\d{15,}");
                    Matcher typeMatcher = typePattern.matcher(line);
                    if (typeMatcher.find()) {
                        currentRecord.put("agencyType", typeMatcher.group(1).trim());
                    }
                    // =================================================================

                    extractFirstMatch(line, "(C[A-Z0-9]+)", "transactionSerial", currentRecord);
                    extractFirstMatch(line, "(\\d{15,})", "payeeAccount", currentRecord); // 收款账号
                    extractFirstMatch(line, "([\\d,]+\\.\\d{2})", "amount", currentRecord);

                    // 锚点提取姓名
                    Pattern namePattern = Pattern.compile("(\\d{15,})[\\s\\u00A0]+(.*?)[\\s\\u00A0]+([\\d,]+\\.\\d{2})");
                    Matcher nameMatcher = namePattern.matcher(line);
                    if (nameMatcher.find()) currentRecord.put("payeeName", nameMatcher.group(2).trim());

                    // 锚点提取摘要
                    if (currentRecord.containsKey("transactionSerial")) {
                        Pattern summaryPattern = Pattern.compile("([\\d,]+\\.\\d{2})[\\s\\u00A0]+(.*?)[\\s\\u00A0]+(C[A-Z0-9]+)");
                        Matcher summaryMatcher = summaryPattern.matcher(line);
                        if (summaryMatcher.find()) currentRecord.put("summary", summaryMatcher.group(2).trim());
                    }

                    // 尝试在【当前行】找业务参考号 (万一它就在这行)
                    extractFirstMatch(line, "\\b(20\\d{12,})\\b", "businessRefNo", currentRecord);
                }

                // === 情况B：这不是交易行，但可能是上一笔交易的补充信息 (如业务参考号换行了) ===
                else if (currentRecord != null) {
                    // 如果当前记录还没有业务参考号，试着在这一行找
                    if (!currentRecord.containsKey("businessRefNo")) {
                        // 特征：20开头，13位以上纯数字
                        extractFirstMatch(line, "\\b(20\\d{12,})\\b", "businessRefNo", currentRecord);
                    }
                }
            }

            // 循环结束后，别忘了把最后一条记录加进去
            if (currentRecord != null) {
                resultList.add(currentRecord);
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (document != null) {
                try { document.close(); } catch (IOException e) {}
            }
        }
        return resultList;
    }


    /**
     * 辅助方法：提取首个匹配到的特定格式数据 (不依赖 Key 前缀)
     */
    private static void extractFirstMatch(String text, String regex, String key, Map<String, String> data) {
        if (data.containsKey(key)) return;
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            String value = matcher.group(1).trim();
            data.put(key, value);
        }
    }

    /**
     * 通用正则提取方法
     */
    private static void extract(String text, String regex, String key, Map<String, String> data) {
        if (data.containsKey(key)) return;
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            String value = matcher.group(1).trim();
            data.put(key, value);
        }
    }
    // 简单的测试入口
    public static void main(String[] args) {
        // 请替换为你本地的实际文件路径
        File file = new File("C:\\Temp\\回单.pdf");
        if(file.exists()){
            Map<String, String> result = parse(file);
            System.out.println("解析结果如下：");
            for (String key : result.keySet()) {
                System.out.println(key + ": " + result.get(key));
            }
        }

//        if (file.exists()) {
//            // 调用解析方法，获取列表
//            List<Map<String, String>> results = parseAgencyBill(file);
//
//            System.out.println("============================================================");
//            System.out.println("【解析完成】共提取到 " + results.size() + " 条有效的交易记录");
//            System.out.println("============================================================\n");
//
//            for (int i = 0; i < results.size(); i++) {
//                Map<String, String> row = results.get(i);
//
//                System.out.println(">>> 第 " + (i + 1) + " 条记录详情 <<<");
//
//                // --- 1. 核心交易信息 (每行都不一样) ---
//                System.out.println("  1. 交易日期 (tradeDate):       " + row.get("tradeDate"));
//                System.out.println("  2. 代发种类 (agencyType):      " + row.get("agencyType")); // 【新增】打印代发种类
//                System.out.println("  3. 收款户名 (payeeName):       " + row.get("payeeName"));
//                System.out.println("  4. 收款账号 (payeeAccount):    " + row.get("payeeAccount"));
//                System.out.println("  5. 交易金额 (amount):          " + row.get("amount"));
//                System.out.println("  6. 交易摘要 (summary):         " + row.get("summary"));
//                System.out.println("  7. 交易流水 (transactionSerial): " + row.get("transactionSerial"));
//
//                // --- 2. 公共页面信息 (整页通用) ---
//                System.out.println("  8. 付款账号 (payerAccount):    " + row.get("payerAccount"));
//                System.out.println("  9. 付款户名 (payerName):       " + row.get("payerName"));
//                System.out.println("  10.经办日期 (handleDateRange): " + row.get("handleDateRange"));
//                System.out.println("  11.业务参考 (businessRefNo):   " + row.get("businessRefNo"));
//                System.out.println("  12.验证码   (verifyCode):      " + row.get("verifyCode"));
//                System.out.println("  13.来源文件 (fileName):        " + row.get("fileName"));
//
//                System.out.println("------------------------------------------------------------\n");
//            }
//        } else {
//            System.err.println("错误：找不到文件 -> " + file.getAbsolutePath());
//        }
    }
}
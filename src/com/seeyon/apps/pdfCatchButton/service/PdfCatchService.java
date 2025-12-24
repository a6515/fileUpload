package com.seeyon.apps.pdfCatchButton.service;

import com.seeyon.apps.pdfCatchButton.vo.TaskProgress;
import java.util.function.Consumer;

public interface PdfCatchService {

    // 兼容旧接口
    String updateSalaryStatus(String yurref, String matchName, Long targetValue, Long fileId);

    // 旧的同步导入接口 (保留以防报错)
    String processImportNew(Long formId, String zipUrl, int type);

    /**
     * [新接口] 异步导入流程
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @param company 公司名称
     * @param formId 表单ID
     * @param reporter 进度回调函数
     */
    void executeAsyncImport(String startDate, String endDate, String company, Long formId, Consumer<TaskProgress> reporter);
}
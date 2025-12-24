package com.seeyon.apps.pdfCatchButton.utils;
import com.seeyon.ctp.common.AppContext;
import com.seeyon.ctp.common.constants.ApplicationCategoryEnum;
import com.seeyon.ctp.common.exceptions.BusinessException;
import com.seeyon.ctp.common.filemanager.manager.FileManager;
import com.seeyon.ctp.common.log.CtpLogFactory;
import com.seeyon.ctp.common.po.filemanager.V3XFile;
import org.apache.commons.logging.Log;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Date;

/**
 * 原生文件上传工具类 (通用兼容版)
 * 适配服务器 API，使用 InputStream 进行文件存储，避免 NoSuchMethodError。
 */
@Component("nativeFileUploaderAll")
public class NativeFileUploader {

    private static final Log LOGGER = CtpLogFactory.getLog(NativeFileUploader.class);

    /**
     * 上传本地文件到致远文件系统 (ctp_file)
     */
    public String uploadFile(File file) throws BusinessException {
        // 1. 参数校验
        if (file == null || !file.exists()) {
            throw new BusinessException("上传失败：文件不存在或为空 -> " + (file == null ? "null" : file.getPath()));
        }

        FileInputStream in = null;
        try {
            // 2. 获取 FileManager
            FileManager fileManager = (FileManager) AppContext.getBean("fileManager");
            if (fileManager == null) {
                throw new BusinessException("无法获取 FileManager 实例，请检查 Spring 上下文。");
            }

            // 3. 【核心修改】将 File 转为 FileInputStream
            // 这样可以匹配服务器端的 save(InputStream, ...) 方法，避开 save(File) 不存在的问题
            in = new FileInputStream(file);

            // 4. 调用保存方法
            // 对应服务器源码 FileManagerImpl.java 第 970 行左右的 save 方法
            V3XFile v3xFile = fileManager.save(
                    in,                                              // 输入流
                    ApplicationCategoryEnum.valueOf(66),       // 分类
                    file.getName(),                                // 文件名
                    new Date(),                                   // 创建时间
                    true                                         // 是否入库
            );

            // 5. 返回文件 ID
            String fileId = String.valueOf(v3xFile.getId());
            LOGGER.info("原生上传成功！File: " + file.getName() + " -> ID: " + fileId);

            return fileId;

        } catch (Exception e) {
            LOGGER.error("原生上传发生异常", e);
            throw new BusinessException("文件存盘失败：" + e.getMessage(), e);
        } finally {
            // 6. 【重要】一定要关闭流，防止文件句柄泄露
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    LOGGER.warn("关闭文件流异常", e);
                }
            }
        }
    }
}
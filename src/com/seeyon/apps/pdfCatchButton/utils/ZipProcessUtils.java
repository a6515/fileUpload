package com.seeyon.apps.pdfCatchButton.utils;

import com.seeyon.ctp.common.exceptions.BusinessException;
import com.seeyon.ctp.common.log.CtpLogFactory;
import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.springframework.stereotype.Component;

import java.io.*;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

@Component
public class ZipProcessUtils {

    private static final Log LOGGER = CtpLogFactory.getLog(ZipProcessUtils.class);

    /**
     * [修改] 只负责下载和解压，不进行上传
     * @param zipUrl 下载链接
     * @param tempDir 外部传入的临时目录，方便外部统一清理
     * @return 解压后的文件列表
     */
    public List<File> downloadAndExtract(String zipUrl, Path tempDir) throws BusinessException {
        List<File> extractedFiles = new ArrayList<>();
        File tempZip = null;

        try {
            tempZip = new File(tempDir.toFile(), "temp_download.zip");

            LOGGER.info("Downloading ZIP: " + zipUrl);
            FileUtils.copyURLToFile(new URL(zipUrl), tempZip, 30000, 30000);

            try (ZipFile zipFile = new ZipFile(tempZip)) {
                Enumeration<? extends ZipEntry> entries = zipFile.entries();
                while (entries.hasMoreElements()) {
                    ZipEntry entry = entries.nextElement();
                    if (entry.isDirectory()) continue;
                    // 过滤掉这就 mac 系统生成的垃圾文件
                    if (entry.getName().contains("__MACOSX") || entry.getName().startsWith(".")) continue;

                    // 解压
                    File extractedFile = extractEntry(zipFile, entry, tempDir);
                    extractedFiles.add(extractedFile);
                }
            }
        } catch (IOException e) {
            throw new BusinessException("ZIP处理失败: " + e.getMessage());
        } finally {
            // 下载完解压完，ZIP包本身就可以删除了，但解压出的文件要留给后面处理
            FileUtils.deleteQuietly(tempZip);
        }
        return extractedFiles;
    }

    private File extractEntry(ZipFile zipFile, ZipEntry entry, Path outputDir) throws IOException {
        String fileName = new File(entry.getName()).getName();
        File outputFile = new File(outputDir.toFile(), fileName);
        try (InputStream is = zipFile.getInputStream(entry);
             FileOutputStream fos = new FileOutputStream(outputFile)) {
            byte[] buffer = new byte[8192];
            int len;
            while ((len = is.read(buffer)) > 0) fos.write(buffer, 0, len);
        }
        return outputFile;
    }
}
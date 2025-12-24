package com.seeyon.apps.pdfCatchButton.utils;
import com.seeyon.cap4.form.bean.FormDataMasterBean;
import com.seeyon.ctp.common.AppContext;
import com.seeyon.ctp.common.authenticate.domain.User;
import com.seeyon.ctp.common.filemanager.manager.AttachmentManager;
import com.seeyon.ctp.common.log.CtpLogFactory;
import com.seeyon.ctp.common.po.filemanager.Attachment;
import com.seeyon.ctp.organization.bo.V3xOrgMember;
import com.seeyon.ctp.organization.manager.OrgManager;
import com.seeyon.ctp.util.UUIDLong;
import org.apache.commons.logging.Log;
import org.springframework.beans.factory.annotation.Value; // 【新增引用】
import org.springframework.stereotype.Component;
import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
/**
 * 附件绑定工具类
 * 修复：支持定时任务环境下的附件保存 (自动切换为管理员身份)
 */
@Component("allPdfCatchattachmentBindingKit")
public class AttachmentBindingKit {

    private static final Log LOGGER = CtpLogFactory.getLog(AttachmentBindingKit.class);

    // 【新增】注入配置文件中的管理员账号
    // 这样测试环境用 gjx666，生产环境用 admin，由 properties 文件决定
    @Value("${${pdf.mode}.admin.login.name}")
    private String adminLoginName;

    /**
     * 核心方法：绑定附件
     */
    public void bindAttachment(FormDataMasterBean masterBean, String fieldName, String fileId, File originFile) {
        try {
            // 1. 获取 AttachmentManager
            AttachmentManager attachmentManager = (AttachmentManager) AppContext.getBean("attachmentManager");
            if (attachmentManager == null) {
                throw new RuntimeException("附件管理器(AttachmentManager)未找到！");
            }

            // 2. 准备 SubReference
            Long subReference = null;
            Object currentVal = masterBean.getFieldValue(fieldName);
            if (currentVal != null && !"".equals(currentVal.toString())) {
                try {
                    subReference = Long.parseLong(currentVal.toString());
                } catch (NumberFormatException e) {
                    subReference = UUIDLong.longUUID();
                }
            } else {
                subReference = UUIDLong.longUUID();
            }

            // 3. 构建 Attachment 对象
            Attachment attachment = new Attachment();
            attachment.setIdIfNew();
            attachment.setReference(masterBean.getId());
            attachment.setSubReference(subReference);
            attachment.setCategory(66);
            attachment.setFileUrl(Long.parseLong(fileId));
            attachment.setFilename(originFile.getName());
            attachment.setMimeType(getMimeType(originFile.getName()));
            attachment.setCreatedate(new Date());
            attachment.setSize(originFile.length());
            attachment.setType(0);

            List<Attachment> atts = new ArrayList<>();
            atts.add(attachment);

            // =========================================================
            // 【核心修复】智能切换保存方式
            // =========================================================
            User currentUser = AppContext.getCurrentUser();

            if (currentUser != null) {
                // A. 按钮点击环境 (有登录人)：直接保存
                attachmentManager.create(atts);
            } else {
                // B. 定时任务环境 (无登录人)：切换为管理员身份保存
                System.out.println(">>> [附件保存] 检测到无登录用户(定时任务)，尝试使用管理员身份...");

                OrgManager orgManager = (OrgManager) AppContext.getBean("orgManager");
                if (orgManager != null) {
                    // 【修改点】使用变量 adminLoginName，不再硬编码 "gjx666"
                    V3xOrgMember admin = orgManager.getMemberByLoginName(adminLoginName);

                    if (admin != null) {
                        // 使用重载方法：显式传入 MemberID 和 AccountID，绕过 User 上下文检查
                        attachmentManager.create(atts, admin.getId(), admin.getOrgAccountId());
                        System.out.println(">>> [附件保存] 已使用管理员 [" + admin.getName() + "] 身份保存成功。");
                    } else {
                        LOGGER.error("严重错误：未找到配置的管理员用户 (" + adminLoginName + ")，附件保存可能失败！");
                        // 最后的挣扎
                        attachmentManager.create(atts);
                    }
                } else {
                    LOGGER.error("无法获取 OrgManager Bean。");
                }
            }

            // 4. 更新表单字段
            masterBean.addFieldValue(fieldName, subReference);
            LOGGER.info("附件绑定完成！SubRef: " + subReference + ", fileId: " + fileId);

        } catch (Exception e) {
            LOGGER.error("附件绑定异常", e);
            throw new RuntimeException(e);
        }
    }

    private String getMimeType(String fileName) {
        String name = fileName.toLowerCase();
        if (name.endsWith(".pdf")) return "application/pdf";
        if (name.endsWith(".jpg") || name.endsWith(".jpeg")) return "image/jpeg";
        if (name.endsWith(".png")) return "image/png";
        if (name.endsWith(".doc") || name.endsWith(".docx")) return "application/msword";
        return "application/octet-stream";
    }
}
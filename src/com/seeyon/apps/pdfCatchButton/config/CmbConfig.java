package com.seeyon.apps.pdfCatchButton.config;

import com.seeyon.apps.pdfCatchButton.controller.AllPdfCatchController;
import com.seeyon.ctp.common.log.CtpLogFactory;
import org.apache.commons.logging.Log;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import javax.annotation.PostConstruct;

/**
 * 全局招行配置中心
 * 所有的接口类直接访问这里的 public static 变量
 */
@Component("cmbConfigDetials")
public class CmbConfig {
    private static final Log LOGGER = CtpLogFactory.getLog(CmbConfig.class);

    // 1. 这里的 @Value 负责从 properties 文件里抓取值 (实例变量)
    @Value("${${cmb.mode}.cmb.url}")
    private String urlVal;

    @Value("${${cmb.mode}.cmb.uid}")
    private String uidVal;

    @Value("${${cmb.mode}.cmb.publicKey}")
    private String publicKeyVal;

    @Value("${${cmb.mode}.cmb.privateKey}")
    private String privateKeyVal;

    @Value("${${cmb.mode}.cmb.symKey}")
    private String symKeyVal;

    // 2. 定义 public static 变量，供外界（那些接口类）直接读取
    // 注意：变量名大写是静态常量的习惯，但这里是变量，为了区分我用全大写
    public static String URL;
    public static String UID;
    public static String PUBLIC_KEY;
    public static String PRIVATE_KEY;
    public static String SYM_KEY;
    // 【新增】定义一把全局锁对象
    // 任何拿到这个对象锁的任务才能运行，另一个必须等待
    public static final Object TASK_LOCK = new Object();

    @PostConstruct
    public void init() {
        // 3. Spring 初始化完后，把抓到的值赋值给静态变量
        URL = this.urlVal;
        UID = this.uidVal;
        PUBLIC_KEY = this.publicKeyVal;
        PRIVATE_KEY = this.privateKeyVal;
        SYM_KEY = this.symKeyVal;
        System.out.println("\n");
        System.out.println(">>> 招行全局配置已刷新，当前环境：" + URL);
        LOGGER.info(">>> 招行全局配置已刷新，当前环境：" + URL);
        System.out.println("\n");
    }
}
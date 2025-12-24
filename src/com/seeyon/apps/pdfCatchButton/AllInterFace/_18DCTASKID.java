package com.seeyon.apps.pdfCatchButton.AllInterFace;



import com.seeyon.apps.pdfCatchButton.config.CmbConfig;
import com.seeyon.apps.pdfCatchButton.utils.DcHelper;
import org.bouncycastle.crypto.CryptoException;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.Security;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;

/**
 * 招商银行银企直联国密免前置/SaaS对接示例，本示例仅供参考，不保证各种异常场景运行，请勿直接使用，如有错漏请联系对接人员。运行时，请使用所获取的测试资源替换 用户编号、公私钥、对称密钥、服务商编号等信息。
 *
 * @author cmb.firmbank
 * @date 2023/7/20
 * 账务查询 16.异步打印结果查询
 * 测试成功：返回结果 响应报文:
 * {"response":{"body":{"fileurl":"http://s3gw.cmburl.cn:8081/s/L2x4NTUwMS10YXNremlwLXVhdC8xOTUwMDA3NDAyOTcwMTYxMTU0LnppcD9BV1NBY2Nlc3NLZXlJZD1seDU1MDEtcHJ0c3ZyLXVhdCZFeHBpcmVzPTE3NTg5NTk5NTImU2lnbmF0dXJlPUlFU2ZDbk9yekVmMUlaJTJGbGRDMGUwQnludEx3JTNEJnJlc3BvbnNlLWNvbnRlbnQtZGlzcG9zaXRpb249YXR0YWNobWVudCUzQmZpbGVuYW1lJTNEMTk1MDAwNzQwMjk3MDE2MTE1NC56aXA=/MPm6LjBAHWvUiHpElDBMAuWCrWI=/KS-lx5501-prtsvr-uat-dmz/0","fintim":"2025-07-29 09:36:16"},"head":{"bizcode":"","funcode":"DCTASKID","reqid":"2025092615591003761693870","resultcode":"SUC0000","resultmsg":"","rspid":"202509261559121630001cdcserver-6cc4d9b58-g69d5","userid":"U005076306"}},"signature":{"sigdat":"6Zqrnt6EVPHwrJdCHPqTW/mmB4rG8fqLtoLs+49BHz8UvempVyE9d/VA4qkxGFUzqef9LG38aTrzH1FaLuaUNQ==","sigtim":"20250926155912"}}
 */
public class _18DCTASKID {

    private static final int BOUND_START = 1000000;
    private static final int BOUND_END = 9000000;
    private static Random random = new Random();

    private _18DCTASKID() {
    }

    public static String GET_PDFUrl(String taskid) throws GeneralSecurityException, IOException, CryptoException {
        // 装载BC库,必须在应用的启动类中调用此函数
        Security.addProvider(new BouncyCastleProvider());
        System.setProperty("sun.net.http.retryPost", "false");

        String funcode = "DCTASKID";  // 从接口文档获取接口名称

        // 准备接口数据，生产请替换为具体接口请求报文,包含所需的请求字段
        String currentDatetime = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
        String reqid = new SimpleDateFormat("yyyyMMddHHmmssSSSS").format(new Date()) + (BOUND_START + random.nextInt(BOUND_END));

        String data = "{\"request\":{\"body\":{\"taskid\":\""+taskid+"\","
                + "\"qwenab\":\"true\"},"
                + "\"head\":{\"funcode\":\"DCTASKID\",\"userid\":\"" + CmbConfig.UID + "\",\"reqid\":\"" + reqid + "\"}},"
                + "\"signature\":{\"sigdat\":\"__signature_sigdat__\",\"sigtim\":\"" + currentDatetime + "\"}}";

        DcHelper dchelper = new DcHelper(
                CmbConfig.URL,
                CmbConfig.UID,
                CmbConfig.PRIVATE_KEY,
                CmbConfig.PUBLIC_KEY,
                CmbConfig.SYM_KEY
        );
        String response = dchelper.sendRequest(data, funcode);
        return response;
    }



}

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
 * 账务查询 15.电子回单异步查询
 * 测试成功：返回结果 响应报文:
 * {"response":{"body":{"asycalhdz1":{"rtncod":"SUC0000","rtndat":"IV8x5VlRHmRUolw1APg7qq5Sm5DfxiH4Cbs6nkvJLVg=","rtnmsg":"","taskid":"1971484089255665666"}},"head":{"bizcode":"","funcode":"ASYCALHD","reqid":"2025092615564708037403535","resultcode":"SUC0000","resultmsg":"","rspid":"202509261556497900001cdcserver-6cc4d9b58-zw9sn","userid":"U005076306"}},"signature":{"sigdat":"tcuDg40nPX4vmXK94crEbzOZLAx9N09COV8Pq0G6jUfUqLxc6xUfTp9YsKrlRpX3+W89rBo9B44XSUDA8DL5aA==","sigtim":"20250926155649"}}
 *
 */
public class _17ASYCALHD {

    private static final int BOUND_START = 1000000;
    private static final int BOUND_END = 9000000;
    private static Random random = new Random();

    private _17ASYCALHD() {
    }

    public static String ID_Query(String companyName, String begdat, String enddat) throws GeneralSecurityException, IOException, CryptoException {
        // 装载BC库,必须在应用的启动类中调用此函数
        Security.addProvider(new BouncyCastleProvider());
        System.setProperty("sun.net.http.retryPost", "false");

        // 业务接口名，这里是查询分行号信息接口，生产请替换为对应接口名
        String funcode = "ASYCALHD";
        // 准备接口数据，生产请替换为具体接口请求报文,包含所需的请求字段
        String currentDatetime = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
        String eacnbr;
        if ("致远".equals(companyName)) {
            eacnbr = "575905368510006";
            System.out.println("---------------------------此次获取致远--------回单--------------------------------");
        } else if ("搭见".equals(companyName)) {
            eacnbr = "575905373410000";
            System.out.println("---------------------------此次获取搭见--------回单--------------------------------");
        } else {
            // 关键：遇到未知的公司名，必须抛出异常阻断流程，防止错发
            throw new IllegalArgumentException("未知的公司名称: " + companyName + "，无法匹配银行业务参数");
        }
        //begindat示例格式1 2025-11-10
        String reqid = new SimpleDateFormat("yyyyMMddHHmmssSSSS").format(new Date()) + (BOUND_START + random.nextInt(BOUND_END));
        String data = "{\"request\":{\"body\":{\"primod\":\"PDF\","
                + "\"eacnbr\":\""+ eacnbr + "\","
                + "\"begdat\":\""+begdat+"\","
                + "\"enddat\":\""+enddat+"\","
                + "\"rrcflg\":\"\","
                + "\"begamt\":\"\","
                + "\"endamt\":\"\","
                + "\"rrccod\":\"\"},"
                + "\"head\":{\"funcode\":\"ASYCALHD\",\"userid\":\"" + CmbConfig.UID + "\",\"reqid\":\"" + reqid + "\"}},"
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




    private static void process(String response) {
        System.out.println("返回结果 " + response);

    }
}

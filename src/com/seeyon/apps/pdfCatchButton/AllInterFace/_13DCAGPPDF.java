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
 */

/**
 * 代发代扣-13.代发明细对账单查询请求DCAGPPDF
 * 测试失败：Exception in thread "main" java.io.IOException: 访问目标地址 http://cdctest.cmburl.cn:80/cdcserver/api/v2 失败:CDCServer: DCAT009-您未开通直联业务 [N66010][测试]
 *
 * 	at com.yupi.DcHelper.sendRequest(DcHelper.java:115)
 * 	at com.yupi.DCAGPPDF.main(DCAGPPDF.java:72)
 */
public class _13DCAGPPDF {

    private static final int BOUND_START = 1000000;
    private static final int BOUND_END = 9000000;
    private static Random random = new Random();

    public static String idQuery(String companyName, String begdat, String enddat,String prtmod) throws GeneralSecurityException, IOException, CryptoException {
        // 装载BC库,必须在应用的启动类中调用此函数
        Security.addProvider(new BouncyCastleProvider());
        System.setProperty("sun.net.http.retryPost", "false");

        // 业务接口名，这里是代发明细对账单查询请求接口，生产请替换为对应接口名
        String funcode = "DCAGPPDF";
        // 准备接口数据，生产请替换为具体接口请求报文,包含所需的请求字段
        String currentDatetime = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String reqid = new SimpleDateFormat("yyyyMMddHHmmssSSSS").format(new Date()) + (BOUND_START + random.nextInt(BOUND_END));

        String buscod;
        String busmod;
        String payeac;


        if (companyName.contains("致远")) {
            buscod = "N03010";
            busmod = "S200E";
            payeac="575905368510006";
            System.out.println("---------------------------"+sdf.format(new Date())+"--此次抓取致远--------对账单 --------------------------------");
        } else if (companyName.contains("搭见")) {
            buscod = "N03020";
            busmod = "S200D";
            payeac="575905373410000";
            System.out.println("---------------------------"+sdf.format(new Date())+"--此次抓取搭见--------对账单--------------------------------");
        } else if (companyName.contains("测试")){
            buscod = "N03010";
            busmod = "S3017";
            payeac="128964530610000";
            System.out.println("---------------------------"+sdf.format(new Date())+"--此次抓取测试--------对账单--------------------------------");

        }
        else {
            // 关键：遇到未知的公司名，必须抛出异常阻断流程，防止错发
            return "未知的公司名称: " + companyName + "，无法匹配银行业务参数";

        }



        String data = "{\"request\":{\"body\":{\"payeac\":\""+payeac+"\",\"begdat\":\"" + begdat + "\"," +
                "\"enddat\":\"" + enddat + "\",\"buscod\":\""+buscod+"\",\"busmod\":\""+busmod+"\",\"eacnam\":\"\"," +
                "\"eacnbr\":\"\",\"ptyref\":\"\",\"trxsrl\":\"\"," +
                "\"minamt\":\"\",\"maxamt\":\"\",\"prtmod\":\""+prtmod+"\"," +
                "\"begidx\":\"0\",\"pagsiz\":\"200\",\"cntkey\":\"\"},\"head\":{\"funcode\":\"" + funcode + "\"," +
                "\"userid\":\"" + CmbConfig.UID + "\",\"reqid"
                + "\":\"" + reqid + "\"}},\"signature\":{\"sigdat\":\"__signature_sigdat__\"," +
                "\"sigtim\":\"" + currentDatetime + "\"}}";

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

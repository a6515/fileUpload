package com.seeyon.apps.pdfCatchButton.AllInterFace;

import com.seeyon.apps.pdfCatchButton.utils.DcHelper;
import com.seeyon.apps.pdfCatchButton.config.CmbConfig; // 记得导入你的配置类
import org.bouncycastle.crypto.CryptoException;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.Security;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;

public class _14DCTASKID {
    private static final int BOUND_START = 1000000;
    private static final int BOUND_END = 9000000;
    private static Random random = new Random();


    public static String pdfGet(String taskid) throws GeneralSecurityException, IOException, CryptoException {
        Security.addProvider(new BouncyCastleProvider());
        System.setProperty("sun.net.http.retryPost", "false");

        String funcode = "DCTASKID";
        String currentDatetime = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
        String reqid = new SimpleDateFormat("yyyyMMddHHmmssSSSS").format(new Date()) + (BOUND_START + random.nextInt(BOUND_END));

        // 注意：这里直接使用 CmbConfig.UID
        String data = "{\"request\":{\"body\":{\"taskid\":\""+taskid+"\"}," +
                "\"head\":{\"funcode\":\"" + funcode + "\"," +
                "\"userid\":\"" + CmbConfig.UID + "\",\"reqid\":\"" + reqid + "\"}}," +
                "\"signature\":{\"sigdat\":\"__signature_sigdat__\"," +
                "\"sigtim\":\"" + currentDatetime + "\"}}";

        // 【关键修改】直接从配置类取值传给 DcHelper
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
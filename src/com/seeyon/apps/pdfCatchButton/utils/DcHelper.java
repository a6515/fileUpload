package com.seeyon.apps.pdfCatchButton.utils;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.seeyon.ctp.common.log.CtpLogFactory;
import org.apache.commons.logging.Log;
import org.bouncycastle.asn1.*;
import org.bouncycastle.crypto.CryptoException;
import org.bouncycastle.crypto.params.ECDomainParameters;
import org.bouncycastle.crypto.params.ECPrivateKeyParameters;
import org.bouncycastle.crypto.params.ECPublicKeyParameters;
import org.bouncycastle.crypto.params.ParametersWithID;
import org.bouncycastle.crypto.signers.SM2Signer;
import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jce.spec.ECParameterSpec;
import org.bouncycastle.math.ec.ECPoint;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.net.ssl.*;
import java.io.*;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.util.*;


/**
 * 招商银行银企直联国密免前置/SaaS对接示例，本示例仅供参考，不保证各种异常场景运行，请勿直接使用，如有错漏请联系对接人员。运行时，请使用所获取的测试资源替换 用户编号、公私钥、对称密钥、服务商编号等信息。
 *
 * @author cmb.firmbank
 * @date 2023/7/20
 */
public class DcHelper {

    private static final int LENGTH_32 = 32;
    private static final int USERID_LEN = 16;
    private static final int CONNECT_TIMEOUT = 15000;
    private static final int READ_TIMEOUT = 60000;
    private static final int STATUS_OK = 200;
    private static Base64.Encoder encoder = Base64.getEncoder();
    private static Base64.Decoder decoder = Base64.getDecoder();

    // 请求URL
    private final String url;
    // 企业网银用户号
    private final String uid;
    // 国密算法向量，根据用户号生成
    private final byte[] userId;
    // 算法，固定为国密算法
    private final String alg;
    // 客户私钥
    private final byte[] privateKey;
    // 银行公钥
    private final byte[] publicKey;
    // 协商的对称密钥
    private final byte[] symKey;
    private static final Log LOGGER = CtpLogFactory.getLog(DcHelper.class);

    public DcHelper(String url, String uid, String privateKey, String publicKey, String symKey) {
        LOGGER.info("进入了DChelper的构造方法");
        this.url = url;
        this.uid = uid;
        this.userId = getUserId(uid);
        this.alg = "SM";
        this.privateKey = decoder.decode(privateKey);
        this.publicKey = decoder.decode(publicKey);
        this.symKey = symKey.getBytes(StandardCharsets.UTF_8);
    }

    public String sendRequest(String data, String funcode) throws IOException, CryptoException, GeneralSecurityException {
        LOGGER.info("进入了sendrequest方法");
        // 对请求报文做排序
        JsonObject requestJson = new Gson().fromJson(data, JsonObject.class);
        String source = recursiveKeySort(requestJson);
        // 生成签名
        byte[] signature = cmbSM2SignWithSM3(userId, privateKey, source.getBytes(StandardCharsets.UTF_8));
        // 替换签名字段
        requestJson.getAsJsonObject("signature").addProperty("sigdat", new String(encoder.encode(signature), StandardCharsets.UTF_8));

        // 对数据进行对称加密
        String request = requestJson.toString();
        byte[] encryptRequest = cmbSM4Crypt(symKey, userId, request.getBytes(StandardCharsets.UTF_8), 1);
        String encryptedRequest = new String(encoder.encode(encryptRequest), StandardCharsets.UTF_8);

        // 发送请求
        HashMap<String, String> map = new HashMap<>();
        map.put("UID", uid);
        map.put("ALG", alg);
        map.put("DATA", URLEncoder.encode(encryptedRequest, StandardCharsets.UTF_8.displayName()));
        map.put("FUNCODE", funcode);
        String response = httpPost(url, map);
        if (response.startsWith("CDCServer:")) {
            if (response.contains("错误次数已达上限")) {
                LOGGER.error("错误次数已达上限666666666");
                throw new IOException("接口调用次数已达当日上限，请明天再试: " + response);
            }
            LOGGER.error("访问目标地址 " + url + " 失败:" + response);
            throw new IOException("访问目标地址 " + url + " 失败:" + response);
        }

        // 返回结果解密
        response = new String((cmbSM4Crypt(symKey, userId, decoder.decode(response), 2)), StandardCharsets.UTF_8);

        // 验证签名是否正确
        JsonObject responseJson = new Gson().fromJson(response, JsonObject.class);
        JsonObject signatureJson = responseJson.getAsJsonObject("signature");
        String responseSignature = signatureJson.get("sigdat").getAsString();
        signatureJson.addProperty("sigdat", "__signature_sigdat__");
        responseJson.add("signature", signatureJson);
        String responseSorted = recursiveKeySort(responseJson);
        boolean verify = cmbSM2VerifyWithSM3(userId, publicKey, responseSorted.getBytes(StandardCharsets.UTF_8), decoder.decode(responseSignature));
        if (!verify) {
            throw new IOException("响应报文的签名无效");
        }
        LOGGER.info("sendrequest发送完成");
        return response;
    }

    private static String recursiveKeySort(JsonObject json) {
        StringBuilder appender = new StringBuilder();
        appender.append("{");
        Iterator<String> keys = new TreeSet<>(json.keySet()).iterator();
        boolean isFirstEle = true;
        while (keys.hasNext()) {
            if (!isFirstEle) {
                appender.append(",");
            }
            String key = keys.next();
            Object val = json.get(key);
            if (val instanceof JsonObject) {
                appender.append("\"").append(key).append("\":");
                appender.append(recursiveKeySort((JsonObject)val));
            } else if (val instanceof JsonArray) {
                JsonArray jarray = (JsonArray)val;
                appender.append("\"").append(key).append("\":[");
                boolean isFirstArrEle = true;
                for (int i = 0; i < jarray.size(); i++) {
                    if (!isFirstArrEle) {
                        appender.append(",");
                    }
                    Object obj = jarray.get(i);
                    if (obj instanceof JsonObject) {
                        appender.append(recursiveKeySort((JsonObject)obj));
                    } else {
                        appender.append(obj.toString());
                    }
                    isFirstArrEle = false;
                }
                appender.append("]");
            } else {
                String value = val.toString();
                appender.append("\"").append(key).append("\":").append(value);
            }
            isFirstEle = false;
        }
        appender.append("}");
        return appender.toString();
    }

    private static byte[] getUserId(String uid) {
        return (uid + "0000000000000000").substring(0, USERID_LEN).getBytes();
    }

    private static String httpPost(String httpUrl, Map<String, String> param) throws IOException, GeneralSecurityException {
        HttpURLConnection connection = null;
        String result;
        try {
            URL url = new URL(httpUrl);
            SSLContext sslcontext;
            sslcontext = SSLContext.getInstance("SSL");
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init((KeyStore)null);
            X509TrustManager defaultTm = null;
            for (TrustManager tm : tmf.getTrustManagers()) {
                if (tm instanceof X509TrustManager) {
                    defaultTm = (X509TrustManager)tm;
                    break;
                }
            }
            sslcontext.init(null, new TrustManager[]{defaultTm}, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sslcontext.getSocketFactory());

            connection = (HttpURLConnection)url.openConnection();
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(CONNECT_TIMEOUT);
            connection.setReadTimeout(READ_TIMEOUT);
            connection.setInstanceFollowRedirects(true);

            connection.setDoOutput(true);
            connection.setDoInput(true);

            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            try (OutputStream os = connection.getOutputStream()) {
                os.write(createLinkString(param).getBytes());
                if (connection.getResponseCode() != STATUS_OK) {
                    InputStream is = connection.getErrorStream();
                    BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
                    StringBuilder sbf = new StringBuilder();
                    String temp;
                    while ((temp = br.readLine()) != null) {
                        sbf.append(temp);
                        sbf.append("\r\n");
                    }

                    result = sbf.toString();
                    br.close();
                    is.close();
                } else {
                    InputStream is = connection.getInputStream();
                    BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
                    StringBuilder sbf = new StringBuilder();
                    String temp;
                    while ((temp = br.readLine()) != null) {
                        sbf.append(temp);
                    }
                    result = sbf.toString();
                    br.close();
                    is.close();
                }
            }
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
        return result;
    }

    private static String createLinkString(Map<String, String> params) {
        ArrayList<String> keys = new ArrayList<>(params.keySet());
        Collections.sort(keys);

        StringBuilder prestr = new StringBuilder();
        for (int i = 0; i < keys.size(); i++) {
            String key = keys.get(i);
            String value = params.get(key);
            if (i == keys.size() - 1) {
                prestr.append(key).append("=").append(value);
            } else {
                prestr.append(key).append("=").append(value).append("&");
            }
        }
        return prestr.toString();
    }

    // 以下是加解密相关的函数

    private static byte[] cmbSM2SignWithSM3(byte[] id, byte[] privkey, byte[] msg) throws IOException, CryptoException {
        if (privkey == null || msg == null) {
            throw new CryptoException("CMBSM2SignWithSM3 input error");
        }
        ECPrivateKeyParameters privateKey = encodePrivateKey(privkey);
        SM2Signer signer = new SM2Signer();
        ParametersWithID parameters = new ParametersWithID(privateKey, id);
        signer.init(true, parameters);
        signer.update(msg, 0, msg.length);
        return decodeDERSignature(signer.generateSignature());
    }

    private static ECPrivateKeyParameters encodePrivateKey(byte[] value) {
        BigInteger d = new BigInteger(1, value);
        ECParameterSpec spec = ECNamedCurveTable.getParameterSpec("sm2p256v1");
        ECDomainParameters ecParameters = new ECDomainParameters(spec.getCurve(), spec.getG(), spec.getN(), spec.getH(), spec.getSeed());
        return new ECPrivateKeyParameters(d, ecParameters);
    }

    private static byte[] decodeDERSignature(byte[] signature) throws IOException {
        ASN1InputStream stream = new ASN1InputStream(new ByteArrayInputStream(signature));
        ASN1Sequence primitive = (ASN1Sequence)stream.readObject();
        Enumeration<ASN1Integer> enumeration = primitive.getObjects();
        BigInteger intR = enumeration.nextElement().getValue();
        BigInteger intS = enumeration.nextElement().getValue();
        byte[] bytes = new byte[LENGTH_32 * 2];
        byte[] r = format(intR.toByteArray());
        byte[] s = format(intS.toByteArray());
        System.arraycopy(r, 0, bytes, 0, LENGTH_32);
        System.arraycopy(s, 0, bytes, LENGTH_32, LENGTH_32);
        return bytes;
    }

    private static byte[] format(byte[] value) {
        if (value.length == LENGTH_32) {
            return value;
        } else {
            byte[] bytes = new byte[LENGTH_32];
            if (value.length > LENGTH_32) {
                System.arraycopy(value, value.length - LENGTH_32, bytes, 0, LENGTH_32);
            } else {
                System.arraycopy(value, 0, bytes, LENGTH_32 - value.length, value.length);
            }
            return bytes;
        }
    }

    private static byte[] cmbSM4Crypt(byte[] key, byte[] iv, byte[] input, int mode) throws GeneralSecurityException {
        SecretKeySpec spec = new SecretKeySpec(key, "SM4");
        IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);
        Cipher cipher = Cipher.getInstance("SM4/CBC/PKCS7Padding", BouncyCastleProvider.PROVIDER_NAME);
        cipher.init(mode, spec, ivParameterSpec);
        return cipher.doFinal(input);
    }

    private static boolean cmbSM2VerifyWithSM3(byte[] id, byte[] pubkey, byte[] msg, byte[] signature) throws IOException {

        if (pubkey == null || msg == null || signature == null) {
            throw new IllegalArgumentException("CMBSM2VerifyWithSM3 input error");
        }
        ECPublicKeyParameters publicKey = encodePublicKey(pubkey);
        SM2Signer signer = new SM2Signer();
        ParametersWithID parameters = new ParametersWithID(publicKey, id);
        signer.init(false, parameters);
        signer.update(msg, 0, msg.length);
        return signer.verifySignature(encodeDERSignature(signature));
    }

    private static ECPublicKeyParameters encodePublicKey(byte[] value) {
        byte[] x = new byte[LENGTH_32];
        byte[] y = new byte[LENGTH_32];
        System.arraycopy(value, 1, x, 0, LENGTH_32);
        System.arraycopy(value, LENGTH_32 + 1, y, 0, LENGTH_32);
        BigInteger intX = new BigInteger(1, x);
        BigInteger intY = new BigInteger(1, y);
        ECParameterSpec spec = ECNamedCurveTable.getParameterSpec("sm2p256v1");
        ECPoint intQ = spec.getCurve().createPoint(intX, intY);
        ECDomainParameters ecParameters = new ECDomainParameters(spec.getCurve(), spec.getG(), spec.getN(), spec.getH(), spec.getSeed());
        return new ECPublicKeyParameters(intQ, ecParameters);
    }

    private static byte[] encodeDERSignature(byte[] signature) throws IOException {
        byte[] r = new byte[LENGTH_32];
        byte[] s = new byte[LENGTH_32];
        System.arraycopy(signature, 0, r, 0, LENGTH_32);
        System.arraycopy(signature, LENGTH_32, s, 0, LENGTH_32);
        ASN1EncodableVector vector = new ASN1EncodableVector();
        vector.add(new ASN1Integer(new BigInteger(1, r)));
        vector.add(new ASN1Integer(new BigInteger(1, s)));
        return (new DERSequence(vector)).getEncoded();
    }

}

package com.hao14293.im.common.utils;

import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Base64;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

/**
 * 签名加解密
 * @Author: hao14293
 * @Date: 2023/6/1
 */
public class SigAPI {

    private final long appId;
    private final String key;

    public SigAPI(long appId, String key) {
        this.appId = appId;
        this.key = key;
    }

    /**
     * 解密方法
     * @param userSig
     * @return
     */
    public static JSONObject decodeUserSig(String userSig) {
        JSONObject sigDoc = new JSONObject(true);

        try {
            byte[] decodeUrlByte = Base64URL.base64DecodeUrlNotReplace(userSig.getBytes());
            byte[] decompressByte = decompress(decodeUrlByte);
            String decodeText = new String(decompressByte, "UTF-8");

            if (StringUtils.isNotBlank(decodeText)) {
                sigDoc = JSONObject.parseObject(decodeText);
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return sigDoc;
    }

    /**
     * 解压缩
     * @param data
     * @return
     */
    public static byte[] decompress(byte[] data) {
        byte[] output = new byte[0];

        Inflater decompresser = new Inflater();
        decompresser.reset();
        decompresser.setInput(data);

        ByteArrayOutputStream o = new ByteArrayOutputStream(data.length);
        try {
            byte[] buf = new byte[1024];
            while (!decompresser.finished()) {
                int i = decompresser.inflate(buf);
                o.write(buf, 0, i);
            }
            output = o.toByteArray();
        } catch (Exception e) {
            output = data;
            e.printStackTrace();
        } finally {
            try {
                o.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        decompresser.end();
        return output;
    }

    public String genUserSig(String userId, long expire) {
        return genUserSig(userId, expire, null);
    }

    private String hmacsha256(String identifier, long currTime, long expire, String base64Userbuf) {
        String contentToBeSigned = "TLS.identifier:" + identifier + "\n"
                + "TLS.appId:" + appId + "\n"
                + "TLS.expireTime:" + currTime + "\n"
                + "TLS.expire:" + expire + "\n";
        if (null != base64Userbuf) {
            contentToBeSigned += "TLS.userbuf:" + base64Userbuf + "\n";
        }
        try {
            byte[] byteKey = key.getBytes(StandardCharsets.UTF_8);
            Mac hmac = Mac.getInstance("HmacSHA256");
            SecretKeySpec keySpec = new SecretKeySpec(byteKey, "HmacSHA256");
            hmac.init(keySpec);
            byte[] byteSig = hmac.doFinal(contentToBeSigned.getBytes(StandardCharsets.UTF_8));
            return (Base64.getEncoder().encodeToString(byteSig)).replaceAll("\\s*", "");
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            return "";
        }
    }

    private String genUserSig(String userId, long expire, byte[] userbuf) {
        long curTime = System.currentTimeMillis() / 1000;

        JSONObject sigDoc = new JSONObject();
        sigDoc.put("TLS.identifier", userId);
        sigDoc.put("TLS.appid", appId);
        sigDoc.put("TLS.expire", expire);
        sigDoc.put("TLS.expireTime", curTime);

        String base64UserBuf = null;
        if (null != userbuf) {
            base64UserBuf = Base64.getEncoder().encodeToString(userbuf).replaceAll("\\s*", "");
            sigDoc.put("TLS.userbuf", base64UserBuf);
        }
        String sig = hmacsha256(userId, curTime, expire, base64UserBuf);
        if (sig.length() == 0) {
            return "";
        }
        sigDoc.put("TLS.sig", sig);
        Deflater compressor = new Deflater();
        compressor.setInput(sigDoc.toString().getBytes(StandardCharsets.UTF_8));
        compressor.finish();
        byte[] compressedBytes = new byte[2048];
        int compressedBytesLength = compressor.deflate(compressedBytes);
        compressor.end();
        return (new String(Base64URL.base64EncodeUrl(Arrays.copyOfRange(compressedBytes,
                0, compressedBytesLength)))).replaceAll("\\s*", "");
    }

    public String getUserSig(String userId, long expire, long time, byte[] userbuf) {
        JSONObject sigDoc = new JSONObject();
        sigDoc.put("TLS.identifier", userId);
        sigDoc.put("TLS.appId", appId);
        sigDoc.put("TLS.expire", expire);
        sigDoc.put("TLS.expireTime", time);

        String base64UserBuf = null;
        if (null != userbuf) {
            base64UserBuf = Base64.getEncoder().encodeToString(userbuf).replaceAll("\\s*", "");
            sigDoc.put("TLS.userbuf", base64UserBuf);
        }
        String sig = hmacsha256(userId, time, expire, base64UserBuf);
        if (sig.length() == 0) {
            return "";
        }

        sigDoc.put("TLS.sig", sig);
        Deflater compressor = new Deflater();
        compressor.setInput(sigDoc.toString().getBytes(StandardCharsets.UTF_8));
        compressor.finish();
        byte[] compressedBytes = new byte[2048];
        int compressedBytesLength = compressor.deflate(compressedBytes);
        compressor.end();
        return (new String(Base64URL.base64EncodeUrl(Arrays.copyOfRange(compressedBytes,
                0, compressedBytesLength)))).replaceAll("\\s*", "");
    }
}

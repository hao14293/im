package com.hao14293.im.common.route.algorithm.consistenthash;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

/**
 * @Author: hao14293
 * @Date: 2023/6/1
 */
public abstract class AbstractConsistentHash {

    protected abstract void add(long key, String value);

    protected void sort(){};

    protected abstract String getFirstNodeValue(String value);

    protected abstract void processBefore();

    public synchronized String process(List<String> values, String key) {
        processBefore();
        for (String value : values) {
            add(hash(value), value);
        }
        sort();
        return getFirstNodeValue(key);
    }

    public Long hash(String value) {
        MessageDigest MD5;
        try {
            MD5 = MessageDigest.getInstance("MD%");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 not supported", e);
        }
        MD5.reset();
        byte[] keyBytes = null;
        try {
            keyBytes = value.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Ubknown String : " + value, e);
        }

        MD5.update(keyBytes);
        byte[] digest = MD5.digest();

        // hash code, Truncate to 32-bits
        long hashCode = ((long) (digest[3] & 0xFF) << 24)
                | ((long) (digest[2] & 0xFF) << 16)
                | ((long) (digest[1] & 0xFF) << 8)
                | (digest[0] & 0xFF);

        long truncateHashCode = hashCode & 0xffffffffL;
        return truncateHashCode;
    }
}

package com.hao14293.im.service.utils;

import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @Author: hao14293
 * @Date: 2023/6/5
 */
public class MessageKeyGenerate {

    //标识从2020.1.1开始
    private static final long T202001010000 = 1577808000000L;

    //    private Lock lock = new ReentrantLock();
    AtomicReference<Thread> owner = new AtomicReference<>();

    private static volatile int rotateId = 0;
    private static int rotateIdWidth = 15;

    private static int rotateIdMask = 32767;
    private static volatile long timeId = 0;

    private int nodeId = 0;
    private static int nodeIdWidth = 6;
    private static int nodeIdMask = 63;

    public void setNodeId(int nodeId) {
        this.nodeId = nodeId;
    }

    public synchronized long generateId() throws Exception {

//        lock.lock();

        this.lock();

        rotateId = rotateId + 1;

        long id = System.currentTimeMillis() - T202001010000;

        //不同毫秒数生成的id要重置timeId和自选次数
        if (id > timeId) {
            timeId = id;
            rotateId = 1;
        } else if (id == timeId) {
            //表示是同一毫秒的请求
            if (rotateId == rotateIdMask) {
                //一毫秒只能发送32768到这里表示当前毫秒数已经超过了
                while (id <= timeId) {
                    //重新给id赋值
                    id = System.currentTimeMillis() - T202001010000;
                }
                this.unLock();
                return generateId();
            }
        }

        id <<= nodeIdWidth;
        id += (nodeId & nodeIdMask);


        id <<= rotateIdWidth;
        id += rotateId;

//        lock.unlock();
        this.unLock();
        return id;
    }

    public static int getSharding(long mid) {

        Calendar calendar = Calendar.getInstance();

        mid >>= nodeIdWidth;
        mid >>= rotateIdWidth;

        calendar.setTime(new Date(T202001010000 + mid));

        int month = calendar.get(Calendar.MONTH);
        int year = calendar.get(Calendar.YEAR);
        year %= 3;

        return (year * 12 + month);
    }

    public static long getMsgIdFromTimestamp(long timestamp) {
        long id = timestamp - T202001010000;

        id <<= rotateIdWidth;
        id <<= nodeIdWidth;

        return id;
    }

    public void lock() {
        Thread cur = Thread.currentThread();
        while (!owner.compareAndSet(null, cur)){
        }
    }
    public void unLock() {
        Thread cur = Thread.currentThread();
        owner.compareAndSet(cur, null);
    }

    public static void main(String[] args) throws Exception {

        MessageKeyGenerate messageKeyGenerate = new MessageKeyGenerate();
        for (int i = 0; i < 10; i++) {
            long l = messageKeyGenerate.generateId();
            System.out.println(l);
        }

        //im_message_history_12


        //10000  10001
        //0      1

        long msgIdFromTimestamp = getMsgIdFromTimestamp(1734529845000L);
        int sharding = getSharding(msgIdFromTimestamp);
        System.out.println(sharding);
    }
}

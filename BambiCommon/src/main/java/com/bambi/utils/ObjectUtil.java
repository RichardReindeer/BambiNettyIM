package com.bambi.utils;

import com.alibaba.fastjson.JSON;
import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;

/**
 * 描述：
 *      <br><b>字符串转对象工具类</b><br>
 *      提供Gson以及FastJson两种api
 * <pre>
 * HISTORY
 * ****************************************************************************
 *  ID     DATE          PERSON          REASON
 *  1      2023/3/1 21:41    Bambi        Create
 * ****************************************************************************
 * </pre>
 *
 * @author Bambi
 * @since 1.0
 */
public class ObjectUtil {
    private static Logger logger = LoggerFactory.getLogger(ObjectUtil.class);

    public static byte[] Object2JsonBytes(Object obj) {

        //尽量把对象转换成JSON保存更稳妥

        String json = ObjectToJson(obj);
        try {
            return json.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static <T> T JsonBytes2Object(byte[] bytes, Class<T> tClass) {

        //尽量把对象转换成JSON保存更稳妥
        try {
            String json = new String(bytes, "UTF-8");
            T t = JsonToObjectByFastJson(json, tClass);
            return t;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 使用Gson转换成字符串
     *
     * @param obj
     * @return
     */
    public static String ObjectToJsonByGson(Object obj) {

        String json = new Gson().toJson(obj);
        return json;
    }

    /**
     * 使用fastJson将字符串转换成对象
     * @param json
     * @param tClass
     * @return
     * @param <T>
     */
    public static <T> T JsonToObjectByFastJson(String json, Class<T> tClass) {
        T t = JSON.parseObject(json, tClass);
        return t;
    }

    public static byte[] ObjectToByte(Object obj) {
        byte[] bytes = null;
        try {
            // object to bytearray
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            ObjectOutputStream oo = new ObjectOutputStream(bo);
            oo.writeObject(obj);

            bytes = bo.toByteArray();

            bo.close();
            oo.close();


        } catch (Exception e) {
            System.out.println("translation" + e.getMessage());
            e.printStackTrace();
        }
        return bytes;
    }


    public static Object ByteToObject(byte[] bytes) {
        Object obj = null;
        try {
            // bytearray to object
            ByteArrayInputStream bi = new ByteArrayInputStream(bytes);
            ObjectInputStream oi = new ObjectInputStream(bi);

            obj = oi.readObject();
            bi.close();
            oi.close();
        } catch (Exception e) {
            System.out.println("translation" + e.getMessage());
            e.printStackTrace();
        }
        return obj;
    }
}

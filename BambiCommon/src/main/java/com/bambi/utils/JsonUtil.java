package com.bambi.utils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;

/**
 * 描述：
 * <br><b>Json转换工具类</b><br>
 * 因为fastJson与Gson各有各的优点和缺点,<br>
 * 如fastJson有很强的反序列化功能<br>
 * Gson有很强的Pojo2Json功能<br>
 * 所以可以将两种api一起混合使用，并设计此工具类来方便调用
 * <pre>
 * HISTORY
 * ****************************************************************************
 *  ID     DATE          PERSON          REASON
 *  1      2023/3/1 15:29    Bambi        Create
 * ****************************************************************************
 * </pre>
 *
 * @author Bambi
 * @since 1.0
 */
public class JsonUtil {
    private static Logger logger = LoggerFactory.getLogger(JsonUtil.class);

    static GsonBuilder gb = new GsonBuilder();

    private static final Gson gson;

    static {
        gb.disableHtmlEscaping();
        gson = gb.create();
    }
    /**
     * 将Object转换为json后，在进一步转化为数组(byte)
     * @param obj
     * @return
     */
    public static byte[] object2JsonBytes(Object obj) {

        //把对象转换成JSON

        String json = pojoToJsonByGson(obj);
        try {
            return json.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return null;
    }


    /**
     * 使用谷歌 Gson 将 POJO 转成字符串
     *
     * @param obj
     * @return
     */
    public static String pojoToJsonByGson(Object obj) {
        String json = gson.toJson(obj);
        return json;
    }


    public static <T> T jsonBytes2Object(byte[] bytes, Class<T> tClass) {

        //尽量把对象转换成JSON保存更稳妥
        try {
            String json = new String(bytes, "UTF-8");
            T t = jsonToPojoByGson(json, tClass);
            return t;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 使用谷歌 Gson  将字符串转成 POJO对象
     *
     * @param json
     * @param tClass
     * @param <T>
     * @return
     */
    public static <T> T jsonToPojoByGson(String json, Class<T> tClass) {
        T t = gson.fromJson(json, tClass);
        return t;
    }

    /**
     * 使用阿里 Fastjson 将字符串转成 POJO对象
     *
     * @param json
     * @param type
     * @param <T>
     * @return
     */
    public static <T> T jsonToPojoByFastJson(String json, TypeReference<T> type) {
        T t = JSON.parseObject(json, type);
        return t;
    }

    /**
     * 使用 谷歌 json 将字符串转成 POJO对象
     *
     * @param json
     * @param type
     * @param <T>
     * @return
     */
    public static <T> T jsonToPojoByGson(String json, Type type) {
        T t = gson.fromJson(json, type);
        return t;
    }
}

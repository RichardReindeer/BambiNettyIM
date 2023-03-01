package com.bambi.config;

import io.netty.util.AttributeKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 描述：
 * <br><b>系统配置类</b><br>
 * 内部存有zk主路径<br>
 * 服务器端使用zk来进行分布式命名的注册，会创建对应的临时节点，根据临时节点获取对应的服务器节点id
 * <pre>
 * HISTORY
 * ****************************************************************************
 *  ID     DATE          PERSON          REASON
 *  1      2023/2/22 6:38    Bambi        Create
 * ****************************************************************************
 * </pre>
 *
 * @author Bambi
 * @since 1.0
 */
public class SystemConfig {
    private static Logger logger = LoggerFactory.getLogger(SystemConfig.class);


    //工作节点的父路径
    public static final String MANAGE_PATH = "/bambiIm/nodes";

    //工作节点的路径前缀
    public static final String PATH_PREFIX = MANAGE_PATH + "/seq-";
    public static final String PATH_PREFIX_NO_STRIP = "seq-";

    public static final String WEB_URL = "http://localhost:8080";


    public static final AttributeKey<String> CHANNEL_NAME =
            AttributeKey.valueOf("CHANNEL_NAME");

}

package com.bambi.zk;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 描述：
 *      <br><b>Zk客户端创建类</b><br>
 *      使用Curator源码中的示例创建简单的ZK client，如果想进行扩展或修改，可参考curator源码
 * <pre>
 * HISTORY
 * ****************************************************************************
 *  ID     DATE          PERSON          REASON
 *  1      2023/3/1 15:16    Bambi        Create
 * ****************************************************************************
 * </pre>
 *
 * @author Bambi
 * @since 1.0
 */
public class ClientFactory {
    private static Logger logger = LoggerFactory.getLogger(ClientFactory.class);

    // 重试策略等待时长
    private static Integer sleepTimeMS = 1000;
    // 最大重试次数
    private static Integer maxRetries = 3;

    /**
     * 使用简单的方式创建CuratorFramework<br>
     * @param connectString 链接地址
     * @param timeout 超时时长
     * @return
     */
    public static CuratorFramework createClientInSimple(String connectString, String timeout) {
        logger.info("create curatorFramework");
        /**
         * 创建拒绝策略
         */
        ExponentialBackoffRetry exponentialBackoffRetry = new ExponentialBackoffRetry(sleepTimeMS, maxRetries);
        return CuratorFrameworkFactory.newClient(connectString,Integer.parseInt(timeout),Integer.parseInt(timeout),exponentialBackoffRetry);
    }

    /**
     * 添加可选参数的方式创建对应的client<br>
     * @param connectingString 链接地址
     * @param retry 重试策略
     * @param connectionTimeoutMs  超时等待时间(毫秒)
     * @param sessionTimeoutMs session 超时时间(毫秒)
     * @return
     */
    public static CuratorFramework createClientWithOptions(
            String connectingString,
            ExponentialBackoffRetry retry,
            int connectionTimeoutMs,
            int sessionTimeoutMs
    ) {
        return CuratorFrameworkFactory.builder()
                .connectString(connectingString)
                .retryPolicy(retry)
                .connectionTimeoutMs(connectionTimeoutMs)
                .sessionTimeoutMs(sessionTimeoutMs)
                .build();
    }
}

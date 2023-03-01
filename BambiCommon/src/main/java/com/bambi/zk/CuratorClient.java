package com.bambi.zk;

import com.bambi.utils.SpringContextUtil;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.utils.CloseableUtils;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 描述：
 *      <br><b>Zk客户端</b><br>
 *      使用单例模式创建<br>
 *      根据系统中的分布式系统命名设计，成功启动一个服务器之后，就会在zk中注册一个临时顺序节点<br>
 *      本类中继承一些对zk节点的基本操作<br>
 *      在服务器端代码ZookeeperConfig中将其注入spring
 * <pre>
 * HISTORY
 * ****************************************************************************
 *  ID     DATE          PERSON          REASON
 *  1      2023/3/1 15:17    Bambi        Create
 * ****************************************************************************
 * </pre>
 *
 * @author Bambi
 * @since 1.0
 */
public class CuratorClient {
    private static Logger logger = LoggerFactory.getLogger(CuratorClient.class);

    private final String zkSessionTimeOut;
    private CuratorFramework client;
    private String zkAddress = "127.0.0.1:2181";
    public static CuratorClient instance = null; // 单例
    private static CuratorClient singleton = null;

    // 在
    public static CuratorClient getSingleton() {
        if (null == singleton) {
            singleton = SpringContextUtil.getBean("CuratorClient");
        }
        return singleton;
    }

    public CuratorClient(String zkSessionTimeOut, String zkAddress) {
        this.zkSessionTimeOut = zkSessionTimeOut;
        this.zkAddress = zkAddress;
        // 初始化
        init();
    }

    /**
     * 如果已经存在client 则直接返回
     */
    public void init() {
        if (null != client) {
            return;
        }
        client = ClientFactory.createClientInSimple(zkAddress, zkSessionTimeOut);
        client.start();
        instance = this;
    }

    /**
     * 关闭客户端链接
     */
    public void destroy() {
        CloseableUtils.closeQuietly(client);
    }

    /**
     * 创建 持久性节点
     * @param zkPath 节点存储路径
     * @param data 携带数据
     */
    public void createZNode(String zkPath, String data) {
        logger.debug("createZNode is starting !!!");

        try {
            // 创建装填节点数据的数组
            byte[] payload = "to set content".getBytes("UTF-8");
            if(data!=null){
                payload = data.getBytes("UTF-8");
            }
            client.create()
                    .creatingParentsIfNeeded()
                    .withMode(CreateMode.PERSISTENT)
                    .forPath(zkPath,payload);
        }  catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * 删除节点<br>
     * 目前采用最简单的删除方式，如果需要在后台尝试删除请不要用这个方法<br>
     * @param zkPath 节点路径
     */
    public void deleteZNode(String zkPath){
        try {
            if(checkZNodeExist(zkPath)){
                client.delete().forPath(zkPath);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 检查节点是否存在
     * @param zkPath
     * @return 如果节点不存在 则返回false
     */
    public boolean checkZNodeExist(String zkPath){
        logger.debug("checkZNodeExist is starting ");
        try {
            Stat stat = client.checkExists().forPath(zkPath);
            if(stat == null){
                logger.info("节点不存在 path : {}",zkPath);
                return false;
            }else {
                logger.info("节点存在");
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 创建临时的顺序节点
     * @param zkPath 节点路径
     * @return 返回节点路径
     */
    public String createEphemeralSeqNode(String zkPath){
        try {
            String s = client.create().creatingParentsIfNeeded()
                    .withMode(CreateMode.EPHEMERAL_SEQUENTIAL)
                    .forPath(zkPath);
            return s;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public String getZkSessionTimeOut() {
        return zkSessionTimeOut;
    }

    public CuratorFramework getClient() {
        return client;
    }

    public void setClient(CuratorFramework client) {
        this.client = client;
    }

    public String getZkAddress() {
        return zkAddress;
    }

    public void setZkAddress(String zkAddress) {
        this.zkAddress = zkAddress;
    }

    public static CuratorClient getInstance() {
        return instance;
    }

    public static void setInstance(CuratorClient instance) {
        CuratorClient.instance = instance;
    }

    public static void setSingleton(CuratorClient singleton) {
        CuratorClient.singleton = singleton;
    }
}

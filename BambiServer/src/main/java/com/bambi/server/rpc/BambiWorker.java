package com.bambi.server.rpc;

import com.bambi.bean.entity.BambiNode;
import com.bambi.config.SystemConfig;
import com.bambi.utils.JsonUtil;
import com.bambi.zk.CuratorClient;
import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * 描述：
 * <br><b>Worker</b><br>
 * 实现命名服务的具体逻辑，使用zk获取对应的分布式命名<br>
 * 此类是逻辑处理类，不支持序列化
 * <pre>
 * HISTORY
 * ****************************************************************************
 *  ID     DATE          PERSON          REASON
 *  1      2023/2/22 2:39    Bambi        Create
 * ****************************************************************************
 * </pre>
 *
 * @author Bambi
 * @since 1.0
 */
public class BambiWorker {
    private static Logger logger = LoggerFactory.getLogger(BambiWorker.class);
    // ZK 相关
    private CuratorFramework client = null;
    //
    private String pathRegistered = null;
    private boolean inited = false;

    private BambiNode localNode;
    private static BambiWorker singleInstance = null; // 单例创建

    private BambiWorker() {
    }

    // 创建Worker
    public synchronized static BambiWorker getInstance() {
        if (singleInstance == null) {
            singleInstance = new BambiWorker();
            singleInstance.localNode = new BambiNode();
        }
        return singleInstance;
    }

    /**
     * 创建临时节点
     */
    public synchronized void init() {
        // 1. 判断是否已经创建过
        if (inited) {
            return;
        }
        inited = true;
        // 2. 获取客户端对象
        if (client == null) {
            // client = CuratorClient.instance.getClient(); // 因为在构造函数中对客户端进行了初始化
            client = new CuratorClient("3000", "127.0.0.1:2181").getClient(); // TODO 测试用
        }
        if (localNode == null) {
            localNode = new BambiNode();
        }
        // 3. 删除空的父亲节点
        deleteEmptyParentNode(SystemConfig.MANAGE_PATH);
        // 4. 创建父节点
        createParentNodeIfNeeded(SystemConfig.MANAGE_PATH);
        try {
            // 5. 创建ZNode
            byte[] payload = JsonUtil.object2JsonBytes(localNode); // 将当前worker的基础数据放入当前的zk节点中
            pathRegistered = client.create()
                    .creatingParentsIfNeeded()
                    .withMode(CreateMode.EPHEMERAL_SEQUENTIAL)
                    .forPath(SystemConfig.PATH_PREFIX, payload);
            logger.debug("打印测试 pathRegistered {}", pathRegistered);
            // 6. 通过节点编号获取id
            localNode.setId(getId());
            logger.debug("本地节点 的 path {} , id = {}", pathRegistered, localNode.getId());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private long getId() {
        return getIdByZkPath(pathRegistered);
    }

    /**
     * 根据节点编号获取对应id
     *
     * @param pathRegistered 节点路径
     * @return
     */
    public long getIdByZkPath(String pathRegistered) {
        if (pathRegistered == null) {
            logger.error("zk path is error");
            throw new RuntimeException("节点路径出错");
        }
        String sid = null;
        int i = pathRegistered.lastIndexOf(SystemConfig.PATH_PREFIX);
        if (i >= 0) {
            i += SystemConfig.PATH_PREFIX.length();
            sid = i <= pathRegistered.length() ? pathRegistered.substring(i) : null;
        }
        if (sid == null) {
            throw new RuntimeException("sid获取异常");
        }
        return Long.parseLong(sid);
    }

    /**
     * 创建父节点<br>
     * 父节点不是临时节点
     *
     * @param managePath
     */
    private void createParentNodeIfNeeded(String managePath) {
        try {
            Stat stat = client.checkExists().forPath(managePath);
            if (stat == null) {
                client.create()
                        .creatingParentsIfNeeded()
                        .withMode(CreateMode.PERSISTENT)
                        .forPath(managePath);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 删除空的父节点
     *
     * @param managePath 父节点路径
     */
    private void deleteEmptyParentNode(String managePath) {
        int i = managePath.lastIndexOf("/");
        String parentPath = managePath.substring(0, i);
        if (checkNodeExist(parentPath)) {
            List<String> childrenNodes = getChildrenNode(parentPath);
            if (childrenNodes != null && childrenNodes.isEmpty()) {
                delPath(parentPath);
                logger.info("删除空父节点 {}", parentPath);
            }
        }
    }

    /**
     * 删除路径节点上的所有子节点
     *
     * @param parentPath
     * @return
     */
    private boolean delPath(String parentPath) {
        boolean result = false;
        try {
            Void unused = client.delete().forPath(parentPath);
            result = unused == null ? false : true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * 获取当前目录下的所有子节点
     *
     * @param parentPath
     * @return
     */
    private List<String> getChildrenNode(String parentPath) {

        try {
            List<String> strings = client.getChildren().forPath(parentPath);
            if (strings.isEmpty()) {
                logger.info("当前节点为空");
            }
            return strings;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private boolean checkNodeExist(String parentPath) {
        try {
            Stat stat = client.checkExists().forPath(parentPath);
            if (stat == null) {
                logger.info("节点不存在");
                return false;
            } else {
                logger.info("当前路径节点存在 {}", stat.toString());
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public BambiNode getLocalNodeInfo() {
        return localNode;
    }

    static {
        Runtime.getRuntime().addShutdownHook(
                new Thread(() -> {
                    BambiWorker.getInstance().deleteNode();
                }, "关闭worker，删除zk节点")
        );
    }

    private void deleteNode() {
        logger.info("删除worker的node ， path {},id {}", pathRegistered, localNode.getId());
        try {
            Stat stat = client.checkExists().forPath(pathRegistered);
            if (null == stat) {
                logger.info("节点已经不存在");
            } else {
                client.delete().forPath(pathRegistered);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 负载逻辑处理开始
    public boolean incrBalance() {
        if (localNode == null) {
            throw new RuntimeException("本地节点获取失败，还没有初始化本地节点");
        }
        while (true) {
            try {
                localNode.increaseConnected();
                byte[] payload = JsonUtil.object2JsonBytes(localNode);
                client.setData().forPath(pathRegistered, payload);
                return true;
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }
    }

    public boolean descBalance() {
        if (localNode == null) {
            throw new RuntimeException("本地节点获取失败，还没有初始化本地节点");
        }

        while (true) {
            try {
                localNode.decreaseConnected();
                byte[] payload = JsonUtil.object2JsonBytes(localNode);
                client.setData().forPath(pathRegistered, payload);
                return true;
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }

        }
    }

    public void setLocalNodeInfo(String host, Integer ip) {
        localNode.setNettyHost(host);
        localNode.setNettyPort(ip);
    }
}

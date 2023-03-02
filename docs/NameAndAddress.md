# 实现分布式节点的动态命名和注册

> ​		首先，市面上现在已经有很多微服务的注册中心，如Eureka、Nacos；但是出于项目学习的目的，我还是向自己实现一个简单的动态命名注册服务，通过zookeeper。
>
> ​		这个项目(BambiNettyIM),构建初期也是打算尽可能的少用实现好的框架，处于学习目的来构建一个自己的集群逻辑

​			如果服务器数量很多的话，为了能保证系统的正常运行，必须有一个中心化的组件去完成各个服务的整合，也就是要将分散的服务汇总。

### Zookeeper的动态命名

​		对于Zookeeper是如何保证数据的一致性的，网上有很多文章，我可能也会自己整理一篇相关的文章。但是简而言之就是 **zk使用的是ZAB 协议，nacos使用的是RAFT协议**	

​		zk的命名服务主要依赖于其对内部节点的维护能力，在Dubbo框架中便使用了Zk来维护自己的 服务端api地址列表。

​		大致思路是通过服务端启动时在zk上生成路径节点，在节点中存储自己的相关信息，在其他服务尝试订阅时，只需要拿到zk对应路径点下的基础信息即可。本项目的分布式节点命名注册也是如此。



### 定义一个存储基础信息的pojo类

​		这个类是支持序列化的，也可以进行持久化存储，内部的成员为基本的服务器信息；如端口、ip、以及当前服务器的连接数(后期用来做负载均衡判断)

```java
public class BambiNode implements Comparable<BambiNode>, Serializable {
    private static Logger logger = LoggerFactory.getLogger(BambiNode.class);

    private static final long serialVersionUID = -499010884211304846L;
    private long id; // worker节点获取到的id，即使用zk的命名服务生成的ID
    private Integer connectedBalance = 0;
    // 可以读配置
    private String nettyHost = "127.0.0.1";
    private Integer nettyPort = 8954;

    public BambiNode() {
    }

    public BambiNode(String nettyHost, Integer nettyPort) {
        this.nettyHost = nettyHost;
        this.nettyPort = nettyPort;
    }

    @Override
    public String toString() {
        return "bambiNode{" +
                "id=" + id +
                ", connectedBalance=" + connectedBalance +
                ", nettyHost='" + nettyHost + '\'' +
                ", nettyPort=" + nettyPort +
                '}';
    }

    /**
     * 对比当前连接数，返回连接数比较后的大小结果
     *
     * @param node the object to be compared.
     * @return
     */
    @Override
    public int compareTo(BambiNode node) {
        Integer connectedBalance1 = node.connectedBalance;
        Integer connectedBalance2 = this.connectedBalance;
        if (connectedBalance2 > connectedBalance1) {
            return 1;
        } else if (connectedBalance1 > connectedBalance2) {
            return -1;
        }
        return 0;
    }

    public Integer increaseConnected() {
        connectedBalance++;
        return connectedBalance;
    }

    public Integer decreaseConnected() {
        connectedBalance--;
        return connectedBalance;
    }

    // 省略getter setter
}

```



### 具体实现逻辑Worker类

​		而对于节点的命名以及注册等服务则是在另一个逻辑类中进行。

```java
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
                        .withProtection()
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
```

​		这个类的逻辑其实也很清晰，连接之后在zk中创建一个临时顺序节点，并将节点的路径进行基本处理，封装成每个server自己的id，存入上一个类`BambiNode`中。并将这个server的端口等信息，存储在每个节点的byte数组中。

​		因为这个Worker是和服务绑定的，每个服务只有一个worker，所以使用单例模式进行设计


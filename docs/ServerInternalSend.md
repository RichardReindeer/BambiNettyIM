# 分布式中的消息转发

> ​		在Netty的单体项目中，客户端与客户端直接发送消息，都在同一个server上处理起来十分的简单。但是到了集群中，处于不同server中的客户端如何实现消息的发送，就要考虑到节点直接信息的接收与转发。

### 前置

​		对于分布式命名和注册，我使用的是zk，利用第三方api `curator`来在Java中对zk节点进行操作。利用zk的特性，生成临时顺序节点，再根据节点路径截取来实现动态的分布式id命名(代码在`BambiWorker`中实现，会贴在最后。)



### 路由与转发

​		我们可以利用`Curator`的缓存监听实现类 `TreeCache`来进行对zk节点事件的监听。内部维护了一个`ConcurrentHashMap`用来存储节点id与 **转发器** 之间的关联关系。

```java
public class WorkerRouter {
    private static Logger logger = LoggerFactory.getLogger(WorkerRouter.class);
    private static final String PATH = SystemConfig.MANAGE_PATH; // 总节点
    private CuratorFramework client;
    private ConcurrentHashMap<Long, InternalSender> workerMap = new ConcurrentHashMap<>(); // 存储id与转发器的映射关系
    public static WorkerRouter instance = null;

    // 其实没啥意义，就是练习一下jdk8里的使用
    private BiConsumer<BambiNode, InternalSender> runAfterAdd = (bambiNode, internalSender) -> {
        doAfterAdd(bambiNode, internalSender);
    };

    private Consumer<BambiNode> runAfterRemove = (bambiNode) -> {
        doAfterRemove(bambiNode);
    };


    private WorkerRouter() {
    }

    /**
     * 创建WorkerRouter<br>
     * 因为可能存在并发问题，使用synchronized进行方法加锁
     *
     * @return
     */
    public synchronized static WorkerRouter getInstance() {
        if (instance == null) {
            instance = new WorkerRouter();
        }
        return instance;
    }

    // 标志位，用来判断是否已经初始化过
    private boolean inited = false;

    public void init() {
        if (inited) return;
        inited = true;
        if (client == null) {
            // client = CuratorClient.instance.getClient();
            client = new CuratorClient("3000", "127.0.0.1:2181").getClient();
        }

        try {
            // 使用Curator的缓存技术
            // data中可以获取到节点对应的路径、数据和状态
            TreeCache cache = TreeCache.newBuilder(client, SystemConfig.MANAGE_PATH).setCacheData(false).build();
            TreeCacheListener treeCacheListener = new TreeCacheListener() {
                @Override
                public void childEvent(CuratorFramework client, TreeCacheEvent event) throws Exception {
                    ChildData childData = event.getData();
                    if (event.getData() != null && childData.getData().length > 0) {
                        switch (event.getType()) {
                            case INITIALIZED:
                                break;
                            // 方法命名参考curator的示例
                            case NODE_ADDED:
                                logger.info("节点增加 NODE_ADD {} 数据 {}", childData.getPath(), childData.getData());
                                processNodeAdded(childData);
                                break;
                            case NODE_UPDATED:
                                logger.info("节点修改 Node_UPDATED {} 数据 {}", childData.getPath(), childData.getData());
                                // processNodeUpdated(childData);
                                // 暂时没有什么思路，关于节点更新时的操作
                                break;
                            case NODE_REMOVED:
                                logger.info("节点删除 NODE_REMOVED {} 数据{} ", childData.getPath(), childData.getData());
                                processNodeRemoved(childData);
                                break;
                            default:
                                logger.error("节点数据为空");
                                break;
                        }
                    } else if (event.getData() != null && event.getData().getPath().equals(PATH)) {
                        logger.info("父节点创建");
                    }

                }
            };
            // 线程池使用位置参考源码 ListenerContainer
            cache.getListenable().addListener(treeCacheListener, ThreadUtil.getIoIntenseTargetThreadPool());
            cache.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * 节点添加事件逻辑处理
     * 同步worker数据<br>
     * 建立转发器<br>
     * 因为节点的payload中存储着netty服务本身的port，host，id等信息，所以可以反序列化为bambiNode对象<br>
     *
     * @param childData
     */
    private void processNodeAdded(ChildData childData) {
        logger.info("检测到节点更新");
        byte[] payload = childData.getData();
        BambiNode bambiNode = ObjectUtil.JsonBytes2Object(payload, BambiNode.class);
        logger.debug("更新的节点信息为 {}", bambiNode.toString());
        long idByZkPath = BambiWorker.getInstance().getIdByZkPath(childData.getPath());
        bambiNode.setId(idByZkPath);

        // 如果是当前本地节点，则直接返回
        if (bambiNode.equals(BambiWorker.getInstance().getLocalNodeInfo())) {
            logger.info("本地节点更新 path {} , data{} ", childData.getPath(), JsonUtil.pojoToJsonByGson(bambiNode));
            return;
        }

        // 获取转发器
        InternalSender internalSender = workerMap.get(bambiNode.getId());
        // 过滤重复的节点添加
        if (internalSender != null && internalSender.getTargetNode().equals(bambiNode)) {
            logger.info("当前转发器已存在 , 节点为重复添加");
            logger.info("path {} , data {}", childData.getPath(), JsonUtil.pojoToJsonByGson(bambiNode));
            return;
        }
        // 顺便练习一手函数式编程
        if (runAfterAdd != null) {
            runAfterAdd.accept(bambiNode, internalSender);
        }
    }

    /**
     * 节点移除事件的处理逻辑<br>
     *
     * @param childData
     */
    private void processNodeRemoved(ChildData childData) {
        logger.info("检测到节点");
        byte[] payload = childData.getData();
        BambiNode bambiNode = ObjectUtil.JsonBytes2Object(payload, BambiNode.class);
        long id = BambiWorker.getInstance().getIdByZkPath(childData.getPath());
        bambiNode.setId(id);
        logger.info("删除节点 path {},data {}", childData.getPath(), JsonUtil.pojoToJsonByGson(bambiNode));
        if (runAfterRemove != null) {
            runAfterRemove.accept(bambiNode);
        }
    }

    /**
     * 节点连接之后的操作<br>
     * 添加对老转发器的检测，若存在则先关闭<br>
     *
     * @param bambiNode
     * @param internalSender
     * @auther Bambi
     */
    private void doAfterAdd(BambiNode bambiNode, InternalSender internalSender) {
        if (internalSender != null) {
            internalSender.shutDownConnect();
        }
        internalSender = new InternalSender(bambiNode);
        internalSender.doConnect();
        workerMap.put(bambiNode.getId(), internalSender);
        logger.info("创建转发器与bambiNode 间的对应关系 id{} ", bambiNode.getId());
    }

    /**
     * 节点移除后的处理工作
     *
     * @param bambiNode 被移除的节点的基础信息
     * @Update Bug修复，没有关闭InternalSender的连接
     */
    private void doAfterRemove(BambiNode bambiNode) {
        InternalSender internalSender = workerMap.get(bambiNode.getId());
        if (internalSender != null) {
            // 关闭连接 !!!!!
            internalSender.shutDownConnect();
            workerMap.remove(bambiNode.getId());
        }
    }

    public CuratorFramework getClient() {
        return client;
    }

    public void setClient(CuratorFramework client) {
        this.client = client;
    }

    public ConcurrentHashMap<Long, InternalSender> getWorkerMap() {
        return workerMap;
    }

    public void setWorkerMap(ConcurrentHashMap<Long, InternalSender> workerMap) {
        this.workerMap = workerMap;
    }

    public static void setInstance(WorkerRouter instance) {
        WorkerRouter.instance = instance;
    }

    public BiConsumer<BambiNode, InternalSender> getRunAfterAdd() {
        return runAfterAdd;
    }

    public void setRunAfterAdd(BiConsumer<BambiNode, InternalSender> runAfterAdd) {
        this.runAfterAdd = runAfterAdd;
    }

    public Consumer<BambiNode> getRunAfterRemove() {
        return runAfterRemove;
    }

    public void setRunAfterRemove(Consumer<BambiNode> runAfterRemove) {
        this.runAfterRemove = runAfterRemove;
    }

    public boolean isInited() {
        return inited;
    }

    public void setInited(boolean inited) {
        this.inited = inited;
    }

    /**
     * 根据id获取对应的转发器
     *
     * @param id
     * @return
     */
    public InternalSender getInternalSender(long id) {
        InternalSender internalSender = workerMap.get(id);
        if (internalSender != null) {
            return internalSender;
        }
        logger.error("当前节点为创建对应的转发器");
        return null;
    }

    /**
     * 向其他服务器广播信息<br>
     * 判断session如果不是本地节点，则放入转发器中广播
     */
    public void sendNotification(String json) {
        workerMap.keySet().stream().forEach(key -> {
            if (!key.equals(getLocalNode())) {
                InternalSender internalSender = workerMap.get(key);
                ProtoBufMessage.Message message = NotificationMsgBuilder.buildNotification(json);
                internalSender.writeAndFlush(message);
            }
        });
    }

    private Long getLocalNode() {
        return BambiWorker.getInstance().getLocalNodeInfo().getId();
    }
}
```

​		可以看到，这个类的主要作用便是订阅所有的集群节点，并监听节点的状态；并且利用转发器与其他的netty服务器建立长连接，用于信息的转发。



### 转发器

​		至于转发器的设计，其实要更好理解一些；让我们先来想一下转发器的作用；用于将本地服务器的信息发送给其他服务器；在设计实现上，其可以看作一个netty的客户端；利用zk节点保存的数据信息（远程节点的ip、端口号以及id、负载) 将数据信息发送给对应的远程服务器

```java
public class InternalSender {
    private static Logger logger = LoggerFactory.getLogger(InternalSender.class);
    private Channel channel;
    private BambiNode targetNode; // 转发的目标

    private int reConnectedTimes = 0; // 重新连接次数

    private boolean connectFlag = false; // 连接标记

    private Bootstrap bootstrap;
    private EventLoopGroup group;

    public InternalSender(BambiNode targetNode) {
        this.targetNode = targetNode;
        bootstrap = new Bootstrap();
        group = new NioEventLoopGroup();
    }

    /**
     * 创建客户端连接<br>
     * 参考zk的重试策略设计，如果连接失败，实现在指定次数内重试连接
     */
    public void doConnect() {
        String nettyHost = targetNode.getNettyHost();
        Integer nettyPort = targetNode.getNettyPort();
        try {
            if (bootstrap != null && bootstrap.group() == null) {
                bootstrap.group(group)
                        .channel(NioSocketChannel.class)
                        .option(ChannelOption.SO_KEEPALIVE, true)
                        .option(ChannelOption.ALLOCATOR, ByteBufAllocator.DEFAULT);
                bootstrap.remoteAddress(nettyHost, nettyPort);
                bootstrap.handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel socketChannel) throws Exception {
                        socketChannel.pipeline().addLast("decoder", new BambiProtobufDecoder());
                        socketChannel.pipeline().addLast("encoder", new BambiProtobufEncoder());
                        socketChannel.pipeline().addLast("internalHeartBeatHandler", new InternalHeartBeatHandler());
                        // 异常处理器放在最后
                        socketChannel.pipeline().addLast("exceptionHandler", new InternalExceptionHandler());
                    }
                });

                logger.info("开始分布式节点间的连接 {}", targetNode.toString());

                ChannelFuture connect = bootstrap.connect();

                /**
                 * 添加连接监听器<br>
                 * 重试策略参考zk, 三次重试次数GenericFutureListener<br>
                 * 如果连接失败则重连
                 */
                connect.addListener(connectedListener);
            } else if (bootstrap.group() != null) {
                logger.info("重新开启分布式节点连接 {} ", targetNode.toString());
                ChannelFuture connect = bootstrap.connect();
                connect.addListener(closeListener);
            }
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("客户端连接失败");
        }
    }

    /**
     * 关闭连接
     */
    public void shutDownConnect() {
        group.shutdownGracefully();
        connectFlag = false;
    }

    /**
     * TODO 可以进行水位判断
     *
     * @param pMessage
     */
    public void writeAndFlush(Object pMessage) {
        if (connectFlag) {
            channel.writeAndFlush(pMessage);
        } else {
            logger.info("集群节点并未连接 {}", targetNode.toString());
            return;
        }
    }

    /**
     * 因为启动类内if判断需要多次使用监听器，所以独立设计<br>
     *
     * @Date 2023/02/24
     */
    private GenericFutureListener<ChannelFuture> connectedListener =
            new GenericFutureListener<ChannelFuture>() {
                @Override
                public void operationComplete(ChannelFuture future) throws Exception {
                    EventLoop eventLoop = future.channel().eventLoop();
                    if (!future.isSuccess() && ++reConnectedTimes < 3) {
                        eventLoop.schedule(() -> {
                            InternalSender.this.doConnect();
                        }, 10, TimeUnit.SECONDS);
                        connectFlag = false;
                    } else {
                        connectFlag = true;
                        logger.info("分布式节点连接成功 {}", targetNode.toString());
                        channel = future.channel();
                        channel.closeFuture().addListener(closeListener);

                        // 连接成功后发送通知
                        Notification<BambiNode> bambiNodeNotification = new Notification<>(BambiWorker.getInstance().getLocalNodeInfo());
                        bambiNodeNotification.setType(Notification.CONNECT_FINISHED);
                        String s = JsonUtil.pojoToJsonByGson(bambiNodeNotification);
                        ProtoBufMessage.Message message = NotificationMsgBuilder.buildNotification(s);
                        writeAndFlush(message);
                    }
                }
            };

    private GenericFutureListener<ChannelFuture> closeListener =
            new GenericFutureListener<ChannelFuture>() {
                @Override
                public void operationComplete(ChannelFuture future) throws Exception {
                    logger.info("连接已经断开");
                    channel = null;
                    connectFlag = false;
                }
            };

    // getter setter
}
```



### 其他

- Worker类

  - 实现命名服务的具体逻辑，使用zk获取对应的分布式命名

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

- Node 类

  - 用来存储当前节点的信息，可以用于持久化操作

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
      // getter setter
  }
  ```


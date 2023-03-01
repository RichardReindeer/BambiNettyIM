package com.bambi.server.rpc;

import com.bambi.bean.entity.BambiNode;
import com.bambi.bean.msg.ProtoBufMessage;
import com.bambi.config.SystemConfig;
import com.bambi.server.protoBuilder.NotificationMsgBuilder;
import com.bambi.utils.JsonUtil;
import com.bambi.utils.ObjectUtil;
import com.bambi.utils.ThreadUtil;
import com.bambi.zk.CuratorClient;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.TreeCache;
import org.apache.curator.framework.recipes.cache.TreeCacheEvent;
import org.apache.curator.framework.recipes.cache.TreeCacheListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * 描述：
 * <br><b>服务器消息转发器</b><br>
 * 用于实现服务器内部的消息转发，结合类{@link InternalSender} 实现消息的转发<br>
 *      <ul>
 *          <li>使用{@link TreeCacheListener} 监听zk节点的变化，并对其做出对应响应。</li>
 *          <li>创建map映射存储其他服务器节点信息，方便构建转发器{@link InternalSender}</li>
 *      </ul>
 * <pre>
 * HISTORY
 * ****************************************************************************
 *  ID     DATE          PERSON          REASON
 *  1      2023/3/1 20:09    Bambi        Create
 * ****************************************************************************
 * </pre>
 *
 * @author Bambi
 * @since 1.0
 */
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
                    if (event.getData() != null && event.getData().getData().length > 0) {
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

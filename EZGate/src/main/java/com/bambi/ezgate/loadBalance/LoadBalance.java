package com.bambi.ezgate.loadBalance;

import com.bambi.bean.entity.BambiNode;
import com.bambi.config.SystemConfig;
import com.bambi.utils.JsonUtil;
import com.bambi.zk.CuratorClient;
import org.apache.curator.framework.CuratorFramework;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 描述：
 *      <br><b>负载均衡</b><br>
 *      起初设计时，打算在网关使用LoadBalance筛选出最佳服务器节点返回给客户端<br>
 *      但最后还是决定直接返回给客户端所有节点链表，在客户端进行选择
 * <pre>
 * HISTORY
 * ****************************************************************************
 *  ID     DATE          PERSON          REASON
 *  1      2023/3/1 15:26    Bambi        Create
 * ****************************************************************************
 * </pre>
 *
 * @author Bambi
 * @since 1.0
 */
@Deprecated(since = "1.0")
public class LoadBalance {
    private static Logger logger = LoggerFactory.getLogger(LoadBalance.class);

    private CuratorFramework client;
    private String mainPath;

    public LoadBalance(CuratorClient client) {
        this.client = client.getClient();
        this.mainPath = SystemConfig.MANAGE_PATH;
    }

    /**
     * 获取最佳节点，在测试类中有使用<br>
     * @return
     */
    public BambiNode getBestWorker() {
        List<BambiNode> workers = getWorkers();
        workers.stream().forEach(node -> {
            logger.info("节点信息 ： {}", JsonUtil.pojoToJsonByGson(node));
        });
        BambiNode bestNode = balance(workers);
        return bestNode;
    }

    // 按照负载进行排序
    private BambiNode balance(List<BambiNode> workers) {
        if (workers.size() > 0) {
            // 根据balance值由小到大排序
            Collections.sort(workers);

            // 返回balance值最小的那个
            BambiNode node = workers.get(0);

            logger.info("最佳的节点为：{}", JsonUtil.pojoToJsonByGson(node));
            return node;
        } else {
            return null;
        }
    }

    public List<BambiNode> getWorkers() {
        ArrayList<BambiNode> bambiNodes = new ArrayList<>();
        List<String> children = null;
        try {
            children = client.getChildren().forPath(mainPath);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

        for (String child :
                children) {
            byte[] payload = null;
            try {
                payload = client.getData().forPath(mainPath + "/" + child);
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (payload == null) {
                continue;
            }
            BambiNode bambiNode = JsonUtil.jsonBytes2Object(payload, BambiNode.class);
            bambiNode.setId(getIdByPath(child));
            bambiNodes.add(bambiNode);
        }
        return bambiNodes;
    }

    /**
     * 根据路径获取id
     *
     * @param child
     * @return
     */
    private long getIdByPath(String child) {
        String sid = null;
        if (child == null) {
            throw new RuntimeException("节点路径有问题");
        }
        int index = child.lastIndexOf(SystemConfig.PATH_PREFIX_NO_STRIP);
        if (index >= 0) {
            index += SystemConfig.PATH_PREFIX_NO_STRIP.length();
            sid = index <= child.length() ? child.substring(index) : null;
        }

        if (null == sid) {
            throw new RuntimeException("节点ID获取失败");
        }

        return Long.parseLong(sid);
    }

    /**
     * 从zookeeper中删除所有IM节点
     */
    public void removeWorkers() {


        try {
            client.delete().deletingChildrenIfNeeded().forPath(mainPath);
        } catch (Exception e) {
            e.printStackTrace();
        }


    }
}

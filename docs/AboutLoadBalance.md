# 实现分布式节点的负载均衡

[前置文章 zk的javaAPI](http://8.142.7.247:8954/archives/zookeeperjavaapi)

> ​		正常来说一个网关，应该配合SpringCloud 以及nginx进行校验等操作。但是项目初期主要是为了实现netty集群的搭建以及逻辑的互通，所以将网关部分进行了简化

​		架构中负载的发生

- 防止单个netty节点的服务器负载过大，导致不可用
- 在用户登录成功之后，短链接网关需要返回给用户一个最佳的netty节点地址，让用户来建立netty的连接

​		所谓的负载均衡就是为用户挑选一个netty的最佳服务节点



### 负载均衡策略的选择

​		负载均衡策略有很多，轮询、随机、hash等等。在我先前公司的项目中便使用的是类似hash轮询的方式，hash轮询非常适合长连接，也可以提高命中率。但是当节点宕机了会需要二次映射，且可能出现hot key导致的雪崩问题。

​		对于我这个练手项目来说，实现负载均衡其实就相对简单一些。之前的文章也说过，每个节点都维护了自己的负载值。我们只需要获取所有的负载，根据负载值排序，返回可以用的、负载最小的节点数据。

### LoadBalance设计

```java
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
```

​	在Controller中接收客户端登录时发送的短链接(登录具体实现逻辑会在汇总文章中提及)

```java
@RequestMapping(value = "/login/{username}/{password}",method = RequestMethod.GET)
    public String login(
            @PathVariable String username,
            @PathVariable String password
    ){
        logger.info("login is starting !!!! ");

        UserPojo userPojo = new UserPojo();
        userPojo.setUserName(username);
        userPojo.setPassWord(password);
        userPojo.setUserId(userPojo.getUserName());

        // 创建登录回调
        LoginBack back = new LoginBack();

        // 获取最佳服务器
        List<BambiNode> workers = loadBalance.getWorkers();
        back.setbambiNodeList(workers);
        UserDTO userDTO = new UserDTO();
        BeanUtils.copyProperties(userPojo,userDTO);
        back.setUserDTO(userDTO);
        back.setToken(userPojo.getUserId().toString());
        String result = JsonUtil.pojoToJsonByGson(back);
        return result;
    }
```

​		`LoginBack`类主要是封装用户所需信息，客户端通过`feign`调用并接收`loginBack`的返回值，接收返回值之后在客户端筛选(根据负载值从小到大排序)

​		
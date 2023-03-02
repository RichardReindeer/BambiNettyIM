# RouterAndSender

​		就好像在网关的设计部分，客户端使用`feign`调用网关，网关再去调用服务器获取节点数据一样。服务期间也需要彼此的消息互通(因为是集群)

​		RPC很好理解，网上也已经有成百上千的文章，比我理解更透彻的大佬也有很多，这里便不再赘述。本架构中的`WorkerRouter`以及`InternalSender`的作用便是一个简单的RPC

### Router

​		可能你也看到了，这里是使用zk来实现分布式节点的动态命名注册的；也就是说当一台服务器连接之后，需要向zk注册一个临时节点，也就是`BambiWorker`中的逻辑:

```java
try {
    byte[] payload = JsonUtil.object2JsonBytes(localNode); // 将当前worker的基础数据放入当前的zk节点中
    pathRegistered = client.create()
            .creatingParentsIfNeeded()
            .withMode(CreateMode.EPHEMERAL_SEQUENTIAL)
            .forPath(SystemConfig.PATH_PREFIX, payload);
    logger.info("打印测试 pathRegistered {}", pathRegistered);
    localNode.setId(getId());
    logger.info("本地节点 的 path {} , id = {}", pathRegistered, localNode.getId());
} catch (Exception e) {
    e.printStackTrace();
}
```

​		既然可以注册到zk，我们也可以通过监听zk节点的变化来判断其他服务器的上下线情况。这里我们使用的是`Curator`的`TreeCahce`。

```java
if(event.getData()!=null&&event.getData().getData().length>0){
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
}else if(event.getData()!=null && event.getData().getPath().equals(PATH)){
    logger.info("父节点创建");
}
```

​		我们可以轻松的通过节点所存储的字节数组反序列化得到服务器的节点信息，然后便可以与该server建立通道连接。



### Sender

​		哦对了，说起通道我就要提一下Sender了。如果你能够理解客户端向服务器发送消息的逻辑，那服务器与服务器之间可以看作是两个用户在互发消息。所以`Sender`其实是一个简单的Netty客户端；用来将需要广播到其他服务器间的通知发送给zk节点中的server。

```java
public void sendNotification(String json){
    workerMap.keySet().stream().forEach(key ->{
        if(!key.equals(getLocalNode())){
            InternalSender internalSender = workerMap.get(key);
            ProtoMsg.Message message = NotificationMsgBuilder.buildNotification(json);
            internalSender.writeAndFlush(message);
        }
    });
}
```

​		这段广播消息的代码在`WorkerRouter`中，每个服务器都维护了一个map映射，存储着其他服务器的基本信息。当需要通知的时候，便遍历映射拿到与其他节点建立的转发器`Sender`，将信息放入其中，写入即可。

​		具体逻辑可以启动项目断点跑一下，启动逻辑在仓库的[README](https://github.com/RichardReindeer/BambiNettyIM/blob/main/README.md)中
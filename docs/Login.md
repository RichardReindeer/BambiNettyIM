# Login

![Login](.\image\Login.png)

​		客户端逻辑核心类：`CommandController`

​		Server逻辑核心类：`BambiServer`

​		客户端在启动时会最先初始化所有的指令到`CommandContoller`的容器属性中；当检测到登录指令/用户并没有登录，便会进入登录逻辑`userLoginAndConnecting()`；使用`feign`获取到服务器节点列表之后进行筛选处理;并带有一定的重试策略

```java
private void userLoginAndConnecting() {
    if (connectFlag == true) {
        logger.info("已经成功登录，不需要再次登录");
        return;
    }
    LoginCommand login = (LoginCommand) commandMap.get(LoginCommand.KEY);
    login.doAction(scanner);

    UserDTO user = new UserDTO();
    user.setUserId(login.getUsername());
    user.setToken(login.getPassword());
    user.setDevId("11111111");
    logger.info("开始登录EzGate");
    LoginBack loginBack = ShortSocketUtil.login(user.getUserId(), user.getToken());
    List<BambiNode> bambiNodeList = loginBack.getbambiNodeList();
    logger.info("获取到的服务器节点列表为 {}", JsonUtil.pojoToJsonByGson(bambiNodeList));

    BambiNode bestNode = null;
    if (!bambiNodeList.isEmpty()) {
        // 排序时利用    BambiNode的比较逻辑
        Collections.sort(bambiNodeList);
    }

    bambiClient.setConnectListener(connectListener);
    for (int i = 0; i < bambiNodeList.size(); i++) {
        bestNode = bambiNodeList.get(i);
        bambiClient.setServerHost(bestNode.getNettyHost());
        bambiClient.setServerPort(bestNode.getNettyPort());
        bambiClient.startConnect();
        waitThread();
        if (connectFlag) {
            break;
        }
        if (i == bambiNodeList.size()) {
            logger.error("尝试连接服务器节点失败");
            return;
        }
    }

    logger.info("服务器节点连接成功！！！！");

    this.userDTO = user;
    clientSession.setUserDTO(user);
    loginSender.setUserDTO(user);
    loginSender.setClientSession(clientSession);
    loginSender.sendLoginMsg();
    waitThread();
    connectFlag = true;
}
```

​		此处便使用了上文所提到的消息发送器，将拼装好的登录请求信息异步发送至服务器端。

​		服务器端的登录请求数据包处理则位于`LoginRequestHander`中；内部会使用异步回调模式处理并发送登录响应包，此处不再赘述；

​		在登录处理完毕之后，利用Handler的热插拔逻辑，添加心跳响应处理器`HeartBeatHandler`并移除登录处理器。(`HeartBeatHandler`意在避免假死现象，详情阅读文章[心跳与空闲检测的设计](https://github.com/RichardReindeer/BambiNettyIM/blob/main/docs/AboutHeartBeatAndIdel.md))

​		客户端在接收到登录响应数据包之后也会创建自己的心跳包并移除对应的登陆响应处理器
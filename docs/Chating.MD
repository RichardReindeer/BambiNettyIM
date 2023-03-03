# Chat

​		对于聊天设计的重点则在于服务器端如何解决用户A与用户B不在一个服务器上的问题，以及将信息发送至用户B的所有登录设备中。参考办公软件飞书以及其他IM系统，我们的手机和电脑是可以同时登录一个账户的，当收到一条信息，所有的设备都应得到提示，这里便使用到了user与Session的关联关系；

![Chatting](.\image\Chatting.jpg)

​		server这边异步开启线程根据消息中的目标用户`userID`找到其所有的Session；并遍历调用session的`WriteAndFlush()` ， session相关代码设计请阅读头部索引中的session设计相关。

```java
if(msg == null || !(msg instanceof ProtoBufMessage.Message)){
    super.channelRead(ctx, msg);
    return;
}
ProtoBufMessage.Message message = (ProtoBufMessage.Message) msg;
ProtoBufMessage.HeadType type = message.getType();
if(type.equals(chatRedirectReceiver.getHeadType())){
    FutureTaskExecutor.add(()->{
        LocalSession session = LocalSession.getSession(ctx);
        if(session != null || session.isLogin()){
            chatRedirectReceiver.action(session,message);
        }
        ProtoBufMessage.MessageRequest messageRequest = message.getMessageRequest();
        ArrayList<IServerSession> sessionById = SessionManager.getInstance().getSessionById(messageRequest.getTo());
        if(!sessionById.isEmpty()){
            sessionById.forEach(serverSession -> {
                serverSession.writeAndFlush(msg);
            });
        }
    });
}else {
    super.channelRead(ctx, msg);
    return;
}
```


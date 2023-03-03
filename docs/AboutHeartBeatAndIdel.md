# HeartBeatHandler

## 关于服务器的假死现象

​		什么是服务端假死现象？

​		所谓的服务端假死，指的是底层的TCP链接(Socket) 已经断开，但是服务器并没有正常的关闭掉socket，服务器认为这条TCP链接是仍然存在的，这样服务器端便出现了假死现象

>    就好比你给手机插上了充电线，但是充电头其实已经从插座(socket) 上拔下来了

​		假死现象的具体表现：

1.  在服务器端，会有一些处于`TCP_ESTABLEISHED`状态的“正常链接"
2.  但在客户端，链接其实已经断开
3.  客户端可以进行断线重连，但是上一次的链接状态仍然会被服务器端视作有效链接，并且 **服务器的资源无法得到正确的释放，包括socket context 以及收发缓冲区**

​		造成假死现象的原因:

1.  应用程序出现 **线程阻塞** 无法进行数据读写
2.  物理硬件出现问题，如网卡、或者出现机房故障
3.  网络丢包。

**解决假死的方法便是 ： 客户端定时进行心跳检测，服务器端定时进行空闲检测**

​		假死带来的性能问题:

1.  由于每个链接都会消耗CPU以及内存资源，所以大量假死链接会逐渐耗尽服务器资源
2.  IO处理效率降低
3.  严重可导致服务器崩溃



## 空闲检测

​		Netty自带的`IdleStateHandler`继承自`ChannelDuplexHandler` ， 专门用于通道的空闲检测

```java
///Triggers an IdleStateEvent when a Channel has not performed read, write, or both operation for a while.
///Supported idle states
// 源码中携带着使用的例子，可以看源码自行尝试
```

​		在源码中可以看到其对应的构造函数，需要接收四个参数: 

1. 入栈空闲时长 : 如果时间内没有数据入栈，就判定为假死
2. 出栈空闲时长: 如果时间内没有数据出站，就判断为假死
3. 出入站检测时长 : 如果时间内没有出入站，则假死
4. 时间参考单位

```java
public IdleStateHandler(
        long readerIdleTime, long writerIdleTime, long allIdleTime,
        TimeUnit unit) {
    this(false, readerIdleTime, writerIdleTime, allIdleTime, unit);
}
```

​	我便可以编写一个类，来继承`IdleStateHandler`从而使服务器实现对应的检测功能.

```java
/**
 * 描述：
 *      <b> 空闲检测 </b><br>
 *      每间隔一段时间检测子通道是否有数据读写，如果没有则判断IO通道处于假死状态<br>
 * <pre>
 * HISTORY
 * ****************************************************************************
 *  ID     DATE          PERSON          REASON
 *  1      2022/12/11 15:01    Bambi        Create
 * ****************************************************************************
 * </pre>
 *
 * @author Bambi
 * @since 1.0
 */
public class HeartBeatHandler extends IdleStateHandler  {
    private static Logger logger = LoggerFactory.getLogger(HeartBeatHandler.class);

    // 暂时写死一个最大空闲时间
    private static final int READ_IDLE_TIME = 120;
    public HeartBeatHandler() {
        super(READ_IDLE_TIME, 0, 0, TimeUnit.SECONDS);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        logger.debug("HeartBeatHandler is channelReading !!!");
        // 判空
        if(!(msg instanceof ProtoMessage.Message) || msg == null){
            super.channelRead(ctx, msg);
            return;
        }

        ProtoMessage.Message proMsg = (ProtoMessage.Message) msg;
        ProtoMessage.HeadType type = proMsg.getType();
        if(type.equals(ProtoMessage.HeadType.HEART_BEAT)){
            // 如果类型是心跳类型，则将心跳信息发送给客户端
            FutureTaskScheduler.add(()->{
                if(ctx.channel().isActive()){
                    ctx.writeAndFlush(msg);
                }
            });
        }

        // 一定要调用父类的channelRead , 否则idle的入栈检测会失效
        super.channelRead(ctx, msg);
    }

    /**
     * 发生假死的处理逻辑
     * @param ctx
     * @param evt
     * @throws Exception
     */
    @Override
    protected void channelIdle(ChannelHandlerContext ctx, IdleStateEvent evt) throws Exception {
        logger.debug("channelIdle is starting !!!");
        logger.info(READ_IDLE_TIME +" 秒内没有收到信息，正在关闭链接");
        ServerSession.closeSession(ctx);
    }
}
```



## 客户端的心跳检测

​		与服务器端的空闲检测一起可以检测假死现象；客户端需要定时向服务器发送 心跳数据包，处理逻辑参考下文的`HeartBeatClientHandler`

```java
/**
 * 描述：
 * <b>设计思路</b><br>
 * 客户端在该Handler加入pipeline的时候就开始发送心跳，在channelActive方法中进行过<br>
 * 心跳采用定时器<b>schedule</b> , 每隔50s发送一次心跳包<br>
 * 该数值可以后期读取配置，但应与服务器端同时更改
 * <pre>
 * HISTORY
 * ****************************************************************************
 *  ID          DATE              PERSON          REASON
 *  001699   2022/12/7 15:34    WangJinhan        Create
 * ****************************************************************************
 * </pre>
 *
 * @author WangJinhan
 * @since 1.0
 */
@ChannelHandler.Sharable
@Service("heartBeatClientHandler")
public class HeartBeatClientHandler extends ChannelInboundHandlerAdapter {
    private static Logger logger = LoggerFactory.getLogger(HeartBeatClientHandler.class);
    // 定时时间50s
    private static long HEARTBEAT_DELAYTIME = 50;

    /**
     * 在handler进入pipeline时触发
     * @param ctx
     * @throws Exception
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        ClientSession session = ClientSession.getSession(ctx);
        User user = session.getUser();
        HeartBeatConverter heartBeatConverter = new HeartBeatConverter(user, session);
        ProtoMessage.Message build = heartBeatConverter.build();
        heartBeat(ctx, build);
        super.channelActive(ctx);
    }

    /**
     * 发送心跳数据包<br>
     * 采用定时任务中递归调用的方式，将数据包定时发送给服务器
     * @param ctx
     * @param build
     */
    private void heartBeat(ChannelHandlerContext ctx, ProtoMessage.Message build) {
        logger.debug("client's heartBeat is starting!!!");
        ctx.executor().schedule(() -> {
            if (ctx.channel().isActive()) {
                //将心跳包发送给服务器
                logger.debug("开始发送心跳包");
                ctx.writeAndFlush(build);
                // 在此递归调用，从而实现定时发送
                heartBeat(ctx, build);
            }
        }, HEARTBEAT_DELAYTIME, TimeUnit.SECONDS);
    }

    // 在此对服务器回写的数据包进行处理
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg == null || !(msg instanceof ProtoMessage.Message)) {
            super.channelRead(ctx, msg);
            return;
        }
        ProtoMessage.Message protoMsg = (ProtoMessage.Message) msg;
        ProtoMessage.HeadType type = protoMsg.getType();
        if (type.equals(ProtoMessage.HeadType.HEART_BEAT)) {
            logger.info("收到服务器发送的心跳检测信息");
            return;
        } else {
            super.channelRead(ctx, msg);
        }
    }
}
```

​		而对于`HeartBeatConverter` 则比较容易理解，只是将数据转换成对应的数据包格式

```java
public class HeartBeatConverter extends BaseConverter{
    private final User user;

    public HeartBeatConverter(User user, ClientSession clientSession) {
        super(ProtoMessage.HeadType.HEART_BEAT,clientSession);
        this.user = user;
    }

    public ProtoMessage.Message build(){
        ProtoMessage.Message.Builder outerBuilder = getOuterBuilder(-1);
        ProtoMessage.MessageHeartBeat.Builder builder = ProtoMessage.MessageHeartBeat.newBuilder()
                .setSeq(0)
                .setJson("{\"from\":\"client\"}")
                .setUid(user.getUid());
        return outerBuilder.setHeartBeat(builder).build();
    }
}
```

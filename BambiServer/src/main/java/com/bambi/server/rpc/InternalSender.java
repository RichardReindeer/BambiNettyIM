package com.bambi.server.rpc;

import com.bambi.bean.entity.BambiNode;
import com.bambi.bean.msg.ProtoBufMessage;
import com.bambi.decEncHandler.BambiProtobufDecoder;
import com.bambi.decEncHandler.BambiProtobufEncoder;
import com.bambi.im.common.bean.notification.Notification;
import com.bambi.server.handler.internal.InternalExceptionHandler;
import com.bambi.server.handler.internal.InternalHeartBeatHandler;
import com.bambi.server.protoBuilder.NotificationMsgBuilder;
import com.bambi.utils.JsonUtil;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.concurrent.GenericFutureListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * 描述：
 * <br><b>服务器间转发器</b><br>
 * 详细介绍请看文档<a href = "https://github.com/RichardReindeer/BambiNettyIM/blob/main/docs/%20RouterAndSender.MD">RouterAndSession</a><br>
 * 维护一个netty客户端，用来发送广播信息
 * <pre>
 * HISTORY
 * ****************************************************************************
 *  ID     DATE          PERSON          REASON
 *  1      2023/3/1 20:12    Bambi        Create
 * ****************************************************************************
 * </pre>
 *
 * @author Bambi
 * @since 1.0
 */
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
    public Channel getChannel() {
        return channel;
    }

    public void setChannel(Channel channel) {
        this.channel = channel;
    }

    public BambiNode getTargetNode() {
        return targetNode;
    }

    public void setTargetNode(BambiNode targetNode) {
        this.targetNode = targetNode;
    }

    public int getReConnectedTimes() {
        return reConnectedTimes;
    }

    public void setReConnectedTimes(int reConnectedTimes) {
        this.reConnectedTimes = reConnectedTimes;
    }

    public boolean isConnectFlag() {
        return connectFlag;
    }

    public void setConnectFlag(boolean connectFlag) {
        this.connectFlag = connectFlag;
    }

    public Bootstrap getBootstrap() {
        return bootstrap;
    }

    public void setBootstrap(Bootstrap bootstrap) {
        this.bootstrap = bootstrap;
    }

    public EventLoopGroup getGroup() {
        return group;
    }

    public void setGroup(EventLoopGroup group) {
        this.group = group;
    }
}

package com.bambi.client.controller;

import com.bambi.client.handler.ChatMessageHandler;
import com.bambi.client.handler.ClientExceptionHandler;
import com.bambi.client.handler.LoginResponseHandler;
import com.bambi.decEncHandler.BambiProtobufDecoder;
import com.bambi.decEncHandler.BambiProtobufEncoder;
import com.bambi.im.common.bean.dto.UserDTO;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.concurrent.GenericFutureListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

/**
 * 描述：
 * <br><b>简易客户端启动器</b><br>
 * <pre>
 * HISTORY
 * ****************************************************************************
 *  ID     DATE          PERSON          REASON
 *  1      2023/2/27 5:35    Bambi        Create
 * ****************************************************************************
 * </pre>
 *
 * @author Bambi
 * @since 1.0
 */
@Controller("bambiClient")
public class BambiClient {
    private static Logger logger = LoggerFactory.getLogger(BambiClient.class);

    private String serverHost;
    private Integer serverPort;

    private Channel channel;

    // 逻辑处理器 开始
    @Autowired
    private ChatMessageHandler chatMessageHandler;
    @Autowired
    private ClientExceptionHandler exceptionHandler;
    @Autowired
    private LoginResponseHandler loginResponseHandler;
    // 逻辑处理器结束

    // 是否初始化
    private boolean initFlag = true;

    private UserDTO userDTO;

    private GenericFutureListener<ChannelFuture> connectListener;

    private Bootstrap bootstrap;
    private EventLoopGroup group;

    public BambiClient() {
        group = new NioEventLoopGroup();
    }

    public void startConnect() {
        try {
            bootstrap = new Bootstrap();
            bootstrap.group(group);
            bootstrap.channel(NioSocketChannel.class)
                    .option(ChannelOption.SO_KEEPALIVE, true)
                    .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                    .remoteAddress(serverHost, serverPort);
            bootstrap.handler(new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(SocketChannel ch) throws Exception {
                    ch.pipeline().addLast("decoder", new BambiProtobufDecoder());
                    ch.pipeline().addLast("encoder", new BambiProtobufEncoder());
                    ch.pipeline().addLast("loginResponseHandler", loginResponseHandler);
                    ch.pipeline().addLast("chatMsgHandler", chatMessageHandler);
                    ch.pipeline().addLast("exceptionHandler", exceptionHandler);
                }
            });

            logger.info("已经建立连接");
            ChannelFuture connect = bootstrap.connect();
            connect.addListener(connectListener);
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("客户端连接失败");
        }

    }

    private void close() {
        group.shutdownGracefully();
    }

    // getter setter


    public String getServerHost() {
        return serverHost;
    }

    public void setServerHost(String serverHost) {
        this.serverHost = serverHost;
    }

    public Integer getServerPort() {
        return serverPort;
    }

    public void setServerPort(Integer serverPort) {
        this.serverPort = serverPort;
    }

    public Channel getChannel() {
        return channel;
    }

    public void setChannel(Channel channel) {
        this.channel = channel;
    }

    public boolean isInitFlag() {
        return initFlag;
    }

    public void setInitFlag(boolean initFlag) {
        this.initFlag = initFlag;
    }

    public UserDTO getUserDTO() {
        return userDTO;
    }

    public void setUserDTO(UserDTO userDTO) {
        this.userDTO = userDTO;
    }

    public GenericFutureListener<ChannelFuture> getConnectListener() {
        return connectListener;
    }

    public void setConnectListener(GenericFutureListener<ChannelFuture> connectListener) {
        this.connectListener = connectListener;
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

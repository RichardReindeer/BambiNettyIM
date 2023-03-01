package com.bambi.server.stater;

import com.bambi.currentUtils.FutureTaskExecutor;
import com.bambi.decEncHandler.BambiProtobufDecoder;
import com.bambi.decEncHandler.BambiProtobufEncoder;
import com.bambi.server.rpc.BambiWorker;
import com.bambi.server.rpc.WorkerRouter;
import com.bambi.utils.IOUtil;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.InetSocketAddress;

/**
 * 描述：
 *
 * <pre>
 * HISTORY
 * ****************************************************************************
 *  ID     DATE          PERSON          REASON
 *  1      2023/3/1 21:53    Bambi        Create
 * ****************************************************************************
 * </pre>
 *
 * @author Bambi
 * @since 1.0
 */
@Service("bambiNettyServer")
public class BambiNettyServer {
    private static Logger logger = LoggerFactory.getLogger(BambiNettyServer.class);

    @Value("${netty.server.port}")
    private int port;
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;

    private ServerBootstrap serverBootstrap = new ServerBootstrap();

    @Autowired
    private LoginRequestHandler loginRequestHandler;

    @Autowired
    private ServerExceptionHandler serverExceptionHandler;
    @Autowired
    private RemoteNotificationHandler remoteNotificationHandler;
    @Autowired
    private ChatRedirectHandler chatRedirectHandler;

    public void startServer() {
        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup();

        serverBootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .localAddress(new InetSocketAddress(IOUtil.getHostAddress(), port))
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
        serverBootstrap.childHandler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel ch) throws Exception {
                ch.pipeline().addLast("deCoder", new BambiProtobufDecoder());
                ch.pipeline().addLast("enCoder", new BambiProtobufEncoder());
                ch.pipeline().addLast("login", loginRequestHandler);
                ch.pipeline().addLast("remoteNotificationHandler", remoteNotificationHandler);
                ch.pipeline().addLast("chatRedirect", chatRedirectHandler);
                ch.pipeline().addLast("serverException", serverExceptionHandler);
            }
        });

        ChannelFuture future = null;
        boolean isConnected = false;
        while (!isConnected) {
            try {
                future = serverBootstrap.bind().sync();
                logger.info("BambiIMServer is started {}", future.channel().localAddress());
                isConnected = true;
            } catch (Exception e) {
                e.printStackTrace();
                logger.info("连接出现问题 ， 尝试其他端口");
                port++; // 端口自增
                serverBootstrap.localAddress(new InetSocketAddress(port));
            }
        }

        // 启服之后在zk中创建对应的临时节点
        // 生成当前server节点信息bambiNode类
        BambiWorker.getInstance().setLocalNodeInfo(IOUtil.getHostAddress(), port);
        FutureTaskExecutor.add(() -> {
            // 创建对应顺序节点
            BambiWorker.getInstance().init();
            // 创建消息转发器
            WorkerRouter.getInstance().init();
        });

        closeIfJvmClosed();
        try {
            ChannelFuture channelFuture = future.channel().closeFuture();
            channelFuture.sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
            logger.error("关闭通道时发生异常");
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

    private void closeIfJvmClosed() {
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                workerGroup.shutdownGracefully();
                bossGroup.shutdownGracefully();
            }
        }));
    }

}

package com.bambi.client.session;

import com.bambi.im.common.bean.dto.UserDTO;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.GenericFutureListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 描述：
 * <br><b>客户端Session</b><br>
 *      具体Session相关内容请移步文章 <a href = "https://github.com/RichardReindeer/BambiNettyIM/blob/main/docs/AboutSession.MD">AboutSession</a><br>
 *      用来绑定user与Channel ， 在开发中应使用{@link ClientSession#writeAndFlush(Object)}来发送消息<br>
 * <pre>
 * HISTORY
 * ****************************************************************************
 *  ID     DATE          PERSON          REASON
 *  1      2023/2/27 5:17    Bambi        Create
 * ****************************************************************************
 * </pre>
 *
 * @author Bambi
 * @since 1.0
 */
public class ClientSession {
    private static Logger logger = LoggerFactory.getLogger(ClientSession.class);
    public static final AttributeKey<ClientSession> SESSION_KEY = AttributeKey.valueOf("SESSION_KEY");

    private Channel channel;
    private UserDTO userDTO;

    private String sessionID;
    private boolean isConnected = false;
    private boolean isLogin = false;


    public ClientSession(Channel channel) {
        this.channel = channel;
        this.sessionID = String.valueOf(-1);
        channel.attr(ClientSession.SESSION_KEY).set(this);
    }

    public ClientSession getSession(ChannelHandlerContext ctx) {
        ClientSession clientSession = ctx.channel().attr(ClientSession.SESSION_KEY).get();
        return clientSession;
    }

    public ChannelFuture writeAndFlush(Object message) {
        ChannelFuture channelFuture = channel.writeAndFlush(message);
        return channelFuture;
    }

    public void close() {
        isConnected = false;
        ChannelFuture close = channel.close();
        close.addListener(new GenericFutureListener<ChannelFuture>() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                if (future.isSuccess()) {
                    logger.error("连接断开");
                }
            }
        });
    }

    // getter setter
    public Channel getChannel() {
        return channel;
    }

    public void setChannel(Channel channel) {
        this.channel = channel;
    }

    public UserDTO getUserDTO() {
        return userDTO;
    }

    public void setUserDTO(UserDTO userDTO) {
        this.userDTO = userDTO;
    }

    public String getSessionID() {
        return sessionID;
    }

    public void setSessionID(String sessionID) {
        this.sessionID = sessionID;
    }

    public boolean isConnected() {
        return isConnected;
    }

    public void setConnected(boolean connected) {
        isConnected = connected;
    }

    /**
     * 返回当前登录状态<br>
     * 在登录响应处理器中如果登录成功状态会置换<br>
     *
     * @return
     */
    public boolean isLogin() {
        return isLogin;
    }

    public void setLogin(boolean login) {
        isLogin = login;
    }
}

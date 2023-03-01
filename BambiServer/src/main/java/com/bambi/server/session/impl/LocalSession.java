package com.bambi.server.session.impl;

import com.bambi.config.SystemConfig;
import com.bambi.im.common.bean.dto.UserDTO;
import com.bambi.server.session.IServerSession;
import com.bambi.server.session.SessionManager;
import com.bambi.utils.JsonUtil;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.GenericFutureListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

/**
 * 描述：
 * <br><b>本地Session</b><br>
 * 实现Channel User间的相互绑定<br>
 * 使用{@link AttributeKey}来存储当前sessionID ， 在通道内使用SEESION_KEY获取当前session<br>
 * 详细请阅读文章 <a href = "https://github.com/RichardReindeer/BambiNettyIM/blob/main/docs/AboutSession.MD">AboutSession</a><br>
 *
 * <pre>
 * HISTORY
 * ****************************************************************************
 *  ID     DATE          PERSON          REASON
 *  1      2023/2/26 9:42    Bambi        Create
 * ****************************************************************************
 * </pre>
 *
 * @author Bambi
 * @since 1.0
 */
public class LocalSession implements IServerSession {
    private static Logger logger = LoggerFactory.getLogger(LocalSession.class);

    public static final AttributeKey<String> KEY_USER_ID = AttributeKey.valueOf("key_user_id");
    public static final AttributeKey<LocalSession> SESSION_KEY = AttributeKey.valueOf("SESSION_KEY");


    // 本地session管理核心属性， 用来实现user - channel的相互导航
    private Channel channel; // 通道
    private UserDTO userDTO; // 用户实体类
    private final String sessionID; // 唯一id
    private boolean isLogin = false;

    public LocalSession(Channel channel) {
        this.channel = channel;
        sessionID = buildSessionID();
    }

    /**
     * 创建唯一sessionID
     *
     * @return
     */
    private String buildSessionID() {
        String s = UUID.randomUUID().toString();
        return s.replaceAll("-", "");
    }

    /**
     * 实现双向绑定
     *
     * @return
     */
    public LocalSession bindChannel() {
        channel.attr(LocalSession.SESSION_KEY).set(this);
        channel.attr(SystemConfig.CHANNEL_NAME).set(JsonUtil.pojoToJsonByGson(userDTO));
        isLogin = true;
        return this;
    }

    /**
     * 通过channel的attributeKey 获取对应存储的localsession
     *
     * @param context
     * @return
     */
    public static LocalSession getSession(ChannelHandlerContext context) {
        LocalSession localSession = context.channel().attr(LocalSession.SESSION_KEY).get();
        return localSession;
    }

    public LocalSession unBind() {
        isLogin = false;
        SessionManager.getInstance().removeSession(getSessionID());
        this.close();
        return this;
    }

    /**
     * 关闭连接<br>
     * 当session关闭连接时，在SessionManager中通知其他节点，该用户下线
     */
    public synchronized void close() {
        ChannelFuture close = channel.close();
        close.addListener(new GenericFutureListener<ChannelFuture>() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                if (!future.isSuccess()) {
                    logger.error("channel close error ！！！！ , session ID {}", sessionID);
                }
            }
        });
    }

    public UserDTO getUserDTO() {
        return userDTO;
    }

    public void setUserDTO(UserDTO userDTO) {
        this.userDTO = userDTO;
        userDTO.setSessionId(sessionID);
    }

    /**
     * 将msg写入通道<br>
     *  TODO 进行水位检测，当水位过高时暂停写入，将数据暂存在mq或者其他sql中 , 避免出现数据积压
     *
     * @param protobufMsg
     */
    @Override
    public synchronized void writeAndFlush(Object protobufMsg) {
        if (channel.isWritable()) {
            channel.writeAndFlush(protobufMsg);
        } else {
            // 暂存逻辑
            logger.debug("当前通道忙无法写入");
        }
    }

    public void writeAndClose(Object protobufMsg) {
        channel.writeAndFlush(protobufMsg);
        close();
    }

    public String getSessionID() {
        return sessionID;
    }

    @Override
    public boolean isValid() {
        return getUserDTO() != null ? true : false;
    }

    @Override
    public String getUserID() {
        return userDTO.getUserId();
    }

    public Channel getChannel() {
        return channel;
    }

    public void setChannel(Channel channel) {
        this.channel = channel;
    }

    public boolean isLogin() {
        return isLogin;
    }

    public void setLogin(boolean login) {
        isLogin = login;
    }
}

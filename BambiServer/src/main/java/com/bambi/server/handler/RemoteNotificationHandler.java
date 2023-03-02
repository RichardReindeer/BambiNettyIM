package com.bambi.server.handler;

import com.bambi.bean.entity.BambiNode;
import com.bambi.bean.msg.ProtoBufMessage;
import com.bambi.config.SystemConfig;
import com.bambi.im.common.bean.notification.Notification;
import com.bambi.server.session.SessionManager;
import com.bambi.utils.JsonUtil;
import com.google.common.reflect.TypeToken;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 描述：
 *      <br><b>服务器内部通知处理器</b><br>
 *      接收并处理服务器间转发的通知，参考类{@link com.bambi.server.rpc.WorkerRouter}中的消息转发逻辑<br>
 *      接收到通知类别信息之后检测通知类型<br>
 *      如果通知类型为{@link Notification#CONNECT_FINISHED} 则表面这次的通道连接是服务器之间的消息转发通道<br>
 *      则会移除{@link LoginRequestHandler}<br>
 *
 * <pre>
 * HISTORY
 * ****************************************************************************
 *  ID     DATE          PERSON          REASON
 *  1      2023/2/28 21:21    Bambi        Create
 * ****************************************************************************
 * </pre>
 *
 * @author Bambi
 * @since 1.0
 */
@ChannelHandler.Sharable
@Service("remoteNotificationHandler")
public class RemoteNotificationHandler extends ChannelInboundHandlerAdapter {
    private static Logger logger = LoggerFactory.getLogger(RemoteNotificationHandler.class);

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if(msg == null || !(msg instanceof ProtoBufMessage.Message)){
            super.channelRead(ctx, msg);
            return;
        }

        ProtoBufMessage.Message message = (ProtoBufMessage.Message) msg;
        ProtoBufMessage.HeadType type = message.getType();
        if(!(type.equals(ProtoBufMessage.HeadType.MESSAGE_NOTIFICATION))){
            super.channelRead(ctx, msg);
            return;
        }

        ProtoBufMessage.MessageNotification notification = message.getNotification();
        String json = notification.getJson();
        Notification<Notification.ContentWrapper> notify = JsonUtil.jsonToPojoByGson(json, new TypeToken<Notification<Notification.ContentWrapper>>() {
        }.getType());

        if(notify.getType()== Notification.SESSION_OFFLINE){
            // 用户下线
            logger.info("用户下线了 sessionID {}",notify.getWrapperContent());
            SessionManager.getInstance().removeSession(notify.getWrapperContent());
        }
        if(notify.getType() == Notification.SESSION_ONLINE){
            // 用户上线
            logger.info("用户上线了 sessionID {}",notify.getWrapperContent());
        }
        if(notify.getType() == Notification.CONNECT_FINISHED){
            /*logger.info("服务器节点连接成功通知 {}",notify.getData());*/
            Notification<BambiNode> bambiNodeNotification = JsonUtil.jsonToPojoByGson(json, new TypeToken<Notification<BambiNode>>() {
            }.getType());

            logger.info("服务器节点登录成功 {}",json);
            ctx.pipeline().remove("login");
            ctx.channel().attr(SystemConfig.CHANNEL_NAME).set(JsonUtil.pojoToJsonByGson(bambiNodeNotification));

        }

    }
}

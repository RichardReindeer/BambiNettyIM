package com.bambi.server.handler;

import com.bambi.bean.msg.ProtoBufMessage;
import com.bambi.currentUtils.FutureTaskExecutor;
import com.bambi.server.receiver.impl.ChatRedirectReceiver;
import com.bambi.server.session.IServerSession;
import com.bambi.server.session.SessionManager;
import com.bambi.server.session.impl.LocalSession;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;

/**
 * 描述：
 * <br><b>消息回显处理器</b><br>
 * 根据是否获取到session来判断收到的消息是来自用户发送还是中转过来的消息<br>
 * <br>
 * 消息中转发生在其他节点的客户端给当前节点的客户发送信息<br>
 *
 * <pre>
 * HISTORY
 * ****************************************************************************
 *  ID     DATE          PERSON          REASON
 *  1      2023/2/28 21:38    Bambi        Create
 * ****************************************************************************
 * </pre>
 *
 * @author Bambi
 * @since 1.0
 */
@ChannelHandler.Sharable
@Service("chatRedirectHandler")
public class ChatRedirectHandler extends ChannelInboundHandlerAdapter {
    private static Logger logger = LoggerFactory.getLogger(ChatRedirectHandler.class);

    @Autowired
    private ChatRedirectReceiver chatRedirectReceiver;
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
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
    }
}
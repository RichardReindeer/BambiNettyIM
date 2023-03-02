package com.bambi.server.receiver.impl;

import com.bambi.bean.msg.ProtoBufMessage;
import com.bambi.server.receiver.AbstractVerifier;
import com.bambi.server.session.IServerSession;
import com.bambi.server.session.SessionManager;
import com.bambi.server.session.impl.LocalSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;

/**
 * 描述：
 *      <br><b>聊天信息处理器</b><br>
 *      接收到客户端传递过来的消息<br>
 *      根据userID找到他所有的在线设备(即遍历session集合) ,并调用{@link IServerSession#writeAndFlush(Object)}将信息发送
 *      <br>如果目标用户不在线，可以将消息存储在数据库或则和mq中
 * <pre>
 * HISTORY
 * ****************************************************************************
 *  ID     DATE          PERSON          REASON
 *  1      2023/3/1 22:14    Bambi        Create
 * ****************************************************************************
 * </pre>
 *
 * @author Bambi
 * @since 1.0
 */
@Service("chatRedirectReceiver")
public class ChatRedirectReceiver extends AbstractVerifier {
    private static Logger logger = LoggerFactory.getLogger(ChatRedirectReceiver.class);
    @Override
    public ProtoBufMessage.HeadType getHeadType() {
        return ProtoBufMessage.HeadType.MESSAGE_REQUEST;
    }

    @Override
    public boolean action(LocalSession session, ProtoBufMessage.Message message) {
        ProtoBufMessage.MessageRequest messageRequest = message.getMessageRequest();
        logger.info("收到来自 {} 写给 {} 的信息 , 内容为 {}",messageRequest.getFrom(),messageRequest.getTo(),messageRequest.getContent());
        String to = messageRequest.getTo();
        ArrayList<IServerSession> sessionById = SessionManager.getInstance().getSessionById(to);
        if(sessionById!=null){
            sessionById.forEach(serverSession -> {
                serverSession.writeAndFlush(message);
            });
        }else {
            logger.error("目标用户不在线，可以将信息暂存在数据库中");
        }
        return false;
    }
}

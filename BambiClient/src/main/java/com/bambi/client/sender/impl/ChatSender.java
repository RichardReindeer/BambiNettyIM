package com.bambi.client.sender.impl;

import com.bambi.bean.msg.ProtoBufMessage;
import com.bambi.client.protoBuilder.impl.ChatMessageBuilder;
import com.bambi.client.sender.BaseSender;
import com.bambi.im.common.bean.chat.ChatMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 描述：
 *      <br><b>消息发送器</b><br>
 *      判断连接状态并发送信息
 * <pre>
 * HISTORY
 * ****************************************************************************
 *  ID     DATE          PERSON          REASON
 *  1      2023/2/27 13:04    Bambi        Create
 * ****************************************************************************
 * </pre>
 *
 * @author Bambi
 * @since 1.0
 */
@Service("chatSender")
public class ChatSender extends BaseSender {
    private static Logger logger = LoggerFactory.getLogger(ChatSender.class);

    public void sendChatMessage(String toUserId , String content){
        if(isConnected()){
            ChatMessage chatMessage = new ChatMessage(getUserDTO());
            chatMessage.setTo(toUserId);
            chatMessage.setContent(content);
            chatMessage.setMsgType(ChatMessage.MSGTYPE.TEXT);
            chatMessage.setMsgId(System.currentTimeMillis());
            ProtoBufMessage.Message message = ChatMessageBuilder.buildChatMessage(chatMessage,
                    getUserDTO(),
                    getClientSession());
            super.sendMsg(message);
        }else {
            logger.error("建立连接失败");
            return;
        }
    }

    @Override
    protected void sendException(Throwable t) {
        logger.info("聊天信息发送出现异常 {}",t.getMessage());
    }

    @Override
    protected void sendFailed(ProtoBufMessage.Message message) {
        logger.info("聊天信息发送失败");
    }

    @Override
    protected void sendSuccessed(ProtoBufMessage.Message message) {
        logger.info("聊天信息发送成功 , 目标id {}",message.getMessageRequest().getTo());
    }
}

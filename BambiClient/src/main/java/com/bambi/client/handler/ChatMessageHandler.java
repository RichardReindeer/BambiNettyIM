package com.bambi.client.handler;

import com.bambi.bean.msg.ProtoBufMessage;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 描述：
 * <br></rb><b>聊天信息处理类</b><br>
 * 接收到服务器端传递过来的信息<br>
 * 并将信息显示在控制台上
 * <pre>
 * HISTORY
 * ****************************************************************************
 *  ID     DATE          PERSON          REASON
 *  1      2023/2/27 8:40    Bambi        Create
 * ****************************************************************************
 * </pre>
 *
 * @author Bambi
 * @since 1.0
 */
@Service("chatMessageHandler")
public class ChatMessageHandler extends ChannelInboundHandlerAdapter {
    private static Logger logger = LoggerFactory.getLogger(ChatMessageHandler.class);

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg == null || !(msg instanceof ProtoBufMessage.Message)) {
            super.channelRead(ctx, msg);
            return;
        }

        ProtoBufMessage.Message message = (ProtoBufMessage.Message) msg;
        ProtoBufMessage.HeadType type = message.getType();
        if (!type.equals(ProtoBufMessage.HeadType.MESSAGE_REQUEST)) {
            super.channelRead(ctx, msg);
            return;
        }
        ProtoBufMessage.MessageRequest messageRequest = message.getMessageRequest();
        String content = messageRequest.getContent();
        String from = messageRequest.getFrom();
        logger.info("{} 说: {}", from, content);

    }
}

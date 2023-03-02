package com.bambi.client.protoBuilder.impl;

import com.bambi.bean.msg.ProtoBufMessage;
import com.bambi.client.protoBuilder.BaseBuilder;
import com.bambi.client.session.ClientSession;
import com.bambi.im.common.bean.chat.ChatMessage;
import com.bambi.im.common.bean.dto.UserDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 描述：
 *
 * <pre>
 * HISTORY
 * ****************************************************************************
 *  ID     DATE          PERSON          REASON
 *  1      2023/2/27 5:16    Bambi        Create
 * ****************************************************************************
 * </pre>
 *
 * @author Bambi
 * @since 1.0
 */
public class ChatMessageBuilder extends BaseBuilder {
    private static Logger logger = LoggerFactory.getLogger(ChatMessageBuilder.class);

    private ChatMessage chatMessage;
    private UserDTO user;


    public ChatMessageBuilder(ChatMessage chatMessage, UserDTO user, ClientSession session)
    {
        super(ProtoBufMessage.HeadType.MESSAGE_REQUEST, session);
        this.chatMessage = chatMessage;
        this.user = user;

    }


    public ProtoBufMessage.Message build()
    {
        ProtoBufMessage.Message message = buildCommon(-1);
        ProtoBufMessage.MessageRequest.Builder cb
                = ProtoBufMessage.MessageRequest.newBuilder();

        chatMessage.fillMsg(cb);
        return message
                .toBuilder()
                .setMessageRequest(cb)
                .build();
    }

    public static ProtoBufMessage.Message buildChatMessage(
            ChatMessage chatMessage,
            UserDTO user,
            ClientSession session)
    {
        ChatMessageBuilder builder =
                new ChatMessageBuilder(chatMessage, user, session);
        return builder.build();

    }
}

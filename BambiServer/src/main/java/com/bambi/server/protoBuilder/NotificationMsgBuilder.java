package com.bambi.server.protoBuilder;

import com.bambi.bean.msg.ProtoBufMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 描述：
 *      一个普通的protobuf通知类构造builder
 * <pre>
 * HISTORY
 * ****************************************************************************
 *  ID     DATE          PERSON          REASON
 *  1      2023/2/25 9:28    Bambi        Create
 * ****************************************************************************
 * </pre>
 *
 * @author Bambi
 * @since 1.0
 */
@Service("notificationMsgBuilder")
public class NotificationMsgBuilder {
    private static Logger logger = LoggerFactory.getLogger(NotificationMsgBuilder.class);
    public static ProtoBufMessage.Message buildNotification(String json)
    {
        ProtoBufMessage.Message.Builder mb = ProtoBufMessage.Message.newBuilder()
                .setType(ProtoBufMessage.HeadType.MESSAGE_NOTIFICATION);   //设置消息类型
        ProtoBufMessage.MessageNotification.Builder rb =
                ProtoBufMessage.MessageNotification.newBuilder()
                        .setJson(json);
        mb.setNotification(rb.build());
        return mb.build();
    }

}

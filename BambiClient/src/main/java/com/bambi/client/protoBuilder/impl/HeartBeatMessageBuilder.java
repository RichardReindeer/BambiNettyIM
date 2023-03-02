package com.bambi.client.protoBuilder.impl;

import com.bambi.bean.msg.ProtoBufMessage;
import com.bambi.client.protoBuilder.BaseBuilder;
import com.bambi.client.session.ClientSession;
import com.bambi.im.common.bean.dto.UserDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 描述：
 * <br><b>心跳数据包构造器</b><br>
 * <pre>
 * HISTORY
 * ****************************************************************************
 *  ID     DATE          PERSON          REASON
 *  1      2023/2/27 10:50    Bambi        Create
 * ****************************************************************************
 * </pre>
 *
 * @author Bambi
 * @since 1.0
 */
public class HeartBeatMessageBuilder extends BaseBuilder {
    private static Logger logger = LoggerFactory.getLogger(HeartBeatMessageBuilder.class);
    private final UserDTO user;

    public HeartBeatMessageBuilder(UserDTO user, ClientSession session) {
        super(ProtoBufMessage.HeadType.HEART_BEAT, session);
        this.user = user;
    }

    public ProtoBufMessage.Message buildMsg() {
        ProtoBufMessage.Message message = buildCommon(-1);
        ProtoBufMessage.MessageHeartBeat.Builder lb =
                ProtoBufMessage.MessageHeartBeat.newBuilder()
                        .setSeq(0)
                        .setJson("{\"from\":\"client\"}")
                        .setUid(user.getUserId());
        return message.toBuilder().setHeartBeat(lb).build();
    }
}

package com.bambi.client.protoBuilder.impl;

import com.bambi.bean.msg.ProtoBufMessage;
import com.bambi.client.protoBuilder.BaseBuilder;
import com.bambi.client.session.ClientSession;
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
public class LoginMsgBuilder extends BaseBuilder {
    private static Logger logger = LoggerFactory.getLogger(LoginMsgBuilder.class);

    private final UserDTO user;

    public LoginMsgBuilder(
            UserDTO user,
            ClientSession session)
    {
        super(ProtoBufMessage.HeadType.LOGIN_REQUEST, session);
        this.user = user;
    }

    public ProtoBufMessage.Message build()
    {
        ProtoBufMessage.Message message = buildCommon(-1);
        ProtoBufMessage.LoginRequest.Builder lb =
                ProtoBufMessage.LoginRequest.newBuilder()
                        .setDeviceId(user.getDevId())
                        .setPlatform(user.getPlatform().ordinal())
                        .setToken(user.getToken())
                        .setUid(user.getUserId());
        return message.toBuilder().setLoginRequest(lb).build();
    }

    public static ProtoBufMessage.Message buildLoginMsg(
            UserDTO user,
            ClientSession session)
    {
        LoginMsgBuilder builder = new LoginMsgBuilder(user, session);
        return builder.build();

    }
}

package com.bambi.server.protoBuilder;

import com.bambi.bean.msg.ProtoBufMessage;
import com.bambi.config.ProtoInstant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 描述：
 *      protobuf信息构建
 * <pre>
 * HISTORY
 * ****************************************************************************
 *  ID     DATE          PERSON          REASON
 *  1      2023/2/28 19:56    Bambi        Create
 * ****************************************************************************
 * </pre>
 *
 * @author Bambi
 * @since 1.0
 */
@Service("loginResponseBuilder")
public class LoginResponseBuilder {
    private static Logger logger = LoggerFactory.getLogger(LoginResponseBuilder.class);
    /**
     * 登录应答 应答消息protobuf
     */
    public ProtoBufMessage.Message loginResponse(
            ProtoInstant.ResultCodeEnum en, long seqId, String sessionId)
    {
        ProtoBufMessage.Message.Builder mb = ProtoBufMessage.Message.newBuilder()
                .setType(ProtoBufMessage.HeadType.LOGIN_RESPONSE)  //设置消息类型
                .setSequence(seqId)
                .setSessionId(sessionId);  //设置应答流水，与请求对应

        ProtoBufMessage.LoginResponse.Builder b = ProtoBufMessage.LoginResponse.newBuilder()
                .setCode(en.getCode())
                .setInfo(en.getDesc())
                .setExpose(1);

        mb.setLoginResponse(b.build());
        return mb.build();
    }

}

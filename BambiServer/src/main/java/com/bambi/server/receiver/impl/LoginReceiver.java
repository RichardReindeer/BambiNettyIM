package com.bambi.server.receiver.impl;

import com.bambi.bean.msg.ProtoBufMessage;
import com.bambi.config.ProtoInstant;
import com.bambi.im.common.bean.dto.UserDTO;
import com.bambi.server.protoBuilder.LoginResponseBuilder;
import com.bambi.server.receiver.AbstractVerifier;
import com.bambi.server.session.SessionManager;
import com.bambi.server.session.impl.LocalSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import static com.bambi.config.ProtoInstant.ResultCodeEnum.NO_TOKEN;
import static com.bambi.config.ProtoInstant.ResultCodeEnum.SUCCESS;

/**
 * 描述：
 *
 * <pre>
 * HISTORY
 * ****************************************************************************
 *  ID     DATE          PERSON          REASON
 *  1      2023/3/1 22:04    Bambi        Create
 * ****************************************************************************
 * </pre>
 *
 * @author Bambi
 * @since 1.0
 */
@Service("loginReceiver")
public class LoginReceiver extends AbstractVerifier {
    private static Logger logger = LoggerFactory.getLogger(LoginReceiver.class);
    @Autowired
    LoginResponseBuilder loginResponseBuilder;
    @Autowired
    SessionManager sessionManager;

    @Override
    public ProtoBufMessage.HeadType getHeadType() {
        return ProtoBufMessage.HeadType.LOGIN_REQUEST;
    }


    @Override
    public boolean action(LocalSession session, ProtoBufMessage.Message message) {
        ProtoBufMessage.LoginRequest loginRequest = message.getLoginRequest();
        long sequence = message.getSequence();
        UserDTO userDTO = UserDTO.fromMsg(loginRequest);
        if (!checkUser(userDTO)) {
            logger.error("用户验证失败");
            ProtoInstant.ResultCodeEnum noToken = NO_TOKEN;
            ProtoBufMessage.Message responseError = loginResponseBuilder.loginResponse(noToken, sequence, session.getSessionID());
            session.writeAndFlush(responseError);
            return false;
        }
        session.setUserDTO(userDTO);

        session.bindChannel();
        sessionManager.addSession(session);

        ProtoInstant.ResultCodeEnum success = SUCCESS;
        ProtoBufMessage.Message responseMsg = loginResponseBuilder.loginResponse(success, sequence, session.getSessionID());
        session.writeAndFlush(responseMsg);
        return true;
    }

    /**
     * 没做数据库，暂时没有验证逻辑，直接返回true
     *
     * @param userDTO
     * @return
     */
    private boolean checkUser(UserDTO userDTO) {
        return true;
    }
}

package com.bambi.client.sender.impl;

import com.bambi.bean.msg.ProtoBufMessage;
import com.bambi.client.protoBuilder.impl.LoginMsgBuilder;
import com.bambi.client.sender.BaseSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 描述：
 *      <br><b>登录数据发送器</b><br>
 *      在从网关获取到服务器节点后，将登录请求发送给服务器<br>
 *      具体逻辑参考文档 <a href = "https://github.com/RichardReindeer/BambiNettyIM/blob/main/docs/Login.md">Login</a>
 * <pre>
 * HISTORY
 * ****************************************************************************
 *  ID     DATE          PERSON          REASON
 *  1      2023/2/27 13:03    Bambi        Create
 * ****************************************************************************
 * </pre>
 *
 * @author Bambi
 * @since 1.0
 */
@Service("loginSender")
public class LoginSender extends BaseSender {
    private static Logger logger = LoggerFactory.getLogger(LoginSender.class);

    public void sendLoginMsg(){
        if(isConnected()){
            logger.info("连接成功");
            ProtoBufMessage.Message message = LoginMsgBuilder.buildLoginMsg(getUserDTO(), getClientSession());
            logger.info("发送的登录信息是个啥 {}",message);
            super.sendMsg(message);
        }else {
            logger.error("建立连接失败");
            return;
        }
    }
}

package com.bambi.client.handler;

import com.bambi.bean.msg.ProtoBufMessage;
import com.bambi.client.controller.CommandController;
import com.bambi.client.session.ClientSession;
import com.bambi.config.ProtoInstant;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


/**
 * 描述：
 *      <br><b>登录请求Handler</b><br>
 *      用户发送登录数据包通过{@link com.bambi.client.sender.impl.LoginSender} ，登录成功后服务器发送的登录响应则由此类进行处理<br>
 *      登录逻辑完成后便会移除本Handler , 并添加{@link HeartBeatHandler} 来避免假死现象<br>
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
@Service("loginResponseHandler")
@ChannelHandler.Sharable
public class LoginResponseHandler extends ChannelInboundHandlerAdapter {
    private static Logger logger = LoggerFactory.getLogger(LoginResponseHandler.class);

    @Autowired
    private CommandController commandController;
    @Autowired
    private HeartBeatHandler heartBeatHandler;

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg == null || !(msg instanceof ProtoBufMessage.Message)) {
            super.channelRead(ctx, msg);
            return;
        }
        ProtoBufMessage.Message message = (ProtoBufMessage.Message) msg;
        ProtoBufMessage.HeadType type = message.getType();
        if (!type.equals(ProtoBufMessage.HeadType.LOGIN_RESPONSE)) {
            super.channelRead(ctx, msg);
            return;
        }
        // 处理登录响应
        ProtoBufMessage.LoginResponse loginResponse = message.getLoginResponse();
        ProtoInstant.ResultCodeEnum value = ProtoInstant.ResultCodeEnum.values()[loginResponse.getCode()];
        if (value.equals(ProtoInstant.ResultCodeEnum.SUCCESS)) {

            addSession(ctx, message);

            logger.info("节点登录成功");
            commandController.notifyCommandThread();

            ctx.channel().pipeline().addAfter("loginResponseHandler", "heartBeatClientHandler", heartBeatHandler);
            heartBeatHandler.channelActive(ctx);
            // 热插拔逻辑，从当前通道中剔除登录处理器
            ctx.channel().pipeline().remove("loginResponseHandler");
        } else {
            logger.error("服务器节点 登录失败");
            logger.error(value.getDesc());
        }
    }

    /**
     * 在客户端添加session信息
     *
     * @param ctx
     * @param message
     */
    private void addSession(ChannelHandlerContext ctx, ProtoBufMessage.Message message) {
        ClientSession clientSession = ctx.channel().attr(ClientSession.SESSION_KEY).get();
        clientSession.setSessionID(message.getSessionId());
        clientSession.setLogin(true);
    }
}

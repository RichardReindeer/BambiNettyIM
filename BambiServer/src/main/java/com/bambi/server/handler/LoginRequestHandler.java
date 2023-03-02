package com.bambi.server.handler;

import com.bambi.bean.msg.ProtoBufMessage;
import com.bambi.currentUtils.CallbackTask;
import com.bambi.currentUtils.CallbackTaskExecutor;
import com.bambi.server.receiver.impl.LoginReceiver;
import com.bambi.server.session.SessionManager;
import com.bambi.server.session.impl.LocalSession;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 描述：
 * <br><b>登录请求逻辑处理类</b><br>
 * 异步使用登录校验器来进行登录验证；如果登录失败则会调用{@link SessionManager#closeSession(ChannelHandlerContext)} 来关闭连接
 * <pre>
 * HISTORY
 * ****************************************************************************
 *  ID     DATE          PERSON          REASON
 *  1      2023/2/28 9:24    Bambi        Create
 * ****************************************************************************
 * </pre>
 *
 * @author Bambi
 * @since 1.0
 */
@ChannelHandler.Sharable
@Service("loginRequestHandler")
public class LoginRequestHandler extends ChannelInboundHandlerAdapter {
    private static Logger logger = LoggerFactory.getLogger(LoginRequestHandler.class);

    @Autowired
    LoginReceiver loginReceiver;

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg == null || !(msg instanceof ProtoBufMessage.Message)) {
            super.channelRead(ctx, msg);
            return;
        }
        ProtoBufMessage.Message message = (ProtoBufMessage.Message) msg;
        ProtoBufMessage.HeadType type = message.getType();
        if (!type.equals(loginReceiver.getHeadType())) {
            super.channelRead(ctx, msg);
            return;
        }

        LocalSession localSession = new LocalSession(ctx.channel());
        CallbackTaskExecutor.add(new CallbackTask<Boolean>() {
            @Override
            public Boolean execute() throws Exception {
                return loginReceiver.action(localSession, message);
            }

            @Override
            public void onBack(Boolean o) {
                if (o) {
                    logger.info("登录成功 {}", localSession.getUserDTO());
                    ctx.channel().pipeline().addAfter("login", "heartBeat", new HeartBeatHandler());
                    ctx.channel().pipeline().remove("login");
                } else {
                    SessionManager.getInstance().closeSession(ctx);
                    logger.error("登录失败");
                }
            }

            @Override
            public void onException(Throwable t) {
                t.printStackTrace();
                logger.error("登录失败 {}", localSession.getUserDTO());
                SessionManager.getInstance().closeSession(ctx);
            }
        });
    }
}

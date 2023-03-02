package com.bambi.server.handler;

import com.bambi.bean.msg.ProtoBufMessage;
import com.bambi.server.session.SessionManager;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 描述：
 *      <br><b>心跳逻辑处理器</b><br>
 *      处理空闲检测逻辑，以及向客户端返回心跳包
 * <pre>
 * HISTORY
 * ****************************************************************************
 *  ID     DATE          PERSON          REASON
 *  1      2023/2/28 20:06    Bambi        Create
 * ****************************************************************************
 * </pre>
 *
 * @author Bambi
 * @since 1.0
 */
@ChannelHandler.Sharable
@Service("heartBeatHandler")
public class HeartBeatHandler extends IdleStateHandler {
    private static Logger logger = LoggerFactory.getLogger(HeartBeatHandler.class);

    private static final int READ_IDLE_TIME = 1500;
    public HeartBeatHandler() {
        super(READ_IDLE_TIME, 0, 0, TimeUnit.SECONDS);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if(msg == null || !(msg instanceof ProtoBufMessage.Message)){
            super.channelRead(ctx,msg);
            return;
        }

        ProtoBufMessage.Message message = (ProtoBufMessage.Message) msg;

        ProtoBufMessage.HeadType type = message.getType();
        if(type.equals(ProtoBufMessage.HeadType.HEART_BEAT)){
            if(ctx.channel().isActive()){
                // 把心跳包还回去
                ctx.writeAndFlush(msg);
            }
        }

        super.channelRead(ctx,msg);
    }

    /**
     * 假死发生时的处理逻辑
     * @param ctx
     * @param evt
     * @throws Exception
     */
    @Override
    protected void channelIdle(ChannelHandlerContext ctx, IdleStateEvent evt) throws Exception {
        logger.info("{}s内没有读取到数据，关闭连接 ",READ_IDLE_TIME);
        SessionManager.getInstance().closeSession(ctx);
    }

}
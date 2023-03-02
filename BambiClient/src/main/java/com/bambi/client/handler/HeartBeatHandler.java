package com.bambi.client.handler;

import com.bambi.bean.msg.ProtoBufMessage;
import com.bambi.client.protoBuilder.impl.HeartBeatMessageBuilder;
import com.bambi.client.session.ClientSession;
import com.bambi.im.common.bean.dto.UserDTO;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 描述：
 * <b>心跳处理器类</b><br>
 * 递归调用心跳发送逻辑，定时向服务器发送心跳包避免出现假死现象<br>
 * 并接收服务器发送回来的心跳回写数据包。
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
@Service("heartBeatHandler")
@ChannelHandler.Sharable
public class HeartBeatHandler extends ChannelInboundHandlerAdapter {
    private static Logger logger = LoggerFactory.getLogger(HeartBeatHandler.class);

    // 间隔多少毫秒发送心跳
    private static final Integer HEARTBEAT_TIMES = 60;

    /**
     * 当通道激活时发送心跳<br>
     * 在登录响应处理器中触发{@link LoginResponseHandler}
     *
     * @param ctx
     * @throws Exception
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        ClientSession clientSession = ctx.channel().attr(ClientSession.SESSION_KEY).get();
        UserDTO userDTO = clientSession.getUserDTO();
        HeartBeatMessageBuilder heartBeatMsgBuilder = new HeartBeatMessageBuilder(userDTO,clientSession);
        ProtoBufMessage.Message message = heartBeatMsgBuilder.buildMsg();
        super.channelActive(ctx);
        heartBeat(ctx,message);
    }

    /**
     * 在时间间隔内递归调用发送心跳包
     * @param ctx
     * @param message
     */
    private void heartBeat(ChannelHandlerContext ctx, ProtoBufMessage.Message message) {
        ctx.executor().schedule(()->{
            if(ctx.channel().isActive()){
                ctx.writeAndFlush(message);
                heartBeat(ctx,message);
            }
        },HEARTBEAT_TIMES, TimeUnit.SECONDS);
    }

    /**
     * 处理服务器端回显的HeartBeat数据包
     * @param ctx
     * @param msg
     * @throws Exception
     */
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg == null || !(msg instanceof ProtoBufMessage.Message)){
            super.channelRead(ctx, msg);
            return;
        }
        ProtoBufMessage.Message message = (ProtoBufMessage.Message) msg;
        ProtoBufMessage.HeadType type = message.getType();
        if(type.equals(ProtoBufMessage.HeadType.HEART_BEAT)){
            logger.info("收到来自服务器端的数据回写");
            return;
        }else {
            super.channelRead(ctx, msg);
        }
    }
}

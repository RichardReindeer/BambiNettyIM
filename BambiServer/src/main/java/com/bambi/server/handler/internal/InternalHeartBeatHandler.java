package com.bambi.server.handler.internal;

import com.bambi.bean.msg.ProtoBufMessage;
import com.bambi.server.rpc.BambiWorker;
import com.bambi.utils.JsonUtil;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * 描述：
 * <br><b>转发器心跳处理器</b><br>
 * 避免假死现象定时发送心跳数据包
 * <pre>
 * HISTORY
 * ****************************************************************************
 *  ID     DATE          PERSON          REASON
 *  1      2023/2/23 6:12    Bambi        Create
 * ****************************************************************************
 * </pre>
 *
 * @author Bambi
 * @since 1.0
 */
@ChannelHandler.Sharable
public class InternalHeartBeatHandler extends ChannelInboundHandlerAdapter {
    private static Logger logger = LoggerFactory.getLogger(InternalHeartBeatHandler.class);

    String fromInfo = null;
    int seq = 0;
    private static final int HEARTBEAT_TIME = 50;// 心跳时间间隔

    /**
     * 在装填handler时开始发送心跳
     *
     * @param ctx
     * @throws Exception
     */
    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        sendHeartBeat(ctx);
    }

    private void sendHeartBeat(ChannelHandlerContext ctx) {
        ProtoBufMessage.Message message = buildMessage4HeartBeat();
        ctx.executor().schedule(() -> {
            if (ctx.channel().isActive()) {
                logger.info("发送信息");
                ctx.channel().writeAndFlush(message);
                // 直接发送下一次心跳
                sendHeartBeat(ctx);
            }
        }, HEARTBEAT_TIME, TimeUnit.SECONDS);
    }

    /**
     * 将本地节点的基础信息放入信息包中组装
     *
     * @return
     */
    public ProtoBufMessage.Message buildMessage4HeartBeat() {
        if (null == fromInfo) {
            fromInfo = JsonUtil.pojoToJsonByGson(BambiWorker.getInstance().getLocalNodeInfo());
        }

        ProtoBufMessage.Message.Builder mb = ProtoBufMessage.Message.newBuilder()
                .setType(ProtoBufMessage.HeadType.HEART_BEAT)  //设置消息类型
                .setSequence(++seq);                 //设置应答流水，与请求对应
        ProtoBufMessage.MessageHeartBeat.Builder heartBeat =
                ProtoBufMessage.MessageHeartBeat.newBuilder()
                        .setSeq(seq)
                        .setJson(fromInfo)
                        .setUid("-1");
        mb.setHeartBeat(heartBeat.build());
        return mb.build();
    }

    /**
     * 处理接收到的心跳回写信息
     *
     * @param ctx
     * @param msg
     * @throws Exception
     */
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (null == msg || !(msg instanceof ProtoBufMessage.Message)) {
            super.channelRead(ctx, msg);
            return;
        }
        ProtoBufMessage.Message pMessage = (ProtoBufMessage.Message) msg;
        ProtoBufMessage.HeadType type = pMessage.getType();
        if (type.equals(ProtoBufMessage.HeadType.HEART_BEAT)) {
            ProtoBufMessage.MessageHeartBeat heartBeat = pMessage.getHeartBeat();
            logger.info("收到来自 {} 的心跳信息 ", heartBeat.getJson());
            logger.info("心跳信息序列seq {}", heartBeat.getSeq());
            return;
        } else {
            super.channelRead(ctx, msg);
        }
    }
}

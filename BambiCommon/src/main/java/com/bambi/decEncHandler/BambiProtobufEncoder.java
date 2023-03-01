package com.bambi.decEncHandler;

import com.bambi.bean.msg.ProtoBufMessage;
import com.bambi.config.ProtoInstant;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 描述：
 *      <br><b>protobuf数据包简易编码类</b><br>
 *      protobuf 数据包 {@link com.bambi.bean.msg.ProtoBufMessage.Message}
 * <pre>
 * HISTORY
 * ****************************************************************************
 *  ID     DATE          PERSON          REASON
 *  1      2023/3/1 11:57    Bambi        Create
 * ****************************************************************************
 * </pre>
 *
 * @author Bambi
 * @since 1.0
 */
public class BambiProtobufEncoder extends MessageToByteEncoder<ProtoBufMessage.Message> {
    private static Logger logger = LoggerFactory.getLogger(BambiProtobufEncoder.class);

    @Override
    protected void encode(ChannelHandlerContext channelHandlerContext, ProtoBufMessage.Message message, ByteBuf byteBuf) throws Exception {
        encode0(message, byteBuf);
    }


    public static void encode0(ProtoBufMessage.Message message, ByteBuf byteBuf) {
        byteBuf.writeShort(ProtoInstant.MAGIC_CODE);
        byteBuf.writeShort(ProtoInstant.VERSION_CODE);
        byte[] bytes = message.toByteArray();

        int length = bytes.length;
        logger.debug("消息当前长度为: {}", length);
        byteBuf.writeInt(length);
        byteBuf.writeBytes(bytes);
    }
}

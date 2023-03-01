package com.bambi.decEncHandler;

import com.bambi.bean.msg.ProtoBufMessage;
import com.bambi.config.ProtoInstant;
import com.bambi.exception.InvalidFrameException;
import com.google.protobuf.InvalidProtocolBufferException;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * 描述：
 *      <br><b>简易protobuf解码器</b><br>
 *      protobuf 数据包 {@link com.bambi.bean.msg.ProtoBufMessage.Message}
 * <pre>
 * HISTORY
 * ****************************************************************************
 *  ID     DATE          PERSON          REASON
 *  1      2023/3/1 11:53    Bambi        Create
 * ****************************************************************************
 * </pre>
 *
 * @author Bambi
 * @since 1.0
 */
public class BambiProtobufDecoder extends ByteToMessageDecoder {
    private static Logger logger = LoggerFactory.getLogger(BambiProtobufDecoder.class);

    @Override
    protected void decode(ChannelHandlerContext channelHandlerContext, ByteBuf byteBuf, List<Object> list) throws Exception {
        Object outMessage = decode0(channelHandlerContext,byteBuf);
        if(outMessage != null){
            // 如果对象不为空，则获取业务信息
            list.add(outMessage);
        }
    }

    /**
     * 与客户端解码器类似
     * 先标记当前指针位置，再对其包头长度、魔数、版本等进行判断
     *
     * @param channelHandlerContext
     * @param byteBuf
     * @return
     */
    private Object decode0(ChannelHandlerContext channelHandlerContext, ByteBuf byteBuf) throws InvalidFrameException, InvalidProtocolBufferException {
        // 标记当前读取位置
        byteBuf.markReaderIndex();
        if(byteBuf.readableBytes()<8){
            logger.error("当前可读字节小于包头长度");
            return null;
        }

        short magic = byteBuf.readShort();
        if(magic != ProtoInstant.MAGIC_CODE){
            String errorMsg = "客户端口令传递出错: "+ channelHandlerContext.channel().remoteAddress();
            throw new InvalidFrameException(errorMsg);
        }
        short version = byteBuf.readShort();
        if(version != ProtoInstant.VERSION_CODE){
            String errorMsg = "协议版本出错: "+ channelHandlerContext.channel().remoteAddress();
            throw new InvalidFrameException(errorMsg);
        }
        int length = byteBuf.readInt();
        if(length<0){
            logger.error("非法数据，关闭连接");
            channelHandlerContext.close();
        }

        // 消息体长度小于传递过来的消息长度
        if(length > byteBuf.readableBytes()){
            byteBuf.resetReaderIndex();
            return null;
        }

        // 成功读取
        logger.debug("成功接收信息，信息长度为{}",length);
        byte[] array;
        if(byteBuf.hasArray()){
            // 使用堆缓冲
            ByteBuf slice = byteBuf.slice(byteBuf.readerIndex(),length);
            array = slice.array();
            byteBuf.readBytes(array,0,length);
        }else {
            // 直接缓冲
            array = new byte[length];
            byteBuf.readBytes(array,0,length);
        }

        // 字符串转对象
        ProtoBufMessage.Message message = ProtoBufMessage.Message.parseFrom(array);
        return message;
    }
}

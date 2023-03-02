package com.bambi.server.handler.internal;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 描述：
 *<br><b>转发器异常处理器</b><br>
 * 在{@link com.bambi.server.rpc.InternalSender} 中使用<br>
 * 负责在尾部捕获异常<br>
 * <pre>
 * HISTORY
 * ****************************************************************************
 *  ID     DATE          PERSON          REASON
 *  1      2023/2/23 6:40    Bambi        Create
 * ****************************************************************************
 * </pre>
 *
 * @author Bambi
 * @since 1.0
 */
@ChannelHandler.Sharable
public class InternalExceptionHandler extends ChannelInboundHandlerAdapter {
    private static Logger logger = LoggerFactory.getLogger(InternalExceptionHandler.class);

    /**
     * 对异常捕获
     * @param ctx
     * @param cause
     * @throws Exception
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        logger.error("异常信息捕获 {}",cause.getMessage());
        ctx.close();
    }

    /**
     * 异常处理器位于inbound末尾，到此即表示通道读取完毕<br>
     * 需要刷新
     * @param ctx
     * @throws Exception
     */
    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.channel().flush();
    }
}

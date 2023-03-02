package com.bambi.server.handler;

import com.bambi.exception.InvalidFrameException;
import com.bambi.server.session.SessionManager;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 描述：
 *      <br><b>服务器异常处理器类</b><br>
 *      放在pipeline尾部用来捕获并处理异常。
 * <pre>
 * HISTORY
 * ****************************************************************************
 *  ID     DATE          PERSON          REASON
 *  1      2023/2/26 2:14    Bambi        Create
 * ****************************************************************************
 * </pre>
 *
 * @author Bambi
 * @since 1.0
 */
@ChannelHandler.Sharable
@Service("serverExceptionHandler")
public class ServerExceptionHandler extends ChannelInboundHandlerAdapter {
    private static Logger logger = LoggerFactory.getLogger(ServerExceptionHandler.class);

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        if(cause instanceof InvalidFrameException){
            logger.error(cause.getMessage());
        }else {
            cause.printStackTrace();
            logger.error("捕获异常 {}",cause.getMessage());
        }
        SessionManager.getInstance().closeSession(ctx);
        ctx.close();
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        SessionManager.getInstance().closeSession(ctx);
    }

}
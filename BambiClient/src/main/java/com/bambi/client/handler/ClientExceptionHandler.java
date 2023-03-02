package com.bambi.client.handler;

import com.bambi.client.controller.CommandController;
import com.bambi.exception.InvalidFrameException;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 描述：
 *      <br><b>客户端异常处理器</b><br>
 *      位于pipeline业务逻辑处理器末尾，捕获异常并反转对应连接状态
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
@ChannelHandler.Sharable
@Service("clientExceptionHandler")
public class ClientExceptionHandler extends ChannelInboundHandlerAdapter {
    private static Logger logger = LoggerFactory.getLogger(ClientExceptionHandler.class);

    @Autowired
    private CommandController commandController;
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        if(cause instanceof InvalidFrameException){
            logger.error(cause.getMessage());
        }else {
            logger.error(cause.getMessage());
            commandController.setConnectFlag(false);
            commandController.startCommandThread();// 重新连接
        }
        super.exceptionCaught(ctx, cause);
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
    }
}

package com.bambi.client.sender;

import com.bambi.bean.msg.ProtoBufMessage;
import com.bambi.client.session.ClientSession;
import com.bambi.currentUtils.CallbackTask;
import com.bambi.currentUtils.CallbackTaskExecutor;
import com.bambi.im.common.bean.dto.UserDTO;
import io.netty.channel.ChannelFuture;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 描述:
 *  <br><b>消息发送基类</b><br>
 *  消息发送是异步自行的；异步相关类{@link CallbackTaskExecutor}<br>
 *  基类提供异步消息发送基本逻辑，如需自定义逻辑，请重写{@link BaseSender#sendMsg(ProtoBufMessage.Message)},或者在外层进行封装
 * <pre>
 * HISTORY
 * ****************************************************************************
 *  ID     DATE          PERSON          REASON
 *  1      2023/2/27 13:04    Bambi        Create
 * ****************************************************************************
 * </pre>
 *
 * @author Bambi
 * @since 1.0
 */
public abstract class BaseSender {
    private static Logger logger = LoggerFactory.getLogger(BaseSender.class);

    private ClientSession clientSession;
    private UserDTO userDTO;

    public boolean isConnected(){
        if(clientSession == null){
            return false;
        }
        return clientSession.isConnected();
    }

    public void sendMsg(ProtoBufMessage.Message message){

        CallbackTaskExecutor.add(new CallbackTask<Boolean>() {
            @Override
            public Boolean execute() throws Exception {
                if(getClientSession() == null){
                    throw new Exception("客户端session 为空");
                }
                if(!isConnected()){
                    logger.error("连接仍未完成");
                    throw new Exception("connect is not ready");
                }

                final Boolean[] isSuccess = {false};
                ChannelFuture channelFuture = getClientSession().writeAndFlush(message);
                channelFuture.addListener(new GenericFutureListener<Future<? super Void>>() {

                    @Override
                    public void operationComplete(Future<? super Void> future) throws Exception {
                        if(future.isSuccess()){
                            isSuccess[0] = true;
                            logger.info("操作成功");
                        }
                    }
                });

                try {
                    channelFuture.sync();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    throw new Exception("error!!!! on Sender");
                }
                return isSuccess[0];
            }

            @Override
            public void onBack(Boolean aBoolean) {
                if(aBoolean){
                    BaseSender.this.sendSuccessed(message);
                }else {
                    BaseSender.this.sendFailed(message);
                }
            }

            @Override
            public void onException(Throwable t) {
                BaseSender.this.sendException(t);
            }
        });

    }

    protected void sendException(Throwable t) {
        logger.error("发送消息存在异常 {}",t.getMessage());
    }

    protected void sendFailed(ProtoBufMessage.Message message) {
        logger.error("消息发送失败");
    }

    protected void sendSuccessed(ProtoBufMessage.Message message) {
        logger.info("消息发送成功");
    }


    public ClientSession getClientSession() {
        return clientSession;
    }

    public void setClientSession(ClientSession clientSession) {
        this.clientSession = clientSession;
    }

    public UserDTO getUserDTO() {
        return userDTO;
    }

    public void setUserDTO(UserDTO userDTO) {
        this.userDTO = userDTO;
    }
}

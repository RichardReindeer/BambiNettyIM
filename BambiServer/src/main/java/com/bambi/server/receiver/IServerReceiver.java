package com.bambi.server.receiver;

import com.bambi.bean.msg.ProtoBufMessage;
import com.bambi.server.session.impl.LocalSession;

/**
 * 描述：<br>
 *  <b>服务器端信息接收处理任务统一接口</b><br>
 *  每个消息包都应当带有自己的消息类型{@link com.bambi.bean.msg.ProtoBufMessage.HeadType} , 在服务器端接收时根据对应的headType不同，提供不同的实现类去处理对应逻辑<br>
 *  信息接收后的处理逻辑绝大部分需要所接收到的信息以及session，用来建立通道和user之间的关系 <br>
 *  使用时则实现{@link IServerReceiver#action(LocalSession, ProtoBufMessage.Message)} 将接收到的参数传入其中进行逻辑处理<br>
 *  返回值为内部逻辑的执行结果成功与否，结合guava的异步回调使用{@link com.bambi.currentUtils.CallbackTaskExecutor}
 * <pre>
 * HISTORY
 * ****************************************************************************
 *  ID     DATE          PERSON          REASON
 *  1      2023/2/28 19:42    Bambi        Create
 * ****************************************************************************
 * </pre>
 *
 * @author Bambi
 * @since 1.0
 */
public interface IServerReceiver {
    ProtoBufMessage.HeadType getHeadType();
    boolean action(LocalSession session, ProtoBufMessage.Message message);
}
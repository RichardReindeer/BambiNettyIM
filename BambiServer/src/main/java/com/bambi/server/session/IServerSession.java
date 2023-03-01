package com.bambi.server.session;

import com.bambi.server.session.impl.LocalSession;
import com.bambi.server.session.impl.RemoteSession;

/**
 * 描述：
 *      <br><b>服务器端Session公用接口</b><br>
 *      分布式服务需要不止一个服务器节点，多个节点之间的Session通信以及管理则需要做出划分<br>
 *      本地Session{@link LocalSession} 以及远程Session{@link RemoteSession} 都需要拥有其基本逻辑<br>
 *      即关联用户与channel，并提供基本的验证和获取方法<br>
 *      <b>Important</b><br>
 *      实现类需要重写{@link IServerSession#writeAndFlush(Object)} 函数，负责处理通道写入逻辑
 *
 * <pre>
 * HISTORY
 * ****************************************************************************
 *  ID     DATE          PERSON          REASON
 *  1      2023/3/1 16:09    Bambi        Create
 * ****************************************************************************
 * </pre>
 *
 * @author Bambi
 * @since 1.0
 */
public interface IServerSession {

    void writeAndFlush(Object protobufMsg);

    String getSessionID();

    /**
     * 验证用户登录是否合法
     * @return
     */
    boolean isValid();

    /**
     * 获取用户id
     * @return
     */
    String getUserID();
}

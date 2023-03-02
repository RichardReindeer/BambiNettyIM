package com.bambi.server.receiver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 描述：
 *      <br><b>接收信息验证任务抽象类</b><br>
 *      一般来说，需要对接收到的信息进行逻辑验证的类都应实现该抽象类<br>
 *      TODO 内部可以进行基础的鉴权逻辑实现，对应的不同子类也可以重写并实现自己的验证逻辑<br>
 * <pre>
 * HISTORY
 * ****************************************************************************
 *  ID     DATE          PERSON          REASON
 *  1      2023/2/28 19:50    Bambi        Create
 * ****************************************************************************
 * </pre>
 *
 * @author Bambi
 * @since 1.0
 */
public abstract class AbstractVerifier implements IServerReceiver{
    private static Logger logger = LoggerFactory.getLogger(AbstractVerifier.class);
}

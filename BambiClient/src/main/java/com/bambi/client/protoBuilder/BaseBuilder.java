package com.bambi.client.protoBuilder;

import com.bambi.bean.msg.ProtoBufMessage;
import com.bambi.client.session.ClientSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 描述：
 *      protoBuf消息构建器基类<br>
 *      提供默认构造实现<br>
 *
 * <pre>
 * HISTORY
 * ****************************************************************************
 *  ID     DATE          PERSON          REASON
 *  1      2023/2/27 10:51    Bambi        Create
 * ****************************************************************************
 * </pre>
 *
 * @author Bambi
 * @since 1.0
 */
public abstract class BaseBuilder {
    private static Logger logger = LoggerFactory.getLogger(BaseBuilder.class);
    protected ProtoBufMessage.HeadType type;
    private long seqId;
    private ClientSession session;

    public BaseBuilder(ProtoBufMessage.HeadType type, ClientSession session)
    {
        this.type = type;
        this.session = session;
    }

    /**
     * 构建消息 基础部分
     */
    public ProtoBufMessage.Message buildCommon(long seqId)
    {
        this.seqId = seqId;

        ProtoBufMessage.Message.Builder mb =
                ProtoBufMessage.Message
                        .newBuilder()
                        .setType(type)
                        .setSessionId(session.getSessionID())
                        .setSequence(seqId);
        return mb.buildPartial();
    }
}

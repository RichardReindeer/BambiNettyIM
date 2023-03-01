package com.bambi.server.session.impl;

import com.bambi.bean.entity.BambiNode;
import com.bambi.server.redis.dao.SessionRedisDao;
import com.bambi.server.rpc.InternalSender;
import com.bambi.server.rpc.WorkerRouter;
import com.bambi.server.session.IServerSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 描述：
 * <br><b>远程会话Session逻辑实现类</b>
 * <pre>
 * HISTORY
 * ****************************************************************************
 *  ID     DATE          PERSON          REASON
 *  1      2023/2/26 9:42    Bambi        Create
 * ****************************************************************************
 * </pre>
 *
 * @author Bambi
 * @since 1.0
 */
public class RemoteSession implements IServerSession {
    private static Logger logger = LoggerFactory.getLogger(RemoteSession.class);
    private SessionRedisDao sessionRedisDao;
    private boolean valid = true;


    public RemoteSession(SessionRedisDao sessionRedisDao, boolean valid) {
        this.sessionRedisDao = sessionRedisDao;
        this.valid = valid;
    }

    public RemoteSession(SessionRedisDao sessionRedisDao) {
        this.sessionRedisDao = sessionRedisDao;
    }

    @Override
    public void writeAndFlush(Object protobufMsg) {
        BambiNode node = sessionRedisDao.getNode();
        long id = node.getId();
        InternalSender InternalSender = WorkerRouter.getInstance().getInternalSender(id);
        if(InternalSender!=null){
            InternalSender.writeAndFlush(protobufMsg);
        }
    }

    @Override
    public String getSessionID() {
        return sessionRedisDao.getSessionID();
    }

    @Override
    public boolean isValid() {
        return valid;
    }

    @Override
    public String getUserID() {
        return sessionRedisDao.getUserId();
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }
}

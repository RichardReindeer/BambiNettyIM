package com.bambi.server.redis.dao;

import com.bambi.bean.entity.BambiNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;

/**
 * 描述：
 *<br><b>用户Session信息Dao</b><br>
 *    内部存储客户端与服务器之间的关系，带有服务器节点基础信息封装类{@link BambiNode} , 以及对应的userID和sessionID
 * <pre>
 * HISTORY
 * ****************************************************************************
 *  ID     DATE          PERSON          REASON
 *  1      2023/3/1 19:52    Bambi        Create
 * ****************************************************************************
 * </pre>
 *
 * @author Bambi
 * @since 1.0
 */
public class SessionRedisDao implements Serializable {
    private static Logger logger = LoggerFactory.getLogger(SessionRedisDao.class);


    private String userId;
    private String sessionID;
    private BambiNode node;

    public SessionRedisDao(String userId, String sessionID, BambiNode node) {
        this.userId = userId;
        this.sessionID = sessionID;
        this.node = node;
    }

    public SessionRedisDao() {
        userId = "";
        sessionID = "";
        node = new BambiNode("notFound",000);
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getSessionID() {
        return sessionID;
    }

    public void setSessionID(String sessionID) {
        this.sessionID = sessionID;
    }

    public BambiNode getNode() {
        return node;
    }

    public void setNode(BambiNode node) {
        this.node = node;
    }

    @Override
    public String toString() {
        return "SessionRedisDao{" +
                "userId='" + userId + '\'' +
                ", sessionID='" + sessionID + '\'' +
                ", node=" + node +
                '}';
    }
}

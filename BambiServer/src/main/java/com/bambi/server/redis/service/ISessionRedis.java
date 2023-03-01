package com.bambi.server.redis.service;
import com.bambi.server.redis.dao.SessionRedisDao;

/**
 * 描述：
 *<br><b>Session缓存持久化逻辑接口</b>
 * <pre>
 * HISTORY
 * ****************************************************************************
 *  ID     DATE          PERSON          REASON
 *  1      2023/2/26 7:10    Bambi        Create
 * ****************************************************************************
 * </pre>
 *
 * @author Bambi
 * @since 1.0
 */
public interface ISessionRedis {

    /**
     * 保存session到redis缓存中
     * @param sessionRedisDao
     */
    void saveSession(SessionRedisDao sessionRedisDao);

    /**
     * 根据sessionid获取对应session
     * @param sessionId
     * @return
     */
    SessionRedisDao getSessionById(String sessionId);

    /**
     * 移除session
     * @param sessionId
     */
    void removeSessionById(String sessionId);
}

package com.bambi.server.redis.service;


import com.bambi.server.redis.dao.SessionRedisDao;
import com.bambi.server.redis.dao.UserRedisDao;

/**
 * 描述：
 * <br><b>用户数据缓存持久化接口</b>
 * <pre>
 * HISTORY
 * ****************************************************************************
 *  ID     DATE          PERSON          REASON
 *  1      2023/2/26 7:11    Bambi        Create
 * ****************************************************************************
 * </pre>
 *
 * @author Bambi
 * @since 1.0
 */
public interface IUserRedis {

    /**
     * 向redis中存储user
     *
     * @param userRedisDao
     */
    void saveUser(UserRedisDao userRedisDao);

    /**
     * 从redis中根据userid获取对应user
     *
     * @param userId
     * @return
     */
    UserRedisDao getUserByUserId(String userId);

    /**
     * 向user中添加session
     *
     * @param userId
     * @param sessionRedisDao
     */
    void addSession(String userId, SessionRedisDao sessionRedisDao);

    /**
     * 从user中移除session
     *
     * @param userId
     * @param sessionId
     */
    void removeSession(String userId, String sessionId);
}

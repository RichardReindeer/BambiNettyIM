package com.bambi.server.redis.dao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 描述：
 * <br><b>User缓存存储逻辑实现类</b><br>
 *    在架构设计上，我们使用redis来存储用户的session信息<br>
 *    因为用户与session是一对多的关系，所以在设计上将user与session分开存储<br>
 *    user中存储着一个承载着当前登录的所有设备的session集合<br>
 *    Session类请跳转{@link SessionRedisDao}
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
public class UserRedisDao implements Serializable {
    private static Logger logger = LoggerFactory.getLogger(UserRedisDao.class);

    private String userId;

    // key : sessionID , value: Session
    private Map<String,SessionRedisDao> userSessionMap = new LinkedHashMap<>(10);

    public UserRedisDao(String userId) {
        this.userId = userId;
    }

    public void addSession(SessionRedisDao sessionRedisDao){
        userSessionMap.put(sessionRedisDao.getSessionID(),sessionRedisDao);
    }

    public void removeSession(String sessionId){
        userSessionMap.remove(sessionId);
    }


    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public Map<String, SessionRedisDao> getUserSessionMap() {
        return userSessionMap;
    }

    public void setUserSessionMap(Map<String, SessionRedisDao> userSessionMap) {
        this.userSessionMap = userSessionMap;
    }

    @Override
    public String toString() {
        return "UserRedisDao{" +
                "userId='" + userId + '\'' +
                ", userSessionMap=" + userSessionMap +
                '}';
    }
}

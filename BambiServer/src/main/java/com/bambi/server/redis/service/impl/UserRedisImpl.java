package com.bambi.server.redis.service.impl;

import com.bambi.server.redis.dao.SessionRedisDao;
import com.bambi.server.redis.dao.UserRedisDao;
import com.bambi.server.redis.service.IUserRedis;
import com.bambi.utils.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.concurrent.TimeUnit;

/**
 * 描述：
 *      <br><b>用户 缓存持久化逻辑实现类</b><br>
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
@Service("userRedisImpl")
public class UserRedisImpl implements IUserRedis {
    private static Logger logger = LoggerFactory.getLogger(UserRedisImpl.class);

    private static final String REDIS_PREFIX = "UserRedis:uid:";

    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    private static final long CACHE_TIMEOUT = 60 * 4; // 保存时间


    @Override
    public void saveUser(UserRedisDao userRedisDao) {
        logger.info("saveUser is starting !!! UserRedisDao {}", userRedisDao.toString());

        String userKey = REDIS_PREFIX + userRedisDao.getUserId();
        String value = JsonUtil.pojoToJsonByGson(userRedisDao);
        stringRedisTemplate.opsForValue().set(userKey, value, CACHE_TIMEOUT, TimeUnit.MINUTES);
    }

    @Override
    public UserRedisDao getUserByUserId(String userId) {
        String key = REDIS_PREFIX+userId;
        String value = stringRedisTemplate.opsForValue().get(key);
        if(!StringUtils.isEmpty(value)){
            return JsonUtil.jsonToPojoByGson(value,UserRedisDao.class);
        }
        return null;
    }


    @Override
    public void addSession(String userId, SessionRedisDao sessionRedisDao) {
        UserRedisDao userRedisDao = new UserRedisDao(userId);
        if(userRedisDao==null){
            userRedisDao= new UserRedisDao(userId);
        }

        // 将用户保存到
        userRedisDao.addSession(sessionRedisDao);
        saveUser(userRedisDao);
    }

    @Override
    public void removeSession(String userId, String sessionId) {
        UserRedisDao userByUserId = getUserByUserId(userId);
        if(userByUserId == null){
            UserRedisDao userRedisDao = new UserRedisDao(userId);
        }
        userByUserId.removeSession(sessionId);
        saveUser(userByUserId);
    }
}

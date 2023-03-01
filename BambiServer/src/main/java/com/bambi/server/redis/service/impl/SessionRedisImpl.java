package com.bambi.server.redis.service.impl;

import com.bambi.server.redis.dao.SessionRedisDao;
import com.bambi.server.redis.service.ISessionRedis;
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
 * <br><b>SESSION redis持久化逻辑实现</b><br>
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
@Service("sessionRedisImpl")
public class SessionRedisImpl implements ISessionRedis {
    private static Logger logger = LoggerFactory.getLogger(SessionRedisImpl.class);

    private static final String REDIS_PREFIX = "SessionRedis:id:";
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    private static final long SESSION_TIMEOUT = 60*4; // 四小时后断链
    @Override
    public void saveSession(SessionRedisDao sessionRedisDao) {
        logger.debug("saveSession is starting !!! {}",sessionRedisDao.toString());

        String redisKey = REDIS_PREFIX+sessionRedisDao.getSessionID();
        String value = JsonUtil.pojoToJsonByGson(sessionRedisDao);
        stringRedisTemplate.opsForValue().set(redisKey,value,SESSION_TIMEOUT, TimeUnit.MINUTES);
    }

    @Override
    public SessionRedisDao getSessionById(String sessionId) {
        logger.debug("getSessionById is starting !!! sessionID --> {}",sessionId);
        String key = REDIS_PREFIX+sessionId;
        String value = stringRedisTemplate.opsForValue().get(key);
        if(!StringUtils.isEmpty(value)){
            return JsonUtil.jsonToPojoByGson(value,SessionRedisDao.class);
        }
        return null;
    }

    @Override
    public void removeSessionById(String sessionId) {
        String key = REDIS_PREFIX+sessionId;
        stringRedisTemplate.delete(key);
    }
}

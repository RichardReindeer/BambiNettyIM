package com.bambi.server.session;

import com.bambi.bean.entity.BambiNode;
import com.bambi.im.common.bean.notification.Notification;
import com.bambi.server.redis.dao.SessionRedisDao;
import com.bambi.server.redis.dao.UserRedisDao;
import com.bambi.server.redis.service.ISessionRedis;
import com.bambi.server.redis.service.IUserRedis;
import com.bambi.server.rpc.BambiWorker;
import com.bambi.server.rpc.WorkerRouter;
import com.bambi.server.session.impl.LocalSession;
import com.bambi.server.session.impl.RemoteSession;
import com.bambi.utils.JsonUtil;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 描述：
 *
 * <pre>
 * HISTORY
 * ****************************************************************************
 *  ID     DATE          PERSON          REASON
 *  1      2023/3/1 16:08    Bambi        Create
 * ****************************************************************************
 * </pre>
 *
 * @author Bambi
 * @since 1.0
 */
@Service("sessionManager")
public class SessionManager {
    private static Logger logger = LoggerFactory.getLogger(SessionManager.class);

    @Autowired
    private IUserRedis userRedis;
    @Autowired
    private ISessionRedis sessionRedis;

    /**
     * 存储session列表，包含本地session和远程session
     */
    private ConcurrentHashMap<String, IServerSession> sessionMap = new ConcurrentHashMap<>();

    private static SessionManager singleTone = null;

    private SessionManager() {
    }

    public static SessionManager getInstance() {
        return singleTone;
    }

    public static void setSessionManager(SessionManager sessionManager) {
        SessionManager.singleTone = sessionManager;
    }

    /**
     * 在登录成功之后，增加对应的session对象
     */
    public void addSession(LocalSession session) {
        // 1. 保存到本地的session枪弹中
        String sessionID = session.getSessionID();
        sessionMap.put(sessionID, session);
        // 2. 保存到redis缓存
        String userId = session.getUserDTO().getUserId();
        BambiNode localNodeInfo = BambiWorker.getInstance().getLocalNodeInfo();
        SessionRedisDao sessionRedisDao = new SessionRedisDao(userId, sessionID, localNodeInfo);
        sessionRedis.saveSession(sessionRedisDao);
        // 3. 增加用户缓存
        userRedis.addSession(userId, sessionRedisDao);
        // 4.TODO 用户数统计

        BambiWorker.getInstance().incrBalance();// 增加负载
        notifyNodeOnLine(session);
    }

    /**
     * 根据userid找到对应的session<br>
     * 如果本地没有，则创建远程session，加入集合
     *
     * @param userId
     * @return
     */
    public ArrayList<IServerSession> getSessionById(String userId) {
        UserRedisDao userByUserId = userRedis.getUserByUserId(userId);
        if (userByUserId == null) {
            logger.error("userid: {} ,不存在，用户可能没有登录", userId);
            return null;
        }
        Map<String, SessionRedisDao> userSessionMap = userByUserId.getUserSessionMap();
        if (userSessionMap.isEmpty() || userSessionMap == null) {
            logger.info("当前用户已经下线 userId : {}", userByUserId.getUserId());
            return null;
        }
        ArrayList<IServerSession> serverSessions = new ArrayList<>();
        // 查找当前session信息
        userSessionMap.values().stream().forEach(sessionRedisDao -> {
            String sessionID = sessionRedisDao.getSessionID();
            if (!sessionMap.containsKey(sessionID)) {
                // 在远程session中查找
                RemoteSession remoteSession = new RemoteSession(sessionRedisDao);
                sessionMap.put(sessionID, remoteSession);
            }
            serverSessions.add(sessionMap.get(sessionID));
        });

        return serverSessions;
    }

    /**
     * 删除session
     * @param sessionId
     */
    public void removeSession(String sessionId) {
        if(sessionMap.containsKey(sessionId)){
            IServerSession serverSession = sessionMap.get(sessionId);
            // TODO 用户数减少

            String userID = serverSession.getUserID();
            BambiWorker.getInstance().descBalance();// 降低负载
            // 删除redis中的用户session
            userRedis.removeSession(userID,sessionId);
            sessionRedis.removeSessionById(sessionId);
            // 在本地会话中删除
            sessionMap.remove(sessionId);
        }
    }

    /**
     * 远程用户下线时，删除远程session
     */
    public void removeRemoteSession(String sessionID) {
        if(!sessionMap.containsKey(sessionID)){
            return;
        }
        sessionMap.remove(sessionID);
    }


    /**
     * 关闭当前连接<br>
     * 根据ChannelHandlerContext获取对应的localSession对象<br>
     * 并调用其close函数，并广播下线通知
     *
     * @param context
     */
    public void closeSession(ChannelHandlerContext context) {
        LocalSession localSession = context.channel().attr(LocalSession.SESSION_KEY).get();
        if (localSession == null || localSession.isValid()) {
            logger.error(" local session 为空或者非法");
            return;
        }
        localSession.close();
        this.removeSession(localSession.getSessionID());
        notifyNodeOffLine(localSession);
    }

    /**
     * 通知其他服务器用户下线
     * @param localSession
     */
    public void notifyNodeOffLine(LocalSession localSession) {
        if (localSession == null || localSession.isValid()) {
            logger.error(" local session 为空或者非法");
            return;
        }
        int sessionOffline = Notification.SESSION_OFFLINE;
        Notification<Notification.ContentWrapper> contentWrapperNotification = Notification.wrapContent(localSession.getSessionID());
        contentWrapperNotification.setType(sessionOffline);
        WorkerRouter.getInstance().sendNotification(JsonUtil.pojoToJsonByGson(contentWrapperNotification));
    }

    /**
     * 通知其他服务器用户上线
     * @param session
     */
    public void notifyNodeOnLine(LocalSession session) {
        int sessionOnline = Notification.SESSION_ONLINE;
        Notification<Notification.ContentWrapper> contentWrapperNotification = Notification.wrapContent(session.getSessionID());
        contentWrapperNotification.setType(sessionOnline);
        WorkerRouter.getInstance().sendNotification(JsonUtil.pojoToJsonByGson(contentWrapperNotification));
    }

}

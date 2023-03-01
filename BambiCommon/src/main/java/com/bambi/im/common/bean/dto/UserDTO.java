package com.bambi.im.common.bean.dto;

import com.bambi.bean.msg.ProtoBufMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 描述：
 *      <br><b>{@link com.bambi.bean.msg.ProtoBufMessage} 中用户信息的封装</b><br>
 *      在系统框架中，登录，聊天等可以说绝大部分client 与 server之间的传输都需要用到用户数据对象<br>
 *      内部含有平台类型枚举{@link UserDTO#platform} 判断用户当前登录设备所使用的平台<br><br>
 *      <b>对于{@link UserDTO#fromMsg(ProtoBufMessage.LoginRequest)}</b><br>
 *      根据登录数据信息封装用户画像，用于后续逻辑传输
 * <pre>
 * HISTORY
 * ****************************************************************************
 *  ID     DATE          PERSON          REASON
 *  1      2023/3/1 12:05    Bambi        Create
 * ****************************************************************************
 * </pre>
 *
 * @author Bambi
 * @since 1.0
 */
public class UserDTO {
    private static Logger logger = LoggerFactory.getLogger(UserDTO.class);

    String userId;
    String userName;
    String devId;
    String token;
    String nickName = "nickName";
    PLATTYPE platform = PLATTYPE.WINDOWS;

    // windows,mac,android, ios, web , other
    public enum PLATTYPE
    {
        WINDOWS, MAC, ANDROID, IOS, WEB, OTHER;
    }

    private String sessionId;


    public void setPlatform(int platform)
    {
        PLATTYPE[] values = PLATTYPE.values();
        for (int i = 0; i < values.length; i++)
        {
            if (values[i].ordinal() == platform)
            {
                this.platform = values[i];
            }
        }

    }


    @Override
    public String toString()
    {
        return "User{" +
                "uid='" + userId + '\'' +
                ", devId='" + devId + '\'' +
                ", token='" + token + '\'' +
                ", nickName='" + nickName + '\'' +
                ", platform=" + platform +
                '}';
    }

    public static UserDTO fromMsg(ProtoBufMessage.LoginRequest info)
    {
        UserDTO user = new UserDTO();
        user.userId = new String(info.getUid());
        user.devId = new String(info.getDeviceId());
        user.token = new String(info.getToken());
        user.setPlatform(info.getPlatform());
        logger.info("登录中: {}", user.toString());
        return user;
    }

    // getter & setter

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getDevId() {
        return devId;
    }

    public void setDevId(String devId) {
        this.devId = devId;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getNickName() {
        return nickName;
    }

    public void setNickName(String nickName) {
        this.nickName = nickName;
    }

    public PLATTYPE getPlatform() {
        return platform;
    }

    public void setPlatform(PLATTYPE platform) {
        this.platform = platform;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
}

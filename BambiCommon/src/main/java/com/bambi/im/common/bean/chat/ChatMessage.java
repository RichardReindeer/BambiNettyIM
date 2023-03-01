package com.bambi.im.common.bean.chat;

import com.bambi.bean.msg.ProtoBufMessage;
import com.bambi.im.common.bean.dto.UserDTO;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 描述：
 *  <br><b>对数据包{@link ProtoBufMessage} 中的聊天数据信息进行对应封装</b><br>
 *  对多种聊天信息类型创建对应枚举类，用于后期系统架构中的逻辑判断<br>
 *  内部含有对{@link UserDTO} 数据的封装<br> 如果需要更改此类的用户信息装填，请同步修改用户数据类
 *
 * <pre>
 * HISTORY
 * ****************************************************************************
 *  ID     DATE          PERSON          REASON
 *  1      2023/3/1 12:17    Bambi        Create
 * ****************************************************************************
 * </pre>
 *
 * @author Bambi
 * @since 1.0
 */
public class ChatMessage {
    private static Logger logger = LoggerFactory.getLogger(ChatMessage.class);
    //消息类型  1：纯文本  2：音频 3：视频 4：地理位置 5：其他
    public enum MSGTYPE
    {
        TEXT,
        AUDIO,
        VIDEO,
        POS,
        OTHER;
    }

    public ChatMessage(UserDTO user)
    {
        if (null == user)
        {
            return;
        }
        this.user = user;
        this.setTime(System.currentTimeMillis());
        this.setFrom(user.getUserId());
        this.setFromNick(user.getNickName());

    }

    private UserDTO user;

    private long msgId;
    private String from;
    private String to;
    private long time;
    private MSGTYPE msgType;
    private String content;
    private String url;          //多媒体地址
    private String property;     //附加属性
    private String fromNick;     //发送者昵称
    private String json;         //附加的json串


    public void fillMsg(ProtoBufMessage.MessageRequest.Builder cb)
    {
        if (msgId > 0)
        {
            cb.setMsgId(msgId);
        }
        if (StringUtils.isNotEmpty(from))
        {
            cb.setFrom(from);
        }
        if (StringUtils.isNotEmpty(to))
        {
            cb.setTo(to);
        }
        if (time > 0)
        {
            cb.setTime(time);
        }
        if (msgType != null)
        {
            cb.setMsgType(msgType.ordinal());
        }
        if (StringUtils.isNotEmpty(content))
        {
            cb.setContent(content);
        }
        if (StringUtils.isNotEmpty(url))
        {
            cb.setUrl(url);
        }
        if (StringUtils.isNotEmpty(property))
        {
            cb.setProperty(property);
        }
        if (StringUtils.isNotEmpty(fromNick))
        {
            cb.setFromNick(fromNick);
        }

        if (StringUtils.isNotEmpty(json))
        {
            cb.setJson(json);
        }
    }

    public UserDTO getUser() {
        return user;
    }

    public void setUser(UserDTO user) {
        this.user = user;
    }

    public long getMsgId() {
        return msgId;
    }

    public void setMsgId(long msgId) {
        this.msgId = msgId;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public MSGTYPE getMsgType() {
        return msgType;
    }

    public void setMsgType(MSGTYPE msgType) {
        this.msgType = msgType;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getProperty() {
        return property;
    }

    public void setProperty(String property) {
        this.property = property;
    }

    public String getFromNick() {
        return fromNick;
    }

    public void setFromNick(String fromNick) {
        this.fromNick = fromNick;
    }

    public String getJson() {
        return json;
    }

    public void setJson(String json) {
        this.json = json;
    }
}

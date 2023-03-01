package com.bambi.im.common.bean.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 描述：
 *      <br><b>对通知数据信息的封装</b><br>
 *      如果需要更改或者增添新的数据交互逻辑，请在通知类内部添加通知类型type<br><br>
 *      {@link Notification#wrapContent(String)} 是一个·
 *
 * <pre>
 * HISTORY
 * ****************************************************************************
 *  ID     DATE          PERSON          REASON
 *  1      2023/3/1 12:26    Bambi        Create
 * ****************************************************************************
 * </pre>
 *
 * @author Bambi
 * @since 1.0
 */
public class Notification<T> {
    private static Logger logger = LoggerFactory.getLogger(Notification.class);

    public static final int SESSION_ONLINE = 10001; // 用户上线
    public static final int SESSION_OFFLINE= 20001; // 用户下线
    public static final int CONNECT_FINISHED = 30001; // 服务器连接成功

    private int type;
    private T data;

    public Notification() {
    }

    public Notification(T data){
        this.data = data;
    }


    /**
     * 接收通知信息的内容
     * @param content
     * @return
     */
    public static Notification<ContentWrapper> wrapContent(String content){
        logger.debug("wrapContent is starting !!!");
        ContentWrapper contentWrapper = new ContentWrapper();
        contentWrapper.setContent(content);
        return new Notification<ContentWrapper>(contentWrapper);
    }

    /**
     * 获取消息正文
     * @return
     */
    public String getWrapperContent(){
        if(data instanceof ContentWrapper){
            return ((ContentWrapper) data).getContent();
        }
        return null;
    }

    // 2023/02/26 静态内部类不是public无法被外部访问，BUGFIXED
    public static class ContentWrapper{
        String content;

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }
    }







    // getter setter
    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    @Override
    public String toString() {
        String toS = "Notification ( type = "+ getType() +", data = ("+data.toString()+" )";
        return toS;
    }
}

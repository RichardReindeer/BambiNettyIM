package com.bambi.client.command.impl;

import com.bambi.client.command.IBaseCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Scanner;

/**
 * 描述：
 *      <br><b>聊天指令类</b><br>
 *      实现聊天需要知道对方的userid， 以及正确输入聊天内容<br>
 *
 * <pre>
 * HISTORY
 * ****************************************************************************
 *  ID     DATE          PERSON          REASON
 *  1      2023/2/27 5:24    Bambi        Create
 * ****************************************************************************
 * </pre>
 *
 * @author Bambi
 * @since 1.0
 */
@Service("chatCommand")
public class ChatCommand implements IBaseCommand {
    private static Logger logger = LoggerFactory.getLogger(ChatCommand.class);
    public static final String KEY = "2";

    private String userId;
    private String chatMessage;

    @Override
    public void doAction(Scanner scanner) {
        logger.debug("ChatCommand is starting Action");
        logger.info("聊天格式 ： 内容@目标用户");

        while (true){
            try {
                String next = scanner.next();
                String[] split = next.split("@");
                userId = split[0];
                chatMessage = split[1];
                break;
            } catch (Exception e) {
                e.printStackTrace();
                logger.error("信息输入有误，请重新输入");
                logger.error("聊天格式 ： 内容@目标用户");
            }
        }

        logger.debug("当前聊天内容 : {},目标用户 {}",chatMessage,userId);
        logger.info("消息正在发送");
    }

    @Override
    public String getKey() {
        return KEY;
    }

    @Override
    public String getTips() {
        return "聊天 CHAT";
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getChatMessage() {
        return chatMessage;
    }

    public void setChatMessage(String chatMessage) {
        this.chatMessage = chatMessage;
    }
}

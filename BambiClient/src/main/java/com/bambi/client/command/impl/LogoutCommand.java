package com.bambi.client.command.impl;

import com.bambi.client.command.IBaseCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Scanner;

/**
 * 描述：
 *      <br><b>登出指令类</b><br>
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
@Service("logoutCommand")
public class LogoutCommand implements IBaseCommand {
    private static Logger logger = LoggerFactory.getLogger(LogoutCommand.class);

    public static final String KEY = "3";
    private boolean logOutFlag = false;
    @Override
    public void doAction(Scanner scanner) {
        logger.debug("logoutCommand is starting action");
        logger.info("确定要登出吗? y 表示确认");
        String next = scanner.next();
        if(next.equals("y")){
            logOutFlag = true;
            logger.info("正在进行登出逻辑");
        }
    }

    @Override
    public String getKey() {
        return KEY;
    }

    @Override
    public String getTips() {
        return "登出 LOGOUT";
    }

    public boolean isLogOutFlag() {
        return logOutFlag;
    }

    public void setLogOutFlag(boolean logOutFlag) {
        this.logOutFlag = logOutFlag;
    }
}

package com.bambi.client.command.impl;

import com.bambi.client.command.IBaseCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Scanner;

/**
 * 描述：
 *      <br><b>登录指令类</b><br>
 *      检验用户输入的登录信息是否正确<br>
 *      主要检测输入格式
 * <pre>
 * HISTORY
 * ****************************************************************************
 *  ID     DATE          PERSON          REASON
 *  1      2023/2/27 5:18    Bambi        Create
 * ****************************************************************************
 * </pre>
 *
 * @author Bambi
 * @since 1.0
 */
@Service("loginCommand")
public class LoginCommand implements IBaseCommand {
    private static Logger logger = LoggerFactory.getLogger(LoginCommand.class);

    public static final String KEY = "1";

    private String username = null;
    private String password = null;

    public LoginCommand() {
    }

    public LoginCommand(String username, String password) {
        this.username = username;
        this.password = password;
    }

    @Override
    public void doAction(Scanner scanner) {
        logger.info("请输入对应的登录信息 格式为 用户:密码");
        String[] split = null;
        while (true) {
            String next = scanner.next();
            split = next.split(":");
            if (split.length != 2) {
                System.err.println("请以正确格式输入用户名和密码");
            } else {
                break;
            }
        }
        username = split[0];
        password = split[1];

        logger.info("输入格式正确，当前用户名 {}， 当前密码 {}", username, password);
    }

    @Override
    public String getKey() {
        return KEY;
    }

    @Override
    public String getTips() {
        return "登录 LOGIN";
    }

    @Override
    public boolean equals(Object obj) {
        return super.equals(obj);
    }

    @Override
    public String toString() {
        return super.toString();
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}

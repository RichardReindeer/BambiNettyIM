package com.bambi.client.command.impl;

import com.bambi.client.command.IBaseCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Scanner;

/**
 * 描述：
 *      <br><b>客户端指令菜单类</b><br>
 *      用于向用户展示指令菜单
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
@Service("commandMenu")
public class CommandMenu implements IBaseCommand {
    private static Logger logger = LoggerFactory.getLogger(CommandMenu.class);
    private static final String KEY = "0";
    private String allCommandMenu;
    private String commandInput;

    public CommandMenu(String allCommandMenu, String commandInput) {
        this.allCommandMenu = allCommandMenu;
        this.commandInput = commandInput;
    }

    public CommandMenu() {
    }

    @Override
    public void doAction(Scanner scanner) {
        System.err.println("请输入对应指令");
        System.err.println(allCommandMenu);
        commandInput = scanner.next();
    }

    @Override
    public String getKey() {
        return KEY;
    }

    @Override
    public String getTips() {
        return "菜单 MENU";
    }

    public String getAllCommandMenu() {
        return allCommandMenu;
    }

    public void setAllCommandMenu(String allCommandMenu) {
        this.allCommandMenu = allCommandMenu;
    }

    public String getCommandInput() {
        return commandInput;
    }

    public void setCommandInput(String commandInput) {
        this.commandInput = commandInput;
    }
}

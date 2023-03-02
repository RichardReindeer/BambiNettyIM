package com.bambi.client.command;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Scanner;

/**
 * 描述：
 * <br><b>命令行公共接口</b><br>
 *      接收命令行指令以及获取对应命令的key和提示
 * <pre>
 * HISTORY
 * ****************************************************************************
 *  ID     DATE          PERSON          REASON
 *  1      2023/2/27 5:17    Bambi        Create
 * ****************************************************************************
 * </pre>
 *
 * @author Bambi
 * @since 1.0
 */
public interface IBaseCommand {

    void doAction(Scanner scanner);

    String getKey();

    String getTips();
}

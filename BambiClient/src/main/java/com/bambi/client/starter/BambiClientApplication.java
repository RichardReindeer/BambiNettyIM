package com.bambi.client.starter;

import com.bambi.client.controller.CommandController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * 描述：
 *      <br><b>客户端登录启动类</b><br>
 * <pre>
 * HISTORY
 * ****************************************************************************
 *  ID     DATE          PERSON          REASON
 *  1      2023/3/2 13:54    Bambi        Create
 * ****************************************************************************
 * </pre>
 *
 * @author Bambi
 * @since 1.0
 */
@Configuration
@ComponentScan("com.bambi.client")
@SpringBootApplication
public class BambiClientApplication {
    private static Logger logger = LoggerFactory.getLogger(BambiClientApplication.class);
    public static void main(String[] args) {
        logger.info("Client is running !!!!!!");
        ConfigurableApplicationContext run = SpringApplication.run(BambiClientApplication.class);
        CommandController commandController = run.getBean(CommandController.class);
        commandController.initCommandMap();

        try {
            commandController.startCommandThread();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

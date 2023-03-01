package com.bambi.server.stater;

import com.bambi.server.session.SessionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * 描述：
 *      <br><b>服务器启动类</b><br>
 *      在启动类中单例模式创建{@link SessionManager} <br>
 *      并调用nettyServer服务器启动逻辑
 * <pre>
 * HISTORY
 * ****************************************************************************
 *  ID     DATE          PERSON          REASON
 *  1      2023/3/1 16:07    Bambi        Create
 * ****************************************************************************
 * </pre>
 *
 * @author Bambi
 * @since 1.0
 */
@Configuration
@ComponentScan("com.bambi.server")
@SpringBootApplication
public class BambiServerApplication {
    private static Logger logger = LoggerFactory.getLogger(BambiServerApplication.class);
    public static void main(String[] args) {
        ApplicationContext context = SpringApplication.run(BambiServerApplication.class, args);

        // 使用context获取注册进spring的单例Bean
        SessionManager sessionManager = context.getBean(SessionManager.class);
        SessionManager.setSessionManager(sessionManager);

        // 启动服务器
        BambiNettyServer bambiNettyServer = context.getBean(BambiNettyServer.class);
        bambiNettyServer.startServer();
    }
}

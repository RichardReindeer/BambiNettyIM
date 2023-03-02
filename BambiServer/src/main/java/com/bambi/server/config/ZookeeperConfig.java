package com.bambi.server.config;

import com.bambi.utils.SpringContextUtil;
import com.bambi.zk.CuratorClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 描述：
 * zk配置类
 * <pre>
 * HISTORY
 * ****************************************************************************
 *  ID     DATE          PERSON          REASON
 *  1      2023/2/26 7:11    Bambi        Create
 * ****************************************************************************
 * </pre>
 *
 * @author Bambi
 * @since 1.0
 */
@Configuration
public class ZookeeperConfig implements ApplicationContextAware {
    private static Logger logger = LoggerFactory.getLogger(ZookeeperConfig.class);

    @Value("${zookeeper.connect.url}")
    private String zkConnect;

    @Value("${zookeeper.connect.SessionTimeout}")
    private String zkSessionTimeout;


    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        SpringContextUtil.setContext(applicationContext);
    }

    /**
     * bean注册
     *
     * @return
     */
    @Bean(name = "curatorClient")
    public CuratorClient curatorClient() {
        return new CuratorClient(zkSessionTimeout, zkConnect);
    }
}

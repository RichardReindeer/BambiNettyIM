package com.bambi.ezgate.config;

import ch.qos.logback.core.spi.ContextAwareBase;
import com.bambi.ezgate.loadBalance.LoadBalance;
import com.bambi.utils.SpringContextUtil;
import com.bambi.zk.CuratorClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 描述：
 *      <br><b>zk客户端配置类</b><br>
 *      将curatorClient 以及 loadBalance 注入spring中<br>
 *      读取yml中的zk配置
 * <pre>
 * HISTORY
 * ****************************************************************************
 *  ID     DATE          PERSON          REASON
 *  1      2023/3/1 15:26    Bambi        Create
 * ****************************************************************************
 * </pre>
 *
 * @author Bambi
 * @since 1.0
 */
@Configuration
public class ZkClientConfig implements ApplicationContextAware {
    private static Logger logger = LoggerFactory.getLogger(ZkClientConfig.class);

    @Value("${zookeeper.connect.url}")
    private String zkConnect;

    @Value("${zookeeper.connect.SessionTimeout}")
    private String zkSessionTimeout;


    /**
     * @see BeanInitializationException
     */
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException
    {

        SpringContextUtil.setContext(applicationContext);

    }


    @Bean(name = "curatorClient")
    public CuratorClient curatorClient()
    {
        return new CuratorClient(zkSessionTimeout,zkConnect);
    }

    @Bean(name = "loadBalance")
    public LoadBalance loadBalance(CuratorClient curatorZKClient)
    {

        return new LoadBalance( curatorZKClient);
    }

}

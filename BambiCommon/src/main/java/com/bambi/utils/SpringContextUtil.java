package com.bambi.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

/**
 * 描述：
 *      <br><b>SpringContextUtil</b><br>
 *      对ApplicationContext的相关使用操作进行封装
 * <pre>
 * HISTORY
 * ****************************************************************************
 *  ID     DATE          PERSON          REASON
 *  1      2023/3/1 15:20    Bambi        Create
 * ****************************************************************************
 * </pre>
 *
 * @author Bambi
 * @since 1.0
 */
@Component
public class SpringContextUtil {
    private static Logger logger = LoggerFactory.getLogger(SpringContextUtil.class);

    /**
     * 上下文对象实例
     */
    private static ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException
    {
        SpringContextUtil.applicationContext = applicationContext;
    }

    public static void setContext(ApplicationContext applicationContext)
    {
        SpringContextUtil.applicationContext = applicationContext;
    }

    public static ApplicationContext getApplicationContext()
    {
        return applicationContext;
    }

    /**
     * 通过name获取 Bean.
     */
    public static <T> T getBean(String name)
    {
        return (T) applicationContext.getBean(name);
    }

    /**
     * 通过class获取Bean.
     */
    public static <T> T getBean(Class<T> clazz)
    {
        if (null == applicationContext)
        {
            return null;
        }
        return applicationContext.getBean(clazz);
    }

    /**
     * 通过name,以及Clazz返回指定的Bean
     */
    public static <T> T getBean(String name, Class<T> clazz)
    {
        return applicationContext.getBean(name, clazz);
    }


    /**
     * 获取配置的node ip
     */
    public static String getLocalIP()
    {
        return applicationContext.getEnvironment()
                .getProperty("zookeeper.distribute.local-node-host", "127.0.0.1");
    }

}

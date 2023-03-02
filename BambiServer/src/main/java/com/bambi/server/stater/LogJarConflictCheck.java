package com.bambi.server.stater;

import com.bambi.utils.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.URL;
import java.util.Set;

/**
 * 描述：
 *
 * <pre>
 * HISTORY
 * ****************************************************************************
 *  ID     DATE          PERSON          REASON
 *  1      2023/3/2 11:48    Bambi        Create
 * ****************************************************************************
 * </pre>
 *
 * @author Bambi
 * @since 1.0
 */
@Service
public class LogJarConflictCheck implements BeanFactoryPostProcessor {
    private static Logger logger = LoggerFactory.getLogger(LogJarConflictCheck.class);
    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory configurableListableBeanFactory) throws BeansException {
        try {
            Class<LoggerFactory> loggerFactoryClazz = LoggerFactory.class;
            Constructor<LoggerFactory> constructor = loggerFactoryClazz.getDeclaredConstructor();
            constructor.setAccessible(true);
            LoggerFactory instance = constructor.newInstance();
            Method method = loggerFactoryClazz.getDeclaredMethod("findPossibleStaticLoggerBinderPathSet");
            // 强制进入
            method.setAccessible(true);
            Set<URL> staticLoggerBinderPathSet = (Set<URL>)method.invoke(instance);
            if (CollectionUtils.isEmpty(staticLoggerBinderPathSet)) {
                handleLogJarConflict(staticLoggerBinderPathSet, "Class path is Empty.添加对应日志jar包");
            }
            if (staticLoggerBinderPathSet.size() == 1) {
                return;
            }
            handleLogJarConflict(staticLoggerBinderPathSet, "Class path contains multiple SLF4J bindings. 注意排包");
        } catch (Throwable t) {
            t.getStackTrace();
        }
    }
    /**
     * 日志jar包冲突报警
     * @param staticLoggerBinderPathSet jar包路径
     * @param tip 提示语
     */
    private void handleLogJarConflict (Set<URL> staticLoggerBinderPathSet, String tip) {
        String ip = getLocalHostIp();
        StringBuilder detail = new StringBuilder();
        detail.append("ip为").append(ip).append("; 提示语为").append(tip);
        if (!CollectionUtils.isEmpty(staticLoggerBinderPathSet)) {
            String path = JsonUtil.pojoToJsonByGson(staticLoggerBinderPathSet);
            detail.append("; 重复的包路径分别为 ").append(path);
        }
        String logDetail = detail.toString();

        //TODO 使用自定义报警通知logDetail信息
        logger.error(logDetail);
    }

    private String getLocalHostIp() {
        String ip;
        try {
            InetAddress addr = InetAddress.getLocalHost();
            ip = addr.getHostAddress();
        } catch (Exception var2) {
            ip = "";
        }
        return ip;
    }
}

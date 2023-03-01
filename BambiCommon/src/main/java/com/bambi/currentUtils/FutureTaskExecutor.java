package com.bambi.currentUtils;

import com.bambi.utils.ThreadUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * 描述：
 *      创建混合型线程池<br>
 *      使用add函数将接收到的线程任务放入线程池中执行
 * <pre>
 * HISTORY
 * ****************************************************************************
 *  ID     DATE          PERSON          REASON
 *  1      2023/2/26 3:56    Bambi        Create
 * ****************************************************************************
 * </pre>
 *
 * @author Bambi
 * @since 1.0
 */
public class FutureTaskExecutor {
    private static Logger logger = LoggerFactory.getLogger(FutureTaskExecutor.class);

    static ThreadPoolExecutor threadPoolExecutor;
    static {
        threadPoolExecutor = ThreadUtil.getMixedTargetThreadPool();
    }

    private FutureTaskExecutor(){}

    public static void add(Runnable task){
        threadPoolExecutor.submit(()->{task.run();});
    }
}

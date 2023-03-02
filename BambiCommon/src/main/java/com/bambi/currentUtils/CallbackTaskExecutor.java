package com.bambi.currentUtils;

import com.bambi.utils.ThreadUtil;
import com.google.common.util.concurrent.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Callable;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 描述：
 *      使用guava实现异步回调<br>
 *      在方法{@link CallbackTaskExecutor#add}() 中，接收{@link CallbackTask}<br>
 *      并在内部执行回调逻辑，回调中执行其对应的{@link CallbackTask#onBack(Object)}以及{@link CallbackTask#onException(Throwable)}函数
 * <pre>
 * HISTORY
 * ****************************************************************************
 *  ID     DATE          PERSON          REASON
 *  1      2023/2/28 6:54    Bambi        Create
 * ****************************************************************************
 * </pre>
 *
 * @author Bambi
 * @since 1.0
 */
public class CallbackTaskExecutor {
    private static Logger logger = LoggerFactory.getLogger(CallbackTaskExecutor.class);

    static ListeningExecutorService listeningPool = null;
    static {
        ThreadPoolExecutor mixedTargetThreadPool = ThreadUtil.getMixedTargetThreadPool();
        listeningPool = MoreExecutors.listeningDecorator(mixedTargetThreadPool);
    }

    private CallbackTaskExecutor(){}

    /**
     * 添加对应任务
     * @param callbackTask
     * @param <R>
     */
    public static <R> void add(CallbackTask<R> callbackTask){
        ListenableFuture<R> submit = listeningPool.submit(new Callable<R>() {
            @Override
            public R call() throws Exception {
                R execute = callbackTask.execute();
                return execute;
            }
        });
        Futures.addCallback(submit,
                new FutureCallback<R>() {
                    @Override
                    public void onSuccess(R result) {
                        callbackTask.onBack(result);
                    }

                    @Override
                    public void onFailure(Throwable t) {
                        callbackTask.onException(t);
                    }
                },listeningPool);
    }
}

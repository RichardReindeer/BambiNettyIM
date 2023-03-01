package com.bambi.currentUtils;

/**
 * 描述：
 *      <b>CallbackTask</b><br>
 *      对callable线程内部任务逻辑的封装接口<br>
 *      将带有返回值的任务逻辑放入 {@link CallbackTask#execute()} 中执行<br>
 *      {@link CallbackTask#onBack(Object)} 以及 {@link CallbackTask#onException(Throwable)}则是为了guava的FutureCallback准备<br>
 *      {@link CallbackTask#onBack(Object)}接收执行成功之后的结果<br>
 *      {@link CallbackTask#onException(Throwable)}则接收报错后的抛出异常。
 * <pre>
 * HISTORY
 * ****************************************************************************
 *  ID     DATE          PERSON          REASON
 *  1      2023/2/28 6:38    Bambi        Create
 * ****************************************************************************
 * </pre>
 *
 * @author Bambi
 * @since 1.0
 */
public interface CallbackTask<R> {

    R execute() throws Exception;

    /**
     * 异步任务回调结果，对应{@link com.google.common.util.concurrent.FutureCallback#onSuccess(Object)}
     * @param r
     */
    void onBack(R r);

    /**
     * 异步任务执行失败的异常捕获 对应{@link com.google.common.util.concurrent.FutureCallback#onFailure(Throwable)}
     * @param t
     */
    void onException(Throwable t);
}

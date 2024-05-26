package com.think.event;

/**
 * 处理事件订阅者抛出异常
 *
 * @author veione
 */
@FunctionalInterface
public interface SubscriberExceptionHandler {
    /**
     * 处理订阅者抛出的异常
     *
     * @param exception 异常对象
     * @param context   异常上下文
     */
    void handleException(Throwable exception, SubscriberExceptionContext context);
}

package com.think.event;

/**
 * 订阅调用接口
 *
 * @author veione
 */
@FunctionalInterface
public interface SubscriberInvoker<T, E> {
    /**
     * 调用事件
     *
     * @param event 事件对象
     */
    void invoke(E event) throws Exception;
}

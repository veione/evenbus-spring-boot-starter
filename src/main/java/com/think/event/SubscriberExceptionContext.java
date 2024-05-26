package com.think.event;

import java.lang.reflect.Method;

/**
 * 订阅者抛出异常上下文
 *
 * @param eventBus         事件通道
 * @param event            事件对象
 * @param subscriber       订阅者
 * @param subscriberMethod 订阅方法
 */
public record SubscriberExceptionContext(EventBus eventBus, Object event, Object subscriber, Method subscriberMethod) {
}

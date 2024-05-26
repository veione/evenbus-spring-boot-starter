package com.think.event.annotation;

import com.think.event.ThreadMode;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Documented
public @interface Subscribe {
    /**
     * 指定事件订阅方法的线程模式，即在哪个事件执行事件订阅方法处理事件，默认为POSTING
     *
     * @return
     */
    ThreadMode threadMode() default ThreadMode.POSTING;

    /**
     * 自定义线程池名称
     *
     * @return
     */
    String threadPoolName() default "";

    /**
     * 指定事件订阅方法的优先级,默认0,如果多个事件订阅方法可以接收相同事件的,则优先级高的先接收到事件
     *
     * @return
     */
    int priority() default 0;
}

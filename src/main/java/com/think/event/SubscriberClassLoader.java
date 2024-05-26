package com.think.event;

/**
 * 自定义类加载器
 *
 * @author veione
 */
public class SubscriberClassLoader extends ClassLoader {

    public SubscriberClassLoader() {
        super(Thread.currentThread().getContextClassLoader());
    }

    /**
     * 将字节数组转化为Class对象
     *
     * @param name 类全名
     * @param data class数组
     * @return
     */
    public Class<?> defineClass(String name, byte[] data) {
        return this.defineClass(name, data, 0, data.length);
    }
}

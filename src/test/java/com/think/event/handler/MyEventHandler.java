package com.think.event.handler;

import com.think.event.ThreadMode;
import com.think.event.annotation.Subscribe;
import com.think.event.struct.LoginEvent;

public class MyEventHandler {

    @Subscribe(threadMode = ThreadMode.ASYNC, priority = 10)
    public void handleLoginEvent(LoginEvent event) {
        System.out.println("event1 = " + event + ", thread = " + Thread.currentThread().getName());
    }

    @Subscribe(priority = 20)
    public void handleLoginEvent2(LoginEvent event) {
        System.out.println("event2 = " + event + ", thread = " + Thread.currentThread().getName());
    }

    @Subscribe
    public void handleString(String string) {
        System.out.println("收到了字符串事件：" + string);
    }

    @Subscribe
    public void handleInt(Integer value) {
        System.out.println("收到了整型事件：" + value);
    }
}

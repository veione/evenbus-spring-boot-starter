package com.think.event;

import com.think.event.annotation.Subscribe;

public class MyEventController {

    @Subscribe(threadMode = ThreadMode.ASYNC, priority = 10)
    public void onEvent(Object event) {
        System.out.println("MyEventController event = " + event);
    }
}

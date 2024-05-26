package com.think.event;

import com.think.event.handler.MyEventHandler;
import com.think.event.struct.LoginEvent;
import org.junit.jupiter.api.Test;

public class TestEventBus {

    @Test
    public void testEventBus() {
        MyEventHandler handler = new MyEventHandler();
        EventBus.getDefault().register(handler);

        EventBus.getDefault().post(new LoginEvent(100201, "张三"));

        EventBus.getDefault().post("Hello,EventBus");
        EventBus.getDefault().post(123);

        EventBus.getDefault().unregister(handler);
    }
}

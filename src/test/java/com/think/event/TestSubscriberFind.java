package com.think.event;

import com.think.event.handler.MyEventHandler;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestSubscriberFind {

    @Test
    public void testFinder() {
        SubscriberMethodFinder finder = new SubscriberMethodFinder();
        List<SubscriberMethod> subscriberMethods = finder.findSubscriberMethods(MyEventHandler.class);
        assertEquals(subscriberMethods.size(), 4);
    }
}

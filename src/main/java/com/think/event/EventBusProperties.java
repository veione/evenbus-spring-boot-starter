package com.think.event;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("com.think.eventbus")
public class EventBusProperties {

    private boolean enabled = true;
}

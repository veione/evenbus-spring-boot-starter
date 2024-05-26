package com.think.event;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Event auto configuration.
 *
 * @author veione
 */
@Configuration
@EnableConfigurationProperties(EventBusProperties.class)
@ConditionalOnProperty(prefix = "com.think.eventbus", name = "enabled", havingValue = "true")
public class EventAutoConfiguration {

    @Bean
    public SubscriberPostProcessor subscriberPostProcessor() {
        return new SubscriberPostProcessor();
    }
}

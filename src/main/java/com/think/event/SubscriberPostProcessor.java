package com.think.event;

import com.think.event.annotation.SubscribeService;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.core.annotation.AnnotationUtils;

import java.lang.annotation.Annotation;

/**
 * Subscriber bean post processor.
 *
 * @author veione
 */
public class SubscriberPostProcessor implements BeanPostProcessor, ApplicationListener<ContextClosedEvent>, ApplicationContextAware {

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        Class<?> clazz = bean.getClass();
        Annotation annotation = AnnotationUtils.findAnnotation(clazz, SubscribeService.class);
        if (annotation != null) {
            // register event
            EventBus.getDefault().register(bean);
        }

        return bean;
    }

    @Override
    public void onApplicationEvent(ContextClosedEvent event) {
        EventBus.getDefault().shutdown();
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        EventBus.getDefault().setApplicationContext(applicationContext);
    }
}

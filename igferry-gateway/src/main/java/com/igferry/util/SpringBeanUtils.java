package com.igferry.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEvent;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
@Slf4j
public class SpringBeanUtils implements ApplicationContextAware {

    private static ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        if (Objects.isNull(SpringBeanUtils.applicationContext)) {
            SpringBeanUtils.applicationContext = applicationContext;
        }
    }

    public static ApplicationContext getApplicationContext() {
        return applicationContext;
    }

    public static <T> T getBean(Class<T> clazz) {
        return getApplicationContext().getBean(clazz);
    }

    public static <T> T getBean(String name, Class<T> requiredType) {
        return getApplicationContext().getBean(name, requiredType);
    }

    public static void publishEvent(ApplicationEvent event) {
        log.info("publish event:{} at:{}", event.getClass(), event.getTimestamp());
        getApplicationContext().publishEvent(event);
    }

}

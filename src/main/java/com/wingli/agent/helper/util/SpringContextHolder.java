package com.wingli.agent.helper.util;

import com.alibaba.dubbo.config.spring.ReferenceBean;
import com.alibaba.dubbo.config.spring.AnnotationBean;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SpringContextHolder implements ApplicationContextAware {
    private static ApplicationContext applicationContext = null;

    public SpringContextHolder() {
    }

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        SpringContextHolder.applicationContext = applicationContext;
    }

    public static <T> T getBean(Class<T> type) {
        assertContextInjected();
        return applicationContext.getBean(type);
    }

    public static <T> T getReference(Class<T> type){
        try {
            AnnotationBean annotationBean = getBean(com.alibaba.dubbo.config.spring.AnnotationBean.class);
            Field field = annotationBean.getClass().getDeclaredField("referenceConfigs");
            if (!field.isAccessible()) {
                field.setAccessible(true);
            }
            ConcurrentHashMap<String, ReferenceBean<?>> referenceMap = (ConcurrentHashMap<String, ReferenceBean<?>>) field.get(annotationBean);
            for (Map.Entry<String, ReferenceBean<?>> entry : referenceMap.entrySet()) {
                if (entry.getKey().contains(type.getCanonicalName())) {
                    return (T)entry.getValue().get();
                }
            }
            return null;
        }catch (Exception e){
            e.printStackTrace();
            return null;
        }
    }

    public static void assertContextInjected() {
        if (applicationContext == null) {
            throw new RuntimeException("applicationContext未注入");
        }
    }

}

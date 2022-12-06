package cn.smilefamily.context;

import cn.smilefamily.BeanNotFoundException;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;

public interface BeanFactory {
    Object getBean(String name);
    public List<?> getBeansByAnnotation(Class<? extends Annotation> annotation);
    public default Object getBean(Class<?> clazz) {
        return getBean(clazz.getName());
    }
    public default Object[] getBeans(Class<?>... classes) {
        List<Object> targetBeans = new ArrayList<>();
        for (Class<?> clazz : classes) {
            Object bean = getBean(clazz);
            if (bean == null) {
                throw new BeanNotFoundException(clazz.getName());
            }
            targetBeans.add(bean);
        }
        return targetBeans.toArray();
    }
    public default Object getBean(String name, Object defaultValue) {
        Object bean = getBean(name);
        return bean == null ? defaultValue : bean;
    }
}

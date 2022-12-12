package cn.smilefamily.context;

import cn.smilefamily.BeanNotFoundException;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public interface BeanFactory {
    String getName();

    <T> T getBean(String name);

    <T> T getBean(String name, Type beanType);

    public List<?> getBeansByAnnotation(Class<? extends Annotation> annotation);

    public default <T> T getBean(Class<T> clazz) {
        return getBean(clazz.getName(), clazz);
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

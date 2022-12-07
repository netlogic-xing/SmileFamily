package cn.smilefamily.bean;

import cn.smilefamily.BeanNotFoundException;
import cn.smilefamily.context.BeanFactory;

import java.util.function.Consumer;

public record Dependency(String name, Class<?> beanClass, boolean required, String description, boolean external) {
    public void setDepValue(BeanFactory beanFactory, Consumer<Object> assigner) {
        Object val = beanFactory.getBean(name, beanClass);
        if (val == null && required) {
            throw new BeanNotFoundException(name + " not found. " + (external ? " This dependency is external. " : "") + description);
        } else if (val != null) {
            assigner.accept(val);
        }
    }

    public Object getDepValue(BeanFactory beanFactory) {
        Object val = beanFactory.getBean(name, beanClass);
        if (val == null && required) {
            throw new BeanNotFoundException(name + " not found. " + (external ? " This dependency is external. " : "") + description);
        }
        return val;
    }
}

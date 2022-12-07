package cn.smilefamily.context;

import cn.smilefamily.bean.BeanDefinition;

import java.lang.annotation.Annotation;
import java.util.List;

public class YamlContext implements Context {
    @Override
    public <T> T getBean(String name) {
        return null;
    }

    @Override
    public <T> T getBean(String name, Class<T> beanClass) {
        return null;
    }

    @Override
    public List<?> getBeansByAnnotation(Class<? extends Annotation> annotation) {
        return null;
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public void setParent(BeanContext parent) {

    }

    @Override
    public List<BeanDefinition> export() {
        return null;
    }

    @Override
    public void importBeanDefinitions(List<BeanDefinition> bds) {

    }

    @Override
    public void build() {

    }
}

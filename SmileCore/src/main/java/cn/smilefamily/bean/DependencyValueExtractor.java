package cn.smilefamily.bean;

import cn.smilefamily.context.BeanFactory;

@FunctionalInterface
public interface DependencyValueExtractor {
    public Object extract(BeanFactory beanFactory);
}

package cn.smilefamily.context;

import cn.smilefamily.bean.BeanDefinition;

import java.util.concurrent.ConcurrentMap;

public interface ContextScopeSupportable {
    void createScope(String scope, ConcurrentMap<BeanDefinition, Object> scopedContext);

    public ConcurrentMap<BeanDefinition, Object> getScopedBeanContainer(String scopeName);

    void destroyScope(String scope);
}

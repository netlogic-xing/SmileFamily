package cn.smilefamily.bean;

import cn.smilefamily.annotation.Scope;

public interface BeanDefinition {
    String getName();

    Class<?> getType();

    String getDescription();

    String getSource();

    default void initialize(){
    }

    default void reset() {
    }

    default void destroy(Object bean){
    }

    boolean isExported();

    default boolean isSingleton() {
        return true;
    }

    default boolean isCustomizedScope() {
        return false;
    }

    default boolean isPrototype() {
        return false;
    }

    default String getScope() {
        return Scope.Singleton;
    }

    default Object getProxy() {
        return null;
    }

    default void setProxy(Object proxy){
    }

    Object getBeanInstance();
}

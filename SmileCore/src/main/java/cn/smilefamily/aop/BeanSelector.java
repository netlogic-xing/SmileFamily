package cn.smilefamily.aop;

import java.lang.reflect.Type;

@FunctionalInterface
public interface BeanSelector {
    boolean match(String name, Class<?> type);
}

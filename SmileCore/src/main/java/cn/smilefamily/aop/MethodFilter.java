package cn.smilefamily.aop;

import java.lang.reflect.Method;

public interface MethodFilter {
    boolean include(Method method);
}

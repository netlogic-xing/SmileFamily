package cn.smilefamily.util;

import cn.smilefamily.annotation.Injected;
import cn.smilefamily.annotation.Value;
import cn.smilefamily.bean.Dependency;
import cn.smilefamily.bean.ValueExtractors;
import cn.smilefamily.BeanInitializationException;
import org.reflections.Reflections;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class BeanUtils {
    /**
     * 从方法或构造函数的参数及field获取bean名字
     * @param p
     * @param defaultName
     * @return
     */
    public static String getBeanName(AnnotatedElement p, String defaultName) {
        String name = null;
        Injected injected = p.getAnnotation(Injected.class);
        if (injected != null && injected.name() != null && !injected.name().equals("")) {
            name = injected.name();
        }
        if (name == null) {
            name = defaultName;
        }
        return name;
    }

    public static Object newInstance(Class<?> clazz) {
        try {
            return clazz.getConstructor().newInstance();
        } catch (Exception e) {
            throw new BeanInitializationException(e);
        }
    }

    public static Object newInstance(Constructor constructor, Object... args) {
        try {
            return constructor.newInstance(args);
        } catch (Exception e) {
            throw new BeanInitializationException(e);
        }
    }

    public static Object invoke(Method method, Object target, Object... args) {
        try {
            return method.invoke(target, args);
        } catch (Exception e) {
            throw new BeanInitializationException(e);
        }
    }
    public static Object invokeStatic(Method method, Object... args) {
        try {
            return method.invoke(null, args);
        } catch (Exception e) {
            throw new BeanInitializationException(e);
        }
    }

    public static void setField(Field field, Object target, Object value) {
        try {
            field.setAccessible(true);
            field.set(target, value);
        } catch (IllegalAccessException e) {
            throw new BeanInitializationException(e);
        }
    }

    //此方法可用Reflections改造
    public static Set<Class<?>> findAllAnnotatedClassIn(String packageName, Class<? extends Annotation> annotationClass) {
        Reflections reflections = new Reflections(packageName);
        return reflections.getTypesAnnotatedWith(annotationClass);
    }

    private static Class<?> getClass(String className, String packageName) {
        try {
            return Class.forName(packageName + "."
                    + className.substring(0, className.lastIndexOf('.')));
        } catch (ClassNotFoundException e) {
            // handle the exception
        }
        return null;
    }

    /**
     * 获取方法或构造函数参数代表的依赖项
     * 方法参数和构造函数参数的autowired默认可以省略
     *
     * @param e
     * @return
     */
    public static List<Dependency> getParameterDeps(Executable e) {
        return Arrays.stream(e.getParameters()).map(p -> {
            if (p.isAnnotationPresent(Value.class)) {
                Value value = p.getAnnotation(Value.class);
                return new Dependency(value.value(), false,
                        ValueExtractors.getValueExtractor(p.getType(), value)
                );
            }
            String beanName = getBeanName(p, p.getType().getName());
            if (p.isAnnotationPresent(Injected.class)) {
                return new Dependency(beanName, p.getAnnotation(Injected.class).required());
            }
            return new Dependency(beanName);

        }).toList();
    }
}

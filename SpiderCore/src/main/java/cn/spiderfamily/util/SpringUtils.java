package cn.spiderfamily.util;

import cn.spiderfamily.annotation.Autowired;
import cn.spiderfamily.annotation.Value;
import cn.spiderfamily.bean.BeanDependence;
import cn.spiderfamily.bean.ValueExtractors;
import com.google.common.base.Strings;
import cn.spiderfamily.BeanInitializationException;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.*;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class SpringUtils {
    /**
     * 从方法或构造函数的参数及field获取bean名字
     * @param p
     * @param defaultName
     * @return
     */
    public static String getBeanName(AnnotatedElement p, String defaultName) {
        String name = null;
        Autowired autowired = p.getAnnotation(Autowired.class);
        if (autowired != null && autowired.name() != null && !autowired.name().equals("")) {
            name = autowired.name();
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

    public static void setField(Field field, Object target, Object value) {
        try {
            field.setAccessible(true);
            field.set(target, value);
        } catch (IllegalAccessException e) {
            throw new BeanInitializationException(e);
        }
    }

    //此方法可用Reflections改造
    public static Set<Class<?>> findAllClassesUsingClassLoader(String packageName) {
        InputStream stream = ClassLoader.getSystemClassLoader()
                .getResourceAsStream(packageName.replaceAll("[.]", "/"));
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
        return reader.lines()
                .filter(line -> line.endsWith(".class"))
                .map(line -> getClass(line, packageName))
                .collect(Collectors.toSet());
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
    public static List<BeanDependence> getParameterDeps(Executable e) {
        return Arrays.stream(e.getParameters()).map(p -> {
            if (p.isAnnotationPresent(Value.class)) {
                Value value = p.getAnnotation(Value.class);
                return new BeanDependence(value.value(), false,
                        ValueExtractors.getValueExtractor(p.getType(), value)
                );
            }
            String beanName = getBeanName(p, p.getType().getName());
            if (p.isAnnotationPresent(Autowired.class)) {
                return new BeanDependence(beanName, p.getAnnotation(Autowired.class).required());
            }
            return new BeanDependence(beanName);

        }).toList();
    }
}

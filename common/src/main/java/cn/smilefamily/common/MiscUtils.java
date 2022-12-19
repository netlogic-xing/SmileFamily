package cn.smilefamily.common;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.stream.Collectors;

public class MiscUtils {
    public static String shortName(String longName) {
        String[] vals = longName.split("\\.");
        if (vals.length <= 1) {
            return longName;
        }
        return Arrays.stream(vals)
                .limit(vals.length - 1)
                .filter(v -> v.length() != 0)
                .map(v -> v.charAt(0) + "")
                .collect(Collectors.joining("."))
                + "." + vals[vals.length - 1];
    }
    public static Class<?> loadClass(String className) throws ClassNotFoundException {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        if (classLoader == null) {
            classLoader = MiscUtils.class.getClassLoader();
        }
        return classLoader.loadClass(className);
    }

    public static Object newInstance(Constructor constructor, Object... args) {
        try {
            return constructor.newInstance(args);
        } catch (Exception e) {
            throw new ObjectInitializationException(e);
        }
    }

    public static Object invoke(Method method, Object target, Object... args) {
        try {
            return method.invoke(target, args);
        } catch (Exception e) {
            throw new ObjectInitializationException(e);
        }
    }

    public static Object invokeStatic(Method method, Object... args) {
        try {
            return method.invoke(null, args);
        } catch (Exception e) {
            throw new ObjectInitializationException(e);
        }
    }

    public static void setField(Field field, Object target, Object value) {
        try {
            field.setAccessible(true);
            field.set(target, value);
        } catch (IllegalAccessException e) {
            throw new ObjectInitializationException(e);
        }
    }

    public static void main(String[] args) {
        System.out.println(shortName(MiscUtils.class.getName()));
        System.out.println(shortName("xx.xx.last"));
        System.out.println(shortName("xx.last"));
        System.out.println(shortName("last"));
        Integer a =null;
        int ab = a;
    }
}

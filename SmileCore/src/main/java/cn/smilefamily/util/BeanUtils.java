package cn.smilefamily.util;

import cn.smilefamily.BeanInitializationException;
import cn.smilefamily.annotation.External;
import cn.smilefamily.annotation.Injected;
import cn.smilefamily.annotation.Value;
import cn.smilefamily.bean.Dependency;
import cn.smilefamily.context.SimpleExpressionSyntaxException;
import org.reflections.Reflections;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;
import java.util.function.BiFunction;

public class BeanUtils {
    /**
     * 从方法或构造函数的参数及field获取bean名字
     *
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

    public static String expression(String source, BiFunction<String, String, String> valueProvider) {
        return unescape(expression(source, false, source, valueProvider));
    }

    private static String expression(String source, boolean singleVariable, String original, BiFunction<String, String, String> valueProvider) {
        int tokenBegin = source.indexOf("${");
        if (tokenBegin == -1) {
            return singleVariable ? apply(source, valueProvider) : source;
        }
        StringBuilder sb = new StringBuilder();
        sb.append(source, 0, tokenBegin);
        int tokenEnd = findMatched(source, original, '{', '}');
        if (tokenEnd == -1) {
            throw new SimpleExpressionSyntaxException("No end token } found in " + source);
        }
        String var = expression(source.substring(tokenBegin + 2, tokenEnd), true, original, valueProvider);
        if (var == null) {//如果表达式中部分因子为null，则整个表达式为null
            return null;
        }
        sb.append(var);
        String rest = expression(source.substring(tokenEnd + 1), false, original, valueProvider);
        if (rest == null) {
            return null;
        }
        sb.append(rest);
        return singleVariable ? apply(sb.toString(), valueProvider) : sb.toString();
    }

    private static String apply(String source, BiFunction<String, String, String> valueProvider) {
        String[] values = source.split("\\s*:\\s*", 2);
        String key = values[0];
        String defaultValue = values.length == 2 ? values[1] : null;
        return valueProvider.apply(key, defaultValue);
    }

    public static String unescape(String source) {
        if (source == null) {
            return null;
        }
        return source.replaceAll("\\\\([\\{\\}])", "$1");
    }

    private static int findMatched(String source, String original, char left, char right) {
        int deep = 0;
        char lastChar = '0';
        for (int i = 0; i < source.length(); i++) {
            char c = source.charAt(i);
            if (c == left && lastChar != '\\') {
                deep++;
                continue;
            }
            if (c != right || lastChar == '\\') {
                continue;
            }

            deep--;
            if (deep == 0) {
                return i;
            }
            lastChar = c;
        }
        throw new SimpleExpressionSyntaxException("{ and } not matched in " + original);
    }

    public static void main(String[] args) {
        String source = "http://${${nameplate}_ip}:${${nameplate2}_port}/xx?id=$\\{id\\}";
        String source1 = "$\\{host\\}";
        System.out.println(source1);
        System.out.println(unescape(source1));
        Map<String, String> context = new HashMap<>();
        context.put("nameplate", "hostauto");
        context.put("nameplate2", "xx");
        String value = expression(source, (key, defautValue) -> {
            System.out.println("--" + key);
            return context.getOrDefault(key, key);
        });
        System.out.println(value);
        System.out.println(expression("${xingchenayang}_v", (key, defautValue) -> {
            return "xxy";
        }));
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
            External external = p.getAnnotation(External.class);
            String desc = external == null ? "" : external.value();
            if (p.isAnnotationPresent(Value.class)) {
                Value value = p.getAnnotation(Value.class);
                return new Dependency(value.value(), p.getType(), false, desc, external != null);
            }
            String beanName = getBeanName(p, p.getType().getName());
            if (p.isAnnotationPresent(Injected.class)) {
                return new Dependency(beanName, p.getType(), p.getAnnotation(Injected.class).required(), desc, external != null);
            }
            return new Dependency(beanName, p.getType(), false, desc, external != null);
        }).toList();
    }
}

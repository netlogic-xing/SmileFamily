package cn.smilefamily.bean;

import cn.smilefamily.annotation.core.Value;
import cn.smilefamily.util.BeanUtils;

import java.lang.reflect.Type;
import java.util.Optional;
import java.util.function.Function;

/**
 * ValueExtractor工厂
 */
public class ValueExtractors {
    /**
     * 可根据类型对值进行转换
     *
     * @param type
     * @param value
     * @return
     */
    public static DependencyValueExtractor getValueExtractor(Type type, Value value) {
        //如果是目标类型是基本类型或字符串(通常是properties文件配置）
        if (type instanceof Class clazz && (clazz.isPrimitive() || String.class.isAssignableFrom(clazz))) {
            return context -> {
                String exp = value.value();
                String val = BeanUtils.expression(exp, (name, defaultVal) -> {
                    String bean = context.getBean(name);
                    return bean == null ? defaultVal : bean;
                });
                if (int.class.isAssignableFrom(clazz) || Integer.class.isAssignableFrom(clazz)) {
                    return convert(val, Integer::parseInt);
                }
                if (short.class.isAssignableFrom(clazz) || Short.class.isAssignableFrom(clazz)) {
                    return convert(val, Short::parseShort);
                }
                if (long.class.isAssignableFrom(clazz) || Long.class.isAssignableFrom(clazz)) {
                    return convert(val, Long::parseLong);
                }
                if (float.class.isAssignableFrom(clazz) || Float.class.isAssignableFrom(clazz)) {
                    return convert(val, Float::parseFloat);
                }
                if (double.class.isAssignableFrom(clazz) || Double.class.isAssignableFrom(clazz)) {
                    return convert(val, Double::parseDouble);
                }
                if (byte.class.isAssignableFrom(clazz) || Byte.class.isAssignableFrom(clazz)) {
                    return convert(val, Byte::parseByte);
                }
                if (boolean.class.isAssignableFrom(clazz) || Boolean.class.isAssignableFrom(clazz)) {
                    return convert(val, Boolean::parseBoolean);
                }
                if (char.class.isAssignableFrom(clazz) || Character.class.isAssignableFrom(clazz)) {
                    return convert(val, v -> v.charAt(0));
                }
                return val;
            };
        }
        return null;
    }

    private static Object convert(String value, Function<String, Object> converter) {
        Optional<String> optionalValue = Optional.ofNullable(value);
        return optionalValue.map(converter).orElse(null);
    }
}

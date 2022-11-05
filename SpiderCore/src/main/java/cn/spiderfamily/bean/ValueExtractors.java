package cn.spiderfamily.bean;

import cn.spiderfamily.annotation.Value;

import java.util.Optional;
import java.util.function.Function;

/**
 * ValueExtractor工厂
 */
public class ValueExtractors {
    /**
     * 可根据类型对值进行转换
     *
     * @param clazz
     * @param value
     * @return
     */
    public static DependenceValueExtractor getValueExtractor(Class<?> clazz, Value value) {
        return context -> {
            String val = (String) context.getBean(value.value());
            val = val == null && value.defaultValue() != null && !value.defaultValue().equals("") ? value.defaultValue() : val;
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

    private static Object convert(String value, Function<String, Object> converter) {
        Optional<String> optionalValue = Optional.ofNullable(value);
        return optionalValue.map(converter).orElse(null);
    }
}

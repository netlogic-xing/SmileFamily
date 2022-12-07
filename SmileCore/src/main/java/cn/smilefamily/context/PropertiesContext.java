package cn.smilefamily.context;

import cn.smilefamily.BeanInitializationException;
import cn.smilefamily.bean.BeanDefinition;
import cn.smilefamily.bean.PropertyBeanDefinition;
import cn.smilefamily.common.DelayedTaskExecutor;
import cn.smilefamily.util.BeanUtils;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.*;
import java.util.function.Function;

/**
 * 代表一个属性文件的context，仅共host context内部使用。
 */
public class PropertiesContext implements Context {

    private String propertiesFileName;
    private Properties properties;
    private Context host;
    private Map<String, BeanDefinition> beanDefinitions = new HashMap<>();

    private DelayedTaskExecutor propertyEvaluator;
    private boolean constructionComplete = false;

    public PropertiesContext(String propertiesFileName, Context host) {
        this.host = host;
        propertyEvaluator = new DelayedTaskExecutor(() -> constructionComplete);
        this.propertiesFileName = propertiesFileName;
        properties = new Properties();
        if (propertiesFileName.startsWith("classpath:")) {
            String path = propertiesFileName.substring("classpath:".length());
            try {
                properties.load(this.getClass().getResourceAsStream(path));
                //add properties
                properties.forEach((key, val) -> {
                    String keyString = (String) key;
                    String valString = (String) val;
                    if (!keyString.contains("${") && !valString.contains("${")) {
                        if (keyString.startsWith("@")) {
                            beanDefinitions.put(keyString, new PropertyBeanDefinition(keyString, valString, propertiesFileName));
                        }
                        return;
                    }
                    propertyEvaluator.addFirst(keyString, () -> {
                        String realKey = BeanUtils.expression(keyString, (placeHolder, defaultVal) -> {
                            //这里通过host(parent)获取bean，目的是能在更大范围内查找依赖
                            String value = System.getProperty(placeHolder, (String) getBean(placeHolder, defaultVal));
                            if (value == null) {
                                throw new BeanInitializationException("placeHolder ${" + placeHolder + "} of " + keyString + " cannot be resolved in " + propertiesFileName + ".");
                            }
                            return value;
                        });
                        beanDefinitions.put(realKey, new PropertyBeanDefinition(realKey, propertiesFileName,
                                () -> BeanUtils.expression(valString, (name, defaultVal) -> System.getProperty(name, (String) host.getBean(name, defaultVal)))));
                    });
                    constructionComplete = true;
                    propertyEvaluator.execute();
                });
            } catch (IOException e) {
                throw new PropertiesLoadException(e);
            }
        }
    }

    /**
     * 本类的getBean方法仅仅在本类内部查找。
     *
     * @param name
     * @return
     */
    @Override
    public <T> T getBean(String name) {
        BeanDefinition bd = beanDefinitions.get(name);
        if (bd != null) {
            return (T) bd.getBeanInstance();
        }
        return (T) properties.getProperty(name);
    }

    @Override
    public <T> T getBean(String nameExpression, Class<T> clazz) {
        String val = BeanUtils.expression(nameExpression, (name, defaultVal) -> {
            String bean = (String) getBean(name);
            return bean == null ? defaultVal : bean;
        });
        if (int.class.isAssignableFrom(clazz) || Integer.class.isAssignableFrom(clazz)) {
            return (T) convert(val, Integer::parseInt);
        }
        if (short.class.isAssignableFrom(clazz) || Short.class.isAssignableFrom(clazz)) {
            return (T) convert(val, Short::parseShort);
        }
        if (long.class.isAssignableFrom(clazz) || Long.class.isAssignableFrom(clazz)) {
            return (T) convert(val, Long::parseLong);
        }
        if (float.class.isAssignableFrom(clazz) || Float.class.isAssignableFrom(clazz)) {
            return (T) convert(val, Float::parseFloat);
        }
        if (double.class.isAssignableFrom(clazz) || Double.class.isAssignableFrom(clazz)) {
            return (T) convert(val, Double::parseDouble);
        }
        if (byte.class.isAssignableFrom(clazz) || Byte.class.isAssignableFrom(clazz)) {
            return (T) convert(val, Byte::parseByte);
        }
        if (boolean.class.isAssignableFrom(clazz) || Boolean.class.isAssignableFrom(clazz)) {
            return (T) convert(val, Boolean::parseBoolean);
        }
        if (char.class.isAssignableFrom(clazz) || Character.class.isAssignableFrom(clazz)) {
            return (T) convert(val, v -> v.charAt(0));
        }
        return (T) val;
    }
    private Object convert(String value, Function<String, Object> converter) {
        Optional<String> optionalValue = Optional.ofNullable(value);
        return optionalValue.map(converter).orElse(null);
    }
    @Override
    public List<?> getBeansByAnnotation(Class<? extends Annotation> annotation) {
        throw new UnsupportedOperationException("PropertiesContext doesn't support getBeansByAnnotation");
    }

    @Override
    public String getName() {
        return propertiesFileName;
    }

    @Override
    public void setParent(BeanContext parent) {
        this.host = parent;
    }

    @Override
    public List<BeanDefinition> export() {
        return beanDefinitions.values().stream().filter(bd -> bd.isExported()).toList();
    }

    @Override
    public void importBeanDefinitions(List<BeanDefinition> bds) {
        throw new UnsupportedOperationException("PropertiesContext doesn't support importBeanDefinitions");
    }

    @Override
    public void build() {
        constructionComplete = true;
        propertyEvaluator.execute();
    }
}

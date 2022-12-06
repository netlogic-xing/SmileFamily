package cn.smilefamily.context;

import cn.smilefamily.BeanInitializationException;
import cn.smilefamily.bean.BeanDefinition;
import cn.smilefamily.bean.PropertyBeanDefinition;
import cn.smilefamily.common.DelayedTaskExecutor;
import cn.smilefamily.util.BeanUtils;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

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
    public Object getBean(String name) {
        BeanDefinition bd = beanDefinitions.get(name);
        if (bd != null) {
            return bd.getBeanInstance();
        }
        return properties.getProperty(name);
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

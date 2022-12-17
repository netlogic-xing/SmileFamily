package cn.smilefamily.context;

import cn.smilefamily.BeanInitializationException;
import cn.smilefamily.annotation.core.Profile;
import cn.smilefamily.aop.AdvisorDefinition;
import cn.smilefamily.bean.BeanDefinition;
import cn.smilefamily.bean.PropertyBeanDefinition;
import cn.smilefamily.common.DelayedTaskExecutor;
import cn.smilefamily.util.BeanUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.*;

/**
 * 代表一个属性文件的context，仅共host context内部使用。
 */
class PropertiesContext implements Context {
    private record PropertySource(String source, Map<String, String> properties) {
    }

    /**
     * 多个配置源，后面的优先级高于前面的
     */
    private List<PropertySource> propertySources = new ArrayList<>();
    private Context host;
    private Map<String, BeanDefinition> beanDefinitions = new HashMap<>();

    private DelayedTaskExecutor propertyEvaluator;
    private boolean constructionComplete = false;

    public PropertiesContext(Context host) {
        this.host = host;
    }

    public void addProperties(String source, Map<String, String> properties) {
        this.propertySources.add(new PropertySource(source, properties));
    }

    public void initialize() {
        propertyEvaluator = new DelayedTaskExecutor(() -> constructionComplete);
        propertySources.forEach(propSrc -> {
            propSrc.properties.forEach((key, val) -> {
                if (!key.contains("${") && !val.contains("${")) {
                    if (key.startsWith("@")) {
                        beanDefinitions.put(key, new PropertyBeanDefinition(key, val, host.getName() + "/" + propSrc.source));
                    }
                    return;
                }
                propertyEvaluator.addFirst(key, () -> {
                    String realKey = BeanUtils.expression(key, (placeHolder, defaultVal) -> {
                        //这里通过host(parent)获取bean，目的是能在更大范围内查找依赖
                        String value = System.getProperty(placeHolder, (String) host.getBean(placeHolder, defaultVal));
                        if (value == null) {
                            throw new BeanInitializationException("placeHolder ${" + placeHolder + "} of " + key + " cannot be resolved in " + host.getName() + ".");
                        }
                        return value;
                    });
                    beanDefinitions.put(realKey,
                            new PropertyBeanDefinition(realKey,
                                    host.getName() + "/" + propSrc.source,
                                    () -> BeanUtils.expression(val, (name, defaultVal) -> System.getProperty(name, (String) host.getBean(name, defaultVal)))));
                });
            });
        });
        //后加入的配置源优先级高，在查找时应该先找到
        Collections.reverse(propertySources);
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
        for (PropertySource propertySource : propertySources) {
            String value = propertySource.properties.get(name);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    @Override
    public <T> T getBean(String name, Type beanType) {
        return (T) getBean(name);
    }

    @Override
    public List<?> getBeansByAnnotation(Class<? extends Annotation> annotation) {
        throw new UnsupportedOperationException("PropertiesContext doesn't support getBeansByAnnotation");
    }

    @Override
    public List<AdvisorDefinition> getAdvisorDefinitions() {
        throw new UnsupportedOperationException("PropertiesContext doesn't support getAdvisorDefinitions");
    }

    @Override
    public String getName() {
        return host.getName();
    }

    /**
     * Find active profile from:
     * 1. Java Systems
     * 2. PropertiesContext(self)
     * 3. default value
     * @return
     */
    @Override
    public String getProfile() {
        return System.getProperty(Profile.ACTIVE_KEY, (String) getBean(Profile.ACTIVE_KEY));
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
        if (constructionComplete) {
            return;
        }
        constructionComplete = true;
        propertyEvaluator.execute();
    }
}

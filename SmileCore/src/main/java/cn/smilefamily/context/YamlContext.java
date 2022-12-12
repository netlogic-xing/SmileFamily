package cn.smilefamily.context;

import cn.smilefamily.bean.BeanDefinition;
import cn.smilefamily.util.BeanUtils;
import com.fasterxml.jackson.databind.JsonNode;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class YamlContext implements Context {
    private record YamlSource(String source, JsonNode root) {
    }

    private List<YamlSource> yamlSources = new ArrayList<>();
    private Context host;

    public YamlContext(Context host) {
        this.host = host;
    }

    public void addYamlDoc(String source, JsonNode doc) {
        this.yamlSources.add(new YamlSource(source, doc));
    }

    /**
     * 本类的getBean方法仅仅在本类内部查找。
     *
     * @param name
     * @return
     */
    @Override
    public <T> T getBean(String name) {
        return (T) getBean(name, Object.class);
    }

    @Override
    public <T> T getBean(String nameExpression, Type beanType) {
        if(!nameExpression.startsWith("/")){
            return null;
        }
        for (YamlSource yamlSource : yamlSources) {
            JsonNode jsonNode = yamlSource.root.at(nameExpression);
            if (jsonNode.isMissingNode()) {
                continue;
            }
            return BeanUtils.toObject(jsonNode, beanType);
        }
        return null;
    }

    @Override
    public List<?> getBeansByAnnotation(Class<? extends Annotation> annotation) {
        throw new UnsupportedOperationException("PropertiesContext doesn't support getBeansByAnnotation");
    }

    @Override
    public String getName() {
        return this.host.getName();
    }

    @Override
    public String getProfile() {
        throw new UnsupportedOperationException("YamlContext doesn't support getProfile");
    }

    @Override
    public void setParent(BeanContext parent) {
        this.host = parent;
    }

    @Override
    public List<BeanDefinition> export() {
        return Collections.emptyList();
    }

    @Override
    public void importBeanDefinitions(List<BeanDefinition> bds) {
        throw new UnsupportedOperationException("PropertiesContext doesn't support importBeanDefinitions");
    }

    @Override
    public void build() {
    }
}

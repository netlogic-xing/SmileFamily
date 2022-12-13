package cn.smilefamily.bean;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class PropertyBeanDefinition implements BeanDefinition {
    private String name;

    private String value;
    private boolean exported;
    private String description;
    private String source;
    private Class<?> type = String.class;

    public PropertyBeanDefinition(String name, String value, String source) {
        this.name = name;
        this.value = value;
        this.exported = name.startsWith("@");//@开头的属性是导出的
        this.source = source;
    }

    public PropertyBeanDefinition(String name, String source, Supplier<?> factory) {
        this.name = name;
        this.exported = name.startsWith("@");
        this.source = source;
        this.factory = factory;
    }

    public String toString() {
        return "Property(" + name + ")= " + getBeanInstance() + "\n\tfrom: " + source;
    }

    //用于生成Bean实例的工厂函数
    private Supplier<?> factory;

    @Override
    public String getName() {
        return name;
    }


    @Override
    public Class<?> getType() {
        return type;
    }

    @Override
    public String getDescription() {
        if (description == null) {
            description = name + "=" + getBeanInstance();
        }
        return description;
    }

    @Override
    public String getSource() {
        return source;
    }

    @Override
    public boolean isExported() {
        return exported;
    }

    @Override
    public Object getBeanInstance() {
        if (factory == null) {
            return value;
        }
        return factory.get();
    }
}

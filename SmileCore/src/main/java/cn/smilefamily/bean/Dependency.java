package cn.smilefamily.bean;

import cn.smilefamily.BeanNotFoundException;
import cn.smilefamily.context.BeanFactory;
import cn.smilefamily.util.BeanUtils;

import java.lang.reflect.Type;
import java.util.function.Consumer;

public record Dependency(String name, Type depType, boolean required, String description, boolean external,
                         DependencyValueExtractor extractor) {
    //todo:考虑把依赖的类型也传进来。以便在这里可以调用context.getBean(name,class)
    public Dependency(String name, Type depType, String description, boolean external) {
        this(name, depType, true, description, external, getValueExtractor(name, depType));
    }

    private static DependencyValueExtractor getValueExtractor(String name, Type depType) {
        return context -> {
            if (name.contains("${")) {
                String realName = BeanUtils.expression(name, (key, defaultVal) -> {
                    String val = context.getBean(key);
                    return val != null ? val : defaultVal == null ? key : defaultVal;
                });
                return context.getBean(realName, depType);
            }
            return context.getBean(name, depType);
        };
    }

    public Dependency(String name, Type depType, boolean required, String description, boolean external) {
        this(name, depType, required, description, external, getValueExtractor(name, depType));
    }

    public void setDepValue(BeanFactory beanFactory, Consumer<Object> assigner) {
        try {
            Object val = this.extractor.extract(beanFactory);
            if (val == null && required) {
                throw new BeanNotFoundException(name + " not found. " + (external ? " This dependency is external. " : "") + description);
            } else if (val != null) {
                assigner.accept(val);
            }
        } catch (Exception e) {
            throw new BeanDependencyInjectException("Inject bean " + name + " failed.", e);
        }
    }

    public Object getDepValue(BeanFactory beanFactory) {
        try {
            Object val = this.extractor.extract(beanFactory);
            if (val == null && required) {
                throw new BeanNotFoundException(name + " not found. " + (external ? " This dependency is external. " : "") + description);
            }
            return val;
        } catch (Exception e) {
            throw new BeanDependencyInjectException("Inject bean " + name + " failed.", e);
        }
    }
}

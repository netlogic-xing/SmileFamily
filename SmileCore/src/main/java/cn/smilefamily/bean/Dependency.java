package cn.smilefamily.bean;

import cn.smilefamily.BeanNotFoundException;
import cn.smilefamily.context.Context;

import java.util.function.Consumer;

public record Dependency(String name, boolean required, String description, boolean external,
                         DependencyValueExtractor extractor) {
    public Dependency(String name, String description, boolean external) {
        this(name, true, description, external, context -> context.getBean(name));
    }

    public Dependency(String name, boolean required, String description, boolean external) {
        this(name, required, description, external, context -> context.getBean(name));
    }

    public void setDepValue(Context context, Consumer<Object> assigner) {
        Object val = this.extractor.extract(context);
        if (val == null && required) {
            throw new BeanNotFoundException(name + " not found. " + (external ? " This dependency is external. " : "") + description);
        } else if (val != null) {
            assigner.accept(val);
        }
    }

    public Object getDepValue(Context context) {
        Object val = this.extractor.extract(context);
        if (val == null && required) {
            throw new BeanNotFoundException(name + " not found. " + (external ? " This dependency is external. " : "") + description);
        }
        return val;
    }
}

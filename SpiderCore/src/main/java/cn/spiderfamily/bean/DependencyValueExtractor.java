package cn.spiderfamily.bean;

import cn.spiderfamily.context.Context;

@FunctionalInterface
public interface DependencyValueExtractor {
    public Object extract(Context context);
}

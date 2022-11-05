package cn.spiderfamily.bean;

import cn.spiderfamily.context.Context;

@FunctionalInterface
public interface DependenceValueExtractor {
    public Object extract(Context context);
}

package cn.smilefamily.bean;

import cn.smilefamily.context.Context;

@FunctionalInterface
public interface DependencyValueExtractor {
    public Object extract(Context context);
}

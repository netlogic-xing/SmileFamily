package cn.smilefamily.context;

import cn.smilefamily.bean.BeanDefinition;

import java.util.List;

public interface ContextManageable {
    String getName();

    String getProfile();

    void setParent(Context parent);

    List<BeanDefinition> export();

    void importBeanDefinitions(List<BeanDefinition> bds);

    void prepare();
    void build();

    Context getContext();

    default void destroy() {

    }
}

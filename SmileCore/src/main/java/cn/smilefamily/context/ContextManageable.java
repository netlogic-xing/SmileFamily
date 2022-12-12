package cn.smilefamily.context;

import cn.smilefamily.bean.BeanDefinition;

import java.util.List;

public interface ContextManageable {
    String getName();

    String getProfile();

    void setParent(BeanContext parent);

    List<BeanDefinition> export();

    void importBeanDefinitions(List<BeanDefinition> bds);

    void build();
}

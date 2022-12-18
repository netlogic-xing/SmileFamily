package cn.smilefamily.bean;

import java.util.Collections;
import java.util.List;

public interface BeanDefinition {
    String getSource();

    default String getDescription() {
        return null;
    }
    Class<?> getType();

    boolean isExported();

    String getName();

    /**
     * 支持别名
     * @return
     */
    default List<String> getAliases() {
        return Collections.singletonList(getName());
    }

    /**
     * 用于创建bean，注入依赖，执行post construct等
     */
    default void initialize() {

    }

    /**
     * 在bean创建前执行，由于设置依赖关系等
     */
    default void preInitialize(){

    }

    Object getBeanInstance();

    /**
     * 销毁bean
     *
     * @param bean
     */
    default void destroy(Object bean) {

    }
}

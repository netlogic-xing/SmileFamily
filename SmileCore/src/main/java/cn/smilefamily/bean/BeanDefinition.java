package cn.smilefamily.bean;

public interface BeanDefinition {
    String getSource();

    default String getDescription(){
        return null;
    }
    default boolean isSingleton(){
        return true;
    }
     Class<?> getType();

    default boolean isPrototype(){
        return false;
    }

    default boolean isCustomizedScope(){
        return false;
    }

    boolean isExported();

    String getName();

    default void initialize(){

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

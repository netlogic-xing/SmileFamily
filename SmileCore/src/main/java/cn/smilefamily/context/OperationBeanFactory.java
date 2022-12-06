package cn.smilefamily.context;

import cn.smilefamily.bean.BeanDefinition;
import cn.smilefamily.bean.GeneralBeanDefinition;

import java.util.Collections;
import java.util.Date;
import java.util.function.Supplier;

public interface OperationBeanFactory extends BeanFactory{

    default void addBean(String name, Class<?> clazz, String source, Supplier<Object> factory) {
        putBean(name, clazz, factory, source);
    }
    BeanDefinition putBean(String name, Class<?> clazz, Supplier<Object> factory, String source);

    public default void addBean(Object bean, String source) {
        addBean(bean.getClass().getName(), bean, source);
    }


    public default void addBean(Object bean) {
        addBean(bean.getClass().getName(), bean, "add by user@" + new Date());
    }



    public default void addBeanAndInjectDependencies(Object bean) {
        this.addBeanAndInjectDependencies(bean.getClass().getName(), bean, "add by user@" + new Date());
    }

    public default void addBeanAndInjectDependencies(Object bean, String source) {
        this.addBeanAndInjectDependencies(bean.getClass().getName(), bean, source);
    }
    /**
     * Add bean to context directly and autowire the bean. Usually, used after context is built.
     *
     * @param name
     * @param bean
     */
    public default void addBeanAndInjectDependencies(String name, Object bean, String source) {
        BeanDefinition bd = putBean(name, bean.getClass(), () -> bean, source);
        bd.initialize();
    }

    public default void addBeanByFactory(String name, Class<?> clazz, Supplier<Object> factory) {
        putBean(name, clazz, factory, "add by user@" + new Date());
    }

    /**
     * Add bean to context directly. Used for some special objects.Usually, this bean will be autowired into other beans.
     *
     * @param name
     * @param bean
     */
    public default void addBean(String name, Object bean, String source) {
        putBean(name, bean.getClass(), () -> bean, source);
    }

    public default void addBean(String name, Object bean) {
        putBean(name, bean.getClass(), () -> bean, "add by user@" + new Date());
    }

    public default Object inject(Object bean) {
        BeanDefinition bd = new GeneralBeanDefinition(this, null, bean.getClass().getName(), bean.getClass(), null, null, Collections.emptyList(), () -> bean);
        bd.initialize();
        return bd.getBeanInstance();
    }

    public default Object create(Class<?> clazz) {
        BeanDefinition bd = new GeneralBeanDefinition(this, null, clazz.getName(), clazz);
        bd.initialize();
        return bd.getBeanInstance();
    }
}

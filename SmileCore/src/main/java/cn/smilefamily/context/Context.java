package cn.smilefamily.context;

import cn.smilefamily.aop.AdvisorDefinition;
import cn.smilefamily.bean.BeanDefinition;
import cn.smilefamily.bean.BeanDefinitionBase;

import java.lang.annotation.Annotation;
import java.util.Date;
import java.util.List;
import java.util.function.Supplier;

public interface Context extends ContextManageable, BeanFactory {
    /**
     * 用于实现aop，最终用户不需要使用此方法
     *
     * @return
     */
    public List<AdvisorDefinition> getAdvisorDefinitions();

    public List<?> getBeansByAnnotation(Class<? extends Annotation> annotation);

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
        bd.preInitialize();
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

    public default <T> T inject(T bean) {
        BeanDefinition bd = BeanDefinitionBase.create(this, bean);
        bd.preInitialize();
        return (T) bd.getBeanInstance();
    }

    public default <T> T create(Class<T> clazz) {
        BeanDefinition bd = BeanDefinitionBase.create(this, "free-bean add by user", clazz);
        bd.preInitialize();
        return (T) bd.getBeanInstance();
    }
}

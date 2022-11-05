package cn.spiderfamily.context;

import cn.spiderfamily.util.SpringUtils;
import cn.spiderfamily.BeanNotFoundException;
import cn.spiderfamily.bean.BeanDefinition;

import java.lang.annotation.Annotation;
import java.util.*;

/**
 * Bean容器类
 */
public class Context {
    /**
     * parent环境，查找bean时如果在自身没找到，就到parent找。
     */
    private Context parent;
    private Map<String, Object> beans = new HashMap<>();

    public Context(Context parent) {
        this.parent = parent;
    }

    public Context() {
    }

    public Object getBean(String name) {
        Object bean = this.beans.get(name);
        if(bean == null&&parent != null){
            bean = parent.getBean(name);
        }
        return bean;
    }

    public List<?> getBeansByAnnotation(Class<? extends Annotation> annotation) {
        List<Object> annotatedBeans = beans.values().stream().filter(o -> o.getClass().isAnnotationPresent(annotation)).toList();
        if(parent != null){
            annotatedBeans.addAll(parent.getBeansByAnnotation(annotation));
        }
        return annotatedBeans;
    }

    public void addBean(BeanDefinition bd) {
        beans.put(bd.getName(), bd.getBeanInstance());
    }

    public Object getBean(Class<?> clazz) {
        return getBean(clazz.getName());
    }

    public Object[] getBeans(Class<?>... classes) {
        List<Object> targetBeans = new ArrayList<>();
        for (Class<?> clazz : classes) {
            Object bean = getBean(clazz);
            if (bean == null) {
                throw new BeanNotFoundException(clazz.getName());
            }
            targetBeans.add(bean);
        }
        return targetBeans.toArray();
    }

    public Object[] getBeans(List<Class<?>> classes) {
        return getBeans(classes.toArray(new Class<?>[0]));
    }

    public Object[] getBeans(String... deps) {
        List<Object> targetBeans = new ArrayList<>();
        for (String dep : deps) {
            Object bean = getBean(dep);
            if (bean == null) {
                throw new BeanNotFoundException(dep);
            }
            targetBeans.add(bean);
        }
        return targetBeans.toArray();
    }
}

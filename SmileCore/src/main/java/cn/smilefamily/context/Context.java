package cn.smilefamily.context;

import cn.smilefamily.bean.Dependency;
import cn.smilefamily.util.BeanUtils;
import com.google.common.base.Strings;
import cn.smilefamily.BeanNotFoundException;
import cn.smilefamily.annotation.Bean;
import cn.smilefamily.annotation.Configuration;
import cn.smilefamily.annotation.Import;
import cn.smilefamily.bean.BeanDefinition;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.*;
import java.util.stream.Collectors;

/**
 * JavaConfig类解析器，解析配置类，生成BeanDefinition集合，并最终生成Context
 */
public class Context {
    /**
     * parent环境，查找bean时如果在自身没找到，就到parent找。
     */
    private Context parent;
    /**
     * All beans defined here
     */
    private Map<String, BeanDefinition> beanDefinitions = new HashMap<>();

    public Context(Class<?> configClass) {
        this(configClass, null, null);
    }

    public Context(Class<?> configClass, Context parent) {
        this(configClass, parent, null);
    }

    public Context(Class<?> configClass, String initProperties) {
        this(configClass, null, initProperties);
    }

    public Context(String initProperties){
        this(null,  null,  initProperties);
    }

    public Context(Class<?> configClass, Context parent, String initPropertiesFile) {
        this.parent = parent;
        if(!Strings.isNullOrEmpty(initPropertiesFile)){
            addPropertiesToContext(initPropertiesFile);
        }
        if(configClass == null){
            return;
        }
        // Add bean defined within the config class
        beanDefinitions.putAll(buildBeanDefinitionsFromConfigClass(configClass));

        Configuration configuration = configClass.getAnnotation(Configuration.class);
        if (configuration == null) {
            return;
        }
        //处理注入的属性文件
        Arrays.stream(configuration.properties()).filter(uri -> !Strings.isNullOrEmpty(uri)).forEach(uri -> {
            addPropertiesToContext(uri);
        });


        //Add bean from package
        Arrays.stream(configuration.scanPackages()).map(p -> buildBeanDefinitionsFromPackage(p)).forEach(bd -> {
            beanDefinitions.putAll(bd);
        });
        Import importConfigs = configClass.getAnnotation(Import.class);
        if (importConfigs != null) {
            // Add bean imported from imported config
            Arrays.stream(importConfigs.value()).forEach(importConfigClass -> {
                beanDefinitions.putAll(buildBeanDefinitionsFromConfigClass(importConfigClass));
            });
        }
        //Add special bean context self.
        addBean(this);
    }

    public Object getBean(String name) {
        BeanDefinition bd = this.beanDefinitions.get(name);
        if(bd == null&&parent != null){
            return parent.getBean(name);
        }
        if(bd== null){
            return null;
        }
        return bd.getBeanInstance();
    }

    public List<?> getBeansByAnnotation(Class<? extends Annotation> annotation) {
        List<Object> annotatedBeans = beanDefinitions.values().stream().map(bd->bd.getBeanInstance())
                .filter(o -> o.getClass().isAnnotationPresent(annotation)).toList();
        if(parent != null){
            annotatedBeans.addAll(parent.getBeansByAnnotation(annotation));
        }
        return annotatedBeans;
    }

    public void addBean(BeanDefinition bd) {
        bd.createInstance(this);
        beanDefinitions.put(bd.getName(), bd);
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

    public void buildContext() {
        createBeans(beanDefinitions.values().stream().toList());
        beansInject(beanDefinitions.values().stream().toList());
        beansPostConstruct(beanDefinitions.values().stream().toList());
    }

    private void createBeans(List<BeanDefinition> currentBds) {
        currentBds.forEach(bd -> {
            createBeans(bd.getDependencies().stream()
                    .filter(this::checkDependence)
                    .map(dep -> getBeanDefinition(dep.name())).toList());
            bd.createInstance(this);
        });
    }

    private boolean checkDependence(Dependency dep){
        if(beanDefinitions.containsKey(dep.name())){
            return true;
        }
        if(dep.required()){
            throw new BeanNotFoundException(dep.name());
        }
        return false;
    }

    private BeanDefinition getBeanDefinition(String name) {
        if (!beanDefinitions.containsKey(name)) {
            throw new BeanNotFoundException(name);
        }
        return beanDefinitions.get(name);
    }

    private void beansInject(List<BeanDefinition> currentBds) {
        currentBds.forEach(bd -> {
            beansInject(bd.getDependencies().stream()
                    .filter(this::checkDependence)
                    .map(dep -> getBeanDefinition(dep.name())).toList());
            bd.callAutowiredMethods(this);
        });
    }

    private void beansPostConstruct(List<BeanDefinition> currentBds) {
        currentBds.forEach(bd -> {
            beansPostConstruct(bd.getDependencies().stream()
                    .filter(this::checkDependence)
                    .map(dep -> getBeanDefinition(dep.name())).toList());
            bd.callPostConstruct();
        });
    }

    private Map<String, BeanDefinition> buildBeanDefinitionsFromPackage(String packageName) {
        return BeanUtils.findAllClassesUsingClassLoader(packageName).stream()
                .filter(c -> c.isAnnotationPresent(Bean.class))
                .map(c -> {
                    String name = c.getName();
                    Bean bean = c.getAnnotation(Bean.class);
                    if (bean != null && !bean.name().equals("")) {
                        name = bean.name();
                    }
                    return new BeanDefinition(name, c);
                })
                .collect(Collectors.toMap(b -> b.getName(), b -> b));
    }

    private void addPropertiesToContext(String propertiesFile) {
        Properties properties = new Properties();
        if (propertiesFile.startsWith("classpath:")) {
            propertiesFile = propertiesFile.substring("classpath:".length());
            try {
                properties.load(this.getClass().getResourceAsStream(propertiesFile));
                //add properties
                properties.forEach((key, val) -> {
                    addBean((String) key, val);
                });

            } catch (IOException e) {
                throw new PropertiesLoadException(e);
            }
        }
    }

    private Map<String, BeanDefinition> buildBeanDefinitionsFromConfigClass(Class<?> configClass) {
        BeanDefinition root = new BeanDefinition(configClass);
        root.createInstance(this);
        Map<String, BeanDefinition> bd = Arrays.stream(configClass.getMethods())
                .filter(m -> {
                    return m.isAnnotationPresent(Bean.class);
                })
                .map(m -> {
                    String name = m.getAnnotation(Bean.class).name();
                    if (name == null || name.equals("")) {
                        name = m.getReturnType().getName();
                    }
                    return new BeanDefinition(name, m.getReturnType(), BeanUtils.getParameterDeps(m), () -> {
                        return BeanUtils.invoke(m, root.getBeanInstance(), this.getBeans(m.getParameterTypes()));
                    });
                }).collect(Collectors.toMap(b -> b.getName(), b -> b));
        bd.put(root.getName(), root);
        return bd;
    }

    /**
     * Add bean to context directly. Used for some special objects.Usually, this bean will be autowired into other beans.
     *
     * @param name
     * @param bean
     */
    public void addBean(String name, Object bean) {
        putBean(name, bean);
    }

    private BeanDefinition putBean(String name, Object bean){
        BeanDefinition bd = new BeanDefinition(name, bean.getClass(), Collections.emptyList(), () -> bean);
        beanDefinitions.put(name, bd);
        return bd;
    }

    public void addBean(Object bean) {
        addBean(bean.getClass().getName(), bean);
    }

    /**
     * Add bean to context directly and autowire the bean. Usually, used after context is built.
     * @param name
     * @param bean
     */
    public void addBeanAndInjectDependencies(String name, Object bean){
       BeanDefinition bd = putBean(name, bean);
       bd.createInstance(this);
       bd.callAutowiredMethods(this);
       bd.callPostConstruct();
    }


    public void addBeanAndInjectDependencies(Object bean){
        this.addBeanAndInjectDependencies(bean.getClass().getName(), bean);
    }
}

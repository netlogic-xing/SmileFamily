package cn.smilefamily.context;

import cn.smilefamily.BeanInitializationException;
import cn.smilefamily.BeanNotFoundException;
import cn.smilefamily.annotation.*;
import cn.smilefamily.bean.BeanDefinition;
import cn.smilefamily.bean.GeneralBeanDefinition;
import cn.smilefamily.bean.PropertyBeanDefinition;
import cn.smilefamily.common.DelayedTaskExecutor;
import cn.smilefamily.util.BeanUtils;
import com.google.common.base.Strings;
import javassist.util.proxy.Proxy;
import javassist.util.proxy.ProxyFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.*;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * JavaConfig类解析器，解析配置类，生成BeanDefinition集合，并最终生成Context
 */
public class Context {
    private static final Logger logger = LoggerFactory.getLogger(Context.class);
    /**
     * context唯一标识，用于多个context管理
     */
    private String name = "root";
    private static String SCOPED_BEAN_CONTAINER_PREFIX = "smile.scoped.bean.container:";
    /**
     * parent环境，查找bean时如果在自身没找到，就到parent找。
     */
    private Context parent;
    /**
     * All beans defined here
     */
    private Map<String, BeanDefinition> beanDefinitions = new HashMap<>();
    private ThreadLocal<Map<String, ConcurrentMap<BeanDefinition, Object>>> threadLocalScopedBeanContainer = new ThreadLocal<>();

    private DelayedTaskExecutor propertyEvaluator;
    private boolean constructionComplete = false;

    public Context(Class<?> configClass) {
        this(configClass, null, null);
    }

    public Context(Class<?> configClass, Context parent) {
        this(configClass, parent, null);
    }

    public Context(Class<?> configClass, String initProperties) {
        this(configClass, null, initProperties);
    }

    public Context(String initProperties) {
        this(null, null, initProperties);
    }

    public String getName() {
        return name;
    }

    public void setParent(Context parent) {
        this.parent = parent;
    }

    public List<BeanDefinition> export() {
        return beanDefinitions.values().stream().filter(BeanDefinition::isExported).toList();
    }

    public void importBeanDefinitions(List<BeanDefinition> bds) {
        addBeanDefinitions(bds);
    }

    public List<BeanDefinition> getBeanDefinitions() {
        return beanDefinitions.values().stream().toList();
    }

    public Context(Class<?> configClass, Context parent, String initPropertiesFile) {
        this.parent = parent;
        propertyEvaluator = new DelayedTaskExecutor(() -> constructionComplete);
        if (!Strings.isNullOrEmpty(initPropertiesFile)) {
            addPropertiesToContext(initPropertiesFile);
        }
        if (configClass == null) {
            return;
        }
        Configuration configuration = configClass.getAnnotation(Configuration.class);

        if (configuration != null && !configuration.name().equals("")) {
            name = configuration.name();
        }

        // Add bean defined within the config class
        buildBeanDefinitionsFromConfigClass(configClass, configClass.getName() + "[" + name + "]");

        constructionComplete = true;
        propertyEvaluator.execute();
        //Add special bean context self.
        addBean(this, "Special bean");
        ContextManager.getInstance().addContext(this);
    }

    private Map<String, ConcurrentMap<BeanDefinition, Object>> getScopedContainer() {
        Map<String, ConcurrentMap<BeanDefinition, Object>> container = threadLocalScopedBeanContainer.get();
        if (container == null && parent != null) {
            container = parent.getScopedContainer();
        }
        return container;
    }

    /**
     * 获取bean唯一方式
     *
     * @param name
     * @return
     */
    public Object getBean(String name) {
        BeanDefinition bd = this.beanDefinitions.get(name);
        if (bd == null && parent != null) {
            return parent.getBean(name);
        }
        if (bd == null) {
            return null;
        }
        if (bd.isPrototype()) {
            bd.reset();
            bd.initialize();
        }
        if (bd.isSingleton()) {
            bd.initialize();
        }
        if (bd.isCustomizedScope()) {
            if (bd.getProxy() == null) {
                setProxy(bd);
            }
            return bd.getProxy();
        }
        Object beanInstance = bd.getBeanInstance();
        return beanInstance;
    }

    public Object getBean(String name, Object defaultValue) {
        Object bean = getBean(name);
        return bean == null ? defaultValue : bean;
    }

    private void setProxy(BeanDefinition bd) {
        ProxyFactory factory = new ProxyFactory();
        factory.setSuperclass(bd.getType());
        //factory.writeDirectory="./code";
        Object proxyBean = BeanUtils.newInstance(factory.createClass());
        ((Proxy) proxyBean).setHandler((self, m, proceed, args) -> {
            logger.debug("intercept " + m.getName() + "@" + bd.getName() + "@" + bd.getScope());
            ConcurrentMap container = getScopedContainer().get(SCOPED_BEAN_CONTAINER_PREFIX + bd.getScope());
            if (container == null) {
                throw new BeanInitializationException("Cannot use bean " + bd.getName() + " with scope " + bd.getScope() + ", current thread is not attached to scope " + bd.getScope());
            }
            Object target = container.computeIfAbsent(bd, key -> {
                logger.debug("====== create new real instance for " + bd.getName());
                bd.reset();
                bd.initialize();
                return bd.getBeanInstance();
            });
            return m.invoke(target, args);
        });
        bd.setProxy(proxyBean);
    }

    public void createScope(String scope, ConcurrentMap<BeanDefinition, Object> scopedContext) {
        logger.info("create " + scope + " context " + Thread.currentThread());
        Map<String, ConcurrentMap<BeanDefinition, Object>> container = threadLocalScopedBeanContainer.get();
        if (container == null) {
            container = new HashMap<>();
            threadLocalScopedBeanContainer.set(container);
        }
        container.put(SCOPED_BEAN_CONTAINER_PREFIX + scope, scopedContext);
    }

    public void destroyScope(String scope) {
        logger.info("destroy " + scope + " context");
        Map<String, ConcurrentMap<BeanDefinition, Object>> container = getScopedContainer();
        if (container == null) {
            throw new BeanInitializationException("container for " + scope + " not existed");
        }
        Map<BeanDefinition, Object> scopedContext = container.get(SCOPED_BEAN_CONTAINER_PREFIX + scope);
        if (scopedContext == null) {
            throw new BeanInitializationException("scope " + scope + " not existed yet");
        }
        scopedContext.forEach((bd, bean) -> {
            bd.destroy(bean);
        });
        container.remove(SCOPED_BEAN_CONTAINER_PREFIX + scope);
    }

    public List<?> getBeansByAnnotation(Class<? extends Annotation> annotation) {
        List<Object> annotatedBeans = new ArrayList<>(beanDefinitions.values().stream().filter(bd -> bd.getType().isAnnotationPresent(annotation)).map(bd -> getBean(bd.getName())).toList());
        if (parent != null) {
            annotatedBeans.addAll(parent.getBeansByAnnotation(annotation));
        }
        return annotatedBeans;
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

    public void build() {
        beanDefinitions.values().forEach(bd -> {
            bd.initialize();
        });
    }

    private List<BeanDefinition> buildBeanDefinitionsFromPackage(String packageName, String source) {
        return BeanUtils.findAllAnnotatedClassIn(packageName, Bean.class).stream().map(c -> {
            String name = c.getName();
            Bean bean = c.getAnnotation(Bean.class);
            if (bean != null && !bean.name().equals("")) {
                name = bean.name();
            }
            return new GeneralBeanDefinition(this, source, name, c);
        }).collect(Collectors.toList());
    }

    private void addPropertiesToContext(String propertiesFile) {
        Properties properties = new Properties();
        if (propertiesFile.startsWith("classpath:")) {
            String path = propertiesFile.substring("classpath:".length());
            try {
                properties.load(this.getClass().getResourceAsStream(path));
                //add properties
                properties.forEach((key, val) -> {
                    String keyString = (String) key;
                    String valString = (String) val;
                    if (!keyString.contains("${") && !valString.contains("${")) {
                        addProperty(keyString, valString, propertiesFile);
                        return;
                    }
                    propertyEvaluator.addFirst(keyString, () -> {
                        String realKey = BeanUtils.expression(keyString, (placeHolder, defaultVal) -> {
                            String value = System.getProperty(placeHolder, (String) getBean(placeHolder, defaultVal));
                            if (value == null) {
                                throw new BeanInitializationException("placeHolder ${" + placeHolder + "} of " + keyString + " cannot be resolved in " + propertiesFile + ".");
                            }
                            return value;
                        });
                        addProperty(realKey, propertiesFile, () -> BeanUtils.expression(valString, (name, defaultVal) -> System.getProperty(name, (String) getBean(name, defaultVal))));
                    });
                });
            } catch (IOException e) {
                throw new PropertiesLoadException(e);
            }
        }
    }

    private void addBeanDefinitions(BeanDefinition... bds) {
        addBeanDefinitions(Arrays.stream(bds).toList());
    }

    private void addBeanDefinitions(List<BeanDefinition> bds) {
        for (BeanDefinition bd : bds) {
            BeanDefinition old = beanDefinitions.put(bd.getName(), bd);
            if (old != null) {
                logger.info("Bean " + bd.getName() + " is replaced");
            }
        }
    }

    private void buildBeanDefinitionsFromConfigClass(Class<?> configClass, String source) {
        BeanDefinition configDefinition = new GeneralBeanDefinition(this, source, configClass.getName(), configClass);
        addBeanDefinitions(configDefinition);
        addBeanDefinitions(Arrays.stream(configClass.getMethods()).filter(m -> {
            return m.isAnnotationPresent(Bean.class);
        }).map(m -> {
            String name = m.getAnnotation(Bean.class).name();
            if (name == null || name.equals("")) {
                name = m.getReturnType().getName();
            }
            Scope scope = m.getAnnotation(Scope.class);
            String scopeValue = null;
            if (scope != null) {
                scopeValue = scope.value();
            }
            return new GeneralBeanDefinition(this, configClass.getName() + "." + m.getName() + "()",
                    name, m.getReturnType(), scopeValue, m.getAnnotation(Export.class), BeanUtils.getParameterDeps(m),
                    () -> BeanUtils.invoke(m, getBean(configDefinition.getName()), this.getBeans(m.getParameterTypes())));
        }).collect(Collectors.toList()));
        Configuration configuration = configClass.getAnnotation(Configuration.class);
        if (configuration != null) {
            //处理注入的属性文件
            Arrays.stream(configuration.properties()).filter(uri -> !Strings.isNullOrEmpty(uri)).forEach(uri -> {
                addPropertiesToContext(uri);
            });


            //Add bean from package
            Arrays.stream(configuration.scanPackages()).map(p -> buildBeanDefinitionsFromPackage(p, "scan " + p + " by " + configClass.getName())).forEach(bd -> {
                addBeanDefinitions(bd);
            });
            Import importConfigs = configClass.getAnnotation(Import.class);
            if (importConfigs != null) {
                // Add bean imported from imported config
                Arrays.stream(importConfigs.value()).forEach(importConfigClass -> {
                    buildBeanDefinitionsFromConfigClass(importConfigClass, "imported by " + configClass.getName());
                });
            }
        }
    }

    public void addBeanByFactory(String name, Class<?> clazz, Supplier<Object> factory) {
        putBean(name, clazz, factory, "add by user@" + new Date());
    }

    /**
     * Add bean to context directly. Used for some special objects.Usually, this bean will be autowired into other beans.
     *
     * @param name
     * @param bean
     */
    public void addBean(String name, Object bean, String source) {
        putBean(name, bean.getClass(), () -> bean, source);
    }

    private void addBean(String name, Class<?> clazz, String source, Supplier<Object> factory) {
        putBean(name, clazz, factory, source);
    }

    private void addProperty(String name, String value, String source) {
        PropertyBeanDefinition bd = new PropertyBeanDefinition(name, value, source);
        addBeanDefinitions(bd);
    }

    private void addProperty(String name, String source, Supplier<?> factory) {
        PropertyBeanDefinition bd = new PropertyBeanDefinition(name, source, factory);
        addBeanDefinitions(bd);
    }

    public void addBean(String name, Object bean) {
        putBean(name, bean.getClass(), () -> bean, "add by user@" + new Date());
    }


    private GeneralBeanDefinition putBean(String name, Class<?> clazz, Supplier<Object> factory, String source) {
        GeneralBeanDefinition bd = new GeneralBeanDefinition(this, source, name, clazz, null, clazz.getAnnotation(Export.class), Collections.emptyList(), factory);
        addBeanDefinitions(bd);
        return bd;
    }

    public void addBean(Object bean, String source) {
        addBean(bean.getClass().getName(), bean, source);
    }

    public void addBean(Object bean) {
        addBean(bean.getClass().getName(), bean, "add by user@" + new Date());
    }

    /**
     * Add bean to context directly and autowire the bean. Usually, used after context is built.
     *
     * @param name
     * @param bean
     */
    public void addBeanAndInjectDependencies(String name, Object bean, String source) {
        GeneralBeanDefinition bd = putBean(name, bean.getClass(), () -> bean, source);
        bd.initialize();
    }


    public void addBeanAndInjectDependencies(Object bean) {
        this.addBeanAndInjectDependencies(bean.getClass().getName(), bean, "add by user@" + new Date());
    }

    public void addBeanAndInjectDependencies(Object bean, String source) {
        this.addBeanAndInjectDependencies(bean.getClass().getName(), bean, source);
    }

    public Object inject(Object bean) {
        GeneralBeanDefinition bd = new GeneralBeanDefinition(this, null, bean.getClass().getName(), bean.getClass(), null, null, Collections.emptyList(), () -> bean);
        bd.initialize();
        return bd.getBeanInstance();
    }

    public Object create(Class<?> clazz) {
        GeneralBeanDefinition bd = new GeneralBeanDefinition(this, null, clazz.getName(), clazz);
        bd.initialize();
        return bd.getBeanInstance();
    }

}

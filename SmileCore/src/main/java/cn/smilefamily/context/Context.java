package cn.smilefamily.context;

import cn.smilefamily.BeanInitializationException;
import cn.smilefamily.BeanNotFoundException;
import cn.smilefamily.annotation.Bean;
import cn.smilefamily.annotation.Configuration;
import cn.smilefamily.annotation.Import;
import cn.smilefamily.annotation.Scope;
import cn.smilefamily.bean.BeanDefinition;
import cn.smilefamily.bean.Dependency;
import cn.smilefamily.util.BeanUtils;
import com.google.common.base.Strings;
import javassist.util.proxy.Proxy;
import javassist.util.proxy.ProxyFactory;
import net.sf.cglib.core.DebuggingClassWriter;
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.*;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

/**
 * JavaConfig类解析器，解析配置类，生成BeanDefinition集合，并最终生成Context
 */
public class Context {
    private static final Logger logger = LoggerFactory
            .getLogger(Context.class);
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

    public Context(Class<?> configClass, Context parent, String initPropertiesFile) {
        this.parent = parent;
        if (!Strings.isNullOrEmpty(initPropertiesFile)) {
            addPropertiesToContext(initPropertiesFile);
        }
        if (configClass == null) {
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
        return bd.getBeanInstance();
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

    private void setProxyCglib(BeanDefinition bd) {
        System.setProperty(DebuggingClassWriter.DEBUG_LOCATION_PROPERTY, "./code");
        // 通过CGLIB动态代理获取代理对象的过程
        Enhancer enhancer = new Enhancer();
        // 设置enhancer对象的父类
        enhancer.setSuperclass(bd.getType());
        // 设置enhancer的回调对象
        enhancer.setCallback((MethodInterceptor) (obj, m, args, proxy) -> {
            logger.info("----intercept " + m.getName() + "@" + bd.getName() + "@" + bd.getScope());
            ConcurrentMap container = getScopedContainer().get(SCOPED_BEAN_CONTAINER_PREFIX + bd.getScope());
            if (container == null) {
                throw new BeanInitializationException("Cannot use bean " + bd.getName() + " with scope " + bd.getScope() + ", current thread is not attached to scope " + bd.getScope());
            }
            Object target = container.computeIfAbsent(bd, key -> {
                logger.info("====== create new real instance for " + bd.getName());
                bd.reset();
                bd.initialize();
                return bd.getBeanInstance();
            });
            return m.invoke(target, args);
        });
        bd.setProxy(enhancer.create());
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
        List<Object> annotatedBeans = new ArrayList<>(beanDefinitions.values().stream()
                .filter(bd -> bd.getType().isAnnotationPresent(annotation))
                .map(bd -> getBean(bd.getName())).toList());
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

    public void build() {
        beanDefinitions.values().forEach(bd -> {
            bd.initialize();
        });
    }

    private void createBeans(List<BeanDefinition> currentBds) {
        currentBds.forEach(bd -> {
            createBeans(bd.getDependencies().stream()
                    .filter(this::checkDependence)
                    .map(dep -> getBeanDefinition(dep.name())).toList());
            bd.createInstance();
        });
    }

    private boolean checkDependence(Dependency dep) {
        if (beanDefinitions.containsKey(dep.name())) {
            return true;
        }
        if (dep.required()) {
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
            bd.injectDependencies();
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
        return BeanUtils.findAllAnnotatedClassIn(packageName, Bean.class).stream()
                .map(c -> {
                    String name = c.getName();
                    Bean bean = c.getAnnotation(Bean.class);
                    if (bean != null && !bean.name().equals("")) {
                        name = bean.name();
                    }
                    return new BeanDefinition(this, name, c);
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
        BeanDefinition root = BeanDefinition.create(this, configClass);
        root.createInstance();
        Map<String, BeanDefinition> bd = Arrays.stream(configClass.getMethods())
                .filter(m -> {
                    return m.isAnnotationPresent(Bean.class);
                })
                .map(m -> {
                    String name = m.getAnnotation(Bean.class).name();
                    if (name == null || name.equals("")) {
                        name = m.getReturnType().getName();
                    }
                    Scope scope = m.getAnnotation(Scope.class);
                    String scopeValue = null;
                    if (scope != null) {
                        scopeValue = scope.value();
                    }
                    return new BeanDefinition(this, name, m.getReturnType(), scopeValue, BeanUtils.getParameterDeps(m), () -> {
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

    private BeanDefinition putBean(String name, Object bean) {
        BeanDefinition bd = new BeanDefinition(this, name, bean.getClass(), null, Collections.emptyList(), () -> bean);
        beanDefinitions.put(name, bd);
        return bd;
    }

    public void addBean(Object bean) {
        addBean(bean.getClass().getName(), bean);
    }

    /**
     * Add bean to context directly and autowire the bean. Usually, used after context is built.
     *
     * @param name
     * @param bean
     */
    public void addBeanAndInjectDependencies(String name, Object bean) {
        BeanDefinition bd = putBean(name, bean);
        bd.initialize();
    }


    public void addBeanAndInjectDependencies(Object bean) {
        this.addBeanAndInjectDependencies(bean.getClass().getName(), bean);
    }

    public Object inject(String name, Object bean) {
        BeanDefinition bd = new BeanDefinition(this, name, bean.getClass(), null, Collections.emptyList(), () -> bean);
        bd.initialize();
        return bd.getBeanInstance();
    }

    public Object create(Class<?> clazz) {
        BeanDefinition bd = new BeanDefinition(this, clazz.getName(), clazz);
        bd.initialize();
        return bd.getBeanInstance();
    }
}

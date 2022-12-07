package cn.smilefamily.context;

import cn.smilefamily.BeanInitializationException;
import cn.smilefamily.annotation.*;
import cn.smilefamily.bean.BeanDefinition;
import cn.smilefamily.bean.GeneralBeanDefinition;
import cn.smilefamily.util.BeanUtils;
import com.google.common.base.Strings;
import javassist.util.proxy.Proxy;
import javassist.util.proxy.ProxyFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.util.*;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * JavaConfig类解析器，解析配置类，生成BeanDefinition集合，并最终生成Context
 */
public class BeanContext implements Context, ContextScopeSupportable, OperationBeanFactory {
    private static final Logger logger = LoggerFactory.getLogger(BeanContext.class);
    /**
     * context唯一标识，用于多个context管理
     */
    private String name = "root";
    private static String SCOPED_BEAN_CONTAINER_PREFIX = "smile.scoped.bean.container:";
    /**
     * parent环境，查找bean时如果在自身没找到，就到parent找。
     */
    private BeanContext parent;

    private List<Context> attachedContexts = new ArrayList<>();
    /**
     * All beans defined here
     */
    private Map<String, BeanDefinition> beanDefinitions = new HashMap<>();
    private ThreadLocal<Map<String, ConcurrentMap<BeanDefinition, Object>>> threadLocalScopedBeanContainer = new ThreadLocal<>();
    private boolean initialized;

    public BeanContext(Class<?> configClass) {
        this(configClass, null, null);
    }

    public BeanContext(Class<?> configClass, BeanContext parent) {
        this(configClass, parent, null);
    }

    public BeanContext(Class<?> configClass, String initProperties) {
        this(configClass, null, initProperties);
    }

    public BeanContext(String initProperties) {
        this(null, null, initProperties);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setParent(BeanContext parent) {
        this.parent = parent;
    }

    @Override
    public List<BeanDefinition> export() {
        return beanDefinitions.values().stream().filter(BeanDefinition::isExported).map(bd -> (BeanDefinition) bd).toList();
    }

    @Override
    public void importBeanDefinitions(List<BeanDefinition> bds) {
        addBeanDefinitions(bds);
    }

    public List<BeanDefinition> getBeanDefinitions() {
        return beanDefinitions.values().stream().map(bd -> (BeanDefinition) bd).toList();
    }

    public BeanContext(Class<?> configClass, BeanContext parent, String initPropertiesFile) {
        this.parent = parent;
        if (!Strings.isNullOrEmpty(initPropertiesFile)) {
            addPropertiesContext(initPropertiesFile);
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

        //Add special bean context self.
        addBean(this, "Special bean");
        ApplicationManager.getInstance().addContext(this);
    }

    private Map<String, ConcurrentMap<BeanDefinition, Object>> getScopedContainer() {
        Map<String, ConcurrentMap<BeanDefinition, Object>> container = threadLocalScopedBeanContainer.get();
        if (container == null && parent != null) {
            container = parent.getScopedContainer();
        }
        return container;
    }

    /**
     * 获取bean唯一方式，先找自身，再找附属的context，最后委托parent
     *
     * @param name
     * @return
     */
    @Override
    public <T> T getBean(String name) {
      return (T) getBean(name, Object.class);
    }

    @Override
    public <T> T getBean(String name, Class<T> beanClass) {
        Object bean = getBeanInThisContext(name);
        if (bean != null) {
            return (T) bean;
        }
        for (Context context : attachedContexts) {
            bean = context.getBean(name, beanClass);
            if (bean != null) {
                return (T) bean;
            }
        }
        if (parent != null) {
            return parent.getBean(name, beanClass);
        }
        return null;
    }

    private Object getBeanInThisContext(String name) {
        BeanDefinition bd = this.beanDefinitions.get(name);
        if (bd == null) {
            return null;
        }
        if (bd.isPrototype()) {
            ((GeneralBeanDefinition) bd).reset();
            bd.initialize();
        }
        if (bd.isSingleton()) {
            bd.initialize();
        }
        if (bd.isCustomizedScope()) {
            if (((GeneralBeanDefinition) bd).getProxy() == null) {
                setProxy((GeneralBeanDefinition) bd);
            }
            return ((GeneralBeanDefinition) bd).getProxy();
        }
        Object beanInstance = bd.getBeanInstance();
        return beanInstance;
    }

    private void setProxy(GeneralBeanDefinition bd) {
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

    @Override
    public void createScope(String scope, ConcurrentMap<BeanDefinition, Object> scopedContext) {
        logger.info("create " + scope + " context " + Thread.currentThread());
        Map<String, ConcurrentMap<BeanDefinition, Object>> container = threadLocalScopedBeanContainer.get();
        if (container == null) {
            container = new HashMap<>();
            threadLocalScopedBeanContainer.set(container);
        }
        container.put(SCOPED_BEAN_CONTAINER_PREFIX + scope, scopedContext);
    }

    @Override
    public void destroyScope(String scope) {
        logger.info("destroy " + scope + " context");
        Map<String, ConcurrentMap<BeanDefinition, Object>> container = getScopedContainer();
        if (container == null) {
            throw new BeanInitializationException("container for " + scope + " not existed");
        }
        Map<? extends BeanDefinition, Object> scopedContext = container.get(SCOPED_BEAN_CONTAINER_PREFIX + scope);
        if (scopedContext == null) {
            throw new BeanInitializationException("scope " + scope + " not existed yet");
        }
        scopedContext.forEach((bd, bean) -> {
            bd.destroy(bean);
        });
        container.remove(SCOPED_BEAN_CONTAINER_PREFIX + scope);
    }

    @Override
    public List<?> getBeansByAnnotation(Class<? extends Annotation> annotation) {
        List<Object> annotatedBeans = new ArrayList<>(beanDefinitions.values().stream().filter(bd -> bd.getType().isAnnotationPresent(annotation)).map(bd -> getBean(bd.getName())).toList());
        if (parent != null) {
            annotatedBeans.addAll(parent.getBeansByAnnotation(annotation));
        }
        return annotatedBeans;
    }


    @Override
    public void build() {
        if (initialized) {
            return;
        }
        beanDefinitions.values().forEach(bd -> {
            bd.initialize();
        });
        initialized = true;
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

    private void addPropertiesContext(String propertiesFile) {
        attachedContexts.add(new PropertiesContext(propertiesFile, this));
    }

    private void addBeanDefinition(BeanDefinition bd) {
        addBeanDefinitions(Collections.singletonList(bd));
    }

    private void addBeanDefinitions(List<? extends BeanDefinition> bds) {
        for (BeanDefinition bd : bds) {
            BeanDefinition old = beanDefinitions.put(bd.getName(), bd);
            if (old != null) {
                logger.info("Bean " + bd.getName() + " is replaced");
            }
        }
    }

    private void buildBeanDefinitionsFromConfigClass(Class<?> configClass, String source) {
        GeneralBeanDefinition configDefinition = GeneralBeanDefinition.create(this, source, configClass);
        addBeanDefinition(configDefinition);
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
            return new GeneralBeanDefinition(this, configClass.getName() + "." + m.getName() + "()", name, m.getReturnType(), scopeValue, m.getAnnotation(Export.class), BeanUtils.getParameterDeps(m), () -> {
                return BeanUtils.invoke(m, getBean(configDefinition.getName()), this.getBeans(m.getParameterTypes()));
            });
        }).collect(Collectors.toList()));
        Configuration configuration = configClass.getAnnotation(Configuration.class);
        if (configuration != null) {
            //处理注入的属性文件
            Arrays.stream(configuration.properties()).filter(uri -> !Strings.isNullOrEmpty(uri)).forEach(uri -> {
                addPropertiesContext(uri);
            });


            //Add bean from package
            Arrays.stream(configuration.scanPackages()).map(p -> buildBeanDefinitionsFromPackage(p, "scan " + p + " by " + configClass.getName()))
                    .forEach(bds -> {
                        addBeanDefinitions(bds);
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

    @Override
    public BeanDefinition putBean(String name, Class<?> clazz, Supplier<Object> factory, String source) {
        GeneralBeanDefinition bd = new GeneralBeanDefinition(this, source, name, clazz, null, clazz.getAnnotation(Export.class), Collections.emptyList(), factory);
        addBeanDefinition(bd);
        return bd;
    }

}

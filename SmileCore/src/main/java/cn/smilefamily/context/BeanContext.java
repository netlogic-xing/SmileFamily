package cn.smilefamily.context;

import cn.smilefamily.BeanInitializationException;
import cn.smilefamily.annotation.*;
import cn.smilefamily.bean.BeanDefinition;
import cn.smilefamily.bean.GeneralBeanDefinition;
import cn.smilefamily.common.DelayedTaskExecutor;
import cn.smilefamily.util.BeanUtils;
import cn.smilefamily.util.FileUtils;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Strings;
import javassist.util.proxy.Proxy;
import javassist.util.proxy.ProxyFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
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

    private PropertiesContext environment;
    private YamlContext configBeanFactory;

    private DelayedTaskExecutor yamlContextInitExecutor;

    private boolean readyToInitYamlContext;
    private String profile;

    //此方法仅仅用于测试，正常使用不应该设置helper
    public static void setHelper(BeanContextHelper helper) {
        BeanContext.helper = helper;
    }

    //为方便测试，把对一些静态方法的依赖封装到help类
    private static BeanContextHelper helper = new BeanContextHelper();
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

    /**
     * 正常情况下，应该从parent获取，才能保证本context能被正常初始化
     * Find active profile from:
     * 1. parent
     * 2. self environment
     *
     * @return
     */
    @Override
    public String getProfile() {
        if (profile != null) {
            return profile;
        }
        if (parent != null) {
            profile = parent.getProfile();
        }
        if (profile == null) {
            profile = environment.getProfile();
        }
        return profile;
    }

    @Override
    public void setParent(BeanContext parent) {
        this.parent = parent;
    }

    @Override
    public List<BeanDefinition> export() {
        return beanDefinitions.values().stream().filter(BeanDefinition::isExported).toList();
    }

    @Override
    public void importBeanDefinitions(List<BeanDefinition> bds) {
        addBeanDefinitions(bds);
    }

    public List<BeanDefinition> getBeanDefinitions() {
        return beanDefinitions.values().stream().toList();
    }

    public BeanContext(Class<?> configClass, BeanContext parent, String initPropertiesFile) {
        yamlContextInitExecutor = new DelayedTaskExecutor(() -> readyToInitYamlContext);
        this.parent = parent;
        if (configClass != null) {
            Configuration configuration = configClass.getAnnotation(Configuration.class);
            if (configuration != null && !configuration.name().equals("")) {
                name = configuration.name();
            }
        }
        this.environment = new PropertiesContext(this);
        this.configBeanFactory = new YamlContext(this);
        if (!Strings.isNullOrEmpty(initPropertiesFile)) {
            processConfigFile(initPropertiesFile);
        }

        if (configClass != null) {
            // Add bean defined within the config class
            buildBeanDefinitionsFromConfigClass(configClass, configClass.getName() + "[" + name + "]");

            //Add special bean context self.
            addBean(this, "Special bean");
            ApplicationManager.getInstance().addContext(this);
        }
        this.environment.initialize();
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
    public <T> T getBean(String name, Type beanType) {
        T bean = (T) getBeanInThisContext(name);
        if (bean != null) {
            return bean;
        }
        if (configBeanFactory != null) {
            bean = configBeanFactory.getBean(name, beanType);
        }
        if (bean != null) {
            return bean;
        }

        if (environment != null) {
            bean = environment.getBean(name, beanType);
        }
        if (bean != null) {
            return bean;
        }
        if (parent != null) {
            return parent.getBean(name, beanType);
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
        environment.build();
        readyToInitYamlContext = true;
        yamlContextInitExecutor.execute();
        configBeanFactory.build();
        beanDefinitions.values().forEach(bd -> {
            bd.initialize();
        });
        initialized = true;
    }

    private List<BeanDefinition> buildBeanDefinitionsFromPackage(String packageName, String source) {
        return BeanUtils.findAllAnnotatedClassIn(packageName, Bean.class).stream()
                .filter(c -> !c.isAnnotationPresent(Profile.class) || c.getAnnotation(Profile.class).value().equals(getProfile()))
                .map(c -> {
                    String name = c.getName();
                    Bean bean = c.getAnnotation(Bean.class);
                    if (bean != null && !bean.name().equals("")) {
                        name = bean.name();
                    }
                    return new GeneralBeanDefinition(this, source, name, c);
                }).collect(Collectors.toList());
    }

    private void processConfigFile(String fileURL) {
        switch (FileUtils.extensionName(fileURL.toLowerCase())) {
            case ".properties" -> {
                //先处理正常配置
                processProperties(fileURL);
                //后处理activeProfile保证activeProfile中的配置优先
                if (getProfile() == null) {
                    return;
                }
                String activeProfilePath = BeanUtils.getActiveProfilePath(fileURL, getProfile());
                processProperties(activeProfilePath);
            }
            case ".yml" -> {
                processYaml(fileURL);
                if (getProfile() == null) {
                    return;
                }
                String activeProfilePath = BeanUtils.getActiveProfilePath(fileURL, getProfile());
                processYaml(activeProfilePath);
            }
            default -> throw new BeanInitializationException("Unsupported config file :" + fileURL);
        }
    }

    private void processYaml(String fileURL) {
        Optional<JsonParser> parser = helper.buildParser(fileURL);
        if (parser.isEmpty()) {
            return;
        }
        BeanUtils.iterateYamlDocs(parser.get(), jsonNode -> {
            Map<String, String> props = BeanUtils.jsonTreeToProperties(jsonNode);
            String profile = props.get(Profile.KEY);
            if (profile == null || profile.equals(getProfile())) {
                environment.addProperties(fileURL, props);
            }
        });
        BeanUtils.closeParser(parser.get());
        JsonParser yamlParser = BeanUtils.buildExpressionSupportedParser(helper.buildParser(fileURL).get(), value -> {
            return BeanUtils.expression(value, (key, defaultVal) -> {
                String val = getBean(key);
                return val != null ? val : defaultVal == null ? key : defaultVal;
            });
        });
        yamlContextInitExecutor.addFirst(() -> {
            BeanUtils.iterateYamlDocs(yamlParser, jsonNode -> {
                JsonNode profileNode = jsonNode.at(Profile.KEY_PATH);
                if (profileNode.isMissingNode() || profileNode.asText().equals(getProfile())) {
                    configBeanFactory.addYamlDoc(fileURL, jsonNode);
                }
            });
        });
        yamlContextInitExecutor.addFirst(() -> BeanUtils.closeParser(yamlParser));
    }

    private void processProperties(String fileURL) {
        helper.propertiesFrom(fileURL).ifPresent(props -> {
            environment.addProperties(fileURL, props);
        });
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
        if (configClass.isAnnotationPresent(Profile.class) && !configClass.getAnnotation(Profile.class).value().equals(getProfile())) {
            return;
        }
        GeneralBeanDefinition configDefinition = GeneralBeanDefinition.create(this, source, configClass);
        addBeanDefinition(configDefinition);
        addBeanDefinitions(Arrays.stream(configClass.getMethods()).filter(m -> m.isAnnotationPresent(Bean.class))
                .filter(m -> !m.isAnnotationPresent(Profile.class) || m.getAnnotation(Profile.class).value().equals(getProfile()))
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
                    return new GeneralBeanDefinition(this, configClass.getName() + "." + m.getName() + "()",
                            name, m.getReturnType(), scopeValue, m.getAnnotation(Export.class), BeanUtils.getParameterDeps(m), () -> {
                        return BeanUtils.invoke(m, getBean(configDefinition.getName()), this.getBeans(m.getParameterTypes()));
                    });
                }).collect(Collectors.toList()));
        Configuration configuration = configClass.getAnnotation(Configuration.class);
        if (configuration != null) {
            //处理注入的属性文件
            Arrays.stream(configuration.files()).filter(uri -> !Strings.isNullOrEmpty(uri)).forEach(uri -> {
                processConfigFile(uri);
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

class BeanContextHelper {
    Optional<JsonParser> buildParser(String fileURL) {
        return BeanUtils.buildParser(fileURL);
    }

    Optional<Map<String, String>> propertiesFrom(String fileURL) {
        return FileUtils.propertiesFrom(fileURL);
    }
}

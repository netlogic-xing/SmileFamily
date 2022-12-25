package cn.smilefamily.context;

import cn.smilefamily.BeanInitializationException;
import cn.smilefamily.annotation.AnnotationRegistry;
import cn.smilefamily.annotation.aop.Aspect;
import cn.smilefamily.annotation.core.*;
import cn.smilefamily.annotation.event.EventListener;
import cn.smilefamily.aop.AdvisorDefinition;
import cn.smilefamily.bean.BeanDefinition;
import cn.smilefamily.bean.BeanDefinitionBase;
import cn.smilefamily.common.DelayedTaskExecutor;
import cn.smilefamily.common.dev.Trace;
import cn.smilefamily.common.dev.TraceInfo;
import cn.smilefamily.common.dev.TraceParam;
import cn.smilefamily.event.*;
import cn.smilefamily.extension.ExtensionManager;
import cn.smilefamily.util.BeanUtils;
import cn.smilefamily.util.FileUtils;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static cn.smilefamily.annotation.EnhancedAnnotatedElement.wrap;
import static cn.smilefamily.common.MiscUtils.shortName;

/**
 * JavaConfig类解析器，解析配置类，生成BeanDefinition集合，并最终生成Context
 */
public class BeanContext implements Context, ContextScopeSupportable {
    private static final Logger logger = LoggerFactory.getLogger(BeanContext.class);
    public static String SCOPED_BEAN_CONTAINER_PREFIX = "smile.scoped.bean.container:";
    /**
     * 如要更改Smile自身事件传播器，应该通过此名字定义.
     * 注意：使用自定义事件传播器要特别小心，parent的自定义事件传播器会被其子context继承。但默认的事件传播器不会。
     * 所以，如果想要用整个体系用用一个传播器则其应该定义为singleton bean，如果要各个context用自己的，则要定义为
     * prototype bean
     */
    public static String CUSTOMIZED_EVENT_MULTICASTER_NAME = "smile.customized.event.multicaster.name";
    //为方便测试，把对一些静态方法的依赖封装到help类
    private static BeanContextHelper helper = new BeanContextHelper();
    /**
     * context唯一标识，用于多个context管理
     */
    private String name = "root";
    private ConcurrentMap<String, EventChannel<?>> channels = new ConcurrentHashMap<>();
    private boolean applicationEventMulticasterReady = false;
    private DelayedTaskExecutor delayedEvenListenerAddingExecutor = new DelayedTaskExecutor(() -> applicationEventMulticasterReady);
    /**
     * parent环境，查找bean时如果在自身没找到，就到parent找。
     */
    private Context parent;

    private PropertiesContext environment;
    private YamlContext configBeanFactory;

    private DelayedTaskExecutor yamlContextInitExecutor;

    private boolean readyToInitYamlContext;
    private String profile;
    /**
     * All beans defined here
     */
    private Map<String, BeanDefinition> beanDefinitions = new HashMap<>();
    private Set<AdvisorDefinition> advisorDefinitions = new HashSet<>();
    private ThreadLocal<Map<String, ConcurrentMap<BeanDefinition, Object>>> threadLocalScopedBeanContainers = new ThreadLocal<>();
    private boolean initialized;
    private boolean prepared;

    public BeanContext(Class<?> configClass) {
        this(configClass, null, null);
    }

    public BeanContext(Class<?> configClass, Context parent) {
        this(configClass, parent, null);
    }

    public BeanContext(Class<?> configClass, String initProperties) {
        this(configClass, null, initProperties);
    }

    public BeanContext(String initProperties) {
        this(null, null, initProperties);
    }

    @Trace
    public BeanContext(Class<?> configClass, Context parent, String initPropertiesFile) {
        //加载扩展
        ExtensionManager.loadExtensions();
        yamlContextInitExecutor = new DelayedTaskExecutor("yamlContextInitExecutor", () -> readyToInitYamlContext);
        this.parent = parent;
        if (configClass != null) {
            //尽早获得config的name，因为解析properties和yml要用
            Configuration configuration = wrap(configClass).getAnnotation(Configuration.class);
            if (configuration != null && !configuration.value().equals("")) {
                name = configuration.value();
            } else {
                name = configClass.getName();
            }
        }
        this.environment = new PropertiesContext(this);
        this.configBeanFactory = new YamlContext(this);
        if (!Strings.isNullOrEmpty(initPropertiesFile)) {
            processConfigFile(initPropertiesFile);
        }

        if (configClass != null) {
            // Add bean defined within the config class
            buildBeanDefinitionsFromConfigClass(configClass);

            //ApplicationManager.getInstance().addContext(this);
        }
        this.environment.initialize();
    }

    //此方法仅仅用于测试，正常使用不应该设置helper
    public static void setHelper(BeanContextHelper helper) {
        BeanContext.helper = helper;
    }

    private void initEventMulticaster() {
        channel(ContextEvent.class, "");
        applicationEventMulticasterReady = true;
        delayedEvenListenerAddingExecutor.execute();
    }

    private ApplicationEventMulticaster findEventMulticaster(Class<?> eventClass, String channel) {
        ApplicationEventMulticaster multicaster = getBean(channel);
        if (multicaster == null) {
            multicaster = getBean(CUSTOMIZED_EVENT_MULTICASTER_NAME);
        }
        if (multicaster == null) {
            multicaster = new ContextEventMulticaster<>(eventClass, this);
        }
        return multicaster;
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
    public void setParent(Context parent) {
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

    @Override
    public boolean isPrepared() {
        return prepared;
    }

    @Override
    public boolean isInitialized() {
        return initialized;
    }

    public List<BeanDefinition> getBeanDefinitions() {
        return beanDefinitions.values().stream().toList();
    }

    @TraceInfo
    public String traceInfo() {
        return shortName(this.getClass().getName()) + "<" + name + ">";
    }

    @Override
    public ConcurrentMap<BeanDefinition, Object> getScopedBeanContainer(String scopeName) {
        Map<String, ConcurrentMap<BeanDefinition, Object>> containers = threadLocalScopedBeanContainers.get();
        if (containers == null && parent != null && parent instanceof ContextScopeSupportable supportableParent) {
            return supportableParent.getScopedBeanContainer(scopeName);
        }
        if (containers == null) {
            throw new BeanInitializationException("containers not existed");
        }
        return containers.get(SCOPED_BEAN_CONTAINER_PREFIX + scopeName);
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
    @Trace
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
        return bd.getBeanInstance();
    }


    @Override
    public void createScope(String scope, ConcurrentMap<BeanDefinition, Object> scopedContext) {
        logger.info("create " + scope + " context " + Thread.currentThread());
        Map<String, ConcurrentMap<BeanDefinition, Object>> container = threadLocalScopedBeanContainers.get();
        if (container == null) {
            container = new HashMap<>();
            threadLocalScopedBeanContainers.set(container);
        }
        container.put(SCOPED_BEAN_CONTAINER_PREFIX + scope, scopedContext);
    }

    @Override
    public void destroyScope(String scope) {
        logger.info("destroy " + scope + " context");
        Map<? extends BeanDefinition, Object> scopedContext = getScopedBeanContainer(scope);
        if (scopedContext == null) {
            throw new BeanInitializationException("scope " + scope + " not existed yet");
        }
        scopedContext.forEach((bd, bean) -> {
            bd.destroy(bean);
        });
        Map<String, ConcurrentMap<BeanDefinition, Object>> containers = threadLocalScopedBeanContainers.get();
        if (parent != null && parent instanceof ContextScopeSupportable supportableParent) {
            supportableParent.destroyScope(scope);
            return;
        }
        if (containers == null) {
            throw new BeanInitializationException("containers not existed");
        }
        containers.remove(SCOPED_BEAN_CONTAINER_PREFIX + scope);
    }

    @Override
    public List<?> getBeansByAnnotation(Class<? extends Annotation> annotation) {
        List<Object> annotatedBeans = new ArrayList<>(beanDefinitions.values().stream().filter(bd -> wrap(bd.getType()).isAnnotationPresent(annotation)).map(bd -> getBean(bd.getName())).toList());
        if (parent != null) {
            annotatedBeans.addAll(parent.getBeansByAnnotation(annotation));
        }
        return annotatedBeans;
    }

    @Override
    public List<AdvisorDefinition> getAdvisorDefinitions() {
        return advisorDefinitions.stream().toList();
    }


    @Override
    @Trace
    public void build() {
        if (initialized) {
            return;
        }
        if (!prepared) {
            prepare();
        }
        beanDefinitions.values().forEach(bd -> {
            bd.initialize();
        });
        initialized = true;
        multicastEvent(new ContextReadyEvent(this));
    }

    @Override
    public ApplicationEventMulticaster getApplicationEventMulticaster() {
        return (ApplicationEventMulticaster) channel(ContextEvent.class, "");
    }

    /**
     * 调用BeanDefinition的preInitialize方法，目的是设置aop，对于单个context，可直接调用build，在多个context的情况下，要先调用
     * prepare，以便跨context的依赖可以被正确设置（aop生效）
     */
    @Override
    public void prepare() {
        if (prepared) {
            return;
        }
        environment.build();
        readyToInitYamlContext = true;
        yamlContextInitExecutor.execute();
        configBeanFactory.build();
        beanDefinitions.values().forEach(bd -> {
            bd.preInitialize();
        });
        prepared = true;
        initEventMulticaster();
        multicastEvent(new ContextPreparedEvent(this));
    }

    private void multicastEvent(ContextEvent event) {
        if (parent != null) {
            parent.getApplicationEventMulticaster().multicastEvent(event);
        }
        getApplicationEventMulticaster().multicastEvent(event);
    }

    @Override
    public void removeEventListener(String listenerBeanName) {

    }

    @Override
    public Context getContext() {
        return this;
    }

    @Override
    public void destroy() {
        multicastEvent(new ContextPreDestroyEvent(this));
        multicastEvent(new ContextDestroyedEvent(this));
    }

    private <T> List<T> scanPackage(String packageName, Class<? extends Annotation> annClass, Function<Class<?>, T> f) {
        return BeanUtils.findAllAnnotatedClassIn(packageName, AnnotationRegistry.getSynonymousAnnotations(annClass)).stream()
                .filter(c -> !wrap(c).isAnnotationPresent(Profile.class) || wrap(c).getAnnotation(Profile.class).value().equals(getProfile()))
                .map(f).collect(Collectors.toList());
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
        yamlContextInitExecutor.enqueue(() -> {
            BeanUtils.iterateYamlDocs(yamlParser, jsonNode -> {
                JsonNode profileNode = jsonNode.at(Profile.KEY_PATH);
                if (profileNode.isMissingNode() || profileNode.asText().equals(getProfile())) {
                    configBeanFactory.addYamlDoc(fileURL, jsonNode);
                }
            });
        });
        yamlContextInitExecutor.enqueue(() -> BeanUtils.closeParser(yamlParser));
    }

    private void processProperties(String fileURL) {
        helper.propertiesFrom(fileURL).ifPresent(props -> {
            environment.addProperties(fileURL, props);
        });
    }

    private void addBeanDefinition(BeanDefinition bd) {
        addBeanDefinitions(Collections.singletonList(bd));
    }

    @Trace
    private void addBeanDefinitions(List<? extends BeanDefinition> bds) {
        for (BeanDefinition bd : bds) {
            if (isAspect(bd)) {
                advisorDefinitions.addAll(AdvisorDefinition.buildFrom(bd.getType(), bd));
            }
            if (isEventListener(bd)) {
                addEventListener(bd);
            }
            for (String name : bd.getAliases()) {
                BeanDefinition old = beanDefinitions.put(name, bd);
                if (old != null) {
                    logger.info(bd + " is replaced. It may be scanned more than once.");
                }
            }
        }
    }

    private void addEventListener(BeanDefinition bd) {
        EventListener eventListener = wrap(bd.getType()).getAnnotation(EventListener.class);
        delayedEvenListenerAddingExecutor.enqueue(() -> {
            for (ConditionEventListener conditionEventListener : ConditionEventListener.constructs(this, bd.getType(), bd.getName())) {
                channel(eventListener.type(), eventListener.channel()).addEventListener(conditionEventListener.matcher(), conditionEventListener);
            }
        });
    }

    private boolean isEventListener(BeanDefinition bd) {
        return wrap(bd.getType()).isAnnotationPresent(EventListener.class);
    }

    private boolean isAspect(BeanDefinition bd) {
        return bd.getType().isAnnotationPresent(Aspect.class);
    }

    /**
     * 递归处理config，先处理import的再处理自身的
     *
     * @param configClass
     */
    private void buildBeanDefinitionsFromConfigClass(Class<?> configClass) {
        //仅处理当前active的profile的config或未标注config
        if (wrap(configClass).isAnnotationPresent(Profile.class) && !wrap(configClass).getAnnotation(Profile.class).value().equals(getProfile())) {
            return;
        }
        Configuration configuration = wrap(configClass).getAnnotation(Configuration.class);
        String source = configClass.getName();
        if (configuration != null) {
            source = Strings.isNullOrEmpty(configClass.getName()) ? source : source + "[" + configuration.value() + "]";
            Import[] importConfigs = wrap(configClass).getAnnotationsByType(Import.class);
            // Add bean imported from imported config
            Arrays.stream(importConfigs).map(Import::value).forEach(importConfigClass -> {
                buildBeanDefinitionsFromConfigClass(importConfigClass);
            });
            //处理注入的属性文件
            PropertySource[] sources = wrap(configClass).getAnnotationsByType(PropertySource.class);
            Arrays.stream(sources).map(PropertySource::value).filter(uri -> !Strings.isNullOrEmpty(uri)).forEach(uri -> {
                processConfigFile(uri);
            });

            //扫描指定包，处理标注为@Bean的Class
            ScanBean[] scanPackages = wrap(configClass).getAnnotationsByType(ScanBean.class);
            Arrays.stream(scanPackages).map(p -> buildBeanDefinitionsFromPackage(p.value(), "scan " + p + " by " + configClass.getName()))
                    .forEach(bds -> {
                        addBeanDefinitions(bds);
                    });
        }
        BeanDefinitionBase configDefinition = BeanDefinitionBase.create(this, source, configClass);
        addBeanDefinition(configDefinition);
        addBeanDefinitions(Arrays.stream(configClass.getMethods()).filter(m -> wrap(m).isAnnotationPresent(Bean.class))
                .filter(m -> !wrap(m).isAnnotationPresent(Profile.class) || wrap(m).getAnnotation(Profile.class).value().equals(getProfile()))
                .map(m -> BeanDefinitionBase.createByMethod(this, configDefinition, m)).collect(Collectors.toList()));
    }

    private List<BeanDefinitionBase> buildBeanDefinitionsFromPackage(String packageName, String source) {
        return scanPackage(packageName, Bean.class, c -> BeanDefinitionBase.create(this, source, c));
    }

    @Override
    @Trace
    public BeanDefinition putBean(String name, @TraceParam(false) Class<?> clazz, @TraceParam(false) Supplier<Object> factory, @TraceParam(false) String source) {
        BeanDefinitionBase bd = BeanDefinitionBase.create(this, name, clazz, factory, source);
        addBeanDefinition(bd);
        return bd;
    }

    @Override
    public <E> EventChannel<E> channel(Class<E> eventClass, String channel) {
        return (EventChannel<E>) channels.computeIfAbsent(channel, key -> {
            Object listenable = getBean(key);
            if (listenable == null) {
                listenable = findEventMulticaster(eventClass, channel);
            }
            return (EventChannel<?>) listenable;
        });
    }

    @Override
    public String getChannelName() {
        return "smile.context.internal.event.channel";
    }

    @Override
    public Class<ContextEvent> getChannelType() {
        return ContextEvent.class;
    }

    @Override
    public void addEventListener(Predicate<? extends ContextEvent> matcher, ApplicationEventListener<? extends ContextEvent> applicationEventListener) {
        delayedEvenListenerAddingExecutor.enqueue(() -> getApplicationEventMulticaster().addEventListener(matcher, applicationEventListener));
    }

    @Override
    public void removeAllEventListeners() {

    }

    @Override
    public void removeEventListener(Predicate<ApplicationEventListener<? extends ContextEvent>> predicate) {

    }

    @Override
    public EventPublisher<ContextEvent> getEventPublisher() {
        return this::multicastEvent;
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

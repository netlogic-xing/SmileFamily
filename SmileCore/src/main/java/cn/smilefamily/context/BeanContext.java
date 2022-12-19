package cn.smilefamily.context;

import cn.smilefamily.BeanInitializationException;
import cn.smilefamily.annotation.AnnotationRegistry;
import cn.smilefamily.annotation.aop.Aspect;
import cn.smilefamily.annotation.aop.ScanAspect;
import cn.smilefamily.annotation.core.*;
import cn.smilefamily.aop.AdvisorDefinition;
import cn.smilefamily.bean.BeanDefinition;
import cn.smilefamily.bean.BeanDefinitionBase;
import cn.smilefamily.common.DelayedTaskExecutor;
import cn.smilefamily.common.dev.Trace;
import cn.smilefamily.common.dev.TraceInfo;
import cn.smilefamily.common.dev.TraceParam;
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
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static cn.smilefamily.annotation.EnhancedAnnotatedElement.wrap;
import static cn.smilefamily.common.MiscUtils.shortName;

/**
 * JavaConfig类解析器，解析配置类，生成BeanDefinition集合，并最终生成Context
 */
public class BeanContext implements Context, ContextScopeSupportable {
    private static final Logger logger = LoggerFactory.getLogger(BeanContext.class);
    /**
     * context唯一标识，用于多个context管理
     */
    private String name = "root";
    public static String SCOPED_BEAN_CONTAINER_PREFIX = "smile.scoped.bean.container:";
    /**
     * parent环境，查找bean时如果在自身没找到，就到parent找。
     */
    private Context parent;

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

    private List<AdvisorDefinition> advisorDefinitions = new ArrayList<>();
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



    public List<BeanDefinition> getBeanDefinitions() {
        return beanDefinitions.values().stream().toList();
    }

    @TraceInfo
    public String traceInfo() {
        return shortName(this.getClass().getName()) + "<" + name + ">";
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
            }else{
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
        return advisorDefinitions;
    }


    @Override
    @Trace
    public void build() {
        if (initialized) {
            return;
        }
        if(!prepared){
            prepare();
        }
        beanDefinitions.values().forEach(bd -> {
            bd.initialize();
        });
        initialized = true;
    }
    @Override
    public void prepare() {
        if(prepared){
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
    }
    @Override
    public Context getContext() {
        return this;
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

    @Trace
    private void addBeanDefinitions(List<? extends BeanDefinition> bds) {
        for (BeanDefinition bd : bds) {
            for (String name : bd.getAliases()) {
                BeanDefinition old = beanDefinitions.put(name, bd);
                if (old != null) {
                    logger.info(bd + " is replaced");
                }
            }
        }
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
            ScanPackage[] scanPackages = wrap(configClass).getAnnotationsByType(ScanPackage.class);
            Arrays.stream(scanPackages).map(p -> buildBeanDefinitionsFromPackage(p.value(), "scan " + p + " by " + configClass.getName()))
                    .forEach(bds -> {
                        addBeanDefinitions(bds);
                    });

            //扫描指定包，处理标注为@Aspect的class
            ScanAspect[] scanAspects = wrap(configClass).getAnnotationsByType(ScanAspect.class);
            Arrays.stream(scanAspects).map(p -> scanPackage(p.value(), Aspect.class, c -> {
                        BeanDefinitionBase bd = BeanDefinitionBase.create(this, "scan " + p + " by " + configClass.getName(), c);
                        advisorDefinitions.addAll(AdvisorDefinition.buildFrom(c, bd));
                        return bd;
                    }))
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
}

class BeanContextHelper {
    Optional<JsonParser> buildParser(String fileURL) {
        return BeanUtils.buildParser(fileURL);
    }

    Optional<Map<String, String>> propertiesFrom(String fileURL) {
        return FileUtils.propertiesFrom(fileURL);
    }
}

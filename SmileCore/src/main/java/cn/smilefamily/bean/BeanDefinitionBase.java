package cn.smilefamily.bean;

import cn.smilefamily.BeanInitializationException;
import cn.smilefamily.annotation.core.Alias;
import cn.smilefamily.annotation.core.*;
import cn.smilefamily.aop.ComposedAdvisor;
import cn.smilefamily.common.DelayedTaskExecutor;
import cn.smilefamily.common.MiscUtils;
import cn.smilefamily.common.dev.TraceInfo;
import cn.smilefamily.context.BeanContext;
import cn.smilefamily.context.Context;
import cn.smilefamily.util.BeanUtils;
import javassist.util.proxy.Proxy;
import javassist.util.proxy.ProxyFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static cn.smilefamily.annotation.EnhancedAnnotatedElement.wrap;

/**
 * Bean定义核心类。在此框架中，不管是通过扫描标注为@Bean的类还是在JavaConfig中配置的Bean，都会先生成BeanDefinition。
 */
public abstract class BeanDefinitionBase implements BeanDefinition {
    private static final Logger logger = LoggerFactory
            .getLogger(BeanDefinitionBase.class);
    private static Deque<String> dependencyStack = new ArrayDeque<>();
    private static Deque<String> debugStack = new ArrayDeque<>();

    private static DelayedTaskExecutor injectExecutor = new DelayedTaskExecutor("injection-executor", dependencyStack::isEmpty);
    private static DelayedTaskExecutor postConstructExecutor = new DelayedTaskExecutor("post-construct-executor", injectExecutor::isEmpty);
    //保持所有此Bean依赖的Bean的名字
    private final List<Dependency> dependencies = new ArrayList<>();
    protected Context context;

    private ComposedAdvisor composedAdvisor;
    //Bean名称（在context中的key）
    private String name;
    private List<String> aliases = new ArrayList<>();
    //Bean对应类型
    private Class<?> type;
    //bean定义来源，来自按个config类，那个配置文件。
    private String source;
    //是否对其他context可见
    private boolean exported;
    //对导出bean的描述
    private String description;

    private String scope;


    public String getScope() {
        return scope;
    }

    //用于生成Bean实例的工厂函数
    protected Supplier<?> factory;
    private Supplier<?> targetFactory;
    //所有标注为@Injected@Value的field
    private Map<Field, Dependency> fieldDependencies = new HashMap<>();
    //所有标注为@Autowired的方法
    private Map<Method, List<Dependency>> methodDependencies = new HashMap<>();
    //标注为@PostConstruct的方法
    private List<Method> initMethods = new ArrayList<>();
    //此BeanDefinition生成的实例（单例），一个BeanInstance不为空，表示bean实例已经生成，可以作为被依赖相注入到别的Bean。但其功能不一定
    //完备，不一定能对外提供服务（也就上程序员用）
    protected Object beanInstance;

    private Object proxyBeanInstance;
    //在aop的情况下，为真正的bean
    private AtomicReference<Object> targetInstance = new AtomicReference<>();
    //标记bean是否创建（创建但未注入，未初始化，仅可以用于建立依赖）
    private boolean beanCreated;

    //inject方法的调用是否已经进入队列
    private boolean beanInjectionPlanned;
    //标记此BeanDefinition是否已经完成了依赖注入，依赖注入完成，表示bean已经基本完备，可支持调用@PostConstruct方法，但不能保证可对外提供
    //服务
    private boolean beanInjectionCompleted;
    //标记此BeanDefinition是否执行了@PostConstruct方法，bean已经功能完备，可对外提供服务
    private boolean beanInitialized;

    private BeanDefinitionBase(Context context, String name, Class<?> clazz) {
        this.context = context;
        this.name = name;
        this.aliases.add(name);
        this.type = clazz;
    }


    @Override
    public List<String> getAliases() {
        return aliases;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return "Bean(" + name + ")\n" +
                "\tfrom: " + source;
    }

    private static String getScope(AnnotatedElement element) {
        String scope = null;
        Scope s = wrap(element).getAnnotation(Scope.class);
        if (s == null) {
            scope = Scope.Singleton;
        } else {
            scope = s.value();
        }
        return scope;
    }

    public static BeanDefinitionBase create(Context context, String name, Class<?> clazz, Supplier<Object> factory, String source) {
        String scope = getScope(clazz);
        return newInstance(context, name, clazz, scope)
                .source(source).factory(factory)
                .export(clazz).aliases(clazz)
                .build();
    }

    /**
     * free-bean
     *
     * @param context
     * @param bean
     * @return
     */
    public static BeanDefinitionBase create(Context context, Object bean) {
        return newInstance(context, "free-bean", bean.getClass(), Scope.Singleton).factory(() -> bean).build();
    }

    private BeanDefinitionBase source(String source) {
        this.source = source;
        return this;
    }

    private BeanDefinitionBase export(AnnotatedElement e) {
        Export export = wrap(e).getAnnotation(Export.class);
        this.exported = export != null;
        this.description = export != null ? export.value() : "";
        return this;
    }

    private BeanDefinitionBase extraDeps(List<Dependency> deps) {
        this.dependencies.addAll(deps);
        return this;
    }

    private BeanDefinitionBase factory(Supplier<Object> factory) {
        this.factory = factory;
        return this;
    }

    private BeanDefinitionBase scope(String scope) {
        this.scope = scope;
        return this;
    }

    private BeanDefinitionBase aliases(AnnotatedElement e) {
        Alias[] names = wrap(e).getAnnotationsByType(Alias.class);
        this.aliases.addAll(Arrays.stream(names).map(alias -> alias.value()).toList());
        return this;
    }

    private static BeanDefinitionBase newInstance(Context context, String name, Class<?> clazz, String scope) {
        BeanDefinitionBase definitionBase = switch (scope) {
            case Scope.Singleton -> new SingletonBeanDefinition(context, name, clazz);
            case Scope.Prototype -> new PrototypeBeanDefinition(context, name, clazz);
            default -> CustomizedScopedBeanDefinition.createProxy(context, name, clazz);
        };
        return definitionBase.scope(scope);
    }

    /**
     * scan package
     *
     * @param context
     * @param source
     * @param c
     * @return
     */
    public static BeanDefinitionBase create(Context context, String source, Class<?> c) {
        return newInstance(context, getName(c), c, getScope(c)).
                source(source).aliases(c).export(c).build();
    }

    private static String getName(Class<?> c) {
        String name = c.getName();
        Bean bean = wrap(c).getAnnotation(Bean.class);
        if (bean != null && !bean.value().equals("")) {
            name = bean.value();
        }
        return name;
    }

    public static BeanDefinitionBase createByMethod(Context context, BeanDefinitionBase configDefinition, Method m) {
        String name = wrap(m).getAnnotation(Bean.class).value();
        if (name == null || name.equals("")) {
            name = m.getReturnType().getName();
        }
        String scopeValue = getScope(m);
        return newInstance(context, name, m.getReturnType(), scopeValue).export(m)
                .extraDeps(BeanUtils.getParameterDeps(m))
                .source(m.getDeclaringClass().getName() + "." + m.getName() + "()")
                .factory(() -> MiscUtils.invoke(m, context.getBean(configDefinition.getName()), context.getBeans(m.getParameterTypes()))).
                aliases(m).build();
    }

    @Override
    public String getSource() {
        return source;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BeanDefinitionBase that = (BeanDefinitionBase) o;
        return context.equals(that.context) && name.equals(that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(context, name);
    }

    private static class SingletonBeanDefinition extends BeanDefinitionBase {
        private SingletonBeanDefinition(Context context, String name, Class<?> clazz) {
            super(context, name, clazz);
        }

        @Override
        public void reset() {
        }
        @Override
        protected void doPrepare() {
            initialize();
        }
    }

    private static class PrototypeBeanDefinition extends BeanDefinitionBase {
        private PrototypeBeanDefinition(Context context, String name, Class<?> clazz) {
            super(context, name, clazz);
        }
        @Override
        protected void doPrepare() {
            reset();
            initialize();
        }
    }

    private static class CustomizedScopedBeanDefinition extends BeanDefinitionBase implements Cloneable {
        private BeanDefinitionBase targetBeanDefinition;

        private CustomizedScopedBeanDefinition(Context context, String name, Class<?> clazz) {
            super(context, name, clazz);
        }

        public static BeanDefinitionBase createProxy(Context context, String name, Class<?> clazz) {

            return new CustomizedScopedBeanDefinition(context, name, clazz);
        }

        @Override
        protected BeanDefinitionBase build() {
            super.build();
            this.targetBeanDefinition = (CustomizedScopedBeanDefinition) this.clone();
            this.factory = () -> createScopedProxy();
            return this;
        }

        @Override
        protected Object clone() {
            try {
                return super.clone();
            } catch (CloneNotSupportedException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        protected void doPrepare() {
            initialize();
        }

        private Object createScopedProxy() {
            ProxyFactory factory = new ProxyFactory();
            factory.setSuperclass(getType());
            //factory.writeDirectory="./code";
            Object proxyBean = BeanUtils.newInstance(factory.createClass());
            ((Proxy) proxyBean).setHandler((self, m, proceed, args) -> {
                logger.debug("intercept " + m.getName() + "@" + getName() + "@" + getScope());
                ConcurrentMap container = ((BeanContext) context).getScopedBeanContainer(getScope());
                if (container == null) {
                    throw new BeanInitializationException("Cannot use bean " + getName() + " with scope " + getScope() + ", current thread is not attached to scope " + getScope());
                }
                Object target = container.computeIfAbsent(targetBeanDefinition, key -> {
                    logger.debug("====== create new real instance for " + getName());
                    targetBeanDefinition.reset();
                    return targetBeanDefinition.getBeanInstance();
                });
                return m.invoke(target, args);
            });
            return proxyBean;
        }
    }

    @Override
    public boolean isExported() {
        return exported;
    }

    public List<Dependency> getDependencies() {
        return dependencies;
    }

    @Override
    @TraceInfo
    public String getName() {
        return name;
    }

    @Override
    public Class<?> getType() {
        return type;
    }

    /**
     * 创建实例，但未执行注入和post construct
     */
    public void createInstance() {
        if (beanCreated) {
            return;
        }
        int stackSize = dependencyStack.size();
        if (dependencyStack.contains(name)) {
            throw new BeanInitializationException("loop dependency:" + dependencyStack.stream().collect(Collectors.joining("->")) + "->" + name);
        }
        dependencyStack.addLast(name);
        logger.debug("dependency chains " + dependencyStack.stream().collect(Collectors.joining("->")));
        if (beanInstance == null) {
            beanInstance = factory.get();
        }
        dependencyStack.removeLast();
        if (stackSize != dependencyStack.size()) {
            throw new BeanInitializationException("dependencies wrong");
        }
        beanCreated = true;
    }

    @Override
    public void initialize() {
        int deep = debugStack.size();
        debugStack.addLast(name);
        //System.out.println("INFO----:"+" ".repeat(deep * 4) + name);
        if (!beanCreated) {
            createInstance();
        }
        if (!beanInjectionPlanned) {
            injectExecutor.enqueue(name, this::injectDependencies);
            postConstructExecutor.enqueue(name, this::callPostConstruct);
            beanInjectionPlanned = true;
        }
        debugStack.removeLast();
    }
    @Override
    public Object getBeanInstance(){
        doPrepare();
        if(proxyBeanInstance != null){
            return proxyBeanInstance;
        }
        return beanInstance;
    }

    protected abstract void doPrepare();

    /**
     * 为aop做准备工作
     */
    @Override
    public void preInitialize() {
        composedAdvisor = new ComposedAdvisor(this.context.getAdvisorDefinitions().stream().filter(a -> a.accept(this)).toList());
        if(composedAdvisor.isEmpty()){
            return;
        }
        ProxyFactory factory = new ProxyFactory();
        factory.setSuperclass(getType());
        //factory.writeDirectory="./code";
        proxyBeanInstance = BeanUtils.newInstance(factory.createClass());
        ((Proxy) proxyBeanInstance).setHandler((self, m, proceed, args) -> {
            logger.debug("intercept " + m.getName() + "@" + getName() + "@" + getScope());
            if(!composedAdvisor.match(m)){
                return m.invoke(beanInstance, args);
            }
            return composedAdvisor.execute(this,beanInstance, self, proceed, m, args);
        });
        //this.targetFactory = this.factory;
        //this.factory = () -> proxyBean;
    }

    public void reset() {
        beanInstance = null;
        beanCreated = false;
        beanInjectionPlanned = false;
        beanInjectionCompleted = false;
        beanInitialized = false;
    }

    /**
     * 为所以标注为@Autowired和@Value的Field注入对应Bean然后在调用标注为@Autowired的方法
     */
    public void injectDependencies() {
        if (beanInjectionCompleted) {
            return;
        }
        fieldDependencies.forEach((f, dep) -> {
            dep.setDepValue(context, val -> {
                MiscUtils.setField(f, beanInstance, val);
            });
        });
        methodDependencies.forEach((m, deps) -> {
            MiscUtils.invoke(m, beanInstance, deps.stream().map(dep -> dep.getDepValue(context)).toArray());
        });
        beanInjectionCompleted = true;
    }

    public void callPostConstruct() {
        if (beanInitialized) {
            return;
        }
        initMethods.forEach(m -> {
            MiscUtils.invoke(m, beanInstance, BeanUtils.getParameterDeps(m).stream().map(p -> p.getDepValue(context)).toArray());
        });
        beanInitialized = true;
    }


    /**
     * 最终创建bean
     */
    protected BeanDefinitionBase build() {
        //@Injected Field依赖
        fieldDependencies = Arrays.stream(this.type.getDeclaredFields())
                .filter(f -> wrap(f).isAnnotationPresent(Injected.class))
                .collect(Collectors.toMap(f -> f, f -> {
                    Injected injected = wrap(f).getAnnotation(Injected.class);
                    External external = wrap(f).getAnnotation(External.class);
                    String desc = external == null ? "" : external.value();
                    String name = BeanUtils.getBeanName(f, f.getType().getName());
                    return new Dependency(name, f.getGenericType(), injected.required(), desc, external != null);
                }));
        //@Value Field依赖
        Map<Field, Dependency> valueFields = Arrays.stream(this.type.getDeclaredFields())
                .filter(f -> wrap(f).isAnnotationPresent(Value.class))
                .collect(Collectors.toMap(f -> f, f -> {
                    Value valueAnnotation = wrap(f).getAnnotation(Value.class);
                    String valueExpression = valueAnnotation.value();
                    External external = wrap(f).getAnnotation(External.class);
                    String desc = external == null ? "" : external.value();
                    DependencyValueExtractor extractor = ValueExtractors.getValueExtractor(f.getGenericType(), valueAnnotation);
                    return new Dependency(valueExpression, String.class, false, desc, external != null, extractor);
                }));
        fieldDependencies.putAll(valueFields);
        //@Injected方法的参数依赖
        methodDependencies = Arrays.stream(this.type.getDeclaredMethods())
                .filter(m -> wrap(m).isAnnotationPresent(Injected.class))
                .collect(Collectors.toMap(m -> m, m -> {
                    return BeanUtils.getParameterDeps(m);
                }));
        initMethods = Arrays.stream(this.type.getDeclaredMethods())
                .filter(m -> wrap(m).isAnnotationPresent(PostConstruct.class))
                .toList();

        if (factory == null) {
            Arrays.stream(this.type.getDeclaredConstructors())
                    .filter(c -> Modifier.isPublic(c.getModifiers()))
                    .filter(c -> wrap(c).isAnnotationPresent(Factory.class))
                    .findFirst()
                    .ifPresent(c -> {//如果有标注为@Factory的有参构造函数，则采用此构造函数生成bean
                        List<Dependency> deps = BeanUtils.getParameterDeps(c);
                        dependencies.addAll(deps);
                        factory = () -> MiscUtils.newInstance(c, deps.stream().map(p -> p.getDepValue(context)).toArray());
                    });
        }

        if (factory == null) {//constructor over static factory method
            Arrays.stream(this.type.getDeclaredMethods())
                    .filter(f -> Modifier.isStatic(f.getModifiers()) && Modifier.isPublic(f.getModifiers()))
                    .filter(f -> this.type.isAssignableFrom(f.getReturnType()))
                    .filter(f -> wrap(f).isAnnotationPresent(Factory.class))
                    .findFirst()
                    .ifPresent(f -> {//采用@Factory静态工厂方法生成实例
                        List<Dependency> deps = BeanUtils.getParameterDeps(f);
                        dependencies.addAll(deps);
                        factory = () -> MiscUtils.invokeStatic(f, deps.stream().map(p -> p.getDepValue(context)).toArray());
                    });
        }

        if (factory == null) {
            factory = () -> BeanUtils.newInstance(type);
        }
        dependencies.addAll(fieldDependencies.values());
        dependencies.addAll(methodDependencies.values().stream().flatMap(Collection::stream).toList());
        return this;
    }

}

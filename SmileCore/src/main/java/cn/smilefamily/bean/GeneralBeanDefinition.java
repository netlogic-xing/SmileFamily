package cn.smilefamily.bean;

import cn.smilefamily.BeanInitializationException;
import cn.smilefamily.annotation.*;
import cn.smilefamily.common.DelayedTaskExecutor;
import cn.smilefamily.context.BeanFactory;
import cn.smilefamily.util.BeanUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Bean定义核心类。在此框架中，不管是通过扫描标注为@Bean的类还是在JavaConfig中配置的Bean，都会先生成BeanDefinition。
 */
public class GeneralBeanDefinition implements BeanDefinition {
    private static final Logger logger = LoggerFactory
            .getLogger(GeneralBeanDefinition.class);
    private static Deque<String> dependencyStack = new ArrayDeque<>();
    private static Deque<String> debugStack = new ArrayDeque<>();

    private static DelayedTaskExecutor injectExecutor = new DelayedTaskExecutor("injection-executor", dependencyStack::isEmpty);
    private static DelayedTaskExecutor postConstructExecutor = new DelayedTaskExecutor("post-construct-executor", injectExecutor::isEmpty);
    //保持所有此Bean依赖的Bean的名字
    private final List<Dependency> dependencies = new ArrayList<>();
    private BeanFactory beanFactory;
    //Bean名称（在context中的key）
    private String name;
    //Bean对应类型
    private Class<?> type;
    //bean定义来源，来自按个config类，那个配置文件。
    private String source;
    //是否对其他context可见
    private boolean exported;
    //对导出bean的描述
    private String description;

    private String scope;

    private Object proxy;

    //用于生成Bean实例的工厂函数
    private Supplier<?> factory;
    //所有标注为@Injected@Value的field
    private Map<Field, Dependency> fieldDependencies = new HashMap<>();
    //所有标注为@Autowired的方法
    private Map<Method, List<Dependency>> methodDependencies = new HashMap<>();
    //标注为@PostConstruct的方法
    private List<Method> initMethods = new ArrayList<>();
    //此BeanDefinition生成的实例（单例），一个BeanInstance不为空，表示bean实例已经生成，可以作为被依赖相注入到别的Bean。但其功能不一定
    //完备，不一定能对外提供服务（也就上程序员用）
    private Object beanInstance;
    //标记bean是否创建（创建但未注入，未初始化，仅可以用于建立依赖）
    private boolean beanCreated;

    //inject方法的调用是否已经进入队列
    private boolean beanInjectionPlanned;
    //标记此BeanDefinition是否已经完成了依赖注入，依赖注入完成，表示bean已经基本完备，可支持调用@PostConstruct方法，但不能保证可对外提供
    //服务
    private boolean beanInjectionCompleted;
    //标记此BeanDefinition是否执行了@PostConstruct方法，bean已经功能完备，可对外提供服务
    private boolean beanInitialized;

    public GeneralBeanDefinition(BeanFactory beanFactory, String source, String name, Class<?> clazz) {
        this.beanFactory = beanFactory;
        this.name = name;
        this.type = clazz;
        this.source = source;
        Export export = type.getAnnotation(Export.class);
        exported = export != null;
        if (exported) {
            this.description = export.value();
        }
        Scope s = type.getAnnotation(Scope.class);
        if (s == null) {
            this.scope = Scope.Singleton;
        } else {
            this.scope = s.value();
        }
        collectDependencies();
    }

    /**
     * 用于在生成JavaConfig中@Bean标注的方法定义的Bean
     *
     * @param name        Bean名称
     * @param clazz       Bean类型
     * @param deps        生成Bean的方法参数，假定全部都能在Context中找到
     * @param factory 闭包，包裹生成Bean的方法及参数
     */
    public GeneralBeanDefinition(BeanFactory beanFactory, String source, String name, Class<?> clazz, String scope, Export export,
                                 List<Dependency> deps, Supplier<?> factory) {
        this(beanFactory, source, name, clazz);
        this.exported = export != null;
        if (this.exported) {
            this.description = export.value();
        }
        if (scope != null && !scope.equals("")) {
            this.scope = scope;
        }
        this.factory = factory;
        this.dependencies.addAll(deps);
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return "Bean(" + name + "/" + scope + ")\n" +
                "\tfrom: " + source;
    }

    public static GeneralBeanDefinition create(BeanFactory beanFactory, String source, Class<?> clazz) {
        return new GeneralBeanDefinition(beanFactory, source, clazz.getName(), clazz);
    }

    @Override
    public String getSource() {
        return source;
    }

    @Override
    public boolean isSingleton() {
        return Scope.Singleton.equals(scope);
    }

    @Override
    public boolean isPrototype() {
        return Scope.Prototype.contains(scope);
    }

    public String getScope() {
        return scope;
    }

    public Object getProxy() {
        return proxy;
    }

    public void setProxy(Object proxy) {
        this.proxy = proxy;
    }

    @Override
    public boolean isCustomizedScope() {
        return !isPrototype() && !isSingleton();
    }

    @Override
    public boolean isExported() {
        return exported;
    }

    public List<Dependency> getDependencies() {
        return dependencies;
    }

    @Override
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
        if (beanInstance == null ) {//优先采用bean工厂闭包生成Bean
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
            injectExecutor.addFirst(name, this::injectDependencies);
            postConstructExecutor.addFirst(name, this::callPostConstruct);
            beanInjectionPlanned = true;
        }
        debugStack.removeLast();
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
            dep.setDepValue(beanFactory, val -> {
                BeanUtils.setField(f, beanInstance, val);
            });
        });
        methodDependencies.forEach((m, deps) -> {
            BeanUtils.invoke(m, beanInstance, deps.stream().map(dep -> dep.getDepValue(beanFactory)).toArray());
        });
        beanInjectionCompleted = true;
    }

    public void callPostConstruct() {
        if (beanInitialized) {
            return;
        }
        initMethods.forEach(m -> {
            BeanUtils.invoke(m, beanInstance, BeanUtils.getParameterDeps(m).stream().map(p -> p.getDepValue(beanFactory)).toArray());
        });
        beanInitialized = true;
    }

    @Override
    public Object getBeanInstance() {
        return beanInstance;
    }

    /**
     * 搜集依赖性
     */
    private void collectDependencies() {
        //@Injected Field依赖
        fieldDependencies = Arrays.stream(this.type.getDeclaredFields())
                .filter(f -> f.isAnnotationPresent(Injected.class))
                .collect(Collectors.toMap(f -> f, f -> {
                    Injected injected = f.getAnnotation(Injected.class);
                    External external = f.getAnnotation(External.class);
                    String desc = external == null ? "" : external.value();
                    String name = BeanUtils.getBeanName(f, f.getType().getName());
                    return new Dependency(name, f.getType(), injected.required(), desc, external != null);
                }));
        //@Value Field依赖
        Map<Field, Dependency> valueFields = Arrays.stream(this.type.getDeclaredFields())
                .filter(f -> f.isAnnotationPresent(Value.class))
                .collect(Collectors.toMap(f -> f, f -> {
                    Value valueAnnotation = f.getAnnotation(Value.class);
                    String valueExpression = valueAnnotation.value();
                    External external = f.getAnnotation(External.class);
                    String desc = external == null ? "" : external.value();
                    return new Dependency(valueExpression, f.getType(), false, desc, external != null);
                }));
        fieldDependencies.putAll(valueFields);
        //@Injected方法的参数依赖
        methodDependencies = Arrays.stream(this.type.getDeclaredMethods())
                .filter(m -> m.isAnnotationPresent(Injected.class))
                .collect(Collectors.toMap(m -> m, m -> {
                    return BeanUtils.getParameterDeps(m);
                }));
        initMethods = Arrays.stream(this.type.getDeclaredMethods())
                .filter(m -> m.isAnnotationPresent(PostConstruct.class))
                .toList();

        if (factory == null) {
            Arrays.stream(this.type.getDeclaredConstructors())
                    .filter(c -> Modifier.isPublic(c.getModifiers()))
                    .filter(c -> c.isAnnotationPresent(Factory.class))
                    .findFirst()
                    .ifPresent(c -> {//如果有标注为@Factory的有参构造函数，则采用此构造函数生成bean
                        List<Dependency> deps = BeanUtils.getParameterDeps(c);
                        dependencies.addAll(deps);
                        factory = () -> BeanUtils.newInstance(c, deps.stream().map(p -> p.getDepValue(beanFactory)).toArray());
                    });
        }

        if (factory == null) {//constructor over static factory method
            Arrays.stream(this.type.getDeclaredMethods())
                    .filter(f -> Modifier.isStatic(f.getModifiers()) && Modifier.isPublic(f.getModifiers()))
                    .filter(f -> this.type.isAssignableFrom(f.getReturnType()))
                    .filter(f -> f.isAnnotationPresent(Factory.class))
                    .findFirst()
                    .ifPresent(f -> {//采用@Factory静态工厂方法生成实例
                        List<Dependency> deps = BeanUtils.getParameterDeps(f);
                        dependencies.addAll(deps);
                        factory = () -> BeanUtils.invokeStatic(f, deps.stream().map(p -> p.getDepValue(beanFactory)).toArray());
                    });
        }

        if (factory == null) {
            factory = () -> BeanUtils.newInstance(type);
        }
        dependencies.addAll(fieldDependencies.values());
        dependencies.addAll(methodDependencies.values().stream().flatMap(Collection::stream).toList());
    }

}

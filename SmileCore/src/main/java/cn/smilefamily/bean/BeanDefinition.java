package cn.smilefamily.bean;

import cn.smilefamily.BeanInitializationException;
import cn.smilefamily.annotation.Injected;
import cn.smilefamily.annotation.PostConstruct;
import cn.smilefamily.annotation.Scope;
import cn.smilefamily.annotation.Value;
import cn.smilefamily.common.DelayedTaskExecutor;
import cn.smilefamily.context.Context;
import cn.smilefamily.util.BeanUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Bean定义核心类。在此框架中，不管是通过扫描标注为@Bean的类还是在JavaConfig中配置的Bean，都会先生成BeanDefinition。
 */
public class BeanDefinition {
    private static final Logger logger = LoggerFactory
            .getLogger(BeanDefinition.class);
    private static Deque<String> dependencyStack = new ArrayDeque<>();
    private static Deque<String> debugStack = new ArrayDeque<>();

    private static DelayedTaskExecutor injectExecutor = new DelayedTaskExecutor("injection-executor", dependencyStack::isEmpty);
    private static DelayedTaskExecutor postConstructExecutor = new DelayedTaskExecutor("post-construct-executor", injectExecutor::isEmpty);
    private Context context;
    //保持所有此Bean依赖的Bean的名字
    private final List<Dependency> dependencies = new ArrayList<>();
    //Bean名称（在context中的key）
    private String name;
    //Bean对应类型
    private Class<?> type;

    private String scope;

    private Object proxy;

    //用于生成Bean实例的构造函数
    private Constructor constructor;
    //用于生成Bean实例的工厂函数
    private Supplier<?> beanCreator;
    //所有标注为@Autowired和@Value的field
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

    public static BeanDefinition create(Context context, Class<?> clazz) {
        return new BeanDefinition(context, clazz.getName(), clazz);
    }

    public BeanDefinition(Context context, String name, Class<?> clazz) {
        this.context = context;
        this.name = name;
        this.type = clazz;
        Scope s = type.getAnnotation(Scope.class);
        if(s == null){
            this.scope = Scope.Singleton;
        }else{
            this.scope = s.value();
        }
        collectDependencies();
    }
    public boolean isSingleton(){
        return Scope.Singleton.equals(scope);
    }

    public boolean isPrototype(){
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

    public boolean isCustomizedScope(){
        return !isPrototype()&&!isSingleton();
    }
    /**
     * 用于在生成JavaConfig中@Bean标注的方法定义的Bean
     *
     * @param name        Bean名称
     * @param clazz       Bean类型
     * @param deps        生成Bean的方法参数，假定全部都能在Context中找到
     * @param beanCreator 闭包，包裹生成Bean的方法及参数
     */
    public BeanDefinition(Context context, String name, Class<?> clazz, String scope, List<Dependency> deps, Supplier<?> beanCreator) {
        this(context, name, clazz);
        if(scope != null && !scope.equals("")){
            this.scope = scope;
        }
        this.beanCreator = beanCreator;
        this.dependencies.addAll(deps);
    }

    public List<Dependency> getDependencies() {
        return dependencies;
    }

    public String getName() {
        return name;
    }

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
        if (beanInstance == null && beanCreator != null) {//优先采用bean工厂闭包生成Bean
            beanInstance = beanCreator.get();
        }
        if (beanInstance == null && constructor != null) {//如果有标注为@Autowired的有参构造函数，则采用此构造函数生成bean
            beanInstance = BeanUtils.newInstance(constructor, BeanUtils.getParameterDeps(constructor).stream().map(p -> p.getDepValue(context)).toArray());
        }
        //默认采用无参构造函数生成bean
        if (beanInstance == null) {
            beanInstance = BeanUtils.newInstance(type);
        }
        dependencyStack.removeLast();
        if (stackSize != dependencyStack.size()) {
            throw new BeanInitializationException("dependencies wrong");
        }
        beanCreated = true;
    }

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

    public void reset(){
        beanInstance = null;
        beanCreated = false;
        beanInjectionPlanned = false;
        beanInjectionCompleted = false;
        beanInitialized = false;
    }

    /**
     * 销毁bean
      * @param bean
     */
    public void destroy(Object bean){

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
                BeanUtils.setField(f, beanInstance, val);
            });
        });
        methodDependencies.forEach((m, deps) -> {
            BeanUtils.invoke(m, beanInstance, deps.stream().map(dep -> dep.getDepValue(context)).toArray());
        });
        beanInjectionCompleted = true;
    }

    public void callPostConstruct() {
        if (beanInitialized) {
            return;
        }
        initMethods.forEach(m -> {
            BeanUtils.invoke(m, beanInstance);
        });
        beanInitialized = true;
    }

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
                    String name = BeanUtils.getBeanName(f, f.getType().getName());
                    return new Dependency(name, injected.required());
                }));
        //@Value Field依赖
        Map<Field, Dependency> valueFields = Arrays.stream(this.type.getDeclaredFields())
                .filter(f -> f.isAnnotationPresent(Value.class))
                .collect(Collectors.toMap(f -> f, f -> {
                    Value valueAnnotation = f.getAnnotation(Value.class);
                    String valueExpression = valueAnnotation.value();
                    DependencyValueExtractor extractor = ValueExtractors.getValueExtractor(f.getType(), valueAnnotation);
                    return new Dependency(valueExpression, false, extractor);
                }));
        fieldDependencies.putAll(valueFields);
        //@Autowired方法的参数依赖
        methodDependencies = Arrays.stream(this.type.getDeclaredMethods())
                .filter(m -> m.isAnnotationPresent(Injected.class))
                .collect(Collectors.toMap(m -> m, m -> {
                    return BeanUtils.getParameterDeps(m);
                }));
        initMethods = Arrays.stream(this.type.getDeclaredMethods())
                .filter(m -> m.isAnnotationPresent(PostConstruct.class))
                .toList();
        if (constructor == null) {
            Arrays.stream(this.type.getDeclaredConstructors())
                    .filter(c -> c.isAnnotationPresent(Injected.class))
                    .findFirst()
                    .ifPresent(c -> {
                        constructor = c;
                    });
        }
        dependencies.addAll(fieldDependencies.values());
        dependencies.addAll(methodDependencies.values().stream().flatMap(Collection::stream).toList());
        if (constructor != null) {
            dependencies.addAll(BeanUtils.getParameterDeps(constructor));
        }
    }

}

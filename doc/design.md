<!-- TOC -->
* [SmileIoC core concepts](#Smileioc-core-concepts)
    * [Bean lifecycle](#bean-lifecycle)
    * [Annotation marked injection](#annotation-marked-injection)
      * [Field Injection](#field-injection)
      * [Method Injection](#method-injection)
      * [Constructor Injection](#constructor-injection)
    * [Bean Construction](#bean-construction)
      * [Annotation marked construction](#annotation-marked-construction)
      * [Java config construction](#java-config-construction)
      * [Freestyle construction](#freestyle-construction)
<!-- TOC -->
# SmileIoC core concepts
### Bean lifecycle
### Annotation marked injection
#### Field Injection
```java
public class AClass{
    //injected default by class name
    @Injected
    private BClass b;
    //injected by specified name
    @Injected(name="cInstance")
    private CClass c;
}
```
#### Method Injection
```java
public class AClass{
    private BClass b;
    private CClass c;
    
    @injected //@injected in method means this method will be called after instance is created and its fields injected
    public void setB(BClass b){//injected default by parameter's type name. 
        this.b = b;
    }
    
    @injected
    public void setC(@Injected(name="cInstance")CClass c){//injected by specified name
        this.c = c;
    }
}
```
#### Constructor Injection

```java
public class AClass{
    private BClass b;
    private CClass c;
    
    @Factory//@Factory in constructor means this constructor will be called to create instance.
    public AClass(@Inject(name="bInstance")BClass b, CClass c){
        this.b = b;
        this.c = c;
    }
}
```
### Bean Construction
#### Annotation marked construction

```java
@Bean//constructs a bean with default name(class name) by default constructor
public class AClass {

}

@Bean(name="bInstance")//constructs a bean with specified name by default constructor
public class BClass {

}
@Bean //constructs a bean by unique constructor
public class CClass{
    private int port;
    private DClass d;
    public CClass(@Value int port, DClass d){//actual parameters are injected
        this.port = port;
        this.d = d;
    }
}
@Bean//constructs a bean by @Factory
public class DClass{
    private int a;
    private String name;
    private BClass b;
    
    @Factory//only one @Factory allowed for one class.
    public DClass(int a, String name, BClass b){
        this.a = a;
        this.name = name;
        this.b = b;
    }
    public DClass(BClass b){
        this.b = b;
    }
}
@Bean//constructs a bean by @Factory
public class DClass{
    private int a;
    private String name;
    private BClass b;

    @Factory//@Factory can annotate a static factory method
    public static DClass createInstance(int a, String name, BClass b){
        return new DClass(a, name, b);
    }
    public DClass (int a, String name, BClass b){
        this.a = a;
        this.name = name;
        this.b = b;
    }
    public DClass(BClass b){
        this.b = b;
    }
}
```
#### Java config construction
```java
public class AppConfig{
    @Bean
    public DClass dBean(){
        return new DClass();
    }
    @Bean
    public CClass cBean(DClass d){
        return new CClass(d);
    }
}
```
#### Freestyle construction
1. Add a specified class to bean config
```java
public class AClass{
   private String name;
   public AClass(String name){
       this.name = name;
   }
}
public class Main{
    public static void main(String[] args) {
        BeanConfig bc = new BeanConfig(AppConfig.class);
        bc.registerBean(AClass.class);//a bean can be registered even if bean config is created(beanContext not built yet)
        bc.buildContext();
        bc.registerBean(BClass.class);//a bean can be registered after beanContext built, if its dependencies were in beanContext.
    }
}
```
2. Add an object to bean beanContext. The dependencies of this object will be injected.
```java
public class CClass{
    private String name;
    @Injected
    private AClass a;
    public CClass(String name){
        this.name = name;
    }
}
public class Main{
    public static void main(String[] args) {
        Context beanContext = new Context(AppConfig.class);
        beanContext.build();
        CClass c = new CClass("test");
        beanContext.addBean(c);//default bean name is class name.
    }
}
```
3. Add a bean by a bean provider.
```java
public class Main{
    public static void main(String[] args) {
        BeanConfig bc = new BeanConfig(AppConfig.class); 
        bc.buildContext();
        CClass c = new CClass("test");
        bc.addBean("cInstance", (beanContext)->{
            return new CClass();
        });//default bean name is class name.
    }
}
```
4. create a bean by bean beanContext.
```java
public class Main{
    public static void main(String[] args) {
        BeanConfig bc = new BeanConfig(AppConfig.class); 
        bc.buildContext();
        Context beanContext = bc.getContext();
        CClass c = beanContext.createBean(new CClass()); //c will be injected but c is not in beanContext.
    }
}
```
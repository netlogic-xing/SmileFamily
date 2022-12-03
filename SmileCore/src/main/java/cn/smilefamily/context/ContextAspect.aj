package cn.smilefamily.context;

public aspect ContextAspect {
    pointcut beanInit(cn.smilefamily.annotation.Context context, Object bean):execution(public (@cn.smilefamily.annotation.Context  *).new(..))
            &&@within(cn.smilefamily.annotation.Context)
            &&target(bean)
            &&@target(context);
    //&&@target(@Bean)&&this(obj)&&@within(module);
    after(cn.smilefamily.annotation.Context context, Object bean):beanInit(context, bean){
        System.out.println(thisJoinPoint.getSourceLocation());
        System.out.println("context(" + context.value() + ")");
        ContextManager.getInstance().getContext(context.value()).inject(bean);
    }
}

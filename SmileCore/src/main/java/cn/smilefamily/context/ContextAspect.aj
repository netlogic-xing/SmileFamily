package cn.smilefamily.context;

import cn.smilefamily.annotation.core.Context;

public aspect ContextAspect {
    pointcut beanInit(Context context, Object bean):execution(public (@cn.smilefamily.annotation.core.Context  *).new(..))
            &&@within(cn.smilefamily.annotation.core.Context)
            &&target(bean)
            &&@target(context);
    //&&@target(@Bean)&&this(obj)&&@within(module);
    after(Context context, Object bean):beanInit(context, bean){
        System.out.println(thisJoinPoint.getSourceLocation());
        System.out.println("context(" + context.value() + ")");
        ApplicationManager.getInstance().getContext(context.value()).inject(bean);
    }
}

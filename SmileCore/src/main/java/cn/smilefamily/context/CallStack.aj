package cn.smilefamily.context;

import cn.smilefamily.bean.GeneralBeanDefinition;
import cn.smilefamily.common.dev.Debug;
import org.aspectj.lang.reflect.ConstructorSignature;
import org.aspectj.lang.reflect.MethodSignature;

import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Optional;

import static cn.smilefamily.common.MiscUtils.shortName;

public aspect CallStack {
    pointcut keypoint(GeneralBeanDefinition bd):execution(public * *(..))&&within(GeneralBeanDefinition)
            &&!execution(* getName())&&target(bd)&&!adviceexecution()
            &&!execution(* get*(..))
            &&!execution(* toString())
            &&!execution(* is*(..));
    pointcut keypoint2(BeanContext context):
           (execution(@cn.smilefamily.common.dev.Trace * * (..))
            ||execution(@cn.smilefamily.common.dev.Trace *.new(..)))
            &&within(BeanContext)&&target(context)
            &&!adviceexecution()
            &&!execution(* getName())
            &&!execution(* toString());

    before(GeneralBeanDefinition bd):keypoint(bd){
        Debug.enter( shortName(thisJoinPoint.getSignature().getDeclaringType().getName()) +"." + thisJoinPoint.getSignature().getName() + "[" + bd.getName() + "]");
    }
    after(GeneralBeanDefinition bd):keypoint(bd){
        Debug.leave();
        //System.out.println("end------" + " ".repeat(4*deep) + thisJoinPoint.getSignature().getName()+ " args:" + Arrays.asList(thisJoinPoint.getArgs()));
    }
    before(BeanContext context):keypoint2(context){
        Executable e;
        if(thisJoinPoint.getSignature() instanceof MethodSignature methodSignature){
           e = methodSignature.getMethod();
        }else if(thisJoinPoint.getSignature() instanceof ConstructorSignature constructorSignature) {
            e= constructorSignature.getConstructor();
        }else{
           e = null;
        }
        Debug.enter(shortName(thisJoinPoint.getSignature().getDeclaringType().getName()) +"."
                + thisJoinPoint.getSignature().getName()
                + "("
                + Debug.getTraceParams(e, thisJoinPoint.getArgs())
                + ")" + "[" + context.getName() + "]");
    }
    after(BeanContext context):keypoint2(context){
        Debug.leave();
        //System.out.println("end------" + " ".repeat(4*deep) + thisJoinPoint.getSignature().getName()+ " args:" + Arrays.asList(thisJoinPoint.getArgs()));
    }
}

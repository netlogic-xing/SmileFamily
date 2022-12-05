package cn.smilefamily.iocexample.external;

import cn.smilefamily.bean.BeanDefinition;
import cn.smilefamily.common.dev.Debug;

public aspect CallStack {
    pointcut keypoint(BeanDefinition bd):execution(public * *(..))&&within(BeanDefinition)&&!execution(* getName())&&target(bd)&&!adviceexecution()
            &&!execution(* get*(..))
            &&!execution(* toString())
            &&!execution(* is*(..));
    before(BeanDefinition bd):keypoint(bd){
        Debug.enter(thisJoinPoint.getSignature().getName() + "[" + bd.getName() + "]");
    }
    after(BeanDefinition bd):keypoint(bd){
        Debug.leave();
        //System.out.println("end------" + " ".repeat(4*deep) + thisJoinPoint.getSignature().getName()+ " args:" + Arrays.asList(thisJoinPoint.getArgs()));
    }
}

package cn.smilefamily.common.dev;
import cn.smilefamily.common.DelayedTaskExecutor;
import org.aspectj.lang.annotation.Before;

import static cn.smilefamily.common.MiscUtils.shortName;

public aspect Dev {
    pointcut keypoint(DelayedTaskExecutor executor):execution(public void execute())&&target(executor);
    before(DelayedTaskExecutor executor):keypoint(executor){
        Debug.enter(shortName(thisJoinPoint.getSignature().getDeclaringType().getName()) +"." + thisJoinPoint.getSignature().getName()+"["+executor.getName()+"]");
    }
    after(DelayedTaskExecutor executor):keypoint(executor){
        Debug.leave();
    }
}

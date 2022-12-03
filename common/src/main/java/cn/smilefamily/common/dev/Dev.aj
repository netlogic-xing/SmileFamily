package cn.smilefamily.common.dev;
import cn.smilefamily.common.DelayedTaskExecutor;
import org.aspectj.lang.annotation.Before;

public aspect Dev {
    pointcut keypoint(DelayedTaskExecutor executor):execution(public void execute())&&target(executor);
    before(DelayedTaskExecutor executor):keypoint(executor){
        Debug.enter(thisJoinPoint.getSignature().getName()+"["+executor.getName()+"]");
    }
    after(DelayedTaskExecutor executor):keypoint(executor){
        Debug.leave();
    }
}

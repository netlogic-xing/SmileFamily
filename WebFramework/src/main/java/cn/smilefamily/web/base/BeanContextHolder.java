package cn.smilefamily.web.base;

import cn.smilefamily.context.BeanFactory;

/**
 * Hold Bean BeanContext. Note: It only valid after BeanConfig is built.
 */
public class BeanContextHolder {
    private static BeanFactory context;
    public static void setContext(BeanFactory ctx){
        context = ctx;
    }

    public static BeanFactory getContext(){
        return context;
    }
}

package cn.smilefamily.web.base;

import cn.smilefamily.context.Context;

/**
 * Hold Bean Context. Note: It only valid after BeanConfig is built.
 */
public class BeanContextHolder {
    private static Context context;
    public static void setContext(Context ctx){
        context = ctx;
    }

    public static Context getContext(){
        return context;
    }
}

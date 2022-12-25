package cn.smilefamily.context;

import cn.smilefamily.bean.BeanDefinition;
import cn.smilefamily.event.ApplicationEventMulticaster;

import java.util.List;

public interface ContextManageable {
    String getName();

    String getProfile();

    void setParent(Context parent);

    List<BeanDefinition> export();

    void importBeanDefinitions(List<BeanDefinition> bds);
    boolean isPrepared();

    boolean isInitialized();

    default boolean isDestroyed(){
        return false;
    }
    void prepare();
    void build();
    ApplicationEventMulticaster getApplicationEventMulticaster();
    Context getContext();

    default void destroy() {

    }
}

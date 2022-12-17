package cn.smilefamily.context;

import org.slf4j.Logger;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * 管理多个context
 */
public class ApplicationManager {
    private static final Logger logger = getLogger(ApplicationManager.class);
    private static ApplicationManager instance = new ApplicationManager();
    private boolean initialized = false;

    private AtomicReference<ContextManageable> rootContext = new AtomicReference<>();

    private ConcurrentMap<String, ContextManageable> children = new ConcurrentHashMap<>();

    public Context getRootContext() {
        return rootContext.get().getContext();
    }

    public void addContext(ContextManageable child) {
        //默认把第一个context设置为root
        if (rootContext.compareAndSet(null, child)) {
            return;
        }
        ContextManageable old = children.putIfAbsent(child.getName(), child);
        child.setParent(rootContext.get().getContext());
        rootContext.get().importBeanDefinitions(child.export());
        if (old != null) {
            logger.info("BeanContext " + child.getName() + " is replaced.");
        }
    }

    public Context getContext(String name) {

        ContextManageable beanContext = children.get(name);
        if (beanContext == null) {
            return rootContext.get().getContext();
        }
        return beanContext.getContext();
    }

    public void setRootContext(ContextManageable rootContext) {
        //显式设置root后，原来默认root降级为child
        ContextManageable old = this.rootContext.getAndSet(rootContext);
        if (old != null) {
            addContext(old);
        }
    }

    public void start() {
        if (initialized) {
            return;
        }
        this.rootContext.get().build();
        children.values().forEach(context -> {
            context.build();
        });
        initialized = true;
    }

    public static ApplicationManager getInstance() {
        return instance;
    }

    private ApplicationManager() {
    }
}

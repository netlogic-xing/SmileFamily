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

    private AtomicReference<BeanContext> rootContext = new AtomicReference<>();

    private ConcurrentMap<String, BeanContext> children = new ConcurrentHashMap<>();

    public BeanContext getRootContext() {
        return rootContext.get();
    }

    public void addContext(BeanContext child) {
        //默认把第一个context设置为root
        if (rootContext.compareAndSet(null, child)) {
            return;
        }
        BeanContext old = children.putIfAbsent(child.getName(), child);
        child.setParent(rootContext.get());
        rootContext.get().importBeanDefinitions(child.export());
        if (old != null) {
            logger.info("BeanContext " + child.getName() + " is replaced.");
        }
    }

    public BeanContext getContext(String name) {

        BeanContext beanContext = children.get(name);
        if (beanContext == null) {
            return rootContext.get();
        }
        return beanContext;
    }

    public void setRootContext(BeanContext rootContext) {
        //显式设置root后，原来默认root降级为child
        BeanContext old = this.rootContext.getAndSet(rootContext);
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

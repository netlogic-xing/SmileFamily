package cn.smilefamily.context;

import org.slf4j.Logger;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * 管理多个context
 */
public class ContextManager {
    private static final Logger logger = getLogger(ContextManager.class);
    private static ContextManager instance = new ContextManager();

    private AtomicReference<Context> rootContext = new AtomicReference<>();

    private ConcurrentMap<String, Context> children = new ConcurrentHashMap<>();

    public Context getRootContext() {
        return rootContext.get();
    }

    public void addContext(Context child) {
        //默认把第一个context设置为root
        if(rootContext.compareAndSet(null, child)){
            return;
        }
        Context old = children.putIfAbsent(child.getName(), child);
        child.setParent(rootContext.get());
        rootContext.get().importBeanDefinitions(child.export());
        if (old != null) {
            logger.info("Context " + child.getName() + " is replaced.");
        }
    }

    public Context getContext(String name) {
        return children.get(name);
    }

    public void setRootContext(Context rootContext) {
        //显式设置root后，原来默认root降级为child
         Context old = this.rootContext.getAndSet(rootContext);
         if(old != null){
             addContext(old);
         }
    }

    public static ContextManager getInstance() {
        return instance;
    }

    private ContextManager() {
    }
}

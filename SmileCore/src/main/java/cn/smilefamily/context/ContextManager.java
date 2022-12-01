package cn.smilefamily.context;

import org.slf4j.Logger;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * 管理多个context
 */
public class ContextManager {
    private static final Logger logger = getLogger(ContextManager.class);
    private static ContextManager instance = new ContextManager();

    private Context rootContext;

    private ConcurrentMap<String, Context> children = new ConcurrentHashMap<>();

    public Context getRootContext() {
        return rootContext;
    }

    public void addChildContext(Context child) {
        Context old = children.putIfAbsent(child.getName(), child);
        child.setParent(rootContext);
        rootContext.importBeanDefinitions(child.export());
        if (old != null) {
            logger.info("Context " + child.getName() + " is replaced.");
        }
    }

    public Context getContext(String name) {
        return children.get(name);
    }

    public void setRootContext(Context rootContext) {
        this.rootContext = rootContext;
    }

    public static ContextManager getInstance() {
        return instance;
    }

    private ContextManager() {
    }
}

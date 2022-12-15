package cn.smilefamily.extension;

import cn.smilefamily.util.BeanUtils;
import cn.smilefamily.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class ExtensionManager {
    private static final Logger logger = LoggerFactory
            .getLogger(ExtensionManager.class);
    private static String extensionConfigFile = "classpath:extension.properties";
    private static final String EXT_CONFIG_KEY = "smile.core.extension.config";

    private static ConcurrentMap<String, Extension> extensionRegistry = new ConcurrentHashMap<>();
    private static boolean loaded = false;

    public static Extension getExtension(String name){
        return extensionRegistry.get(name);
    }

    public synchronized static void loadExtensions() {
        if (loaded) {
            return;
        }
        extensionConfigFile = System.getProperty(EXT_CONFIG_KEY, extensionConfigFile);
        FileUtils.propertiesFrom(extensionConfigFile).ifPresent(extensions -> {
            extensions.forEach((extension, impl) -> {
                logger.info("load " + extension + ", implementation: " + impl);
                try {
                    Class<?> extensionClass = BeanUtils.loadClass(impl);
                    Extension extensionInstance = (Extension) BeanUtils.newInstance(extensionClass);
                    extensionInstance.load();
                    extensionRegistry.put(extensionInstance.name(), extensionInstance);
                } catch (ClassNotFoundException e) {
                    throw new LoadExtensionException(e);
                }
            });
        });
        loaded = true;
    }
}

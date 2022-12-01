package cn.smilefamily.context;

import cn.smilefamily.BeanInitializationException;
import cn.smilefamily.util.BeanUtils;
import javassist.util.proxy.Proxy;
import javassist.util.proxy.ProxyFactory;

import java.util.concurrent.ConcurrentMap;

public class Main {
    public static void main(String[] args) {
        ProxyFactory factory = new ProxyFactory();
        factory.setSuperclass(A.class);
        A a = new A("test");
        Object proxyBean = BeanUtils.newInstance(factory.createClass());
        ((Proxy) proxyBean).setHandler((self, m, proceed, margs) -> {
            System.out.println("intercept " + m.getName());
            return m.invoke(a, margs);
        });

        A aa = (A) proxyBean;

        System.out.println(aa.getAge());
        aa.setAge(2);
        aa.show();
        aa.x=12;
        System.out.println(aa.x);
    }
}

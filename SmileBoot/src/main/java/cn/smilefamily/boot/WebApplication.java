package cn.smilefamily.boot;

import cn.smilefamily.bean.BeanDefinition;
import cn.smilefamily.web.annotation.Request;
import cn.smilefamily.web.annotation.Session;
import cn.smilefamily.web.base.DispatchServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletRequestEvent;
import javax.servlet.ServletRequestListener;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * "Springboot"程序启动类基类
 */
public class WebApplication extends Application {
    private static final Logger logger = LoggerFactory
            .getLogger(WebApplication.class);
    private JettyServer jettyServer;

    public WebApplication() {
        jettyServer = new JettyServer();
        this.getApplicationContext().addBeanAndInjectDependencies(jettyServer, "web server start");
        jettyServer.setApplicationContent(this.getApplicationContext());
        jettyServer.register("/*", DispatchServlet.class);
        jettyServer.addEventListener(new ServletRequestListener() {
            @Override
            public void requestDestroyed(ServletRequestEvent sre) {
                getApplicationContext().destroyScope(Request.REQUEST);
            }

            @Override
            public void requestInitialized(ServletRequestEvent sre) {
                logger.info("request create @" + Thread.currentThread() + "@" + sre.getServletRequest());
                getApplicationContext().createScope(Request.REQUEST, new ConcurrentHashMap<>());
                HttpServletRequest request = (HttpServletRequest) sre.getServletRequest();
                HttpSession session = request.getSession();//force create session to trigger session created event.
                ConcurrentMap<BeanDefinition, Object> sessionScopedContext = (ConcurrentMap<BeanDefinition, Object>) session.getAttribute("smile.session.scope.context");
                if (sessionScopedContext == null) {
                    sessionScopedContext = new ConcurrentHashMap<>();
                    session.setAttribute("smile.session.scope.context", sessionScopedContext);
                }
                getApplicationContext().createScope(Session.SESSION, sessionScopedContext);
            }
        });
        jettyServer.addEventListener(new HttpSessionListener() {
            @Override
            public void sessionCreated(HttpSessionEvent se) {
//                HttpSession session = se.getSession();
//                ConcurrentMap<BeanDefinitionBase, Object> sessionScopedContext  = new ConcurrentHashMap<>();
//                session.setAttribute("smile.session.scope.context", sessionScopedContext);
//                getApplicationContext().createScope("session", sessionScopedContext);
            }

            @Override
            public void sessionDestroyed(HttpSessionEvent se) {
                getApplicationContext().destroyScope(Session.SESSION);
            }
        });
    }

    public void start() {
        try {
            jettyServer.start();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

package cn.smilefamily.boot;

import cn.smilefamily.annotation.Value;
import cn.smilefamily.context.Context;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;

import javax.servlet.http.HttpServlet;
import java.util.EventListener;

/**
 * 演示性嵌入式web服务器
 */
public class JettyServer {
    private Server server;
    private ServletContextHandler servletHandler = new ServletContextHandler(ServletContextHandler.SESSIONS);
    @Value("${spring.server.port}")
    private int port = 8080;

    public void start() throws Exception {
        server = new Server();
        ServerConnector connector = new ServerConnector(server);
        connector.setPort(port);
        server.setConnectors(new Connector[]{connector});
        servletHandler.setContextPath("/");
        servletHandler.setResourceBase(this.getClass().getResource("/").getPath());
        server.setHandler(servletHandler);
        server.start();
    }
    public void setApplicationContent(Context context){
        servletHandler.getServletContext().setAttribute(Context.class.getName(), context);
    }
    public void addEventListener(EventListener listener) {
        servletHandler.addEventListener(listener);
    }

    public void register(String mapping, Class<? extends HttpServlet> servletClass) {
        servletHandler.addServlet(servletClass, mapping);
    }
}

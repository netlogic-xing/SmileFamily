package cn.smilefamily.web.view;

import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ServletContextTemplateResolver;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

public class ThymeleafViewRenderer extends TemplateViewRenderer{
    private ServletContextTemplateResolver templateResolver;
    private TemplateEngine templateEngine;

    public ThymeleafViewRenderer(ServletContext servletContext) {
        this.templateResolver = new ServletContextTemplateResolver(servletContext);
        this.templateResolver.setTemplateMode(TemplateMode.HTML);
        this.templateResolver.setPrefix("/templates");
        this.templateResolver.setSuffix(".html");
        this.templateResolver.setCacheTTLMs(Long.valueOf(1*60*60*1000));
        this.templateResolver.setCacheable(true);
        this.templateEngine = new TemplateEngine();
        this.templateEngine.setTemplateResolver(this.templateResolver);
    }

    @Override
    public void render(String viewPath, HttpServletRequest req, HttpServletResponse resp, Object model) {
        try {
            WebContext webContext = new WebContext(req, resp, req.getServletContext(), req.getLocale(), (Map<String, Object>) model);
            templateEngine.process(viewPath, webContext, resp.getWriter());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}

package cn.spiderfamily.web.view;

import cn.spiderfamily.web.base.BeanContextHolder;
import cn.spiderfamily.web.base.ViewRenderer;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class TemplateViewRenderer implements ViewRenderer {
    private String defaultViewType = "thymeleaf";
    @Override
    public void render(String viewPath, HttpServletRequest req, HttpServletResponse resp, Object model) {
        try {
            ViewRenderer viewRenderer = (ViewRenderer) BeanContextHolder.getContext().getBean(ThymeleafViewRenderer.class);
            viewRenderer.render(viewPath, req, resp, model);
        } catch (Exception e) {
            throw new ViewRenderException(e);
        }
    }
}

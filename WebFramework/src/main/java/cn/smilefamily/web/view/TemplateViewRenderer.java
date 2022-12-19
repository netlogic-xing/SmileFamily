package cn.smilefamily.web.view;

import cn.smilefamily.context.ApplicationManager;
import cn.smilefamily.web.base.ViewRenderer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class TemplateViewRenderer implements ViewRenderer {
    private String defaultViewType = "thymeleaf";

    @Override
    public void render(String viewPath, HttpServletRequest req, HttpServletResponse resp, Object model) {
        try {
            ViewRenderer viewRenderer = ApplicationManager.getInstance().getRootContext().getBean(ThymeleafViewRenderer.class);
            viewRenderer.render(viewPath, req, resp, model);
        } catch (Exception e) {
            throw new ViewRenderException(e);
        }
    }
}

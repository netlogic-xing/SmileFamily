package cn.smilefamily.web.view;

import cn.smilefamily.web.base.ViewRenderer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class PlainViewRenderer implements ViewRenderer {
    @Override
    public void render(String viewPath, HttpServletRequest req, HttpServletResponse resp, Object model) {
        resp.setContentType("text/plain");
        try {
            resp.getWriter().write(model.toString());
        } catch (IOException e) {
            throw new ViewRenderException(e);
        }
    }
}

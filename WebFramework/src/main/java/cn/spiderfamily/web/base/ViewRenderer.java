package cn.spiderfamily.web.base;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

public interface ViewRenderer {
    public  void render(String viewPath, HttpServletRequest req, HttpServletResponse resp, Object model);

}

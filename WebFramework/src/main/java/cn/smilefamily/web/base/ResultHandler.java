package cn.smilefamily.web.base;

import com.google.common.base.Strings;
import cn.smilefamily.web.annotation.Model;
import cn.smilefamily.web.annotation.RequestMapping;
import cn.smilefamily.web.annotation.ResponseBody;
import cn.smilefamily.web.view.JsonViewRenderer;
import cn.smilefamily.web.view.PlainViewRenderer;
import cn.smilefamily.web.view.TemplateViewRenderer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Map;

import static cn.smilefamily.annotation.EnhancedAnnotatedElement.wrap;

public class ResultHandler {
    private String encoding;
    private ViewRenderer renderer;
    private boolean viewPath;
    private String fixedViewPath;
    private boolean model;
    private boolean modelAndView;
    private boolean plain;
    private boolean json;


    public ResultHandler(Method method) {
         this.encoding = wrap(method).getAnnotation(RequestMapping.class).encoding();
        //返回类型为String且未以ResponseBody标注,表示返回的是view的路径,没有model
        if (String.class.isAssignableFrom(method.getReturnType())
                && !wrap(method).isAnnotationPresent(ResponseBody.class)) {
            renderer = new TemplateViewRenderer();
            viewPath = true;
            return;
        }
        //返回类型为Map，并且以Model标注
        if (wrap(method).isAnnotationPresent(Model.class)
                && Map.class.isAssignableFrom(method.getReturnType())) {
            String path = method.getName();
            Model model = wrap(method).getAnnotation(Model.class);
            path = Strings.isNullOrEmpty(model.view()) ? path : model.view();
            renderer = new TemplateViewRenderer();
            this.fixedViewPath = path;
            this.model = true;
            return;
        }
        //返回类型为ModelAndView
        if (ModelAndView.class.isAssignableFrom(method.getReturnType())) {
            renderer = new TemplateViewRenderer();
            this.modelAndView = true;
            return;
        }

        ResponseBody responseBody = wrap(method).getAnnotation(ResponseBody.class);
        //基础类型和字符串默认为text/plain,除非显式指定为ResponseBody(contentType="application/json")
        if (method.getReturnType().isPrimitive() || String.class.isAssignableFrom(method.getReturnType())) {
            if (responseBody != null && responseBody.contentType().equals("text/plain")) {
                renderer = new PlainViewRenderer();
                this.plain = true;
            } else {
                renderer = new JsonViewRenderer();
                this.json = true;
            }
            return;
        }
        //对象类型默认为application/json为，除非显式指定为ResponseBody(contentType="text/plain")
        if (responseBody == null || responseBody.contentType().equals("application/json")) {
            renderer = new JsonViewRenderer();
            this.json = true;
            return;
        }
        if (responseBody != null && responseBody.contentType().equals("text/plain")) {
            renderer = new PlainViewRenderer();
            this.plain = true;
        }
    }

    public void handle(HttpServletRequest req, HttpServletResponse resp, Object result) {
        resp.setCharacterEncoding(encoding);
        if(viewPath){
            renderer.render((String)result, req, resp, Collections.emptyMap());
            return;
        }
        if(model){
            renderer.render(fixedViewPath, req, resp, result);
            return;
        }
        if(modelAndView){
            ModelAndView mav = (ModelAndView) result;
            renderer.render(mav.viewPath(),  req, resp, mav.model());
            return;
        }
        if(plain||json){
            renderer.render(null, req, resp, result);
        }
    }
}

package cn.smilefamily.web.base;

import java.util.HashMap;
import java.util.Map;

public record ModelAndView(String viewPath, Map<String, Object> model) {
    public ModelAndView (String viewPath){
        this(viewPath, new HashMap<>());
    }

    public void addAttribute(String name, Object value){
        this.model.put(name, value);
    }
}

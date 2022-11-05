package cn.spiderfamily.web.base;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import cn.spiderfamily.web.ParseParameterException;
import cn.spiderfamily.web.RequestHandleException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;

public class RequestHandler {
    private Method method;
    private Object target;
    private ObjectMapper objectMapper = new ObjectMapper();
    private List<ParameterValueHolder> parameterValueHolders;

    private ResultHandler resultHandler;

    public RequestHandler(Object target, Method method) {
        this.method = method;
        this.target = target;
        this.resultHandler = new ResultHandler(method);

        parameterValueHolders = Arrays.stream(method.getParameters()).map(ParameterValueHolder::new).toList();
    }


    public void handle(HttpServletRequest req, HttpServletResponse resp, Matcher matcher) {
        try {
            parseParameters(req, resp, matcher);
            Object result = method.invoke(target, parameterValueHolders.stream().map(p -> p.getValue()).toArray());
            resultHandler.handle(req, resp, result);
        } catch (IllegalAccessException e) {
            throw new RequestHandleException(e);
        } catch (InvocationTargetException e) {
            throw new RequestHandleException(e);
        }
    }

    private void parseParameters(HttpServletRequest req, HttpServletResponse resp, Matcher matcher) {
        parameterValueHolders.stream().filter(p -> p.isRequest()).forEach(p -> {
            p.setValue(req);
        });
        parameterValueHolders.stream().filter(p -> p.isResponse()).forEach(p -> {
            p.setValue(resp);
        });
        parameterValueHolders.stream().filter(p -> p.isPathVariable()).forEach(p -> {
            p.setValue(matcher.group(p.getName()));
        });
        parameterValueHolders.stream().filter(p -> p.isRequestParameter()).forEach(p -> {
            p.setValue(req.getHeader(p.getName()));
        });
        parameterValueHolders.stream().filter(p -> p.isRequestParameter()).forEach(p -> {
            p.setValue(req.getParameter(p.getName()));
        });
        if (parameterValueHolders.stream().noneMatch(p -> p.isBodyAttribute() || p.isRequestBody())){
            return;
        }
        JsonNode root;
        try {
            root =objectMapper.readTree(req.getInputStream());
        } catch (IOException e) {
            throw new ParseParameterException(e);
        }
        parameterValueHolders.stream().filter(p -> p.isBodyAttribute()).forEach(p -> {
            try {
                p.setValue(objectMapper.treeToValue(root.get(p.getName()),objectMapper.constructType(p.getType())));
            } catch (JsonProcessingException e) {
                throw new ParseParameterException("Cannot parse " + p.getName() + " from request.", e);
            }
        });
        //@RequestBody 仅允许有一个
        parameterValueHolders.stream().filter(p -> p.isRequestBody()).findFirst().ifPresent(p -> {
            try {
                p.setValue(objectMapper.readValue(req.getInputStream(), objectMapper.constructType(p.getType())));
            } catch (Exception e) {
                throw new ParseParameterException("Cannot parse " + p.getName() + " from request.", e);
            }
        });
    }
}

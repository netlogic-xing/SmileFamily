package cn.smilefamily.web.base;

import cn.smilefamily.web.annotation.*;
import com.google.common.base.Strings;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;

//private ConcurrentMap<> exceptionHandlers;
class ParameterValueHolder {
    private Type type;
    private boolean primitive;
    private boolean array;
    private boolean collection;
    private boolean list;
    private boolean set;
    private boolean map;
    private Object value;
    private String name;
    private boolean pathVariable;
    private boolean requestParameter;
    private boolean requestHeader;
    private boolean requestBody;
    private boolean bodyAttribute;
    private boolean isRequest;
    private boolean isResponse;

    public ParameterValueHolder(Parameter p) {
        this.type = p.getParameterizedType();
        this.name = p.getName();
        if (p.isAnnotationPresent(PathVariable.class)) {
            checkArgument(p.getType().isPrimitive() && p.getType() != Void.class,
                    "@PathVariable only supports primitive type. Unsupported type: " + p.getType());
            this.setPathVariable(true);
            this.setPrimitive(true);
            String val = p.getAnnotation(PathVariable.class).value();
            name = Strings.isNullOrEmpty(val) ? val : name;
            return;
        }

        if (p.isAnnotationPresent(RequestHeader.class)) {
            checkArgument(p.getType().isPrimitive() && p.getType() != Void.class,
                    "@RequestHeader only supports primitive type. Unsupported type: " + p.getType());
            this.setRequestHeader(true);
            this.setPrimitive(true);
            String val = p.getAnnotation(RequestHeader.class).value();
            name = Strings.isNullOrEmpty(val) ? val : name;
            return;
        }

        if (HttpServletRequest.class.isAssignableFrom(p.getType())) {
            this.setRequest(true);
            return;
        }
        if (HttpServletResponse.class.isAssignableFrom(p.getType())) {
            this.setResponse(true);
            return;
        }

        if (p.isAnnotationPresent(RequestBody.class)) {
            checkArgument(!p.getType().isPrimitive() && !p.getType().isArray(),
                    "@RequestBody doesn't support array and primitives.");
            this.setRequestBody(true);
            this.setList(List.class.isAssignableFrom(p.getType()));
            this.setSet(Set.class.isAssignableFrom(p.getType()));
            this.setMap(Map.class.isAssignableFrom(p.getType()));
            this.setCollection(Collection.class.isAssignableFrom(p.getType()));
            return;
        }
        if (p.isAnnotationPresent(BodyAttribute.class)) {
            checkArgument(!p.getType().isArray(),
                    "@BodyAttribute doesn't support array.");
            this.setBodyAttribute(true);
            this.setPrimitive(p.getType().isPrimitive());
            this.setList(List.class.isAssignableFrom(p.getType()));
            this.setSet(Set.class.isAssignableFrom(p.getType()));
            this.setMap(Map.class.isAssignableFrom(p.getType()));
            this.setCollection(Collection.class.isAssignableFrom(p.getType()));
            String val = p.getAnnotation(BodyAttribute.class).value();
            name = Strings.isNullOrEmpty(val) ? val : name;
            return;
        }
        //default as @RequestParameter
        checkArgument(p.getType().isPrimitive() && p.getType() != Void.class
                        || p.getType().isArray() && p.getType().getComponentType().isPrimitive(),
                "@RequestParameter only supports primitive type and Array of primitive type. " +
                        "Unsupported type: " + p.getType());
        this.setRequestParameter(true);
        this.setPrimitive(!p.getType().isArray());
        this.setArray(p.getType().isArray());
        if (p.isAnnotationPresent(RequestParameter.class)) {
            String val = p.getAnnotation(RequestParameter.class).value();
            name = Strings.isNullOrEmpty(val) ? val : name;
        }
    }

    public boolean isBodyAttribute() {
        return bodyAttribute;
    }

    public void setBodyAttribute(boolean bodyAttribute) {
        this.bodyAttribute = bodyAttribute;
    }

    public boolean isRequest() {
        return isRequest;
    }

    public void setRequest(boolean request) {
        isRequest = request;
    }

    public boolean isResponse() {
        return isResponse;
    }

    public void setResponse(boolean response) {
        isResponse = response;
    }

    public boolean isPrimitive() {
        return primitive;
    }

    public void setPrimitive(boolean primitive) {
        this.primitive = primitive;
    }

    public boolean isArray() {
        return array;
    }

    public void setArray(boolean array) {
        this.array = array;
    }

    public boolean isCollection() {
        return collection;
    }

    public void setCollection(boolean collection) {
        this.collection = collection;
    }

    public boolean isList() {
        return list;
    }

    public void setList(boolean list) {
        this.list = list;
    }

    public boolean isSet() {
        return set;
    }

    public void setSet(boolean set) {
        this.set = set;
    }

    public boolean isMap() {
        return map;
    }

    public void setMap(boolean map) {
        this.map = map;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isPathVariable() {
        return pathVariable;
    }

    public void setPathVariable(boolean pathVariable) {
        this.pathVariable = pathVariable;
    }

    public boolean isRequestParameter() {
        return requestParameter;
    }

    public void setRequestParameter(boolean requestParameter) {
        this.requestParameter = requestParameter;
    }

    public boolean isRequestHeader() {
        return requestHeader;
    }

    public void setRequestHeader(boolean requestHeader) {
        this.requestHeader = requestHeader;
    }

    public boolean isRequestBody() {
        return requestBody;
    }

    public void setRequestBody(boolean requestBody) {
        this.requestBody = requestBody;
    }
}

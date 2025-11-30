package com.example.framework.core;

import java.lang.reflect.Method;

public class RouteMapping {
    private Class<?> controllerClass;
    private Method method;
    private String request;

    public RouteMapping(Class<?> controllerClass, Method method, String request) {
        this.controllerClass = controllerClass;
        this.method = method;
        this.request = request;
    }

    public Class<?> getControllerClass() {
        return controllerClass;
    }

    public Method getMethod() {
        return method;
    }

    public String getRequest() {
        return request;
    }

    public void setRequest(String request) {
        this.request = request;
    }

    @Override
    public String toString() {
        return controllerClass.getSimpleName() + "." + method.getName();
    }
}

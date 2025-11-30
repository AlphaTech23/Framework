package com.example.framework.core;

import java.util.HashMap;
import java.util.Map;

public class ModelView {
    private String view;

    private Map<String, Object> attributes = new HashMap<>();

    public ModelView(String view) {
        this.view = view;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public void addAttribute(String name, Object value) {
        attributes.put(name, value);
    }

    public String getView() {
        return this.view;
    }
    public void setView(String view){
        this.view = view;
    }
}

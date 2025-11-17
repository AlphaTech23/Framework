package com.example.framework.core;

import java.util.HasMap;

public class ModelView {
    private String view;

    private HashMap<String, Object> attributes = new HasMap<>();

    public ModelView(String view) {
        this.view = view;
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

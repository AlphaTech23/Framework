package com.example.framework.core;

public interface UserSession {
    public boolean isAuthentified();
    public String[] getRoles();
}
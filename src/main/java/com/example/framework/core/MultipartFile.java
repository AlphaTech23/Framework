package com.example.framework.core;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;

import jakarta.servlet.ServletContext;

public class MultipartFile {

    private final String name;
    private final byte[] content;
    private final ServletContext context;

    public MultipartFile(String name, byte[] content) {
        this.name = name;
        this.content = content;
        this.context = ContextHolder.getServletContext();
    }

    public String getName() {
        return name;
    }

    public byte[] getContent() {
        return content;
    }

    private String extension() {
        String[] parts = name.split("\\.");
        if(parts.length > 1) 
            return parts[parts.length -1];
        else return "";
    }

    public void save(String relativePath) throws IOException {
        Path basePath = (Path) context.getAttribute("upload.root");
        relativePath += "." + extension();
        File target = new File(basePath.toString(), relativePath);
        target.getParentFile().mkdirs();
        try (FileOutputStream fos = new FileOutputStream(target)) {
            fos.write(content);
        }
    }

    public void save() throws IOException {
        Path basePath = (Path) context.getAttribute("upload.root");
        File target = new File(basePath.toString(), name);
        target.getParentFile().mkdirs();
        try (FileOutputStream fos = new FileOutputStream(target)) {
            fos.write(content);
        }
    }
}

package com.example.framework.utils;

import com.example.framework.core.MultipartFile;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.Part;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

public class MultipartFileResolver {

    public static Map<String, List<MultipartFile>> extractFiles(HttpServletRequest request)
            throws IOException, jakarta.servlet.ServletException {

        Map<String, List<MultipartFile>> files = new HashMap<>();

        if (!isMultipart(request)) {
            return files;
        }

        for (Part part : request.getParts()) {
            if (part.getSubmittedFileName() == null || part.getSize() == 0) {
                continue;
            }

            String fieldName = part.getName();
            String fileName  = extractFileName(part);

            byte[] content;
            try (InputStream is = part.getInputStream()) {
                content = is.readAllBytes();
            }

            MultipartFile uploadedFile = new MultipartFile(fileName, content);

            files
                .computeIfAbsent(fieldName, k -> new ArrayList<>())
                .add(uploadedFile);
        }

        return files;
    }

    private static boolean isMultipart(HttpServletRequest request) {
        String contentType = request.getContentType();
        return contentType != null && contentType.toLowerCase().startsWith("multipart/");
    }

    private static String extractFileName(Part part) {
        String submitted = part.getSubmittedFileName();
        if (submitted == null) return "unknown";
        return submitted.replace("\\", "/")
                        .substring(submitted.lastIndexOf('/') + 1);
    }
}

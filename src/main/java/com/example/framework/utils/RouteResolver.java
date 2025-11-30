package com.example.framework.utils;

import com.example.framework.core.RouteMapping;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RouteResolver {

    public static HashMap<String, Object> resolve(String url, Map<String, RouteMapping> mappings) {

        for (String routePattern : mappings.keySet()) {

            String regex = routePattern.replaceAll("\\{[^/]+}", "([^/]+)");

            Pattern pattern = Pattern.compile("^" + regex + "$");
            Matcher matcher = pattern.matcher(url);

            if (matcher.matches()) {

                Map<String, String> vars = extractVariables(routePattern, matcher);

                HashMap<String, Object> result = new HashMap<>();
                result.put("mapping", mappings.get(routePattern));
                result.put("pathVars", vars);

                return result;
            }
        }

        return null;
    }

    private static Map<String, String> extractVariables(String routePattern, Matcher matcher) {
        Map<String, String> vars = new HashMap<>();

        Matcher nameMatcher = Pattern.compile("\\{([^/]+)}").matcher(routePattern);

        int index = 1;
        while (nameMatcher.find()) {
            String varName = nameMatcher.group(1);
            String value = matcher.group(index);
            vars.put(varName, value);
            index++;
        }

        return vars;
    }
}

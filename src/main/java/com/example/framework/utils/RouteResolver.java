package com.example.framework.utils;

import com.example.framework.core.RouteMapping;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RouteResolver {

    public static HashMap<String, Object> resolve(String url, Map<String, List<RouteMapping>> mappings,
            String requestMethod) {

        for (String routePattern : mappings.keySet()) {

            String regex = routePattern.replaceAll("\\{[^/]+}", "([^/]+)");

            Pattern pattern = Pattern.compile("^" + regex + "$");
            Matcher matcher = pattern.matcher(url);

            for (RouteMapping mapping : mappings.get(routePattern)) {
                if (matcher.matches() && (mapping.getRequest().equalsIgnoreCase(requestMethod)
                        || mapping.getRequest().equalsIgnoreCase("ALL"))) {

                    Map<String, String> vars = extractVariables(routePattern, matcher);

                    HashMap<String, Object> result = new HashMap<>();
                    result.put("mapping", mapping);
                    result.put("pathVars", vars);

                    return result;
                }
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

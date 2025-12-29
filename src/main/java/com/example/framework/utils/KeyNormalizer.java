package com.example.framework.utils;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class KeyNormalizer {

    private static final Pattern MULTIDIM_SEGMENT = 
            Pattern.compile("([a-zA-Z0-9_]+)((?:\\[(.*?)\\])*)");

    public static Map<String, String[]> normalize(Map<String, String[]> input) {
        Map<String, List<String>> out = new LinkedHashMap<>();
        Map<String, Integer> counters = new HashMap<>();

        for (var entry : input.entrySet()) {
            String key = entry.getKey();
            String[] values = entry.getValue();

            List<String> segments = Arrays.asList(key.split("\\."));
            int lastArraySegmentPos = findLastArraySegment(segments);
            
            for (String val : values) {
                String normalized = resolveMultidimensional(
                        segments,
                        0,
                        "",
                        lastArraySegmentPos,
                        counters
                );

                out.computeIfAbsent(normalized, k -> new ArrayList<>()).add(val);
            }
        }

        Map<String, String[]> result = new LinkedHashMap<>();
        out.forEach((k, v) -> result.put(k, v.toArray(String[]::new)));
        return result;
    }

    private static int findLastArraySegment(List<String> segments) {
        for (int i = segments.size() - 1; i >= 0; i--) {
            if (segments.get(i).contains("[]")) {
                return i;
            }
        }
        return -1;
    }

    private static String resolveMultidimensional(
            List<String> segments,
            int pos,
            String parent,
            int lastArraySegmentPos,
            Map<String, Integer> counters
    ) {
        if (pos >= segments.size()) return parent;

        String segment = segments.get(pos);
        Matcher m = MULTIDIM_SEGMENT.matcher(segment);
        
        if (!m.matches()) {
            String base = parent.isEmpty() ? segment : parent + "." + segment;
            return resolveMultidimensional(segments, pos + 1, base, lastArraySegmentPos, counters);
        }

        String name = m.group(1);
        String allBrackets = m.group(2); 
        
        List<String> dimensions = extractDimensions(allBrackets);
        
        String base = parent.isEmpty() ? name : parent + "." + name;
        
        StringBuilder currentPath = new StringBuilder(base);
        
        for (int dimIndex = 0; dimIndex < dimensions.size(); dimIndex++) {
            String dimValue = dimensions.get(dimIndex);
            
            if (dimValue == null || dimValue.isEmpty()) {
                if (pos == lastArraySegmentPos && dimIndex == dimensions.size() - 1) {
                    String counterKey = currentPath.toString() + "|dim" + dimIndex;
                    if(parent.contains("[]"))
                        counterKey = parent + "|" + name + "|dim" + dimIndex;
                    int index = counters.compute(counterKey, (k, v) -> v == null ? 0 : v + 1);
                    currentPath.append("[").append(index).append("]");
                } else {
                    currentPath.append("[0]");
                }
            } else {
                currentPath.append("[").append(dimValue).append("]");
            }
        }
        
        return resolveMultidimensional(segments, pos + 1, currentPath.toString(), lastArraySegmentPos, counters);
    }
    
    private static List<String> extractDimensions(String allBrackets) {
        List<String> dimensions = new ArrayList<>();
        
        if (allBrackets == null || allBrackets.isEmpty()) {
            return dimensions;
        }
        
        Pattern dimPattern = Pattern.compile("\\[(.*?)\\]");
        Matcher dimMatcher = dimPattern.matcher(allBrackets);
        
        while (dimMatcher.find()) {
            dimensions.add(dimMatcher.group(1));
        }
        
        return dimensions;
    }
}
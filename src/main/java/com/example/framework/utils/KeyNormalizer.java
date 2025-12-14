package com.example.framework.utils;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class KeyNormalizer {

    private static final Pattern SEGMENT =
            Pattern.compile("([a-zA-Z0-9_]+)(\\[(.*?)\\])?");

    public static Map<String, String[]> normalize(Map<String, String[]> input) {
        Map<String, List<String>> out = new LinkedHashMap<>();
        Map<String, Integer> counters = new HashMap<>();

        for (var entry : input.entrySet()) {
            String key = entry.getKey();
            String[] values = entry.getValue();

            List<String> segments = Arrays.asList(key.split("\\."));
            int lastArrayPos = findLastArraySegment(segments);

            for (String val : values) {
                String normalized = resolve(
                        segments,
                        0,
                        "",
                        lastArrayPos,
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

    private static String resolve(
            List<String> segments,
            int pos,
            String parent,
            int lastArrayPos,
            Map<String, Integer> counters
    ) {
        if (pos >= segments.size()) return parent;

        Matcher m = SEGMENT.matcher(segments.get(pos));
        m.matches();

        String name = m.group(1);
        String idx = m.group(3);

        String base = parent.isEmpty() ? name : parent + "." + name;

        if (idx != null && !idx.isEmpty()) {
            return resolve(segments, pos + 1, base + "[" + idx + "]", lastArrayPos, counters);
        }

        if (idx == null) {
            return resolve(segments, pos + 1, base, lastArrayPos, counters);
        }

        if (pos != lastArrayPos) {
            return resolve(segments, pos + 1, base + "[0]", lastArrayPos, counters);
        }

        String key = parent + "|" + name;
        int index = counters.compute(key, (k, v) -> v == null ? 0 : v + 1);

        return resolve(segments, pos + 1, base + "[" + index + "]", lastArrayPos, counters);
    }
}

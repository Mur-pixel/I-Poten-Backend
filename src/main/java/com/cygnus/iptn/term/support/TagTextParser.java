package com.cygnus.iptn.term.support;

import java.util.*;
import java.util.regex.*;

public final class TagTextParser {
    private static final Pattern HASH = Pattern.compile("#([^#\\s,;]+)");

    private TagTextParser() {}

    public static List<String> parse(String raw) {
        if (raw == null || raw.isBlank()) return List.of();
        LinkedHashSet<String> set = new LinkedHashSet<>();
        Matcher m = HASH.matcher(raw);
        while (m.find()) {
            String name = m.group(1).trim().toLowerCase();

            // 1) 최소 길이
            if (name.length() < 2) continue;

            // 2) 한글/영문/숫자 없으면 skip
            if (!name.matches(".*[가-힣a-z0-9].*")) continue;

            // 3) 불필요 패턴 skip
            if (name.startsWith("_") || name.startsWith("-") || name.startsWith("?") || name.startsWith("::")) continue;
            if (name.contains("[") || name.contains("]")) continue;

            set.add(name);
        }
        return new ArrayList<>(set);
    }
}

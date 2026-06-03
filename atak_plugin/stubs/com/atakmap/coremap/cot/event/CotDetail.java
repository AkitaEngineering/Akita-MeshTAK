package com.atakmap.coremap.cot.event;

import java.util.HashMap;
import java.util.Map;

public class CotDetail {

    private final Map<String, String> attributes = new HashMap<>();

    public String getAttribute(String key) {
        return attributes.get(key);
    }

    void setAttribute(String key, String value) {
        attributes.put(key, value);
    }
}

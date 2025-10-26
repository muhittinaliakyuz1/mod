package com.example;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class HeartRegistry {
    private static final Map<UUID, Integer> credits = new ConcurrentHashMap<>();

    public static int get(UUID id) {
        return credits.getOrDefault(id, 0);
    }

    public static void add(UUID id, int amount) {
        credits.put(id, get(id) + amount);
    }

    public static boolean consume(UUID id) {
        int v = get(id);
        if (v <= 0) return false;
        credits.put(id, v - 1);
        return true;
    }
}

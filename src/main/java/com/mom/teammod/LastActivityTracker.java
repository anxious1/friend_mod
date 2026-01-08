package com.mom.teammod;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class LastActivityTracker {
    private static final Map<UUID, Instant> MAP = new ConcurrentHashMap<>();

    public static void update(UUID uuid) {
        MAP.put(uuid, Instant.now());
    }

    public static Instant get(UUID uuid) {
        return MAP.getOrDefault(uuid, Instant.now());
    }

    public static void onLogin(UUID uuid) {
        MAP.put(uuid, Instant.now());
    }

    public static void onLogout(UUID uuid) {
        MAP.remove(uuid);
    }
}
package com.mom.teammod;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ClientPlayerNameCache {
    private static final Map<UUID, String> UUID_NAME = new ConcurrentHashMap<>();

    public static void put(UUID id, String name) {
        if (name != null && !name.equals("Unknown") && !name.isEmpty())
            UUID_NAME.put(id, name);
    }
    public static String getName(UUID id) {
        return UUID_NAME.getOrDefault(id, "Loading...");
    }

}
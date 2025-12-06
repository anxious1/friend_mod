// Добавь этот класс в com.mom.teammod
package com.mom.teammod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class OtherPlayerState {
    // Тестовые данные для демонстрации
    private static final Map<UUID, PlayerStatus> testData = new HashMap<>();

    // OtherPlayerState.java
    static {
        testData.put(UUID.fromString("deadbeef-dead-beef-dead-beefdeadbeef"),
                new PlayerStatus("BirdMan", false, false)); // оффлайн
        testData.put(UUID.fromString("11111111-1111-1111-1111-111111111111"),
                new PlayerStatus("BridMan", true, false)); // онлайн
        testData.put(UUID.fromString("22222222-2222-2222-2222-222222222222"),
                new PlayerStatus("Berdamel", false, false)); // оффлайн
        testData.put(UUID.fromString("33333333-3333-3333-3333-333333333333"),
                new PlayerStatus("HerobRine", false, false)); // оффлайн
        testData.put(UUID.fromString("44444444-4444-4444-4444-444444444444"),
                new PlayerStatus("TOPSON", true, false)); // онлайн
    }

    public static PlayerStatus getStatus(UUID playerId) {
        return testData.getOrDefault(playerId,
                new PlayerStatus("Unknown", false, false));
    }

    public static class PlayerStatus {
        public final String name;
        public final boolean online;
        public final boolean afk;

        public PlayerStatus(String name, boolean online, boolean afk) {
            this.name = name;
            this.online = online;
            this.afk = afk;
        }
    }
}
// Добавь этот класс в com.mom.teammod
package com.mom.teammod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class OtherPlayerState {
    // Тестовые данные для демонстрации
    private static final Map<UUID, PlayerStatus> testData = new HashMap<>();

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
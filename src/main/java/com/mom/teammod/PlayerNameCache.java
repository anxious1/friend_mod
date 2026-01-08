package com.mom.teammod;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.server.ServerLifecycleHooks;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerNameCache {
    private static final Map<String, UUID> NAME_UUID = new ConcurrentHashMap<>();
    private static final Map<UUID, String> UUID_NAME = new ConcurrentHashMap<>();

    /* вызывать 1 раз при старте сервера и после каждого логина */
    public static void rebuild() {
        NAME_UUID.clear();
        UUID_NAME.clear();
        MinecraftServer srv = ServerLifecycleHooks.getCurrentServer();
        if (srv == null) return;
        // перебираем всех онлайн-игроков
        srv.getPlayerList().getPlayers().forEach(sp -> onLogin(sp));
    }

    @Nullable
    public static UUID getUUID(String name) {
        return NAME_UUID.get(name.toLowerCase());
    }

    @Nullable
    public static String getName(UUID id) {
        return UUID_NAME.get(id);
    }

    /* добавить при входе игрока */
    public static void onLogin(ServerPlayer player) {
        String n = player.getGameProfile().getName();
        NAME_UUID.put(n.toLowerCase(), player.getUUID());
        UUID_NAME.put(player.getUUID(), n);
    }
}
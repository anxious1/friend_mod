package com.mom.teammod;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

import java.util.UUID;

public final class PlayerStatus {
    private static final long AFK_MS = 10_000;

    public enum Status { ONLINE, AFK, OFFLINE }

    public static Status get(UUID id) {
        Player p = Minecraft.getInstance().level.getPlayerByUUID(id);
        if (p == null) return Status.OFFLINE;

        long idle = System.currentTimeMillis() - ClientPlayerCache.lastInputTime;
        return idle > AFK_MS ? Status.AFK : Status.ONLINE;
    }
}
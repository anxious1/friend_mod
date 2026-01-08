package com.mom.teammod;

import com.mojang.authlib.GameProfile;
import com.mom.teammod.packets.RequestProfilePacket;
import io.redspace.ironsspellbooks.entity.spells.portal.PortalData;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ClientPlayerCache {
    private static final Map<UUID, Byte> STATUS = new HashMap<>();
    public static final Queue<UUID> loadQueue = new ConcurrentLinkedQueue<>();
    private static final Map<UUID, CacheEntry> cache = new HashMap<>();
    public enum PlayerStatus { ONLINE, AFK, OFFLINE }
    public static long lastInputTime = System.currentTimeMillis();
    public static class CacheEntry {
        public GameProfile gameProfile;
        public ItemStack[] lastEquipment = new ItemStack[4];
        public boolean hasMet = false;
        public long lastSeenTime = 0;
        public PlayerStatus status = PlayerStatus.OFFLINE;
        private static final Map<UUID, PortalData> portalData = new HashMap<>();
        public CacheEntry(GameProfile profile) {
            this.gameProfile = profile;
            for (int i = 0; i < 4; i++) lastEquipment[i] = ItemStack.EMPTY;
        }
    }

    public static CacheEntry getOrCreate(UUID uuid) {
        return cache.computeIfAbsent(uuid, u -> new CacheEntry(new GameProfile(u, "Unknown")));
    }
    public static void updateInputTime() {
        lastInputTime = System.currentTimeMillis();
    }
    public static void updateFromProfile(UUID uuid, ProfileManager.Profile profile) {
        CacheEntry entry = getOrCreate(uuid);
        if (profile.getGameProfile() != null) {
            entry.gameProfile = profile.getGameProfile();
        }
        if (profile.getLastEquipment() != null) {
            entry.lastEquipment = profile.getLastEquipment();
        }
        entry.hasMet = true; // считаем, что если профиль есть — значит встречались
    }

    public static void onPlayerSeen(UUID uuid, GameProfile profile, ItemStack[] equipment) {
        CacheEntry entry = getOrCreate(uuid);
        entry.gameProfile = profile;
        entry.lastEquipment = equipment;
        entry.hasMet = true;
        entry.lastSeenTime = System.currentTimeMillis();
    }

    public static boolean hasMet(UUID uuid) {
        return cache.containsKey(uuid) && cache.get(uuid).hasMet;
    }

    public static GameProfile getGameProfile(UUID uuid) {
        return getOrCreate(uuid).gameProfile;
    }

    public static ItemStack[] getLastEquipment(UUID uuid) {
        return getOrCreate(uuid).lastEquipment;
    }
    public static class PortalData {
        public int tier1, tier2, tier3;
    }

    public static void setPortalData(UUID uuid, int t1, int t2, int t3) {
        PortalData pd = new PortalData();
        pd.tier1 = t1;
        pd.tier2 = t2;
        pd.tier3 = t3;
        CacheEntry.portalData.put(uuid, pd);
    }

    public static PortalData getPortalData(UUID uuid) {
        return CacheEntry.portalData.getOrDefault(uuid, new PortalData());
    }
    private String getNameSafe(UUID id){
        GameProfile gp = ClientPlayerCache.getGameProfile(id);
        if (gp == null || "Unknown".equals(gp.getName())) {
            NetworkHandler.INSTANCE.sendToServer(new RequestProfilePacket(id));
            return "Loading..."; // или просто вернуть gp.getName() после запроса
        }
        return gp.getName();
    }

    public static void setStatus(UUID uuid, byte st) {
        STATUS.put(uuid, st);
    }

    public static byte getRawStatus(UUID uuid) {
        return STATUS.getOrDefault(uuid, (byte)0);
    }

    public static boolean isOnline(UUID uuid) {
        return getRawStatus(uuid) != 0;   // 0 = OFFLINE, 1/2 = ONLINE/AFK
    }
}
package com.mom.teammod;

import com.mojang.authlib.GameProfile;
import com.mom.teammod.packets.RequestProfilePacket;
import io.redspace.ironsspellbooks.entity.spells.portal.PortalData;
import net.minecraft.client.Minecraft;
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

        // Профиль с сервера = "данные вообще", но это НЕ означает, что игрок был встречен рядом.
        // Поэтому hasMet здесь НЕ ставим.
        if (profile.getGameProfile() != null) {
            entry.gameProfile = profile.getGameProfile();
        }

        // Снапшот экипировки должен быть "как при встрече".
        // Если мы уже реально видели игрока (hasMet + lastSeenTime > 0), то серверными данными НЕ затираем.
        if (profile.getLastEquipment() != null) {
            boolean alreadyMet = entry.hasMet && entry.lastSeenTime > 0;
            if (!alreadyMet) {
                entry.lastEquipment = profile.getLastEquipment();
            }
        }
    }


    public static void onPlayerSeen(UUID uuid, GameProfile profile, ItemStack[] equipment) {
        CacheEntry entry = getOrCreate(uuid);
        entry.gameProfile = profile;
        entry.lastEquipment = equipment;
        entry.hasMet = true;
        entry.lastSeenTime = System.currentTimeMillis();

        // сохраняем в клиентский профиль
        ProfileManager.Profile clientProfile = ProfileManager.getClientProfile(uuid);
        clientProfile.setGameProfile(profile);
        clientProfile.setLastEquipment(equipment);
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

    private String getNameSafe(UUID id) {
        GameProfile gp = ClientPlayerCache.getGameProfile(id);
        if (gp == null || "Unknown".equals(gp.getName())) {
            NetworkHandler.INSTANCE.sendToServer(new RequestProfilePacket(id));
            return PlayerNameCache.getName(id); // последнее известное имя
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
    public static void saveToDisk() {
        try {
            java.nio.file.Path dir = net.minecraftforge.fml.loading.FMLPaths.CONFIGDIR.get().resolve("teammod");
            java.nio.file.Files.createDirectories(dir);
            java.nio.file.Path file = dir.resolve("client_player_cache.dat");

            net.minecraft.nbt.CompoundTag root = new net.minecraft.nbt.CompoundTag();
            net.minecraft.nbt.ListTag list = new net.minecraft.nbt.ListTag();

            for (Map.Entry<UUID, CacheEntry> e : cache.entrySet()) {
                UUID uuid = e.getKey();
                CacheEntry ce = e.getValue();

                net.minecraft.nbt.CompoundTag tag = new net.minecraft.nbt.CompoundTag();
                tag.putUUID("uuid", uuid);

                if (ce.gameProfile != null && ce.gameProfile.getName() != null) {
                    tag.putString("name", ce.gameProfile.getName());
                }
                tag.putBoolean("hasMet", ce.hasMet);
                tag.putLong("lastSeenTime", ce.lastSeenTime);

                tag.putInt("status", ce.status != null ? ce.status.ordinal() : PlayerStatus.OFFLINE.ordinal());

                net.minecraft.nbt.ListTag eq = new net.minecraft.nbt.ListTag();
                ItemStack[] arr = ce.lastEquipment != null ? ce.lastEquipment : new ItemStack[4];
                for (int i = 0; i < 4; i++) {
                    ItemStack st = (i < arr.length && arr[i] != null) ? arr[i] : ItemStack.EMPTY;
                    net.minecraft.nbt.CompoundTag stTag = new net.minecraft.nbt.CompoundTag();
                    st.save(stTag);
                    eq.add(stTag);
                }
                tag.put("lastEquipment", eq);

                list.add(tag);
            }

            root.put("players", list);

            try (java.io.OutputStream os = java.nio.file.Files.newOutputStream(file)) {
                net.minecraft.nbt.NbtIo.writeCompressed(root, os);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static void loadFromDisk() {
        try {
            java.nio.file.Path file = net.minecraftforge.fml.loading.FMLPaths.CONFIGDIR.get()
                    .resolve("teammod")
                    .resolve("client_player_cache.dat");

            if (!java.nio.file.Files.exists(file)) return;

            net.minecraft.nbt.CompoundTag root;
            try (java.io.InputStream is = java.nio.file.Files.newInputStream(file)) {
                root = net.minecraft.nbt.NbtIo.readCompressed(is);
            }
            if (root == null) return;

            net.minecraft.nbt.ListTag list = root.getList("players", net.minecraft.nbt.Tag.TAG_COMPOUND);
            for (int idx = 0; idx < list.size(); idx++) {
                net.minecraft.nbt.CompoundTag tag = list.getCompound(idx);
                if (!tag.hasUUID("uuid")) continue;

                UUID uuid = tag.getUUID("uuid");
                String name = tag.contains("name") ? tag.getString("name") : "Unknown";

                CacheEntry ce = getOrCreate(uuid);
                ce.gameProfile = new GameProfile(uuid, name);
                ce.hasMet = tag.getBoolean("hasMet");
                ce.lastSeenTime = tag.getLong("lastSeenTime");

                int st = tag.getInt("status");
                PlayerStatus[] vals = PlayerStatus.values();
                ce.status = (st >= 0 && st < vals.length) ? vals[st] : PlayerStatus.OFFLINE;

                if (tag.contains("lastEquipment", net.minecraft.nbt.Tag.TAG_LIST)) {
                    net.minecraft.nbt.ListTag eq = tag.getList("lastEquipment", net.minecraft.nbt.Tag.TAG_COMPOUND);
                    ItemStack[] arr = new ItemStack[4];
                    for (int i = 0; i < 4; i++) {
                        if (i < eq.size()) {
                            arr[i] = ItemStack.of(eq.getCompound(i));
                        } else {
                            arr[i] = ItemStack.EMPTY;
                        }
                    }
                    ce.lastEquipment = arr;
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

}
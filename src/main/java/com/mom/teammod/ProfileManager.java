package com.mom.teammod;

import com.mojang.authlib.GameProfile;
import com.mom.teammod.packets.ProfileSyncPacket;
import com.mom.teammod.packets.RequestProfilePacket;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;
import net.simpleraces.network.SimpleracesModVariables;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static com.mojang.datafixers.TypeRewriteRule.orElse;

@Mod.EventBusSubscriber(modid = TeamMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ProfileManager {

    private static final Map<UUID, Profile> profiles = new HashMap<>();
    public static final Map<UUID, Profile> clientProfiles = new HashMap<>();

    public static class Profile {
        private String race = "";   // пусто = ещё не выбирал

        public String getRace() { return race; }
        public void setRace(String r) { this.race = r; }
        private GameProfile gameProfile;
        private ItemStack[] lastEquipment = new ItemStack[4];
        private UUID playerUUID;
        private String background = "profile_bg1";
        private Map<String, Integer> customStats = new HashMap<>();
        private Map<String, Long> customLongStats = new HashMap<>(); // для long (расстояние)

        // Время входа для текущей сессии (в миллисекундах)
        private long loginTimeMillis = 0;
        private boolean showOnCompass = true;

        public boolean isShowOnCompass() { return showOnCompass; }
        public void setShowOnCompass(boolean v) { this.showOnCompass = v; }

        public Profile(UUID playerUUID) {
            this.playerUUID = playerUUID;
            // Инициализируем все статы нулями
            customStats.put("boss_kills", 0);
            customStats.put("deaths", 0);
            customStats.put("mobs_killed", 0);
            customStats.put("play_time_ticks", 0);

            customLongStats.put("distance_cm", 0L);
        }

        public boolean isRaceSelected() {
            return serializeNBT().contains("selectedRace");
        }

        public String getBackground() { return background; }
        public void setBackground(String bg) { this.background = bg; }

        public void setLoginTime(long time) {
            this.loginTimeMillis = time;
        }

        public long getCurrentSessionMillis() {
            return loginTimeMillis == 0 ? 0 : System.currentTimeMillis() - loginTimeMillis;
        }

        // Инкременты
        public void incrementDeaths() {
            customStats.put("deaths", customStats.getOrDefault("deaths", 0) + 1);
        }

        public void incrementMobsKilled() {
            customStats.put("mobs_killed", customStats.getOrDefault("mobs_killed", 0) + 1);
        }


        public void addPlayTimeTicks(int ticks) {
            customStats.put("play_time_ticks", customStats.getOrDefault("play_time_ticks", 0) + ticks);
        }

        public void addDistanceCm(long cm) {
            customLongStats.put("distance_cm", customLongStats.getOrDefault("distance_cm", 0L) + cm);
        }

        // Геттеры
        public int getDeaths() { return customStats.getOrDefault("deaths", 0); }
        public int getMobsKilled() { return customStats.getOrDefault("mobs_killed", 0); }
        public int getPlayTimeTicks() { return customStats.getOrDefault("play_time_ticks", 0); }
        public long getDistanceCm() { return customLongStats.getOrDefault("distance_cm", 0L); }

        public Map<String, Integer> getCustomStats() { return customStats; }

        public CompoundTag serializeNBT() {
            CompoundTag tag = new CompoundTag();
            tag.putString("background", background);
            if (gameProfile != null) {
                tag.put("gameProfile", net.minecraft.nbt.NbtUtils.writeGameProfile(new CompoundTag(), gameProfile));
            }

            ListTag eqList = new ListTag();
            for (ItemStack stack : lastEquipment) {
                eqList.add((stack == null ? ItemStack.EMPTY : stack).save(new CompoundTag()));
            }
            tag.put("lastEquipment", eqList);

            tag.putBoolean("showOnCompass", showOnCompass);
            CompoundTag statsTag = new CompoundTag();
            customStats.forEach(statsTag::putInt);
            customLongStats.forEach(statsTag::putLong);
            tag.put("customStats", statsTag);
            tag.putString("race", race);
            tag.putLong("loginTime", loginTimeMillis); // сохраняем на случай краша (но лучше при логауте)
            return tag;
        }
        public void deserializeNBT(CompoundTag tag) {
            background = tag.getString("background");

            customStats.clear();
            customLongStats.clear();
            race = tag.getString("race");
            showOnCompass = tag.contains("showOnCompass") ? tag.getBoolean("showOnCompass") : true;
            if (tag.contains("gameProfile", 10)) {
                this.gameProfile = net.minecraft.nbt.NbtUtils.readGameProfile(tag.getCompound("gameProfile"));
            }

            ListTag eqList = tag.getList("lastEquipment", 10);
            for (int i = 0; i < eqList.size() && i < 4; i++) {
                lastEquipment[i] = ItemStack.of(eqList.getCompound(i));
            }
            if (tag.contains("customStats")) {
                CompoundTag statsTag = tag.getCompound("customStats");
                for (String key : statsTag.getAllKeys()) {
                    if (statsTag.contains(key, 3)) { // int
                        customStats.put(key, statsTag.getInt(key));
                    } else if (statsTag.contains(key, 4)) { // long
                        customLongStats.put(key, statsTag.getLong(key));
                    }
                }
            }

            loginTimeMillis = tag.getLong("loginTime");
        }

        public int getBossKills() {
            return customStats.getOrDefault("boss_kills", 0);
        }

        public void incrementBossKills() {
            customStats.put("boss_kills", getBossKills() + 1);
        }

        public GameProfile getGameProfile() {
            return gameProfile != null ? gameProfile : new GameProfile(playerUUID, "Unknown");
        }

        public void setGameProfile(GameProfile profile) {
            this.gameProfile = profile;
        }

        public ItemStack[] getLastEquipment() {
            return lastEquipment;
        }

        public void setLastEquipment(ItemStack[] eq) {
            this.lastEquipment = eq;
        }
    }

    public static Profile getProfile(ServerLevel level, UUID playerUUID) {
        TeamWorldData data = TeamWorldData.get(level);
        return data.getPlayerProfiles().computeIfAbsent(playerUUID, u -> new Profile(u));
    }

    public static Profile getClientProfile(UUID playerUUID) {
        // На клиенте НЕ пытаемся создать новый — только ждем от сервера
        return clientProfiles.computeIfAbsent(playerUUID, uuid -> {
            Profile p = new Profile(uuid);
            p.setBackground("profile_bg1");
            p.setGameProfile(new GameProfile(uuid, "Loading...")); // <-- заглушка, но НЕ Unknown
            return p;
        });
    }

    // Главная правка: безопасная отправка профиля
    public static void syncProfileToClient(ServerPlayer player) {
        UUID uuid = player.getUUID();
        ServerLevel storageLevel = TeamWorldData.storageLevel(player.getServer());
        Profile profile = getProfile(storageLevel, uuid);   // теперь всегда овер
        NetworkHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player),
                new ProfileSyncPacket(uuid, profile));
    }
    public static void syncProfileToClient(ServerPlayer recipient, UUID targetUUID, Profile profile) {
        NetworkHandler.INSTANCE.send(
                PacketDistributor.PLAYER.with(() -> recipient),
                new ProfileSyncPacket(targetUUID, profile)
        );
    }
    
    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        ProfileManager.Profile prof = ProfileManager.getProfile(player.serverLevel(), player.getUUID());

        /* 1. Синхронизируем расу при любом несовпадении */
        var cap = player.getCapability(SimpleracesModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(null);
        if (cap != null && cap.selected) {
            String race = cap.aracha    ? "aracha"
                    : cap.dragon    ? "dragon"
                    : cap.dwarf     ? "dwarf"
                    : cap.elf       ? "elf"
                    : cap.fairy     ? "fairy"
                    : cap.halfdead  ? "halfdead"
                    : cap.merfolk   ? "merfolk"
                    : cap.orc       ? "orc"
                    : cap.serpentin ? "serpentin"
                    : cap.werewolf  ? "werewolf"
                    : "human";
            if (!race.equals(prof.getRace())) {
                prof.setRace(race);
                TeamWorldData.get(player.serverLevel()).setDirty(true);
            }
        }

        /* 2. Отправляем клиенту актуальный профиль */
        ProfileManager.syncProfileToClient(player);
    }

    public static void saveProfileToDisk(ServerLevel level, UUID uuid) {
        TeamWorldData data = TeamWorldData.get(level);
        Profile profile = getProfile(level, uuid);
        data.getPlayerProfiles().put(uuid, profile);
        data.setDirty(true);

        // Форсируем сохранение на диск
        level.getDataStorage().save();
    }
    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            // Сохраняем профиль в мир ТОЛЬКО на сервере
            ServerLevel level = player.serverLevel();
            TeamWorldData data = TeamWorldData.get(level);
            ProfileManager.Profile profile = getProfile(level, player.getUUID());

            // Сохраняем текущую экипировку
            ItemStack[] equipment = new ItemStack[4];
            for (int i = 0; i < 4; i++) {
                equipment[i] = player.getItemBySlot(EquipmentSlot.byTypeAndIndex(EquipmentSlot.Type.ARMOR, i)).copy();
            }
            profile.setLastEquipment(equipment);

            data.getPlayerProfiles().put(player.getUUID(), profile);
            data.setDirty(true);

            // Удаляем из клиентского кэша
            clientProfiles.remove(player.getUUID());
        }
    }
}
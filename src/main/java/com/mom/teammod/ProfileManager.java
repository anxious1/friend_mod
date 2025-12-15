package com.mom.teammod;

import com.mom.teammod.packets.ProfileSyncPacket;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = TeamMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ProfileManager {

    private static final Map<UUID, Profile> profiles = new HashMap<>();
    public static final Map<UUID, Profile> clientProfiles = new HashMap<>();

    public static class Profile {
        private UUID playerUUID;
        private String background = "profile_bg1";
        private Map<String, Integer> customStats = new HashMap<>();
        private Map<String, Long> customLongStats = new HashMap<>(); // для long (расстояние)

        // Время входа для текущей сессии (в миллисекундах)
        private long loginTimeMillis = 0;

        public Profile(UUID playerUUID) {
            this.playerUUID = playerUUID;
            // Инициализируем все статы нулями
            customStats.put("boss_kills", 0);
            customStats.put("deaths", 0);
            customStats.put("mobs_killed", 0);
            customStats.put("play_time_ticks", 0);

            customLongStats.put("distance_cm", 0L);
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

            CompoundTag statsTag = new CompoundTag();
            customStats.forEach(statsTag::putInt);
            customLongStats.forEach(statsTag::putLong);
            tag.put("customStats", statsTag);

            tag.putLong("loginTime", loginTimeMillis); // сохраняем на случай краша (но лучше при логауте)
            return tag;
        }

        public void deserializeNBT(CompoundTag tag) {
            background = tag.getString("background");

            customStats.clear();
            customLongStats.clear();

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
    }

    public static Profile getProfile(UUID playerUUID) {
        return profiles.computeIfAbsent(playerUUID, uuid -> {
            Profile profile = new Profile(uuid);
            profile.setBackground("profile_bg1");
            return profile;
        });
    }

    public static Profile getClientProfile(UUID playerUUID) {
        return clientProfiles.computeIfAbsent(playerUUID, uuid -> {
            Profile p = new Profile(uuid);
            p.setBackground("profile_bg1");
            return p;
        });
    }

    // Главная правка: безопасная отправка профиля
    public static void syncProfileToClient(ServerPlayer player) {
        UUID uuid = player.getUUID();
        Profile profile = getProfile(uuid);

        NetworkHandler.INSTANCE.send(
                PacketDistributor.PLAYER.with(() -> player),
                new ProfileSyncPacket(uuid, profile)
        );
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            Profile profile = getProfile(player.getUUID());
            profile.setLoginTime(System.currentTimeMillis());

            syncProfileToClient(player);

            // Помечаем на всякий случай (если профиль новый)
            TeamWorldData data = TeamWorldData.get(player.serverLevel());
            if (data != null) {
                data.setDirty(true);
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            Profile profile = getProfile(player.getUUID());

            long sessionMillis = profile.getCurrentSessionMillis();
            if (sessionMillis > 0) {
                int ticks = (int)(sessionMillis / 50);
                profile.addPlayTimeTicks(ticks);

                // Помечаем данные как изменённые после добавления времени
                TeamWorldData data = TeamWorldData.get(player.serverLevel());
                if (data != null) {
                    data.setDirty(true);
                }
            }

            clientProfiles.remove(player.getUUID());
            syncProfileToClient(player);
        }
    }
}
package com.mom.teammod;

import com.mom.teammod.packets.ProfileSyncPacket;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = TeamMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ProfileManager {

    private static final Map<UUID, Profile> profiles = new HashMap<>();
    public static final Map<UUID, Profile> clientProfiles = new HashMap<>();

    public static class Profile {
        private UUID playerUUID;
        private String background = "profile_bg1"; // Default, варианты: profile_bg1, profile_bg2, profile_bg3
        private Map<String, Integer> customStats = new HashMap<>(); // Пустые кастом статы, наполнишь позже

        public Profile(UUID playerUUID) {
            this.playerUUID = playerUUID;
            // Placeholder кастом статы
            customStats.put("boss_kills", 0);
            customStats.put("custom_stat1", 0);
            customStats.put("custom_stat2", 0);
        }

        public String getBackground() {
            return background;
        }

        public void setBackground(String bg) {
            this.background = bg;
        }

        public Map<String, Integer> getCustomStats() {
            return customStats;
        }

        public CompoundTag serializeNBT() {
            CompoundTag tag = new CompoundTag();
            tag.putString("background", background);
            CompoundTag statsTag = new CompoundTag();
            customStats.forEach(statsTag::putInt);
            tag.put("customStats", statsTag);
            return tag;
        }

        public void deserializeNBT(CompoundTag tag) {
            background = tag.getString("background");
            CompoundTag statsTag = tag.getCompound("customStats");
            customStats.clear();
            statsTag.getAllKeys().forEach(key -> customStats.put(key, statsTag.getInt(key)));
        }
    }

    public static Profile getProfile(UUID playerUUID) {
        return profiles.computeIfAbsent(playerUUID, uuid -> {
            Profile profile = new Profile(uuid);
            profile.setBackground("profile_bg1"); // дефолтный фон
            return profile;
        });
    }
    public static Profile getClientProfile(UUID playerUUID) {
        return clientProfiles.computeIfAbsent(playerUUID, uuid -> {
            Profile p = new Profile(uuid);
            p.setBackground("profile_bg1"); // дефолт
            return p;
        });
    }

    public static void syncProfileToClient(ServerPlayer player) {
        UUID uuid = player.getUUID();
        Profile profile = getProfile(uuid);
        NetworkHandler.INSTANCE.sendTo(new ProfileSyncPacket(uuid, profile), player.connection.connection, NetworkDirection.PLAY_TO_CLIENT);
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ProfileManager.getProfile(player.getUUID()); // ← создаём профиль
            ProfileManager.syncProfileToClient(player);  // ← отправляем
        }
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        UUID uuid = event.getEntity().getUUID();
        profiles.remove(uuid);
        clientProfiles.remove(uuid);
    }

}
package com.mom.teammod;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.monster.Monster;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = TeamMod.MODID)
public class CustomStatsHandler {

    // Для расстояния — храним последнюю позицию (сервер)
    private static final Map<UUID, BlockPos> lastPlayerPos = new HashMap<>();

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ProfileManager.Profile profile = ProfileManager.getProfile(player.getUUID());
            profile.setLoginTime(System.currentTimeMillis());
            lastPlayerPos.put(player.getUUID(), player.blockPosition());

            ProfileManager.syncProfileToClient(player);

            // Помечаем данные как изменённые
            TeamWorldData data = TeamWorldData.get(player.serverLevel());
            if (data != null) {
                data.setDirty(true);
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ProfileManager.Profile profile = ProfileManager.getProfile(player.getUUID());

            // Добавляем время сессии
            long sessionMillis = profile.getCurrentSessionMillis();
            if (sessionMillis > 0) {
                int ticks = (int)(sessionMillis / 50);
                profile.addPlayTimeTicks(ticks);
            }

            lastPlayerPos.remove(player.getUUID());
            ProfileManager.syncProfileToClient(player);

            // Помечаем данные как изменённые (время добавлено)
            TeamWorldData data = TeamWorldData.get(player.serverLevel());
            if (data != null) {
                data.setDirty(true);
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ProfileManager.Profile profile = ProfileManager.getProfile(player.getUUID());
            profile.incrementDeaths();
            ProfileManager.syncProfileToClient(player);

            // Помечаем данные как изменённые
            TeamWorldData data = TeamWorldData.get(player.serverLevel());
            if (data != null) {
                data.setDirty(true);
            }
        }
    }

    @SubscribeEvent
    public static void onMobKill(LivingDeathEvent event) {
        if (event.getEntity() instanceof Monster && event.getSource().getEntity() instanceof ServerPlayer player) {
            ProfileManager.Profile profile = ProfileManager.getProfile(player.getUUID());
            profile.incrementMobsKilled();
            ProfileManager.syncProfileToClient(player);

            // Помечаем данные как изменённые
            TeamWorldData data = TeamWorldData.get(player.serverLevel());
            if (data != null) {
                data.setDirty(true);
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.side.isServer() && event.phase == TickEvent.Phase.END
                && event.player instanceof ServerPlayer player
                && player.tickCount % 5 == 0) {

            BlockPos currentPos = player.blockPosition();
            BlockPos lastPos = lastPlayerPos.getOrDefault(player.getUUID(), currentPos);

            double dist = currentPos.distSqr(lastPos);
            if (dist > 0.1) {
                long cm = (long)(Math.sqrt(dist) * 100);
                ProfileManager.Profile profile = ProfileManager.getProfile(player.getUUID());
                profile.addDistanceCm(cm);

                // Помечаем данные как изменённые (расстояние)
                TeamWorldData data = TeamWorldData.get(player.serverLevel());
                if (data != null) {
                    data.setDirty(true);
                }
            }

            lastPlayerPos.put(player.getUUID(), currentPos);
        }
    }
}
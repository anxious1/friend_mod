package com.mom.teammod;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.ElderGuardian;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = TeamMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class BossKillHandler {
    @SubscribeEvent
    public static void onBossKill(LivingDeathEvent event) {
        if (event.getEntity() instanceof EnderDragon ||
                event.getEntity() instanceof WitherBoss ||
                event.getEntity() instanceof ElderGuardian) {

            if (event.getSource().getEntity() instanceof ServerPlayer player) {
                ProfileManager.Profile profile = ProfileManager.getProfile(player.serverLevel(), player.getUUID());
                profile.incrementBossKills();
                ProfileManager.syncProfileToClient(player);
                TeamWorldData.get(player.serverLevel()).setDirty();
            }
        }
    }
}
package com.mom.teammod;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.ElderGuardian;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = TeamMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class BossKillHandler {
    @SubscribeEvent
    public static void onBossKill(net.minecraftforge.event.entity.living.LivingDeathEvent event) {
        if (event.getEntity() instanceof net.minecraft.world.entity.boss.enderdragon.EnderDragon ||
                event.getEntity() instanceof net.minecraft.world.entity.boss.wither.WitherBoss ||
                event.getEntity() instanceof net.minecraft.world.entity.monster.ElderGuardian) {
            if (event.getSource().getEntity() instanceof net.minecraft.server.level.ServerPlayer player) {
                ProfileManager.Profile profile = ProfileManager.getProfile(player.getUUID());
                int kills = profile.getCustomStats().getOrDefault("boss_kills", 0);
                profile.getCustomStats().put("boss_kills", kills + 1);
                ProfileManager.syncProfileToClient(player);
            }
        }
    }
}

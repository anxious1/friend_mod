package com.mom.teammod;

import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = TeamMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class TeamEventHandler {
    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (event.getEntity() instanceof Player target && event.getSource().getEntity() instanceof Player source) {
            if (TeamManager.isFriendlyFireDisabled(source, target)) {
                event.setCanceled(true);
            }
        }
    }
}
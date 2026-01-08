package com.mom.teammod;

import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;
import com.mom.teammod.packets.PlayerStatusPacket;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = TeamMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class StatusTicker {
    private static final Duration AFK = Duration.ofSeconds(10);

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        if (e.getServer().getTickCount() % 20 != 0) return; // 1 раз в сек

        Instant now = Instant.now();
        e.getServer().getPlayerList().getPlayers().forEach(player -> {
            UUID id = player.getUUID();
            byte st;
            if (Duration.between(LastActivityTracker.get(id), now).compareTo(AFK) >= 0)
                st = 2; // AFK
            else
                st = 1; // ONLINE

            // шлём всем онлайн-игрокам
            e.getServer().getPlayerList().getPlayers().forEach(p ->
                    NetworkHandler.INSTANCE.send(
                            PacketDistributor.PLAYER.with(() -> p),
                            new PlayerStatusPacket(id, st)));
        });
    }
}
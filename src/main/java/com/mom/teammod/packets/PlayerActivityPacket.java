package com.mom.teammod.packets;

import com.mom.teammod.LastActivityTracker;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class PlayerActivityPacket {

    public static void encode(PlayerActivityPacket pkt, FriendlyByteBuf buf) {
        // ничего не пишем — пакет пустой
    }

    public static PlayerActivityPacket decode(FriendlyByteBuf buf) {
        return new PlayerActivityPacket();
    }

    public static void handle(PlayerActivityPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            var player = ctx.get().getSender();
            if (player != null) {
                LastActivityTracker.update(player.getUUID());
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
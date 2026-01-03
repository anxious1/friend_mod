package com.mom.teammod.packets;

import com.mom.teammod.NetworkHandler;
import com.mom.teammod.ProfileManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.UUID;
import java.util.function.Supplier;

public class RequestProfilePacket {
    private final UUID targetUUID;

    public RequestProfilePacket(UUID targetUUID) {
        this.targetUUID = targetUUID;
    }

    public static void encode(RequestProfilePacket pkt, FriendlyByteBuf buf) {
        buf.writeUUID(pkt.targetUUID);
    }

    public static RequestProfilePacket decode(FriendlyByteBuf buf) {
        return new RequestProfilePacket(buf.readUUID());
    }

    public static void handle(RequestProfilePacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer sender = ctx.get().getSender();
            if (sender != null) {
                ProfileManager.Profile profile = ProfileManager.getProfile(sender.serverLevel(), pkt.targetUUID);

                // Правильная отправка пакета клиенту в 1.20.1 Forge
                NetworkHandler.INSTANCE.send(
                        PacketDistributor.PLAYER.with(() -> sender),
                        new ProfileSyncPacket(pkt.targetUUID, profile)
                );
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
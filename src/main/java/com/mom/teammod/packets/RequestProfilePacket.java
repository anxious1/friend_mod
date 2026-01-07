package com.mom.teammod.packets;

import com.mom.teammod.NetworkHandler;
import com.mom.teammod.ProfileManager;
import com.mom.teammod.TeamWorldData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.UUID;
import java.util.function.Supplier;

public class RequestProfilePacket {
    private final UUID targetUUID;   // убери static

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
            if (sender == null) return;

            // Берём профиль ТОЛЬКО из овервorld
            ServerLevel storageLevel = TeamWorldData.storageLevel(sender.getServer());
            TeamWorldData data = TeamWorldData.get(storageLevel);
            ProfileManager.Profile profile = data.getPlayerProfiles().get(pkt.targetUUID);

            if (profile == null) {
                profile = new ProfileManager.Profile(pkt.targetUUID);   // на всякий случай
            }
            NetworkHandler.INSTANCE.send(
                    PacketDistributor.PLAYER.with(() -> sender),
                    new ProfileSyncPacket(pkt.targetUUID, profile)
            );
        });
        ctx.get().setPacketHandled(true);
    }
}
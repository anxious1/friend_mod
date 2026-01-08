package com.mom.teammod.packets;

import com.mom.teammod.NetworkHandler;
import com.mom.teammod.TeamManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.UUID;
import java.util.function.Supplier;

public class SetFriendlyFirePacket {
    private String teamName;
    private boolean enabled;

    public SetFriendlyFirePacket(String teamName, boolean enabled) {
        this.teamName = teamName;
        this.enabled = enabled;
    }

    public static void encode(SetFriendlyFirePacket pkt, FriendlyByteBuf buf) {
        buf.writeUtf(pkt.teamName);
        buf.writeBoolean(pkt.enabled);
    }

    public static SetFriendlyFirePacket decode(FriendlyByteBuf buf) {
        return new SetFriendlyFirePacket(buf.readUtf(32767), buf.readBoolean());
    }

    public static void handle(SetFriendlyFirePacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            UUID ownerUUID = ctx.get().getSender().getUUID();
            if (TeamManager.setFriendlyFire(pkt.teamName, pkt.enabled, ownerUUID)) {
                TeamManager.syncTeamToAll(pkt.teamName);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
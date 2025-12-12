package com.mom.teammod.packets;

import com.mom.teammod.NetworkHandler;
import com.mom.teammod.TeamManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class RespondInvitationPacket {
    private final String teamName;
    private final boolean accept;

    public RespondInvitationPacket(String teamName, boolean accept) {
        this.teamName = teamName;
        this.accept = accept;
    }

    public static void encode(RespondInvitationPacket msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.teamName);
        buf.writeBoolean(msg.accept);
    }

    public static RespondInvitationPacket decode(FriendlyByteBuf buf) {
        return new RespondInvitationPacket(buf.readUtf(32767), buf.readBoolean());
    }

    public static void handle(RespondInvitationPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            var player = ctx.get().getSender();
            if (player == null) return;

            if (msg.accept) {
                TeamManager.acceptInvitation(msg.teamName, player.getUUID());
            } else {
                TeamManager.declineInvitation(msg.teamName, player.getUUID());
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
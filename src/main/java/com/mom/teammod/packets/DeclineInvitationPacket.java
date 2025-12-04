package com.mom.teammod.packets;

import com.mom.teammod.NetworkHandler;
import com.mom.teammod.TeamManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class DeclineInvitationPacket {
    private String teamName;

    public DeclineInvitationPacket(String teamName) {
        this.teamName = teamName;
    }

    public static void encode(DeclineInvitationPacket pkt, FriendlyByteBuf buf) {
        buf.writeUtf(pkt.teamName);
    }

    public static DeclineInvitationPacket decode(FriendlyByteBuf buf) {
        return new DeclineInvitationPacket(buf.readUtf(32767));
    }

    public static void handle(DeclineInvitationPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            if (TeamManager.declineInvitation(pkt.teamName, ctx.get().getSender().getUUID())) {
                NetworkHandler.INSTANCE.reply(new TeamSyncPacket(pkt.teamName), ctx.get());
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
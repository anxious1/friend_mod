package com.mom.teammod.packets;

import com.mom.teammod.NetworkHandler;
import com.mom.teammod.TeamManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SetInviteOnlyPacket {
    private final String teamName;
    private final boolean inviteOnly;

    public SetInviteOnlyPacket(String teamName, boolean inviteOnly) {
        this.teamName = teamName;
        this.inviteOnly = inviteOnly;
    }

    public static void encode(SetInviteOnlyPacket pkt, FriendlyByteBuf buf) {
        buf.writeUtf(pkt.teamName);
        buf.writeBoolean(pkt.inviteOnly);
    }

    public static SetInviteOnlyPacket decode(FriendlyByteBuf buf) {
        return new SetInviteOnlyPacket(buf.readUtf(32767), buf.readBoolean());
    }

    public static void handle(SetInviteOnlyPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            var sender = ctx.get().getSender();
            if (sender == null) return;

            TeamManager.Team team = TeamManager.getServerTeam(pkt.teamName);
            if (team != null && team.getOwner().equals(sender.getUUID())) {
                team.setInviteOnly(pkt.inviteOnly);
                TeamManager.getData().setDirty(true);
                TeamManager.syncTeamToAll(pkt.teamName);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
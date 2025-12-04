package com.mom.teammod.packets;

import com.mom.teammod.NetworkHandler;
import com.mom.teammod.TeamManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public class LeaveTeamPacket {
    private String teamName;

    public LeaveTeamPacket(String teamName) {
        this.teamName = teamName;
    }

    public static void encode(LeaveTeamPacket pkt, FriendlyByteBuf buf) {
        buf.writeUtf(pkt.teamName);
    }

    public static LeaveTeamPacket decode(FriendlyByteBuf buf) {
        return new LeaveTeamPacket(buf.readUtf(32767));
    }

    public static void handle(LeaveTeamPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            if (TeamManager.leaveTeam(pkt.teamName, ctx.get().getSender().getUUID())) {
                TeamManager.Team team = TeamManager.getTeam(pkt.teamName);
                if (team != null) {
                    for (UUID member : team.getMembers()) {
                        ServerPlayer player = ctx.get().getSender().getServer().getPlayerList().getPlayer(member);
                        if (player != null) {
                            NetworkHandler.INSTANCE.sendTo(new TeamSyncPacket(pkt.teamName), player.connection.connection, net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT);
                        }
                    }
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
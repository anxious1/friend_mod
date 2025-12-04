package com.mom.teammod.packets;

import com.mom.teammod.NetworkHandler;
import com.mom.teammod.TeamManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class DeleteTeamPacket {
    private final String teamName;

    public DeleteTeamPacket(String teamName) {
        this.teamName = teamName;
    }

    public static void encode(DeleteTeamPacket pkt, FriendlyByteBuf buf) {
        buf.writeUtf(pkt.teamName);
    }

    public static DeleteTeamPacket decode(FriendlyByteBuf buf) {
        return new DeleteTeamPacket(buf.readUtf(32767));
    }

    public static void handle(DeleteTeamPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            TeamManager.Team team = TeamManager.getTeam(pkt.teamName);
            if (team != null && team.getOwner().equals(ctx.get().getSender().getUUID())) {
                TeamManager.teams.remove(pkt.teamName);
                team.getMembers().forEach(member -> {
                    TeamManager.playerTeams.getOrDefault(member, java.util.Collections.emptySet()).remove(pkt.teamName);
                });
                // Синхронизация
                team.getMembers().forEach(member -> {
                    var player = ctx.get().getSender().getServer().getPlayerList().getPlayer(member);
                    if (player != null) {
                        NetworkHandler.INSTANCE.sendTo(new TeamSyncPacket(pkt.teamName), player.connection.connection, net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT);
                    }
                });
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
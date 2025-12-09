package com.mom.teammod.packets;

import com.mom.teammod.NetworkHandler;
import com.mom.teammod.TeamManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

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
            if (team == null || !team.getOwner().equals(ctx.get().getSender().getUUID())) {
                return;
            }

            // Удаляем команду и чистим у игроков
            TeamManager.teams.remove(pkt.teamName);
            team.getMembers().forEach(member -> {
                TeamManager.playerTeams
                        .getOrDefault(member, java.util.Collections.emptySet())
                        .remove(pkt.teamName);
            });

            // Отправляем пакет всем участникам (и бывшим участникам) — клиентам
            TeamSyncPacket syncPacket = new TeamSyncPacket(pkt.teamName); // пакет с именем команды → клиент просто удалит её

            team.getMembers().forEach(memberUUID -> {
                ServerPlayer player = ctx.get().getSender().getServer()
                        .getPlayerList().getPlayer(memberUUID);
                if (player != null) {
                    NetworkHandler.INSTANCE.send(
                            PacketDistributor.PLAYER.with(() -> player),
                            syncPacket
                    );
                }
            });
        });
        ctx.get().setPacketHandled(true);
    }
}
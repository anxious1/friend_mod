package com.mom.teammod.packets;

import com.mom.teammod.NetworkHandler;
import com.mom.teammod.TeamManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

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
            UUID leaverUUID = ctx.get().getSender().getUUID();

            if (TeamManager.leaveTeam(pkt.teamName, leaverUUID)) {
                TeamManager.Team team = TeamManager.getServerTeam(pkt.teamName);
                if (team != null) {
                    TeamSyncPacket syncPacket = new TeamSyncPacket(pkt.teamName);

                    // Отправляем ВСЕМ оставшимся участникам
                    for (UUID memberUUID : team.getMembers()) {
                        ServerPlayer player = ctx.get().getSender().getServer()
                                .getPlayerList().getPlayer(memberUUID);
                        if (player != null) {
                            NetworkHandler.INSTANCE.send(
                                    PacketDistributor.PLAYER.with(() -> player),
                                    syncPacket
                            );
                        }
                    }

                    // И обязательно — САМОМУ вышедшему (это критично!)
                    ServerPlayer leaver = ctx.get().getSender();
                    NetworkHandler.INSTANCE.send(
                            PacketDistributor.PLAYER.with(() -> leaver),
                            syncPacket
                    );
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
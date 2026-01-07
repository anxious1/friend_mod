package com.mom.teammod.packets;

import com.mom.teammod.LastActivityTracker;
import com.mom.teammod.NetworkHandler;
import com.mom.teammod.TeamManager;
import com.mom.teammod.TeamScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
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
            LastActivityTracker.update(ctx.get().getSender().getUUID());
            UUID leaverUUID = ctx.get().getSender().getUUID();

            // выходим из команды
            TeamManager.Team team = TeamManager.leaveTeamReturnTeam(pkt.teamName, leaverUUID);
            if (team == null) return;               // команды не было или игрок не в ней
            MinecraftServer server = ctx.get().getSender().getServer();
            if (server == null) return;

            /* 1.  Если команда расформировалась – шлём всем пакет-удаление */
            if (team.getMembers().isEmpty()) {
                TeamSyncPacket delPacket = new TeamSyncPacket(pkt.teamName); // data = null
                server.getPlayerList().getPlayers().forEach(p ->
                        NetworkHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> p), delPacket));
                return;
            }

            /* 2.  Команда жива – шлём всем обновлённые данные */
            TeamSyncPacket syncPacket = new TeamSyncPacket(pkt.teamName, team.serializeNBT());
            server.getPlayerList().getPlayers().forEach(p ->
                    NetworkHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> p), syncPacket));
        });
        // ===== ОЧИСТКА КЛИЕНТСКОГО КЭША у вышедшего =====
        NetworkHandler.INSTANCE.send(
                PacketDistributor.PLAYER.with(() -> ctx.get().getSender()),
                new TeamSyncPacket(pkt.teamName)   // null-данные = "удали у себя"
        );
        ctx.get().setPacketHandled(true);
    }
}
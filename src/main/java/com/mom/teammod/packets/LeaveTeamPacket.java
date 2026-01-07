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

            if (TeamManager.leaveTeam(pkt.teamName, leaverUUID)) {
                // Отправляем обновление ВСЕМ (включая вышедшего)
                TeamSyncPacket syncPacket = new TeamSyncPacket(pkt.teamName);
                MinecraftServer server = ctx.get().getSender().getServer();
                if (server != null) {
                    server.getPlayerList().getPlayers().forEach(p ->
                            NetworkHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> p), syncPacket)
                    );
                }

                // ВАЖНО: НИЧЕГОСЯ НЕ ВЫЗЫВАЕМ returnToTeamScreen() ЗДЕСЬ!
                // Клиент сам закроет профиль и обновит список через TeamSyncPacket
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
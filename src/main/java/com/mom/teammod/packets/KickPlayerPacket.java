package com.mom.teammod.packets;

import com.mom.teammod.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.UUID;
import java.util.function.Supplier;

public class KickPlayerPacket {
    private String teamName;
    private UUID playerUUID;

    public KickPlayerPacket(String teamName, UUID playerUUID) {
        this.teamName = teamName;
        this.playerUUID = playerUUID;
    }

    public static void encode(KickPlayerPacket pkt, FriendlyByteBuf buf) {
        buf.writeUtf(pkt.teamName);
        buf.writeUUID(pkt.playerUUID);
    }

    public static KickPlayerPacket decode(FriendlyByteBuf buf) {
        return new KickPlayerPacket(buf.readUtf(32767), buf.readUUID());
    }

    public static void handle(KickPlayerPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            LastActivityTracker.update(ctx.get().getSender().getUUID());
            UUID kickerUUID = ctx.get().getSender().getUUID();

            if (!TeamManager.kickPlayer(pkt.teamName, pkt.playerUUID, kickerUUID)) return;

            MinecraftServer server = ctx.get().getSender().getServer();
            if (server == null) return;

            /* 1. Кикнутому – удалить у себя */
            ServerPlayer kicked = server.getPlayerList().getPlayer(pkt.playerUUID);
            if (kicked != null) {
                NetworkHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> kicked),
                        new TeamSyncPacket(pkt.teamName));   // data = null
            }

            /* 2. Всем остальным – обновлённые данные команды */
            TeamManager.Team team = TeamManager.getServerTeam(pkt.teamName);
            TeamSyncPacket sync = team == null
                    ? new TeamSyncPacket(pkt.teamName)          // команда исчезла
                    : new TeamSyncPacket(pkt.teamName, team.serializeNBT());

            for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                if (!p.getUUID().equals(pkt.playerUUID)) {     // кроме кикнутого
                    NetworkHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> p), sync);
                }
            }

            /* 3. Если кикнули текущего клиента – вернуться в TeamScreen */
            if (pkt.playerUUID.equals(Minecraft.getInstance().player.getUUID())) {
                Minecraft.getInstance().execute(TeamScreen::returnToTeamScreen);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
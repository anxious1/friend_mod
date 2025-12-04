package com.mom.teammod.packets;

import com.mom.teammod.NetworkHandler;
import com.mom.teammod.TeamManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

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
            if (TeamManager.kickPlayer(pkt.teamName, pkt.playerUUID, ctx.get().getSender().getUUID())) {
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
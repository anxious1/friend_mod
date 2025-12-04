package com.mom.teammod.packets;

import com.mom.teammod.NetworkHandler;
import com.mom.teammod.TeamManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public class TransferOwnershipPacket {
    private String teamName;
    private UUID newOwnerUUID;

    public TransferOwnershipPacket(String teamName, UUID newOwnerUUID) {
        this.teamName = teamName;
        this.newOwnerUUID = newOwnerUUID;
    }

    public static void encode(TransferOwnershipPacket pkt, FriendlyByteBuf buf) {
        buf.writeUtf(pkt.teamName);
        buf.writeUUID(pkt.newOwnerUUID);
    }

    public static TransferOwnershipPacket decode(FriendlyByteBuf buf) {
        return new TransferOwnershipPacket(buf.readUtf(32767), buf.readUUID());
    }

    public static void handle(TransferOwnershipPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            if (TeamManager.transferOwnership(pkt.teamName, pkt.newOwnerUUID, ctx.get().getSender().getUUID())) {
                // Синхронизировать
                TeamManager.Team team = TeamManager.getServerTeam(pkt.teamName);
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
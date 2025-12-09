package com.mom.teammod.packets;

import com.mom.teammod.NetworkHandler;
import com.mom.teammod.TeamManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.UUID;
import java.util.function.Supplier;

public class TransferOwnershipPacket {
    private String teamName;
    UUID newOwnerUUID;

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
            boolean success = TeamManager.transferOwnership(
                    pkt.teamName,
                    pkt.newOwnerUUID,
                    ctx.get().getSender().getUUID()
            );

            if (success) {
                TeamManager.Team team = TeamManager.getServerTeam(pkt.teamName);
                if (team != null) {
                    TeamSyncPacket syncPacket = new TeamSyncPacket(pkt.teamName);

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
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
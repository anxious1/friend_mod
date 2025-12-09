package com.mom.teammod.packets;

import com.mom.teammod.NetworkHandler;
import com.mom.teammod.TeamManager;
import net.minecraft.network.FriendlyByteBuf;
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
            UUID kickerUUID = ctx.get().getSender().getUUID();

            if (TeamManager.kickPlayer(pkt.teamName, pkt.playerUUID, kickerUUID)) {
                TeamManager.Team team = TeamManager.getServerTeam(pkt.teamName);
                if (team != null) {
                    TeamSyncPacket syncPacket = new TeamSyncPacket(pkt.teamName);

                    // Отправляем всем оставшимся участникам + выгнанному (чтобы он увидел удаление команды из списка)
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

                    // Также отправляем выгнанному игроку (он уже не в members, но должен получить обновление)
                    ServerPlayer kickedPlayer = ctx.get().getSender().getServer()
                            .getPlayerList().getPlayer(pkt.playerUUID);
                    if (kickedPlayer != null) {
                        NetworkHandler.INSTANCE.send(
                                PacketDistributor.PLAYER.with(() -> kickedPlayer),
                                syncPacket
                        );
                    }
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
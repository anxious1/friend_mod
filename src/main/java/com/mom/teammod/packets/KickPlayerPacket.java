package com.mom.teammod.packets;

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
                // Отправляем ВСЕМ (включая выгнанного)
                TeamSyncPacket syncPacket = new TeamSyncPacket(pkt.teamName);
                MinecraftServer server = ctx.get().getSender().getServer();
                if (server != null) {
                    server.getPlayerList().getPlayers().forEach(p ->
                            NetworkHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> p), syncPacket)
                    );
                }

                // Если это ТЫ был выгнан — кидаем в TeamScreen
                if (pkt.playerUUID.equals(Minecraft.getInstance().player.getUUID())) {
                    Minecraft.getInstance().execute(() -> TeamScreen.returnToTeamScreen());
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
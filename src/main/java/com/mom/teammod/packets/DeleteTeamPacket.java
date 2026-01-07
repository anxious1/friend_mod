// DeleteTeamPacket.java
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
            LastActivityTracker.update(ctx.get().getSender().getUUID());
            ServerPlayer player = ctx.get().getSender();

            if (TeamManager.deleteTeam(pkt.teamName, player.getUUID())) {
                // Отправляем ВСЕМ игрокам
                TeamSyncPacket syncPacket = new TeamSyncPacket(pkt.teamName);
                MinecraftServer server = player.getServer();
                if (server != null) {
                    server.getPlayerList().getPlayers().forEach(p ->
                            NetworkHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> p), syncPacket)
                    );
                }

                // Возвращаем лидера в TeamScreen
                Minecraft.getInstance().execute(() -> TeamScreen.returnToTeamScreen());
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
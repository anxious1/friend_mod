package com.mom.teammod.packets;

import com.mom.teammod.NetworkHandler;
import com.mom.teammod.TeamManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.function.Supplier;

public class AskMyLeaderTeamsPacket {

    public AskMyLeaderTeamsPacket() { }

    public static void encode(AskMyLeaderTeamsPacket pkt, FriendlyByteBuf buf) { }

    public static AskMyLeaderTeamsPacket decode(FriendlyByteBuf buf) {
        return new AskMyLeaderTeamsPacket();
    }

    /* Сервер: высылаем только команды, где отправитель = владелец */
    public static void handle(AskMyLeaderTeamsPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            /* собираем NBT только своих команд */
            TeamManager.getLeaderTeams(player.getUUID()).forEach(team ->
                    NetworkHandler.INSTANCE.send(
                            PacketDistributor.PLAYER.with(() -> player),
                            new TeamSyncPacket(team.getName(), team.serializeNBT())
                    )
            );
        });
        ctx.get().setPacketHandled(true);
    }
}
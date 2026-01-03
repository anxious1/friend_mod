package com.mom.teammod.packets;

import com.mom.teammod.NetworkHandler;
import com.mom.teammod.TeamManager;
import com.mom.teammod.TeamProfileOwner;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.UUID;
import java.util.function.Supplier;

public class AcceptInvitationPacket {
    private final String teamName;

    public AcceptInvitationPacket(String teamName) {
        this.teamName = teamName;
    }

    public static void encode(AcceptInvitationPacket pkt, FriendlyByteBuf buf) {
        buf.writeUtf(pkt.teamName);
    }

    public static AcceptInvitationPacket decode(FriendlyByteBuf buf) {
        return new AcceptInvitationPacket(buf.readUtf(32767));
    }

    public static void handle(AcceptInvitationPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            UUID playerUUID = ctx.get().getSender().getUUID();

            if (TeamManager.acceptInvitation(pkt.teamName, playerUUID)) {
                TeamManager.Team team = TeamManager.getServerTeam(pkt.teamName);
                if (team != null) {
                    TeamSyncPacket syncPacket = new TeamSyncPacket(pkt.teamName,team.serializeNBT());

                    // Отправляем всем участникам
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

                    // Если это ТЫ принял приглашение — открываем профиль команды
                    if (playerUUID.equals(Minecraft.getInstance().player.getUUID())) {
                        Minecraft.getInstance().execute(() -> {
                            Minecraft mc = Minecraft.getInstance();
                            mc.setScreen(new TeamProfileOwner(null, null, mc.player.getInventory(), Component.literal(pkt.teamName), pkt.teamName, team.getTag(), mc.player.getUUID().equals(team.getOwner()), team.showTag(), team.isFriendlyFire()));
                        });
                    }
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
package com.mom.teammod.packets;

import com.mom.teammod.NetworkHandler;
import com.mom.teammod.TeamManager;
import com.mom.teammod.TeamMemberScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.function.Supplier;

public class RespondInvitationPacket {
    private final String teamName;
    private final boolean accept;

    public RespondInvitationPacket(String teamName, boolean accept) {
        this.teamName = teamName;
        this.accept = accept;
    }

    public static void encode(RespondInvitationPacket msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.teamName);
        buf.writeBoolean(msg.accept);
    }

    public static RespondInvitationPacket decode(FriendlyByteBuf buf) {
        return new RespondInvitationPacket(buf.readUtf(32767), buf.readBoolean());
    }

    public static void handle(RespondInvitationPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            if (msg.accept) {
                boolean success = TeamManager.acceptInvitation(msg.teamName, player.getUUID());
                if (success) {
                    player.sendSystemMessage(Component.literal("§aВы приняли приглашение и вступили в команду §f" + msg.teamName));

                    // Открываем экран ТОЛЬКО у этого игрока
                    NetworkHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), new OpenTeamMemberScreenPacket(msg.teamName));
                } else {
                    player.sendSystemMessage(Component.literal("§cНе удалось принять приглашение."));
                }
            } else {
                TeamManager.declineInvitation(msg.teamName, player.getUUID());
                player.sendSystemMessage(Component.literal("§eВы отклонили приглашение в команду §f" + msg.teamName));
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
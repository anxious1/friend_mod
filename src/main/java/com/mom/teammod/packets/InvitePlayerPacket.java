package com.mom.teammod.packets;

import com.mom.teammod.NetworkHandler;
import com.mom.teammod.TeamManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public class InvitePlayerPacket {
    private final String teamName;
    private final String playerName;

    public InvitePlayerPacket(String teamName, String playerName) {
        this.teamName = teamName;
        this.playerName = playerName;
    }

    public static void encode(InvitePlayerPacket pkt, FriendlyByteBuf buf) {
        buf.writeUtf(pkt.teamName);
        buf.writeUtf(pkt.playerName);
    }

    public static InvitePlayerPacket decode(FriendlyByteBuf buf) {
        return new InvitePlayerPacket(buf.readUtf(32767), buf.readUtf(32767));
    }

    public static void handle(InvitePlayerPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer inviter = ctx.get().getSender();
            ServerPlayer invited = inviter.getServer().getPlayerList().getPlayerByName(pkt.playerName);
            if (invited != null && TeamManager.invitePlayer(pkt.teamName, invited.getUUID(), inviter)) {
                Component acceptButton = Component.literal("[Accept]").setStyle(Component.empty().getStyle()
                        .withClickEvent(new net.minecraft.network.chat.ClickEvent(net.minecraft.network.chat.ClickEvent.Action.RUN_COMMAND, "/accept " + pkt.teamName))
                        .withHoverEvent(new net.minecraft.network.chat.HoverEvent(net.minecraft.network.chat.HoverEvent.Action.SHOW_TEXT, Component.literal("Accept invitation"))));
                Component declineButton = Component.literal("[Decline]").setStyle(Component.empty().getStyle()
                        .withClickEvent(new net.minecraft.network.chat.ClickEvent(net.minecraft.network.chat.ClickEvent.Action.RUN_COMMAND, "/decline " + pkt.teamName))
                        .withHoverEvent(new net.minecraft.network.chat.HoverEvent(net.minecraft.network.chat.HoverEvent.Action.SHOW_TEXT, Component.literal("Decline invitation"))));
                invited.sendSystemMessage(Component.literal("Invited to " + pkt.teamName + " by " + inviter.getName().getString() + ". ").append(acceptButton).append(" ").append(declineButton));
                NetworkHandler.INSTANCE.sendTo(new TeamSyncPacket(pkt.teamName), invited.connection.connection, net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT);
                NetworkHandler.INSTANCE.reply(new TeamSyncPacket(pkt.teamName), ctx.get());
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
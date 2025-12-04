package com.mom.teammod.packets;

import com.mom.teammod.TeamManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public class SendChatPacket {
    private String teamName;
    private String message;

    public SendChatPacket(String teamName, String message) {
        this.teamName = teamName;
        this.message = message;
    }

    public static void encode(SendChatPacket pkt, FriendlyByteBuf buf) {
        buf.writeUtf(pkt.teamName);
        buf.writeUtf(pkt.message);
    }

    public static SendChatPacket decode(FriendlyByteBuf buf) {
        return new SendChatPacket(buf.readUtf(32767), buf.readUtf(32767));
    }

    public static void handle(SendChatPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            TeamManager.Team team = TeamManager.getServerTeam(pkt.teamName);
            if (team != null && team.getMembers().contains(ctx.get().getSender().getUUID())) {
                Component chatMessage = Component.literal("[" + pkt.teamName + "] " + ctx.get().getSender().getName().getString() + ": " + pkt.message);
                for (UUID memberId : team.getMembers()) {
                    ServerPlayer member = ctx.get().getSender().getServer().getPlayerList().getPlayer(memberId);
                    if (member != null) {
                        member.sendSystemMessage(chatMessage);
                    }
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
package com.mom.teammod.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public class PrivateMessagePacket {
    private UUID senderUUID;
    private UUID recipientUUID;
    private String message;

    public PrivateMessagePacket(UUID senderUUID, UUID recipientUUID, String message) {
        this.senderUUID = senderUUID;
        this.recipientUUID = recipientUUID;
        this.message = message;
    }

    public static void encode(PrivateMessagePacket pkt, FriendlyByteBuf buf) {
        buf.writeUUID(pkt.senderUUID);
        buf.writeUUID(pkt.recipientUUID);
        buf.writeUtf(pkt.message);
    }

    public static PrivateMessagePacket decode(FriendlyByteBuf buf) {
        return new PrivateMessagePacket(buf.readUUID(), buf.readUUID(), buf.readUtf());
    }

    public static void handle(PrivateMessagePacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer sender = ctx.get().getSender();
            if (sender != null && sender.getUUID().equals(pkt.senderUUID)) {
                MinecraftServer server = sender.getServer();
                ServerPlayer recipient = server.getPlayerList().getPlayer(pkt.recipientUUID);
                if (recipient != null) {
                    Component pm = Component.literal("[PM from " + sender.getName().getString() + "] " + pkt.message);
                    recipient.sendSystemMessage(pm);
                    sender.sendSystemMessage(Component.literal("[PM to " + recipient.getName().getString() + "] " + pkt.message));
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
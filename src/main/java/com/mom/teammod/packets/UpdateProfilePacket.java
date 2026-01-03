package com.mom.teammod.packets;

import com.mom.teammod.NetworkHandler;
import com.mom.teammod.ProfileManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class UpdateProfilePacket {
    private String background;

    public UpdateProfilePacket(String background) {
        this.background = background;
    }

    public static void encode(UpdateProfilePacket pkt, FriendlyByteBuf buf) {
        buf.writeUtf(pkt.background);
    }

    public static UpdateProfilePacket decode(FriendlyByteBuf buf) {
        return new UpdateProfilePacket(buf.readUtf(32767));
    }

    public static void handle(UpdateProfilePacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer sender = ctx.get().getSender();
            ProfileManager.Profile profile = ProfileManager.getProfile(sender.serverLevel(), sender.getUUID());
            profile.setBackground(pkt.background);
            ProfileManager.syncProfileToClient(ctx.get().getSender());
        });
        ctx.get().setPacketHandled(true);
    }
}
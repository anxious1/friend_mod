package com.mom.teammod.packets;

import com.mom.teammod.NetworkHandler;
import com.mom.teammod.ProfileManager;
import com.mom.teammod.TeamWorldData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class UpdateCompassVisibilityPacket {
    private final boolean showOnCompass;

    public UpdateCompassVisibilityPacket(boolean showOnCompass) {
        this.showOnCompass = showOnCompass;
    }

    public static void encode(UpdateCompassVisibilityPacket pkt, FriendlyByteBuf buf) {
        buf.writeBoolean(pkt.showOnCompass);
    }

    public static UpdateCompassVisibilityPacket decode(FriendlyByteBuf buf) {
        return new UpdateCompassVisibilityPacket(buf.readBoolean());
    }

    public static void handle(UpdateCompassVisibilityPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer sender = ctx.get().getSender();
            if (sender == null) return;

            ProfileManager.Profile profile = ProfileManager.getProfile(sender.serverLevel(), sender.getUUID());
            profile.setShowOnCompass(pkt.showOnCompass);

            TeamWorldData data = TeamWorldData.get(sender.serverLevel());
            if (data != null) {
                data.setDirty(true);
            }

            // Синхронизируем профиль ВСЕМ игрокам (видимость влияет на всех)
            for (ServerPlayer player : sender.getServer().getPlayerList().getPlayers()) {
                ProfileManager.syncProfileToClient(player);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
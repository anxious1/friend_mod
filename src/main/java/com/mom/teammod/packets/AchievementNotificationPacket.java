package com.mom.teammod.packets;

import com.mom.teammod.AchievementToast;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class AchievementNotificationPacket {
    private final String title;
    private final String description;
    private final String iconItem; // "" или null = без иконки
    private final boolean isPositive;

    public AchievementNotificationPacket(String title, String description, String iconItem, boolean isPositive) {
        this.title = title;
        this.description = description;
        this.iconItem = iconItem == null ? "" : iconItem;
        this.isPositive = isPositive;
    }

    public static void encode(AchievementNotificationPacket pkt, FriendlyByteBuf buf) {
        buf.writeUtf(pkt.title);
        buf.writeUtf(pkt.description);
        buf.writeUtf(pkt.iconItem);
        buf.writeBoolean(pkt.isPositive);
    }

    public static AchievementNotificationPacket decode(FriendlyByteBuf buf) {
        return new AchievementNotificationPacket(
                buf.readUtf(),
                buf.readUtf(),
                buf.readUtf(),
                buf.readBoolean()
        );
    }

    public static void handle(AchievementNotificationPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> AchievementToast.show(pkt.title, pkt.description, pkt.iconItem, pkt.isPositive));
        ctx.get().setPacketHandled(true);
    }
}
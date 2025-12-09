package com.mom.teammod.packets;

import com.mom.teammod.NetworkHandler;
import com.mom.teammod.TeamManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

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

            if (invited == null) {
                inviter.sendSystemMessage(Component.literal("§cИгрок не найден или не в сети."));
                return;
            }

            if (TeamManager.invitePlayer(pkt.teamName, invited.getUUID(), inviter)) {
                // Красивое сообщение с кнопками
                Component acceptButton = Component.literal("[Принять]")
                        .withStyle(style -> style
                                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/teammod_accept " + pkt.teamName))
                                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("§aПринять приглашение"))));

                Component declineButton = Component.literal("[Отклонить]")
                        .withStyle(style -> style
                                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/teammod_decline " + pkt.teamName))
                                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("§cОтклонить приглашение"))));

                invited.sendSystemMessage(
                        Component.literal("§eВы получили приглашение в команду §b" + pkt.teamName + " §eот §f" + inviter.getName().getString() + "§e. ")
                                .append(acceptButton)
                                .append(Component.literal(" "))
                                .append(declineButton)
                );

                inviter.sendSystemMessage(Component.literal("§aПриглашение отправлено игроку §f" + pkt.playerName));

                // Синхронизация — безопасно и без крашей
                TeamSyncPacket syncPacket = new TeamSyncPacket(pkt.teamName);

                NetworkHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> invited), syncPacket);
                NetworkHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> inviter), syncPacket);
            } else {
                inviter.sendSystemMessage(Component.literal("§cНе удалось отправить приглашение (возможно, лимит команд или уже приглашён)."));
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
package com.mom.teammod.packets;

import com.mom.teammod.LastActivityTracker;
import com.mom.teammod.NetworkHandler;
import com.mom.teammod.PlayerNameCache;
import com.mom.teammod.TeamManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

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
            LastActivityTracker.update(ctx.get().getSender().getUUID());
            ServerPlayer inviter = ctx.get().getSender();
            if (inviter == null) return;

            /* поиск цели */
            ServerPlayer invited = inviter.getServer().getPlayerList().getPlayerByName(pkt.playerName);
            UUID target = invited == null
                    ? PlayerNameCache.getUUID(pkt.playerName)   // оффлайн
                    : invited.getUUID();
            if (target == null) {
                inviter.sendSystemMessage(Component.literal("§cИгрок не найден."));
                return;
            }

            /* собственно приглашение */
            boolean ok = TeamManager.invitePlayer(pkt.teamName, target, inviter);
            if (!ok) {
                inviter.sendSystemMessage(Component.literal("§cУже в команде / уже приглашён."));
                return;
            }

            /* сообщения */
            if (invited != null) { // онлайн
                Component accept = Component.literal("[Принять]")
                        .withStyle(s -> s.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/teammod_accept " + pkt.teamName))
                                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("§aПринять"))));
                Component decline = Component.literal("[Отклонить]")
                        .withStyle(s -> s.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/teammod_decline " + pkt.teamName))
                                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("§cОтклонить"))));
                invited.sendSystemMessage(
                        Component.literal("§eПриглашение в §b" + pkt.teamName + " §eот §f" + inviter.getName().getString() + "§e. ")
                                .append(accept).append(" ").append(decline)
                );
            }
            inviter.sendSystemMessage(Component.literal("§aПриглашение отправлено."));

            TeamManager.syncTeamToAll(pkt.teamName);
        });
        ctx.get().setPacketHandled(true);
    }
}
package com.mom.teammod.packets;

import com.mom.teammod.LastActivityTracker;
import com.mom.teammod.NetworkHandler;
import com.mom.teammod.TeamManager;
import com.mom.teammod.TeamMemberScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.Collections;
import java.util.HashSet;
import java.util.function.Supplier;

public class JoinTeamPacket {
    private final String teamName;

    public JoinTeamPacket(String teamName) {
        this.teamName = teamName;
    }

    public static void encode(JoinTeamPacket pkt, FriendlyByteBuf buf) {
        buf.writeUtf(pkt.teamName);
    }

    public static JoinTeamPacket decode(FriendlyByteBuf buf) {
        return new JoinTeamPacket(buf.readUtf(32767));
    }

    public static void handle(JoinTeamPacket pkt, Supplier<NetworkEvent.Context> ctx) {

        ctx.get().enqueueWork(() -> {
            LastActivityTracker.update(ctx.get().getSender().getUUID());
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            TeamManager.Team team = TeamManager.getServerTeam(pkt.teamName);
            if (team == null || team.isInviteOnly()) {
                player.sendSystemMessage(Component.literal("§cНельзя вступить: команда закрыта или не существует."));
                return;
            }

            if (team.getMembers().contains(player.getUUID())) {
                player.sendSystemMessage(Component.literal("§eВы уже в этой команде."));
                return;
            }

            if (TeamManager.getData().getPlayerTeams().getOrDefault(player.getUUID(), Collections.emptySet()).size() >= 3) {
                player.sendSystemMessage(Component.literal("§cВы уже состоите в 3 командах."));
                return;
            }

            // Добавляем в команду
            team.addMember(player.getUUID());
            TeamManager.getData().getPlayerTeams().computeIfAbsent(player.getUUID(), k -> new HashSet<>()).add(pkt.teamName);
            team.getInvited().remove(player.getUUID());

            TeamManager.getData().setDirty(true);

            player.sendSystemMessage(Component.literal("§aВы успешно вступили в команду §f" + pkt.teamName));

            TeamManager.syncTeamToAll(pkt.teamName);

            // ← КЛЮЧЕВОЕ ИСПРАВЛЕНИЕ: отправляем пакет только этому игроку, чтобы открыть экран на ЕГО клиенте
            NetworkHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), new OpenTeamMemberScreenPacket(pkt.teamName));
        });
        ctx.get().setPacketHandled(true);
    }
}
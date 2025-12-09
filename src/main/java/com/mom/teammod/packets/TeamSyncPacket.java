package com.mom.teammod.packets;

import com.mom.teammod.TeamManager;
import com.mom.teammod.TeamScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

public class TeamSyncPacket {

    private final String teamName;
    private final UUID owner;
    private final Set<UUID> members = new HashSet<>();
    private final Set<UUID> invited = new HashSet<>();
    private final boolean friendlyFire;
    private final boolean showTag;
    private final boolean showCompass;
    private final String tag; // ← теперь final

    // Конструктор для отправки с сервера
    public TeamSyncPacket(String teamName) {
        TeamManager.Team serverTeam = TeamManager.getServerTeam(teamName);
        if (serverTeam != null) {
            this.teamName = teamName;
            this.owner = serverTeam.getOwner();
            this.tag = serverTeam.getTag();
            this.friendlyFire = serverTeam.isFriendlyFire();
            this.showTag = serverTeam.showTag();
            this.showCompass = serverTeam.showCompass();

            this.members.addAll(serverTeam.getMembers());
            this.invited.addAll(serverTeam.getInvited());
        } else {
            this.teamName = teamName;
            this.owner = null;
            this.tag = "";
            this.friendlyFire = true;
            this.showTag = true;
            this.showCompass = true;
        }
    }

    // Конструктор для чтения из пакета
    public TeamSyncPacket(FriendlyByteBuf buf) {
        this.teamName = buf.readUtf(32767);
        this.owner = buf.readBoolean() ? buf.readUUID() : null;

        int memberCount = buf.readInt();
        for (int i = 0; i < memberCount; i++) {
            members.add(buf.readUUID());
        }

        int invitedCount = buf.readInt();
        for (int i = 0; i < invitedCount; i++) {
            invited.add(buf.readUUID());
        }

        this.friendlyFire = buf.readBoolean();
        this.showTag = buf.readBoolean();
        this.showCompass = buf.readBoolean();
        this.tag = buf.readUtf(32767); // ← ЧИТАЕМ В ТОМ ЖЕ ПОРЯДКЕ!
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(teamName);
        buf.writeBoolean(owner != null);
        if (owner != null) buf.writeUUID(owner);

        buf.writeInt(members.size());
        for (UUID uuid : members) buf.writeUUID(uuid);

        buf.writeInt(invited.size());
        for (UUID uuid : invited) buf.writeUUID(uuid);

        buf.writeBoolean(friendlyFire);
        buf.writeBoolean(showTag);
        buf.writeBoolean(showCompass);
        buf.writeUtf(tag); // ← ПИШЕМ В ТОМ ЖЕ ПОРЯДКЕ!
    }

    public static void handle(TeamSyncPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            TeamManager.Team team = new TeamManager.Team(pkt.teamName, pkt.owner);
            team.setTag(pkt.tag);
            team.setFriendlyFire(pkt.friendlyFire);
            team.setShowTag(pkt.showTag);
            team.setShowCompass(pkt.showCompass);

            team.getMembers().clear();
            team.getMembers().addAll(pkt.members);
            team.getInvited().clear();
            team.getInvited().addAll(pkt.invited);

            TeamManager.clientTeams.put(pkt.teamName, team);

            UUID playerUUID = Minecraft.getInstance().player.getUUID();
            TeamManager.clientPlayerTeams.computeIfAbsent(playerUUID, k -> new HashSet<>()).add(pkt.teamName);

            // Обновляем TeamScreen, если открыт
            if (Minecraft.getInstance().screen instanceof TeamScreen screen) {
                screen.refreshLists();
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
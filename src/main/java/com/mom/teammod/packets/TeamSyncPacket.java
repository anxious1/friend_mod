package com.mom.teammod.packets;

import com.mom.teammod.TeamManager;
import com.mom.teammod.TeamProfileOwner;
import com.mom.teammod.TeamScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

import static com.mom.teammod.TeamManager.clientPlayerTeams;
import static com.mom.teammod.TeamManager.clientTeams;

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
        System.out.println("[TeamSyncPacket] Создаём пакет для команды: " + teamName +
                " | existsOnServer=" + (serverTeam != null));

        if (serverTeam != null) {
            this.teamName = teamName;
            this.owner = serverTeam.getOwner();
            this.tag = serverTeam.getTag();
            this.friendlyFire = serverTeam.isFriendlyFire();
            this.showTag = serverTeam.showTag();
            this.showCompass = serverTeam.showCompass();
            this.members.addAll(serverTeam.getMembers());
            this.invited.addAll(serverTeam.getInvited());
            System.out.println("[TeamSyncPacket] Команда существует, owner=" + owner + ", members=" + members.size());
        } else {
            this.teamName = teamName;
            this.owner = null;
            this.tag = "";
            this.friendlyFire = true;
            this.showTag = true;
            this.showCompass = true;
            System.out.println("[TeamSyncPacket] Команда УДАЛЕНА — отправляем owner=null");
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
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null) return;

            System.out.println("\n=== TeamSyncPacket ПРИШЁЛ НА КЛИЕНТ ===");
            System.out.println("teamName = " + pkt.teamName);
            System.out.println("owner = " + pkt.owner);
            System.out.println("members.size = " + pkt.members.size());
            System.out.println("clientTeams до: " + TeamManager.clientTeams.keySet());
            System.out.println("myTeams до: " + TeamManager.clientPlayerTeams.getOrDefault(mc.player.getUUID(), Set.of()));

            UUID playerUUID = mc.player.getUUID();
            Set<String> myTeams = clientPlayerTeams.computeIfAbsent(playerUUID, k -> new HashSet<>());

            if (pkt.owner == null) {
                System.out.println("[TeamSyncPacket] УДАЛЕНИЕ команды: " + pkt.teamName);
                clientTeams.remove(pkt.teamName);
                myTeams.remove(pkt.teamName);
            } else {
                TeamManager.Team team = new TeamManager.Team(pkt.teamName, pkt.owner);
                team.setTag(pkt.tag);
                team.setFriendlyFire(pkt.friendlyFire);
                team.setShowTag(pkt.showTag);
                team.setShowCompass(pkt.showCompass);
                team.getMembers().clear();
                team.getMembers().addAll(pkt.members);
                team.getInvited().clear();
                team.getInvited().addAll(pkt.invited);

                clientTeams.put(pkt.teamName, team);

                if (pkt.members.contains(playerUUID)) {
                    myTeams.add(pkt.teamName);
                } else {
                    myTeams.remove(pkt.teamName);
                }
            }

            System.out.println("clientTeams после: " + TeamManager.clientTeams.keySet());
            System.out.println("myTeams после: " + myTeams);

            // Обновляем UI
            mc.execute(() -> {
                System.out.println("[TeamSyncPacket] Выполняем refreshLists() в рендер-потоке");
                if (mc.screen instanceof TeamScreen teamScreen) {
                    System.out.println("[TeamSyncPacket] TeamScreen найден — вызываем refreshLists()");
                    teamScreen.refreshLists();
                } else {
                    System.out.println("[TeamSyncPacket] TeamScreen НЕ открыт сейчас. Экран: " +
                            (mc.screen != null ? mc.screen.getClass().getSimpleName() : "null"));
                }
            });

            System.out.println("=== TeamSyncPacket обработан ===\n");
        });
        ctx.get().setPacketHandled(true);
    }
}
package com.mom.teammod.packets;

import com.mom.teammod.TeamManager;
import com.mom.teammod.TeamScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

public class TeamSyncPacket {

    public final String teamName;
    public final CompoundTag teamData; // Полные данные команды или null для удаления/очистки

    // Конструктор для полной очистки всех команд
    public TeamSyncPacket() {
        this.teamName = "";
        this.teamData = null;
    }

    // Конструктор для удаления или обновления конкретной команды
    public TeamSyncPacket(String teamName, CompoundTag teamData) {
        this.teamName = teamName;
        this.teamData = teamData;
    }

    // Конструктор для удаления (teamData = null)
    public TeamSyncPacket(String teamName) {
        this.teamName = teamName;
        this.teamData = null;
    }

    public static void encode(TeamSyncPacket pkt, FriendlyByteBuf buf) {
        buf.writeUtf(pkt.teamName);
        buf.writeBoolean(pkt.teamData != null);
        if (pkt.teamData != null) {
            buf.writeNbt(pkt.teamData);
        }
    }

    public static TeamSyncPacket decode(FriendlyByteBuf buf) {
        String name = buf.readUtf(32767);
        boolean hasData = buf.readBoolean();
        CompoundTag data = hasData ? buf.readNbt() : null;
        return new TeamSyncPacket(name, data);
    }

    public static void handle(TeamSyncPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            System.out.println("[Client] TeamSyncPacket получен! teamName = '" + pkt.teamName + "', hasData = " + (pkt.teamData != null));
            // Этот код выполняется на клиенте
            if (pkt.teamName.isEmpty() && pkt.teamData == null) {
                // Полная очистка всех команд
                TeamManager.clientTeams.clear();
                TeamManager.clientPlayerTeams.clear();
                System.out.println("[Client] Получена команда на полную очистку команд");
                refreshScreenIfOpen();
                return;
            }

            UUID playerUUID = Minecraft.getInstance().player != null ? Minecraft.getInstance().player.getUUID() : null;

            if (pkt.teamData == null) {
                // Удаление команды
                TeamManager.clientTeams.remove(pkt.teamName);
                if (playerUUID != null) {
                    Set<String> myTeams = TeamManager.clientPlayerTeams.getOrDefault(playerUUID, new HashSet<>());
                    myTeams.remove(pkt.teamName);
                }
                System.out.println("[Client] Команда удалена: " + pkt.teamName);
            } else {
                // Создание или обновление команды
                TeamManager.Team team = new TeamManager.Team(pkt.teamName, null);
                team.deserializeNBT(pkt.teamData);
                TeamManager.clientTeams.put(pkt.teamName, team);

                if (playerUUID != null) {
                    Set<String> myTeams = TeamManager.clientPlayerTeams.computeIfAbsent(playerUUID, k -> new HashSet<>());
                    if (team.getMembers().contains(playerUUID)) {
                        myTeams.add(pkt.teamName);
                    } else {
                        myTeams.remove(pkt.teamName);
                    }
                }
                System.out.println("[Client] Команда синхронизирована: " + pkt.teamName + " (members: " + team.getMembers().size() + ")");
            }

            refreshScreenIfOpen();
        });
        ctx.get().setPacketHandled(true);
    }

    // Вынес обновление экрана в отдельный метод, чтобы не дублировать
    private static void refreshScreenIfOpen() {
        Minecraft.getInstance().execute(() -> {
            if (Minecraft.getInstance().screen instanceof TeamScreen screen) {
                screen.refreshLists();
                System.out.println("[Client] TeamScreen обновлён после синхронизации");
            }
        });
    }
}
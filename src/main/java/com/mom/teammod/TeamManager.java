package com.mom.teammod;

import com.electronwill.nightconfig.core.conversion.ConversionTable;
import com.mom.teammod.packets.AchievementNotificationPacket;
import com.mom.teammod.packets.StatsSyncPacket;
import com.mom.teammod.packets.TeamSyncPacket;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.server.ServerLifecycleHooks;
import net.minecraft.world.entity.TamableAnimal;

import java.util.*;
import java.util.stream.Collectors;

@Mod.EventBusSubscriber(modid = TeamMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class TeamManager {
    // Клиентские данные (остаются для отображения)
    public static final Map<UUID, Set<String>> clientPlayerTeams = new HashMap<>();
    public static final Map<String, Team> clientTeams = new HashMap<>();
    private static final int MAX_TEAMS_PER_PLAYER = 3;
    private static final Map<UUID, PlayerStatsData> serverPlayerStats = new HashMap<>();
    public static final Map<UUID, PlayerStatsData> clientPlayerStats = new HashMap<>();

    public static class Team implements INBTSerializable<CompoundTag> {
        private final String name;
        private boolean inviteOnly = true;
        private UUID owner;
        private final Set<UUID> members = new HashSet<>();
        private final Set<UUID> invited = new HashSet<>();
        private boolean friendlyFire = true;
        private String tag = "";
        private int nameColor = 0xFFFFFF;
        private int tagColor = 0xFFFFFF;
        // ← ТВОИ НОВЫЕ ПОЛЯ (теперь приватные + с геттерами/сеттерами)
        private boolean showTag = true;
        private boolean showCompass = true;

        public Team(String name, UUID owner) {
            this.name = name;
            this.owner = owner;
            if (owner != null) {
                this.members.add(owner);
            }
        }

        public String getName() { return name; }
        public UUID getOwner() { return owner; }
        public Set<UUID> getMembers() { return members; }
        public Set<UUID> getInvited() { return invited; }
        public boolean isFriendlyFire() { return friendlyFire; }
        public void setFriendlyFire(boolean friendlyFire) { this.friendlyFire = friendlyFire; }

        public String getTag() { return tag; }
        public void setTag(String tag) { this.tag = tag; }

        public int getNameColor() { return nameColor; }
        public void setNameColor(int color) { this.nameColor = color; }

        public int getTagColor() { return tagColor; }
        public void setTagColor(int color) { this.tagColor = color; }

        public boolean isInviteOnly() { return inviteOnly; }
        public void setInviteOnly(boolean inviteOnly) { this.inviteOnly = inviteOnly; }

        // ← ГЕТТЕРЫ И СЕТТЕРЫ ДЛЯ НОВЫХ ПОЛЕЙ
        public boolean showTag() { return showTag; }
        public void setShowTag(boolean showTag) { this.showTag = showTag; }

        public boolean showCompass() { return showCompass; }
        public void setShowCompass(boolean showCompass) { this.showCompass = showCompass; }

        public boolean addMember(UUID player) {
            return members.add(player);
        }

        public boolean removeMember(UUID player) {
            return members.remove(player); // ← УБРАЛ if (player.equals(owner)) return false;
        }
        public boolean invitePlayer(UUID player) {
            if (members.contains(player) || invited.contains(player)) return false;
            return invited.add(player);
        }
        public boolean cancelInvitation(UUID player) { return invited.remove(player); }
        public boolean transferOwnership(UUID newOwner) {
            if (members.contains(newOwner)) {
                this.owner = newOwner;
                return true;
            }
            return false;
        }

        @Override
        public CompoundTag serializeNBT() {
            CompoundTag tag = new CompoundTag();
            tag.putString("name", name);
            if (owner != null) tag.putUUID("owner", owner);
            tag.putBoolean("friendlyFire", friendlyFire);
            tag.putString("tag", this.tag);
            tag.putInt("nameColor", nameColor);
            tag.putInt("tagColor", tagColor);
            tag.putBoolean("inviteOnly", inviteOnly);

            // ← СОХРАНЯЕМ НОВЫЕ ПОЛЯ
            tag.putBoolean("showTag", showTag);
            tag.putBoolean("showCompass", showCompass);

            ListTag membersTag = new ListTag();
            members.forEach(uuid -> membersTag.add(StringTag.valueOf(uuid.toString())));
            tag.put("members", membersTag);

            ListTag invitedTag = new ListTag();
            invited.forEach(uuid -> invitedTag.add(StringTag.valueOf(uuid.toString())));
            tag.put("invited", invitedTag);

            return tag;
        }

        @Override
        public void deserializeNBT(CompoundTag tag) {
            owner = tag.contains("owner") ? tag.getUUID("owner") : null;
            friendlyFire = tag.getBoolean("friendlyFire");
            this.tag = tag.getString("tag");
            nameColor = tag.getInt("nameColor");
            tagColor = tag.getInt("tagColor");
            inviteOnly = tag.contains("inviteOnly") ? tag.getBoolean("inviteOnly") : true;

            // ← ЧИТАЕМ НОВЫЕ ПОЛЯ (с дефолтами на случай старых команд)
            showTag = tag.contains("showTag") ? tag.getBoolean("showTag") : true;
            showCompass = tag.contains("showCompass") ? tag.getBoolean("showCompass") : true;

            members.clear();
            if (tag.contains("members")) {
                tag.getList("members", 8).forEach(strTag -> members.add(UUID.fromString(strTag.getAsString())));
            }
            invited.clear();
            if (tag.contains("invited")) {
                tag.getList("invited", 8).forEach(strTag -> invited.add(UUID.fromString(strTag.getAsString())));
            }
        }
    }

    public static Team getServerTeam(String teamName) {
        TeamWorldData data = getData();
        return data != null ? data.getTeams().get(teamName) : null;
    }

    public static boolean createTeam(String teamName, String tag, boolean friendlyFire, boolean showTag, boolean showCompass, Player owner) {
        System.out.println("[TeamManager] createTeam вызван: " + teamName + " от " + owner.getName().getString());

        if (!(owner instanceof ServerPlayer serverOwner)) {
            System.out.println("[TeamManager] owner не ServerPlayer");
            return false;
        }

        TeamWorldData data = getData();
        if (data == null) {
            System.out.println("[TeamManager] data == null — FAIL");
            serverOwner.sendSystemMessage(Component.literal("§cОшибка сервера. Перезайдите в мир и попробуйте снова."));
            return false;
        }

        Map<String, TeamManager.Team> teams = data.getTeams();
        Map<UUID, Set<String>> playerTeams = data.getPlayerTeams();

        Set<String> ownerTeams = playerTeams.computeIfAbsent(owner.getUUID(), k -> new HashSet<>());

        if (teams.containsKey(teamName) || ownerTeams.size() >= MAX_TEAMS_PER_PLAYER) {
            serverOwner.sendSystemMessage(Component.literal("§cНе удалось создать команду. Проверьте имя и лимит (макс. 3)."));
            return false;
        }

        TeamManager.Team team = new TeamManager.Team(teamName, owner.getUUID());
        team.setTag(tag);
        team.setFriendlyFire(friendlyFire);
        team.setShowTag(showTag);
        team.setShowCompass(showCompass);

        teams.put(teamName, team);
        ownerTeams.add(teamName);  // ← Теперь добавляется в настоящий HashSet!

        data.setDirty(true);

        serverOwner.sendSystemMessage(Component.literal("§aКоманда §f" + teamName + "§a успешно создана!"));
        sendAchievement(serverOwner, "Команда создана!", "Вы успешно создали команду §b" + teamName, "COMPASS", true);

        System.out.println("[TeamManager] Команда создана на сервере. Отправляем синхронизацию...");

        // Принудительно отправляем очистку + новую команду владельцу
        NetworkHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> serverOwner), new TeamSyncPacket());
        NetworkHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> serverOwner), new TeamSyncPacket(teamName, team.serializeNBT()));

        // И всем остальным
        syncAllTeamsToAllPlayers();

        return true;
    }

    public static boolean invitePlayer(String teamName, UUID player, Player inviter) {
        if (!(inviter instanceof ServerPlayer serverInviter)) {
            return false;
        }

        TeamWorldData data = getData();
        if (data == null) return false;

        Map<String, Team> teams = data.getTeams();
        Map<UUID, Set<String>> playerTeams = data.getPlayerTeams();

        Team team = teams.get(teamName);
        if (team == null || !team.getOwner().equals(inviter.getUUID())) {
            serverInviter.sendSystemMessage(Component.literal("§cНе удалось отправить приглашение."));
            return false;
        }

        if (playerTeams.getOrDefault(player, Collections.emptySet()).size() >= MAX_TEAMS_PER_PLAYER) {
            serverInviter.sendSystemMessage(Component.literal("§cНе удалось отправить приглашение (возможно, лимит команд)."));
            return false;
        }

        if (team.invitePlayer(player)) {
            ServerPlayer invited = serverInviter.getServer().getPlayerList().getPlayer(player);
            if (invited != null) {
                invited.sendSystemMessage(Component.literal("§eВы получили приглашение в команду §b" + teamName + " §eот §f" + inviter.getName().getString()));
                sendAchievement(invited, "Приглашение в команду", "Вы получили приглашение в §b" + teamName + " от §f" + inviter.getName().getString(), "PAPER", true);
            }
            serverInviter.sendSystemMessage(Component.literal("§aПриглашение отправлено игроку §f" + (invited.getName().getString())));
            sendAchievement(serverInviter, "Приглашение отправлено", "Приглашение в §b" + teamName + " отправлено", "PAPER", true);

            data.setDirty(true);
            syncTeamToAll(teamName);
            return true;
        }

        serverInviter.sendSystemMessage(Component.literal("§cНе удалось отправить приглашение (возможно, уже приглашён)."));
        return false;
    }

    public static TeamWorldData getData() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            System.out.println("[TeamManager.getData] server == null");
            return null;
        }

        ServerLevel overworld = server.overworld();
        if (overworld == null) {
            System.out.println("[TeamManager.getData] overworld == null");
            return null;
        }

        TeamWorldData data = TeamWorldData.get(overworld);

        if (data == null) {
            System.out.println("[TeamManager.getData] data == null после TeamWorldData.get()");
        } else {
            System.out.println("[TeamManager.getData] data OK! teams.size() = " + data.getTeams().size());
        }

        return data;
    }

    public static boolean acceptInvitation(String teamName, UUID player) {
        TeamWorldData data = getData();
        if (data == null) return false;

        Map<String, Team> teams = data.getTeams();
        Map<UUID, Set<String>> playerTeams = data.getPlayerTeams();

        Team team = teams.get(teamName);
        if (team == null || !team.getInvited().contains(player)) {
            return false;
        }

        if (playerTeams.getOrDefault(player, Collections.emptySet()).size() >= MAX_TEAMS_PER_PLAYER) {
            return false;
        }

        team.cancelInvitation(player);
        team.addMember(player);
        playerTeams.computeIfAbsent(player, k -> new HashSet<>()).add(teamName);

        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            ServerPlayer p = server.getPlayerList().getPlayer(player);
            if (p != null) {
                sendAchievementToTeam(teamName, "Новый участник", "Игрок §f" + p.getName().getString() + " вступил в команду", "COMPASS", true);
            }
        }

        data.setDirty(true);
        syncTeamToAll(teamName);
        return true;
    }

    public static boolean declineInvitation(String teamName, UUID player) {
        TeamWorldData data = getData();
        if (data == null) return false;

        Team team = data.getTeams().get(teamName);
        if (team == null) return false;

        if (team.cancelInvitation(player)) {
            data.setDirty(true);
            syncTeamToAll(teamName);

            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server != null) {
                ServerPlayer p = server.getPlayerList().getPlayer(player);
                if (p != null) {
                    sendAchievement(p, "Приглашение отклонено", "Вы отклонили приглашение в §b" + teamName, null, false);
                }
            }
            return true;
        }
        return false;
    }

    public static boolean leaveTeam(String teamName, UUID playerUUID) {
        TeamWorldData data = getData();
        if (data == null) return false;

        Map<String, Team> teams = data.getTeams();
        Map<UUID, Set<String>> playerTeams = data.getPlayerTeams();

        Team team = teams.get(teamName);
        if (team == null || !team.getMembers().contains(playerUUID)) {
            return false;
        }

        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        ServerPlayer player = server != null ? server.getPlayerList().getPlayer(playerUUID) : null;

        boolean wasOwner = team.getOwner().equals(playerUUID);
        boolean teamWillBeDisbanded = team.getMembers().size() == 1;

        if (wasOwner && !teamWillBeDisbanded) {
            UUID newOwner = team.getMembers().stream()
                    .filter(uuid -> !uuid.equals(playerUUID))
                    .findFirst()
                    .orElse(null);

            if (newOwner != null) {
                team.owner = newOwner;
                ServerPlayer newOwnerPlayer = server.getPlayerList().getPlayer(newOwner);
                if (newOwnerPlayer != null) {
                    newOwnerPlayer.sendSystemMessage(
                            Component.literal("§aВы стали новым лидером команды §f" + teamName));
                }
                if (player != null) {
                    player.sendSystemMessage(
                            Component.literal("§eВы покинули команду §f" + teamName + "§e. Лидерство передано."));
                }
            }
        } else if (teamWillBeDisbanded) {
            if (player != null) {
                player.sendSystemMessage(
                        Component.literal("§eВы покинули команду §f" + teamName + "§e. Команда расформирована."));
            }
        } else {
            if (player != null) {
                player.sendSystemMessage(
                        Component.literal("§aВы покинули команду §f" + teamName));
            }
        }

        team.removeMember(playerUUID);
        playerTeams.computeIfAbsent(playerUUID, k -> new HashSet<>()).remove(teamName);

        if (team.getMembers().isEmpty()) {
            teams.remove(teamName);
        }

        sendAchievementToTeam(teamName, "Участник покинул команду", "Игрок §f" + (player != null ? player.getName().getString() : "неизвестно") + " покинул команду", "BARRIER", false);

        data.setDirty(true);
        syncTeamToAll(teamName);

        return true;
    }

    public static boolean kickPlayer(String teamName, UUID targetUUID, UUID kickerUUID) {
        TeamWorldData data = getData();
        if (data == null) return false;

        Map<String, Team> teams = data.getTeams();
        Map<UUID, Set<String>> playerTeams = data.getPlayerTeams();

        Team team = teams.get(teamName);
        if (team == null || !team.getOwner().equals(kickerUUID) || !team.getMembers().contains(targetUUID)) {
            return false;
        }

        // Убираем игрока из команды
        team.removeMember(targetUUID);
        playerTeams.computeIfAbsent(targetUUID, k -> new HashSet<>()).remove(teamName);

        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        ServerPlayer target = server != null ? server.getPlayerList().getPlayer(targetUUID) : null;
        ServerPlayer kicker = server != null ? server.getPlayerList().getPlayer(kickerUUID) : null;

        if (target != null) {
            sendAchievement(target, "Вы исключены", "Вы были исключены из команды §b" + teamName, "BARRIER", false);
        }
        sendAchievementToTeam(teamName, "Участник исключён", "Игрок §f" + target.getName().getString() + " исключён из команды", "BARRIER", false);

        if (kicker != null) {
            kicker.sendSystemMessage(Component.literal("§aВы исключили игрока из команды §f" + teamName));
        }

        // Если остался только один участник — он становится владельцем (на всякий случай)
        if (team.getMembers().size() == 1) {
            team.owner = team.getMembers().iterator().next();
        }

        // Команда удаляется ТОЛЬКО если в ней не осталось участников
        if (team.getMembers().isEmpty()) {
            teams.remove(teamName);
        }

        data.setDirty(true);

        // Синхронизация: отправляем обновлённое состояние команды ВСЕМ
        // Если команда осталась — все видят её без кикнутого игрока
        // Если команда удалена — всем приходит пакет на удаление
        syncTeamToAll(teamName);

        return true;
    }

    public static boolean transferOwnership(String teamName, UUID newOwner, UUID currentOwner) {
        TeamWorldData data = getData();
        if (data == null) return false;

        Team team = data.getTeams().get(teamName);
        if (team == null || !team.getOwner().equals(currentOwner)) {
            return false;
        }

        if (team.transferOwnership(newOwner)) {
            data.setDirty(true);
            syncTeamToAll(teamName);
            return true;
        }
        return false;
    }

    public static boolean setFriendlyFire(String teamName, boolean enabled, UUID owner) {
        TeamWorldData data = getData();
        if (data == null) return false;

        Team team = data.getTeams().get(teamName);
        if (team == null || !team.getOwner().equals(owner)) {
            return false;
        }

        String msg = enabled ? "Дружественный огонь включён" : "Дружественный огонь выключён";
        String icon = enabled ? "IRON_SWORD" : "SHIELD";
        sendAchievementToTeam(teamName, msg, "В команде §b" + teamName + " " + (enabled ? "включён" : "выключен") + " дружественный огонь", icon, !enabled);

        team.setFriendlyFire(enabled);
        data.setDirty(true);
        syncTeamToAll(teamName);
        return true;
    }

    public static List<UUID> getTeammates(UUID player) {
        Set<String> playerTeamNames = clientPlayerTeams.getOrDefault(player, Collections.emptySet());
        return playerTeamNames.stream()
                .map(clientTeams::get)
                .filter(Objects::nonNull)
                .flatMap(team -> team.getMembers().stream())
                .filter(uuid -> !uuid.equals(player))
                .distinct()
                .collect(Collectors.toList());
    }

    public static boolean isFriendlyFireDisabled(Player player1, Player player2) {
        // Если игроки одинаковы, урон от самого себя всегда разрешен
        if (player1.getUUID().equals(player2.getUUID())) {
            return false;
        }

        TeamWorldData data = getData();
        if (data == null) return false;

        Map<UUID, Set<String>> playerTeams = data.getPlayerTeams();
        Map<String, Team> teams = data.getTeams();

        Set<String> player1Teams = playerTeams.getOrDefault(player1.getUUID(), Collections.emptySet());
        Set<String> player2Teams = playerTeams.getOrDefault(player2.getUUID(), Collections.emptySet());

        return player1Teams.stream()
                .anyMatch(teamName -> player2Teams.contains(teamName) && teams.containsKey(teamName) && !teams.get(teamName).isFriendlyFire());
    }

    public static boolean deleteTeam(String teamName, UUID ownerUUID) {
        TeamWorldData data = getData();
        if (data == null) return false;

        Team team = data.getTeams().get(teamName);
        if (team == null || !team.getOwner().equals(ownerUUID)) {
            return false;
        }

        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        for (UUID memberUUID : new HashSet<>(team.getMembers())) {
            ServerPlayer p = server != null ? server.getPlayerList().getPlayer(memberUUID) : null;
            data.getPlayerTeams().computeIfAbsent(memberUUID, k -> new HashSet<>()).remove(teamName);
            if (p != null) {
                p.sendSystemMessage(Component.literal("§cКоманда §f" + teamName + "§c была удалена лидером"));
            }
        }

        for (UUID invited : new HashSet<>(team.getInvited())) {
            ServerPlayer p = server != null ? server.getPlayerList().getPlayer(invited) : null;
            if (p != null) {
                p.sendSystemMessage(Component.literal("§cПриглашение в команду §f" + teamName + "§c отменено"));
            }
        }

        sendAchievementToTeam(teamName, "Команда удалена", "Команда §b" + teamName + " была удалена", "BARRIER", false);

        data.getTeams().remove(teamName);
        data.setDirty(true);
        syncTeamToAll(teamName);
        return true;
    }

    public static boolean isFriendlyFireDisabled(Entity attacker, Entity target) {
        // Если атакующий и цель — один и тот же объект, урон разрешен
        if (attacker == target) {
            return false;
        }

        UUID attackerOwner = null;
        UUID targetOwner = null;

        // Определяем владельцев для игроков и прирученных существ
        if (attacker instanceof Player playerAttacker) {
            attackerOwner = playerAttacker.getUUID();
        } else if (attacker instanceof TamableAnimal tameable) {
            if (tameable.getOwner() instanceof Player owner) {
                attackerOwner = owner.getUUID();
            }
        }

        if (target instanceof Player playerTarget) {
            targetOwner = playerTarget.getUUID();
        } else if (target instanceof TamableAnimal tameable) {
            if (tameable.getOwner() instanceof Player owner) {
                targetOwner = owner.getUUID();
            }
        }

        // Если владельцы не определены, урон не блокируется
        if (attackerOwner == null || targetOwner == null) {
            return false;
        }

        TeamWorldData data = getData();
        if (data == null) return false;

        Map<UUID, Set<String>> playerTeams = data.getPlayerTeams();
        Map<String, Team> teams = data.getTeams();

        Set<String> attackerTeams = playerTeams.getOrDefault(attackerOwner, Collections.emptySet());
        Set<String> targetTeams = playerTeams.getOrDefault(targetOwner, Collections.emptySet());

        return attackerTeams.stream()
                .anyMatch(teamName -> targetTeams.contains(teamName) && teams.containsKey(teamName) && !teams.get(teamName).isFriendlyFire());
    }

    public static Set<String> getPlayerTeams(UUID player) {
        return clientPlayerTeams.getOrDefault(player, Collections.emptySet());
    }

    public static Team getTeam(String teamName) {
        return clientTeams.get(teamName);
    }

    @SubscribeEvent
    public static void onLivingAttack(LivingAttackEvent event) {
        Entity sourceEntity = event.getSource().getEntity();
        LivingEntity target = event.getEntity();

        if (sourceEntity != null && isFriendlyFireDisabled(sourceEntity, target)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        UUID playerId = event.getEntity().getUUID();

        // Очищаем только клиентские данные (серверные остаются в мире)
        clientPlayerTeams.remove(playerId);

        // Удаляем пустые команды только из клиентского кэша
        clientTeams.entrySet().removeIf(entry -> entry.getValue().getMembers().isEmpty());
    }

    // 1. Универсальный метод — вставь его один раз в класс
    public static void syncTeamToAll(String teamName) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;

        TeamWorldData data = getData();
        if (data == null) return;

        TeamManager.Team team = data.getTeams().get(teamName);
        TeamSyncPacket packet = team != null
                ? new TeamSyncPacket(teamName, team.serializeNBT())
                : new TeamSyncPacket(teamName); // null data = удаление

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            NetworkHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), packet);
        }
    }

    public static void syncAllTeamsToAllPlayers() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;

        TeamWorldData data = getData();
        if (data == null) return;

        System.out.println("[TeamManager] syncAllTeamsToAllPlayers: отправляем " + data.getTeams().size() + " команд");

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            // Очистка
            NetworkHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), new TeamSyncPacket());
            System.out.println("[TeamManager] Отправили очистку игроку " + player.getName().getString());

            // Все команды
            for (TeamManager.Team team : data.getTeams().values()) {
                NetworkHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), new TeamSyncPacket(team.getName(), team.serializeNBT()));
                System.out.println("[TeamManager] Отправили команду " + team.getName() + " игроку " + player.getName().getString());
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            // Создаём профиль сразу
            ProfileManager.Profile profile = ProfileManager.getProfile(player.serverLevel(), player.getUUID());

            // Синхронизируем обычный профиль (background и т.д.)
            ProfileManager.syncProfileToClient(player);

            // Синхронизируем статистику — НО ОТКЛАДЫВАЕМ на 1 тик, когда handshake точно завершён
            player.server.submitAsync(() -> {
                PlayerStatsData stats = new PlayerStatsData(player.getStats());
                TeamManager.serverPlayerStats.put(player.getUUID(), stats);

                // Отправляем статистику ТОЛЬКО этому игроку
                NetworkHandler.INSTANCE.send(
                        PacketDistributor.PLAYER.with(() -> player),
                        new StatsSyncPacket(player.getUUID(), stats)
                );
            });
        }
    }

    // Отправить одному игроку
    public static void sendAchievement(ServerPlayer player, String title, String description, String iconItem, boolean isPositive) {
        NetworkHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player),
                new AchievementNotificationPacket(title, description, iconItem, isPositive));
    }

    // Отправить всем в команде (включая лидера)
    public static void sendAchievementToTeam(String teamName, String title, String description, String iconItem, boolean isPositive) {
        Team team = getServerTeam(teamName);
        if (team == null) return;

        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;

        for (UUID uuid : team.getMembers()) {
            ServerPlayer p = server.getPlayerList().getPlayer(uuid);
            if (p != null) {
                sendAchievement(p, title, description, iconItem, isPositive);
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ServerLevel overworld = player.server.overworld();
            if (overworld != null) {
                TeamWorldData.get(overworld); // Инициализация
            }

            // Задержка 1 тик, чтобы клиент успел подключиться
            player.server.submitAsync(() -> {
                syncAllTeamsToAllPlayers();
            });
        }
    }

    @SubscribeEvent
    public static void onLivingChangeTarget(LivingChangeTargetEvent event) {
        LivingEntity mob = event.getEntity();
        LivingEntity newTarget = event.getNewTarget();

        if (newTarget instanceof Player targetPlayer && mob instanceof TamableAnimal tameable && tameable.isTame()) {
            if (tameable.getOwner() instanceof Player ownerPlayer) {
                if (isFriendlyFireDisabled(ownerPlayer, targetPlayer)) {
                    event.setNewTarget(null);
                }
            }
        }
    }
}
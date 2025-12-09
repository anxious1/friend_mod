package com.mom.teammod;

import com.mom.teammod.packets.TeamSyncPacket;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.server.ServerLifecycleHooks;
import net.minecraft.world.entity.TamableAnimal;

import java.util.*;
import java.util.stream.Collectors;

@Mod.EventBusSubscriber(modid = TeamMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class TeamManager {
    public static final Map<UUID, Set<String>> playerTeams = new HashMap<>();
    public static final Map<String, Team> teams = new HashMap<>();
    public static final Map<UUID, Set<String>> clientPlayerTeams = new HashMap<>();
    public static final Map<String, Team> clientTeams = new HashMap<>();
    private static final int MAX_TEAMS_PER_PLAYER = 3;
    static {
        // Тестовые команды
        createTestTeam("Alpha", "A11");
        createTestTeam("Bravo", "B22");
        createTestTeam("Charlie", "C33");
        createTestTeam("Delta", "D44");
        createTestTeam("Echo", "E55");
        createTestTeam("Foxtrot", "F66");
        createTestTeam("Golf", "G77");
        createTestTeam("Hotel", "H88");
    }

    private static void createTestTeam(String name, String tag) {
        Team team = new Team(name, null);
        team.setTag(tag);
        clientTeams.put(name, team);
    }


    public static class Team implements INBTSerializable<CompoundTag> {
        private final String name;
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

        // ← ГЕТТЕРЫ И СЕТТЕРЫ ДЛЯ НОВЫХ ПОЛЕЙ
        public boolean showTag() { return showTag; }
        public void setShowTag(boolean showTag) { this.showTag = showTag; }

        public boolean showCompass() { return showCompass; }
        public void setShowCompass(boolean showCompass) { this.showCompass = showCompass; }

        public boolean addMember(UUID player) { return members.add(player); }
        public boolean removeMember(UUID player) {
            if (player.equals(owner)) return false;
            return members.remove(player);
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
        return teams.get(teamName);
    }

    // 2. createTeam — полностью исправлен
    public static boolean createTeam(String teamName, String tag, boolean friendlyFire, boolean showTag, boolean showCompass, Player owner) {
        if (!(owner instanceof ServerPlayer serverOwner)) {
            return false;
        }
        if (teams.containsKey(teamName) || playerTeams.getOrDefault(owner.getUUID(), Collections.emptySet()).size() >= MAX_TEAMS_PER_PLAYER) {
            serverOwner.sendSystemMessage(Component.translatable("commands.teammod.create.failed", teamName));
            return false;
        }

        Team team = new Team(teamName, owner.getUUID());

        // ← ВОТ ЭТИ СТРОКИ ТЫ ЗАБЫЛ!
        team.setTag(tag);
        team.setFriendlyFire(friendlyFire);
        team.setShowTag(showTag);
        team.setShowCompass(showCompass);

        teams.put(teamName, team);
        playerTeams.computeIfAbsent(owner.getUUID(), k -> new HashSet<>()).add(teamName);

        serverOwner.sendSystemMessage(Component.translatable("commands.teammod.create.success", teamName));

        syncTeamToAll(teamName);
        return true;
    }


    // 3. invitePlayer — полностью исправлен
    public static boolean invitePlayer(String teamName, UUID player, Player inviter) {
        if (!(inviter instanceof ServerPlayer serverInviter)) {
            return false;
        }
        Team team = teams.get(teamName);
        if (team == null || !team.getOwner().equals(inviter.getUUID())) {
            serverInviter.sendSystemMessage(Component.translatable("commands.teammod.invite.failed", "Player", teamName));
            return false;
        }
        if (playerTeams.getOrDefault(player, Collections.emptySet()).size() >= MAX_TEAMS_PER_PLAYER) {
            serverInviter.sendSystemMessage(Component.translatable("commands.teammod.invite.failed", "Player", teamName));
            return false;
        }
        if (team.invitePlayer(player)) {
            ServerPlayer invited = serverInviter.getServer().getPlayerList().getPlayer(player);
            if (invited != null) {
                invited.sendSystemMessage(Component.translatable("commands.teammod.invite.received", teamName, inviter.getName()));
                serverInviter.sendSystemMessage(Component.translatable("commands.teammod.invite.success", invited.getName(), teamName));
            }
            syncTeamToAll(teamName); // ← исправлено
            return true;
        }
        serverInviter.sendSystemMessage(Component.translatable("commands.teammod.invite.failed", "Player", teamName));
        return false;
    }

    // 4. acceptInvitation — полностью исправлен
    public static boolean acceptInvitation(String teamName, UUID player) {
        Team team = teams.get(teamName);
        if (team == null || !team.getInvited().contains(player)) {
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server != null) {
                ServerPlayer p = server.getPlayerList().getPlayer(player);
                if (p != null) p.sendSystemMessage(Component.translatable("commands.teammod.accept.failed", teamName));
            }
            return false;
        }
        if (playerTeams.getOrDefault(player, Collections.emptySet()).size() >= MAX_TEAMS_PER_PLAYER) {
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server != null) {
                ServerPlayer p = server.getPlayerList().getPlayer(player);
                if (p != null) p.sendSystemMessage(Component.translatable("commands.teammod.accept.failed", teamName));
            }
            return false;
        }
        team.cancelInvitation(player);
        team.addMember(player);
        playerTeams.computeIfAbsent(player, k -> new HashSet<>()).add(teamName);
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            ServerPlayer p = server.getPlayerList().getPlayer(player);
            if (p != null) p.sendSystemMessage(Component.translatable("commands.teammod.accept.success", teamName));
        }
        syncTeamToAll(teamName); // ← исправлено
        return true;
    }

    // 5. declineInvitation — полностью исправлен
    public static boolean declineInvitation(String teamName, UUID player) {
        Team team = teams.get(teamName);
        if (team == null) {
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server != null) {
                ServerPlayer p = server.getPlayerList().getPlayer(player);
                if (p != null) p.sendSystemMessage(Component.translatable("commands.teammod.decline.failed", teamName));
            }
            return false;
        }
        if (team.cancelInvitation(player)) {
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server != null) {
                ServerPlayer p = server.getPlayerList().getPlayer(player);
                if (p != null) p.sendSystemMessage(Component.translatable("commands.teammod.decline.success", teamName));
            }
            syncTeamToAll(teamName); // ← исправлено
            return true;
        }
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            ServerPlayer p = server.getPlayerList().getPlayer(player);
            if (p != null) p.sendSystemMessage(Component.translatable("commands.teammod.decline.failed", teamName));
        }
        return false;
    }

    // 6. leaveTeam — полностью исправлен
    public static boolean leaveTeam(String teamName, UUID player) {
        Team team = teams.get(teamName);
        if (team == null) {
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server != null) {
                ServerPlayer p = server.getPlayerList().getPlayer(player);
                if (p != null) p.sendSystemMessage(Component.translatable("commands.teammod.leave.failed", teamName));
            }
            return false;
        }
        if (team.getOwner().equals(player)) {
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server != null) {
                ServerPlayer p = server.getPlayerList().getPlayer(player);
                if (p != null) p.sendSystemMessage(Component.translatable("commands.teammod.leave.failed.owner", teamName));
            }
            return false;
        }
        team.removeMember(player);
        playerTeams.getOrDefault(player, Collections.emptySet()).remove(teamName);
        if (team.getMembers().isEmpty()) {
            teams.remove(teamName);
        }
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            ServerPlayer p = server.getPlayerList().getPlayer(player);
            if (p != null) p.sendSystemMessage(Component.translatable("commands.teammod.leave.success", teamName));
        }
        syncTeamToAll(teamName); // ← исправлено
        return true;
    }

    public static boolean kickPlayer(String teamName, UUID player, UUID owner) {
        Team team = teams.get(teamName);
        if (team == null || !team.getOwner().equals(owner)) {
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server == null) return false;
            ServerPlayer serverOwner = server.getPlayerList().getPlayer(owner);
            if (serverOwner != null) {
                serverOwner.sendSystemMessage(Component.translatable("commands.teammod.kick.failed", "Player", teamName));
            }
            return false;
        }
        team.removeMember(player);
        playerTeams.get(player).remove(teamName);
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return false;
        ServerPlayer kicked = server.getPlayerList().getPlayer(player);
        MinecraftServer server2 = ServerLifecycleHooks.getCurrentServer();
        if (server2 == null) return false;
        ServerPlayer serverOwner = server2.getPlayerList().getPlayer(owner);
        if (kicked != null) {
            kicked.sendSystemMessage(Component.translatable("commands.teammod.kick.notify", teamName));
        }
        if (serverOwner != null) {
            serverOwner.sendSystemMessage(Component.translatable("commands.teammod.kick.success", kicked != null ? kicked.getName() : "Player", teamName));
        }
        if (team.getMembers().isEmpty()) {
            teams.remove(teamName);
        }
        syncTeamToAll(teamName);
        return true;
    }

    public static boolean transferOwnership(String teamName, UUID newOwner, UUID currentOwner) {
        Team team = teams.get(teamName);
        if (team == null || !team.getOwner().equals(currentOwner)) {
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server == null) return false;
            ServerPlayer serverOwner = server.getPlayerList().getPlayer(currentOwner);
            if (serverOwner != null) {
                serverOwner.sendSystemMessage(Component.translatable("commands.teammod.transfer.failed", "Player", teamName));
            }
            return false;
        }
        if (team.transferOwnership(newOwner)) {
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server == null) return false;
            ServerPlayer newOwnerPlayer = server.getPlayerList().getPlayer(newOwner);
            MinecraftServer server2 = ServerLifecycleHooks.getCurrentServer();
            if (server2 == null) return false;
            ServerPlayer currentOwnerPlayer = server2.getPlayerList().getPlayer(currentOwner);
            if (newOwnerPlayer != null) {
                newOwnerPlayer.sendSystemMessage(Component.translatable("commands.teammod.transfer.notify", teamName));
            }
            if (currentOwnerPlayer != null) {
                currentOwnerPlayer.sendSystemMessage(Component.translatable("commands.teammod.transfer.success", newOwnerPlayer != null ? newOwnerPlayer.getName() : "Player", teamName));
            }
            syncTeamToAll(teamName);
            return true;
        }
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return false;
        ServerPlayer serverOwner = server.getPlayerList().getPlayer(currentOwner);
        if (serverOwner != null) {
            serverOwner.sendSystemMessage(Component.translatable("commands.teammod.transfer.failed", "Player", teamName));
        }
        return false;
    }

    public static boolean setFriendlyFire(String teamName, boolean friendlyFire, UUID owner) {
        Team team = teams.get(teamName);
        if (team == null || !team.getOwner().equals(owner)) {
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server == null) return false;
            ServerPlayer serverOwner = server.getPlayerList().getPlayer(owner);
            if (serverOwner != null) {
                serverOwner.sendSystemMessage(Component.translatable("commands.teammod.friendlyfire.failed", teamName));
            }
            return false;
        }
        team.setFriendlyFire(friendlyFire);
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return false;
        ServerPlayer serverOwner = server.getPlayerList().getPlayer(owner);
        if (serverOwner != null) {
            serverOwner.sendSystemMessage(Component.translatable("commands.teammod.friendlyfire." + (friendlyFire ? "enabled" : "disabled"), teamName));
        }
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
        Set<String> player1Teams = playerTeams.getOrDefault(player1.getUUID(), Collections.emptySet());
        Set<String> player2Teams = playerTeams.getOrDefault(player2.getUUID(), Collections.emptySet());
        return player1Teams.stream()
                .anyMatch(teamName -> player2Teams.contains(teamName) && !teams.get(teamName).isFriendlyFire());
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

        // Проверяем команды владельцев
        Set<String> attackerTeams = playerTeams.getOrDefault(attackerOwner, Collections.emptySet());
        Set<String> targetTeams = playerTeams.getOrDefault(targetOwner, Collections.emptySet());
        return attackerTeams.stream()
                .anyMatch(teamName -> targetTeams.contains(teamName) && !teams.get(teamName).isFriendlyFire());
    }
    public static Set<String> getPlayerTeams(UUID player) {
        return clientPlayerTeams.getOrDefault(player, Collections.emptySet());
    }

    public static Team getTeam(String teamName) {
        return clientTeams.get(teamName);
    }

    @SubscribeEvent
    public static void onLivingAttack(LivingAttackEvent event) {
        LivingEntity attacker = event.getSource().getEntity() instanceof LivingEntity ? (LivingEntity) event.getSource().getEntity() : null;
        LivingEntity target = event.getEntity();

        if (attacker != null && isFriendlyFireDisabled(attacker, target)) {
            event.setCanceled(true); // Блокируем урон, если FF выключен для тиммейтов или их питомцев
        }
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        UUID playerId = event.getEntity().getUUID();
        playerTeams.remove(playerId);
        clientPlayerTeams.remove(playerId);
        teams.entrySet().removeIf(entry -> entry.getValue().getMembers().isEmpty());
        clientTeams.entrySet().removeIf(entry -> entry.getValue().getMembers().isEmpty());
    }

    // 1. Универсальный метод — вставь его один раз в класс
    private static void syncTeamToAll(String teamName) {
        Team team = teams.get(teamName);
        if (team == null) return;

        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;

        TeamSyncPacket packet = new TeamSyncPacket(teamName);

        // Участники
        for (UUID uuid : team.getMembers()) {
            ServerPlayer player = server.getPlayerList().getPlayer(uuid);
            if (player != null) {
                NetworkHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), packet);
            }
        }
        // Приглашённые
        for (UUID uuid : team.getInvited()) {
            ServerPlayer player = server.getPlayerList().getPlayer(uuid);
            if (player != null) {
                NetworkHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), packet);
            }
        }
    }
}
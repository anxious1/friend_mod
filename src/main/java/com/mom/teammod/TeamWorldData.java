package com.mom.teammod;

import com.mojang.authlib.GameProfile;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.*;

import static dev.ftb.mods.ftblibrary.util.KnownServerRegistries.server;

public class TeamWorldData extends SavedData {

    private final Map<String, TeamManager.Team> teams = new HashMap<>();
    private final Map<UUID, Set<String>> playerTeams = new HashMap<>();
    private final Map<UUID, ProfileManager.Profile> playerProfiles = new HashMap<>();
    private static TeamWorldData serverCache = null;
    public Map<UUID, ProfileManager.Profile> getPlayerProfiles() {
        return playerProfiles;
    }
    public static final String DATA_NAME = TeamMod.MODID + "_teams";
    private transient ServerLevel level;
    public TeamWorldData() {
    }

    public TeamWorldData(CompoundTag nbt) {
        load(nbt);
    }

    private void load(CompoundTag nbt) {
        teams.clear();
        playerTeams.clear();
        playerProfiles.clear(); // очищаем профили

        ListTag teamsList = nbt.getList("teams", 10);
        for (int i = 0; i < teamsList.size(); i++) {
            CompoundTag teamTag = teamsList.getCompound(i);

            String teamName = teamTag.getString("name");
            if (teamName.isEmpty()) {
                System.out.println("[TeamWorldData] Пропуск команды без имени в NBT!");
                continue;
            }

            TeamManager.Team team = new TeamManager.Team(teamName, null);
            team.deserializeNBT(teamTag);
            // ➜ НЕ ВОССТАНАВЛИВАЕМ ПУСТЫЕ КОМАНДЫ
            if (team.getMembers().isEmpty()) {
                System.out.println("[TeamWorldData] Пропуск пустой команды: " + teamName);
                continue;
            }
            teams.put(teamName, team);
        }

        ListTag playerList = nbt.getList("playerTeams", 10);
        for (int i = 0; i < playerList.size(); i++) {
            CompoundTag entry = playerList.getCompound(i);
            UUID player = entry.getUUID("player");
            ListTag teamNames = entry.getList("teams", 8);
            Set<String> set = new HashSet<>();
            for (int j = 0; j < teamNames.size(); j++) {
                set.add(teamNames.getString(j));
            }
            playerTeams.put(player, set);
        }

        // НОВОЕ: Загрузка профилей игроков
        if (nbt.contains("playerProfiles")) {
            CompoundTag profilesTag = nbt.getCompound("playerProfiles");
            for (String uuidStr : profilesTag.getAllKeys()) {
                try {
                    UUID uuid = UUID.fromString(uuidStr);
                    ProfileManager.Profile profile = new ProfileManager.Profile(uuid);
                    profile.deserializeNBT(profilesTag.getCompound(uuidStr));
                    playerProfiles.put(uuid, profile);
                } catch (Exception e) {
                    System.out.println("[TeamWorldData] Ошибка загрузки профиля для UUID: " + uuidStr);
                    e.printStackTrace();
                }
            }
        }

        System.out.println("[TeamWorldData] Загружено команд: " + teams.size());
    }

    @Override
    public CompoundTag save(CompoundTag nbt) {
        ListTag teamsList = new ListTag();
        for (TeamManager.Team team : teams.values()) {
            teamsList.add(team.serializeNBT());
        }
        nbt.put("teams", teamsList);

        ListTag playerList = new ListTag();
        for (Map.Entry<UUID, Set<String>> entry : playerTeams.entrySet()) {
            CompoundTag e = new CompoundTag();
            e.putUUID("player", entry.getKey());
            ListTag teamNames = new ListTag();
            for (String name : entry.getValue()) {
                teamNames.add(StringTag.valueOf(name));
            }
            e.put("teams", teamNames);
            playerList.add(e);
        }
        nbt.put("playerTeams", playerList);

        // НОВОЕ: Сохранение профилей игроков
        CompoundTag profilesTag = new CompoundTag();
        for (Map.Entry<UUID, ProfileManager.Profile> entry : playerProfiles.entrySet()) {
            profilesTag.put(entry.getKey().toString(), entry.getValue().serializeNBT());
        }
        nbt.put("playerProfiles", profilesTag);
        TeamManager.invalidateCache();
        return nbt;
    }

    public Map<String, TeamManager.Team> getTeams() {
        return teams;
    }

    public Map<UUID, Set<String>> getPlayerTeams() {
        return playerTeams;
    }

    public static TeamWorldData get(ServerLevel level) {
        if (level == null) return null;

        // Используем кэш, если уже загружено
        if (serverCache != null && serverCache.level == level) {
            return serverCache;
        }

        DimensionDataStorage storage = level.getDataStorage();
        serverCache = storage.computeIfAbsent(
                TeamWorldData::loadFromNBT,
                TeamWorldData::new,
                DATA_NAME
        );

        // Сохраняем ссылку на уровень для проверки
        serverCache.level = level;
        return serverCache;
    }

    public Map<UUID, String> getNameMap() {
        Map<UUID, String> map = new HashMap<>();
        playerProfiles.forEach((u, p) -> {
            if (p == null) return;

            GameProfile gp = null;
            try {
                gp = p.getGameProfile();
            } catch (Exception ignored) {}

            String name = (gp != null) ? gp.getName() : null;
            if (name == null) return;

            String n = name.trim();
            if (n.isEmpty()) return;

            // отсекаем заглушки
            if ("Unknown".equalsIgnoreCase(n) || "Loading...".equalsIgnoreCase(n)) return;

            map.put(u, n);
        });
        return map;
    }

    public void putName(UUID u, String name) {
        if (u == null || name == null) return;
        String n = name.trim();
        if (n.isEmpty()) return;

        ProfileManager.Profile prof = playerProfiles.computeIfAbsent(u, ProfileManager.Profile::new);

        // если в профиле уже было нормальное имя — не перезаписываем на Unknown/Loading
        GameProfile old = null;
        try { old = prof.getGameProfile(); } catch (Exception ignored) {}
        if (old != null) {
            String oldName = old.getName();
            if (oldName != null && !oldName.isBlank()
                    && !"Unknown".equalsIgnoreCase(oldName)
                    && !"Loading...".equalsIgnoreCase(oldName)
                    && ("Unknown".equalsIgnoreCase(n) || "Loading...".equalsIgnoreCase(n))) {
                return;
            }
        }

        prof.setGameProfile(new GameProfile(u, n));
        setDirty(true);
    }

    /** Всегда возвращает overworld, т.е. то место, где мы реально храним данные */
    public static ServerLevel storageLevel(MinecraftServer server) {
        return server.overworld();
    }

    public static TeamWorldData loadFromNBT(CompoundTag nbt) {
        TeamWorldData data = new TeamWorldData();
        data.load(nbt);
        return data;
    }
    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ServerLevel storageLevel = TeamWorldData.storageLevel(player.getServer());

            TeamWorldData data = get(storageLevel);
            ProfileManager.Profile profile = ProfileManager.getProfile(storageLevel, player.getUUID());

            data.getPlayerProfiles().put(player.getUUID(), profile);

            // заодно сохраняем имя
            if (player.getGameProfile() != null && player.getGameProfile().getName() != null && !player.getGameProfile().getName().isBlank()) {
                data.putName(player.getUUID(), player.getGameProfile().getName());
            }

            data.setDirty(true);
            storageLevel.getDataStorage().save();
        }
    }

    public ProfileManager.Profile getOrCreateProfile(UUID uuid) {
        return playerProfiles.computeIfAbsent(uuid, u -> {
            // Пытаемся достать из кеша сервера
            ProfileManager.Profile p = new ProfileManager.Profile(uuid);
            setDirty(true);
            return p;
        });
    }
    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        ServerLevel level = event.getServer().overworld();
        TeamWorldData data = get(level); // <-- форсируем загрузку
        System.out.println("[TeamWorldData] Загружено профилей: " + data.getPlayerProfiles().size());
    }
}
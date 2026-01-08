package com.mom.teammod;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;

import java.util.*;

public class TeamWorldData extends SavedData {

    private final Map<String, TeamManager.Team> teams = new HashMap<>();
    private final Map<UUID, Set<String>> playerTeams = new HashMap<>();
    private final Map<UUID, ProfileManager.Profile> playerProfiles = new HashMap<>();

    public Map<UUID, ProfileManager.Profile> getPlayerProfiles() {
        return playerProfiles;
    }
    public static final String DATA_NAME = TeamMod.MODID + "_teams";

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

        return nbt;
    }

    public Map<String, TeamManager.Team> getTeams() {
        return teams;
    }

    public Map<UUID, Set<String>> getPlayerTeams() {
        return playerTeams;
    }

    public static TeamWorldData get(ServerLevel level) {
        System.out.println("[TeamWorldData] get вызван для уровня " + level.dimension().location());

        DimensionDataStorage storage = level.getDataStorage();

        // ПРАВИЛЬНО: первый — пустой конструктор, второй — фабрика загрузки из NBT
        TeamWorldData data = storage.computeIfAbsent(
                TeamWorldData::loadFromNBT,  // фабрика, которая принимает NBT и возвращает объект
                TeamWorldData::new,          // пустой объект
                DATA_NAME
        );

        System.out.println("[TeamWorldData] data получен: " + (data != null ? "OK (teams=" + data.getTeams().size() + ", profiles=" + data.getPlayerProfiles().size() + ")" : "NULL!"));

        return data;
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
}
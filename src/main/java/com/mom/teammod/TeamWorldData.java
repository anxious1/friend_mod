package com.mom.teammod;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;

import java.util.*;

public class TeamWorldData extends SavedData {

    private final Map<String, TeamManager.Team> teams = new HashMap<>();
    private final Map<UUID, Set<String>> playerTeams = new HashMap<>();

    private static final String DATA_NAME = TeamMod.MODID + "_teams";

    public TeamWorldData() {
    }

    public TeamWorldData(CompoundTag nbt) {
        load(nbt);
    }

    private void load(CompoundTag nbt) {
        teams.clear();
        playerTeams.clear();

        ListTag teamsList = nbt.getList("teams", 10);
        for (int i = 0; i < teamsList.size(); i++) {
            CompoundTag teamTag = teamsList.getCompound(i);

            // ВАЖНО: сначала читаем имя из NBT, потом создаём объект
            String teamName = teamTag.getString("name");
            if (teamName.isEmpty()) {
                System.out.println("[TeamWorldData] Пропуск команды без имени в NBT!");
                continue;
            }

            TeamManager.Team team = new TeamManager.Team(teamName, null);
            team.deserializeNBT(teamTag); // ← теперь имя уже есть, всё остальное заполнится
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

        TeamWorldData data = storage.computeIfAbsent(
                TeamWorldData::new,     // новый объект
                TeamWorldData::new,     // загрузка из NBT
                DATA_NAME               // имя файла
        );

        System.out.println("[TeamWorldData] data получен: " + (data != null ? "OK (teams=" + data.getTeams().size() + ")" : "NULL!"));

        return data;
    }
}
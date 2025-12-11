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

    public TeamWorldData() {}

    public TeamWorldData(CompoundTag nbt) {
        // Загрузка команд
        ListTag teamsList = nbt.getList("teams", 10);
        for (int i = 0; i < teamsList.size(); i++) {
            CompoundTag teamTag = teamsList.getCompound(i);
            TeamManager.Team team = new TeamManager.Team("", null);
            team.deserializeNBT(teamTag);
            teams.put(team.getName(), team);
        }

        // Загрузка playerTeams
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
    }

    @Override
    public CompoundTag save(CompoundTag nbt) {
        // Сохранение команд
        ListTag teamsList = new ListTag();
        for (TeamManager.Team team : teams.values()) {
            teamsList.add(team.serializeNBT());
        }
        nbt.put("teams", teamsList);

        // Сохранение playerTeams
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
        DimensionDataStorage storage = level.getDataStorage();
        return storage.computeIfAbsent(TeamWorldData::load, TeamWorldData::new, DATA_NAME);
    }

    private static TeamWorldData load(CompoundTag nbt) {
        return new TeamWorldData(nbt);
    }
}
package com.mom.teammod;

import java.util.UUID;

public final class TeamQuestHelper {
    /** Средний процент выполненных квестов по всем участникам команды */
    public static int getTeamAverageQuestProgress(String teamName) {
        TeamManager.Team team = TeamManager.clientTeams.get(teamName);
        if (team == null || team.getMembers().isEmpty()) return 0;

        int totalCompleted = 0;
        int totalQuests    = 0;

        for (UUID uuid : team.getMembers()) {
            totalCompleted += FTBQuestsStats.getCompletedQuests(uuid);
            totalQuests    += FTBQuestsStats.getTotalQuests();
        }
        // если квестов вообще нет – возвращаем 0, а не NaN
        return totalQuests == 0 ? 0 : (totalCompleted * 100) / totalQuests;
    }
}
package com.mom.teammod;

import dev.ftb.mods.ftbquests.quest.Chapter;
import dev.ftb.mods.ftbquests.quest.Quest;
import dev.ftb.mods.ftbquests.quest.ServerQuestFile;
import dev.ftb.mods.ftbquests.quest.TeamData;
import net.minecraft.client.Minecraft;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class FTBQuestsStats {

    private static ServerQuestFile getQuestFile() {
        return ServerQuestFile.INSTANCE;
    }

    private static TeamData getTeamData(UUID playerUUID) {
        // Безопасно получаем ServerPlayer (только если он онлайн и на интегрированном сервере)
        Player player = Minecraft.getInstance().level != null ?
                Minecraft.getInstance().level.getPlayerByUUID(playerUUID) : null;

        if (player instanceof ServerPlayer serverPlayer) {
            return TeamData.get(serverPlayer);
        }
        return null; // Если игрок оффлайн или это чистый клиент — возвращаем null (статистика будет 0)
    }

    public static int getTotalQuests() {
        ServerQuestFile file = getQuestFile();
        if (file == null) return 0;
        AtomicInteger count = new AtomicInteger(0);
        file.forAllQuests(q -> count.incrementAndGet());
        return count.get();
    }

    public static int getCompletedQuests(UUID playerUUID) {
        TeamData data = getTeamData(playerUUID);
        if (data == null) return 0;

        ServerQuestFile file = getQuestFile();
        if (file == null) return 0;

        AtomicInteger count = new AtomicInteger(0);
        file.forAllQuests(quest -> {
            if (data.isCompleted(quest)) {
                count.incrementAndGet();
            }
        });
        return count.get();
    }

    public static int getQuestProgressPercent(UUID playerUUID) {
        int total = getTotalQuests();
        if (total == 0) return 0;
        int completed = getCompletedQuests(playerUUID);
        return (int) Math.round((completed / (double) total) * 100);
    }

    public static int getTotalChapters() {
        ServerQuestFile file = getQuestFile();
        if (file == null) return 0;
        AtomicInteger count = new AtomicInteger(0);
        file.forAllChapters(ch -> count.incrementAndGet());
        return count.get();
    }

    public static int getCompletedChapters(UUID playerUUID) {
        TeamData data = getTeamData(playerUUID);
        if (data == null) return 0;

        ServerQuestFile file = getQuestFile();
        if (file == null) return 0;

        AtomicInteger completed = new AtomicInteger(0);
        file.forAllChapters(chapter -> {
            if (chapter.getQuests().isEmpty()) return;

            boolean allCompleted = true;
            for (Quest quest : chapter.getQuests()) {
                if (!data.isCompleted(quest)) {
                    allCompleted = false;
                    break;
                }
            }

            if (allCompleted) {
                completed.incrementAndGet();
            }
        });
        return completed.get();
    }
}
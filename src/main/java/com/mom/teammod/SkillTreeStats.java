package com.mom.teammod;

import daripher.skilltree.capability.skill.IPlayerSkills;
import daripher.skilltree.capability.skill.PlayerSkillsProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.util.LazyOptional;

import java.util.UUID;

public class SkillTreeStats {

    private static final int MAX_LEVEL_FOR_PROGRESS = 100; // <-- Здесь меняй максимум (100, 150 и т.д.)

    public static int getLevel(UUID playerUUID) {
        Player player = Minecraft.getInstance().level.getPlayerByUUID(playerUUID);
        if (player == null) return 0;

        LazyOptional<IPlayerSkills> optional = player.getCapability(PlayerSkillsProvider.CAPABILITY);
        if (optional.isPresent()) {
            IPlayerSkills skills = optional.resolve().orElse(null);
            if (skills != null) {
                return skills.getCurrentLevel();
            }
        }
        return 0;
    }

    public static double getCurrentExp(UUID playerUUID) {
        Player player = Minecraft.getInstance().level.getPlayerByUUID(playerUUID);
        if (player == null) return 0;

        LazyOptional<IPlayerSkills> optional = player.getCapability(PlayerSkillsProvider.CAPABILITY);
        if (optional.isPresent()) {
            IPlayerSkills skills = optional.resolve().orElse(null);
            if (skills != null) {
                return skills.getSkillExperience();
            }
        }
        return 0;
    }

    public static int getNextLevelCost(UUID playerUUID) {
        Player player = Minecraft.getInstance().level.getPlayerByUUID(playerUUID);
        if (player == null) return 1;

        LazyOptional<IPlayerSkills> optional = player.getCapability(PlayerSkillsProvider.CAPABILITY);
        if (optional.isPresent()) {
            IPlayerSkills skills = optional.resolve().orElse(null);
            if (skills != null) {
                return skills.getNextLevelCost();
            }
        }
        return 1;
    }

    public static int getProgressPercent(UUID playerUUID) {
        double current = getCurrentExp(playerUUID);
        int cost = getNextLevelCost(playerUUID);
        if (cost <= 0) return 100;
        return (int) Math.min(100, (current / cost) * 100);
    }

    public static int getOverallProgressPercent(UUID playerUUID) {
        int currentLevel = getLevel(playerUUID);
        if (currentLevel >= MAX_LEVEL_FOR_PROGRESS) return 100;
        if (currentLevel <= 0) return 0;

        // Прогресс от 1 до MAX (на 1 уровне — ~1%, на MAX — 100%)
        return (int) ((currentLevel / (double) MAX_LEVEL_FOR_PROGRESS) * 100);
    }
}
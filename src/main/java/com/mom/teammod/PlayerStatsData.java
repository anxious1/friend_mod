package com.mom.teammod;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.stats.Stats;
import net.minecraft.stats.StatsCounter;

public class PlayerStatsData {
    public int deaths;
    public int mobsKilled;
    public long distanceCm;
    public int playTimeTicks;
    public int chestsOpened;

    public PlayerStatsData() {}

    public PlayerStatsData(StatsCounter stats) {
        this.deaths = stats.getValue(Stats.CUSTOM.get(Stats.DEATHS));
        this.mobsKilled = stats.getValue(Stats.CUSTOM.get(Stats.MOB_KILLS));
        this.distanceCm = stats.getValue(Stats.CUSTOM.get(Stats.WALK_ONE_CM)) +
                stats.getValue(Stats.CUSTOM.get(Stats.SPRINT_ONE_CM)) +
                stats.getValue(Stats.CUSTOM.get(Stats.SWIM_ONE_CM)) +
                stats.getValue(Stats.CUSTOM.get(Stats.FLY_ONE_CM)) +
                stats.getValue(Stats.CUSTOM.get(Stats.BOAT_ONE_CM)) +
                stats.getValue(Stats.CUSTOM.get(Stats.MINECART_ONE_CM)) +
                stats.getValue(Stats.CUSTOM.get(Stats.HORSE_ONE_CM)) +
                stats.getValue(Stats.CUSTOM.get(Stats.PIG_ONE_CM)) +
                stats.getValue(Stats.CUSTOM.get(Stats.STRIDER_ONE_CM));
        this.playTimeTicks = stats.getValue(Stats.CUSTOM.get(Stats.PLAY_TIME));
        this.chestsOpened = stats.getValue(Stats.CUSTOM.get(Stats.OPEN_CHEST));
    }

    public CompoundTag serialize() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("deaths", deaths);
        tag.putInt("mobsKilled", mobsKilled);
        tag.putLong("distanceCm", distanceCm);
        tag.putInt("playTimeTicks", playTimeTicks);
        tag.putInt("chestsOpened", chestsOpened);
        return tag;
    }

    public void deserialize(CompoundTag tag) {
        deaths = tag.getInt("deaths");
        mobsKilled = tag.getInt("mobsKilled");
        distanceCm = tag.getLong("distanceCm");
        playTimeTicks = tag.getInt("playTimeTicks");
        chestsOpened = tag.getInt("chestsOpened");
    }
}
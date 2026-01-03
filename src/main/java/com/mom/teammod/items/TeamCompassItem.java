package com.mom.teammod.items;

import com.mom.teammod.ProfileManager;
import com.mom.teammod.TeamManager;
import com.mom.teammod.TeamWorldData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class TeamCompassItem extends Item {
    public TeamCompassItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) return InteractionResultHolder.pass(stack);

        ProfileManager.Profile profile = ProfileManager.getProfile((ServerLevel) level, player.getUUID());
        if (!profile.isShowOnCompass()) {
            player.displayClientMessage(Component.literal("У ВАС ВЫКЛЮЧЕН КОМАНДНЫЙ КОМПАС"), true);
            return InteractionResultHolder.success(stack);
        }

        TeamWorldData data = TeamWorldData.get((ServerLevel) level);
        var myTeams = data.getPlayerTeams().getOrDefault(player.getUUID(), Collections.emptySet());
        List<UUID> valid = new ArrayList<>();

        for (String teamName : myTeams) {
            TeamManager.Team team = data.getTeams().get(teamName);
            if (team != null && team.showCompass()) {
                for (UUID member : team.getMembers()) {
                    if (!member.equals(player.getUUID())) {
                        ProfileManager.Profile targetProfile = data.getPlayerProfiles().getOrDefault(member, new ProfileManager.Profile(member));
                        if (targetProfile.isShowOnCompass()) {
                            valid.add(member);
                        }
                    }
                }
            }
        }

        if (valid.isEmpty()) {
            if (stack.hasTag()) stack.getTag().remove("TrackedPlayer");
            player.displayClientMessage(Component.literal("Нет доступных тиммейтов"), true);
            return InteractionResultHolder.success(stack);
        }

        CompoundTag tag = stack.getOrCreateTag();
        UUID current = tag.hasUUID("TrackedPlayer") ? tag.getUUID("TrackedPlayer") : null;
        int idx = valid.indexOf(current);
        if (idx == -1) idx = -1;
        int nextIdx = (idx + 1) % valid.size();
        UUID next = valid.get(nextIdx);
        tag.putUUID("TrackedPlayer", next);

        String name = ((ServerPlayer) player).getServer().getPlayerList().getPlayer(next).getName().getString();
        player.displayClientMessage(Component.literal("Отслеживается " + name), true);

        return InteractionResultHolder.success(stack);
    }
}
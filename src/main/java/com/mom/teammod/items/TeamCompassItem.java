package com.mom.teammod.items;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CompassItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class TeamCompassItem extends CompassItem { // Наследуемся от CompassItem для базовой логики компаса
    public TeamCompassItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        // Логика ПКМ: Переключение на следующего тиммейта
        // Пока заглушка — позже добавим реальную логику с тиммейтами
        player.displayClientMessage(Component.literal("Переключение на следующего тиммейта..."), true); // Сообщение сверху
        // TODO: Циклическое переключение, указание на позицию тиммейта

        return super.use(level, player, hand); // Вызываем супер для базовой анимации
    }
}
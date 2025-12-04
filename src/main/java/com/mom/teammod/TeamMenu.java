package com.mom.teammod;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class TeamMenu extends AbstractContainerMenu {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, TeamMod.MODID);
    public static final RegistryObject<MenuType<TeamMenu>> TEAM_MENU = MENUS.register("team_menu",
            () -> IForgeMenuType.create((windowId, inv, data) -> new TeamMenu(windowId, inv)));

    public TeamMenu(int windowId, Inventory playerInventory) {
        super(TEAM_MENU.get(), windowId);
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY; // No slots, so no item movement
    }

    public static void register(IEventBus eventBus) {
        MENUS.register(eventBus);
    }
}
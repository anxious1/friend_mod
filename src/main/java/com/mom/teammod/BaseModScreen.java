package com.mom.teammod;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;

public abstract class BaseModScreen extends Screen {
    protected Screen parentScreen;

    protected BaseModScreen(Screen parentScreen, Component title) {
        super(title);
        this.parentScreen = parentScreen;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) { // ESC
            this.onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void onClose() {
        if (parentScreen != null) {
            minecraft.setScreen(parentScreen);
        } else {
            minecraft.setScreen(new InventoryScreen(minecraft.player));
        }
    }
}
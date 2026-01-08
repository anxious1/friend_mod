package com.mom.teammod;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class ReloadButton extends Button {
    private static final ResourceLocation RELOAD    = new ResourceLocation("teammod","textures/gui/reload.png");
    private static final ResourceLocation RELOAD_NO = new ResourceLocation("teammod","textures/gui/reload_no.png");

    private boolean working = false;
    private int cooldown = 0;

    public ReloadButton(int x, int y, Runnable onPress){
        super(x, y, 16, 16, Component.empty(),
                b -> { if(!((ReloadButton)b).working){ ((ReloadButton)b).start(); onPress.run(); } },
                s -> Component.empty());
    }

    private void start(){ working = true; cooldown = 20; }   // 1 сек

    @Override
    public void renderWidget(GuiGraphics g, int mx, int my, float pt){
        RenderSystem.setShaderTexture(0, working ? RELOAD_NO : RELOAD);
        g.blit(working ? RELOAD_NO : RELOAD, getX(), getY(), 0, 0, 16, 16, 16, 16);
    }

    public void tick(){ if(--cooldown <= 0) working = false; }
}
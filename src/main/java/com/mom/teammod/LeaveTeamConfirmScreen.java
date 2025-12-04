package com.mom.teammod;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mom.teammod.packets.LeaveTeamPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class LeaveTeamConfirmScreen extends Screen {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(TeamMod.MODID, "textures/gui/leave_team_window.png");
    private final Screen parent;
    private final String teamName;

    public LeaveTeamConfirmScreen(Screen parent, String teamName) {
        super(Component.literal("Leave Team"));
        this.parent = parent;
        this.teamName = teamName;
    }

    @Override
    protected void init() {
        super.init();
        int x = (width - 176) / 2;
        int y = (height - 166) / 2;

        addRenderableWidget(Button.builder(Component.literal("Confirm"), button -> {
            NetworkHandler.INSTANCE.sendToServer(new LeaveTeamPacket(teamName));
            minecraft.setScreen(parent);
        }).pos(x + 20, y + 100).size(60, 20).build());

        addRenderableWidget(Button.builder(Component.literal("Cancel"), button -> minecraft.setScreen(parent))
                .pos(x + 96, y + 100).size(60, 20).build());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics);
        RenderSystem.setShaderTexture(0, TEXTURE);
        int x = (width - 176) / 2;
        int y = (height - 166) / 2;
        guiGraphics.blit(TEXTURE, x, y, 0, 0, 176, 166);

        guiGraphics.drawCenteredString(font, "Leave " + teamName + "?", x + 88, y + 50, 0xFFFFFF);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }
}
package com.mom.teammod;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import java.util.UUID;

public class OtherProfileScreen extends Screen {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(TeamMod.MODID, "textures/gui/other_profile_background.png");
    private static final int GUI_WIDTH = 256;
    private static final int GUI_HEIGHT = 170;

    private static final int SKIN_FRAME_WIDTH = 87;
    private static final int SKIN_FRAME_HEIGHT = 100;
    private static final int FRAME_MARGIN_LEFT = 15;

    private final UUID otherUUID;
    private final Screen parent;
    private Player otherPlayer;

    public OtherProfileScreen(UUID otherUUID, Screen parent, Component title) {
        super(title);
        this.otherUUID = otherUUID;
        this.parent = parent;
        this.otherPlayer = minecraft.level.getPlayerByUUID(otherUUID);
    }

    @Override
    protected void init() {
        super.init();
        int x = (width - GUI_WIDTH) / 2;
        int y = (height - GUI_HEIGHT) / 2;

        addRenderableWidget(Button.builder(
                Component.translatable("gui.teammod.back_to_inventory"),
                button -> minecraft.setScreen(new InventoryScreen(minecraft.player))
        ).pos(x + 10, y - 30).size(100, 20).build());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics);
        renderBg(guiGraphics, partialTick, mouseX, mouseY);

        if (otherPlayer != null) {
            int frameX = (width - GUI_WIDTH) / 2 + FRAME_MARGIN_LEFT;
            int frameY = (height - GUI_HEIGHT) / 2 + (GUI_HEIGHT - SKIN_FRAME_HEIGHT) / 2;

            int playerX = frameX + SKIN_FRAME_WIDTH / 2;
            int playerY = frameY + SKIN_FRAME_HEIGHT - 10;

            InventoryScreen.renderEntityInInventoryFollowsMouse(
                    guiGraphics,
                    playerX,
                    playerY,
                    38,
                    (float)playerX - mouseX,
                    (float)(frameY + SKIN_FRAME_HEIGHT / 2) - mouseY,
                    otherPlayer
            );
        }

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderTexture(0, TEXTURE);
        int x = (width - GUI_WIDTH) / 2;
        int y = (height - GUI_HEIGHT) / 2;
        guiGraphics.blit(TEXTURE, x, y, 0, 0, GUI_WIDTH, GUI_HEIGHT, GUI_WIDTH, GUI_HEIGHT);
    }
}
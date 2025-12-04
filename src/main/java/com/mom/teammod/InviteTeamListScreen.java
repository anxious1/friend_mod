package com.mom.teammod;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mom.teammod.packets.InvitePlayerPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import java.util.Set;
import java.util.UUID;

public class InviteTeamListScreen extends Screen {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(TeamMod.MODID, "textures/gui/team_invite_list_background.png");
    private final Screen parent;
    private final UUID targetUUID;

    public InviteTeamListScreen(Screen parent, UUID targetUUID) {
        super(Component.literal("Invite to Team"));
        this.parent = parent;
        this.targetUUID = targetUUID;
    }

    @Override
    protected void init() {
        super.init();
        int x = (width - 176) / 2;
        int y = (height - 166) / 2;

        Set<String> myTeams = TeamManager.getPlayerTeams(minecraft.player.getUUID());
        int buttonY = y + 20;

        for (String teamName : myTeams) {
            TeamManager.Team team = TeamManager.getTeam(teamName);
            boolean canInvite = team != null
                    && !team.getMembers().contains(targetUUID)
                    && !team.getInvited().contains(targetUUID);

            addRenderableWidget(Button.builder(
                    Component.literal(teamName + (canInvite ? "" : " (invited)")),
                    button -> {
                        if (canInvite) {
                            String targetName = minecraft.getConnection().getPlayerInfo(targetUUID).getTabListDisplayName() != null
                                    ? minecraft.getConnection().getPlayerInfo(targetUUID).getTabListDisplayName().getString()
                                    : "Player";
                            NetworkHandler.INSTANCE.sendToServer(new InvitePlayerPacket(teamName, targetName));
                        }
                        minecraft.setScreen(parent);
                    }
            ).pos(x + 10, buttonY).size(150, 20).build());

            buttonY += 25;
        }

        addRenderableWidget(Button.builder(Component.literal("Cancel"), button -> minecraft.setScreen(parent))
                .pos(x + 10, y + 140).size(150, 20).build());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics);
        RenderSystem.setShaderTexture(0, TEXTURE);
        int x = (width - 176) / 2;
        int y = (height - 166) / 2;
        guiGraphics.blit(TEXTURE, x, y, 0, 0, 176, 166);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }
}
package com.mom.teammod;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.util.*;

import static com.mom.teammod.TeamManager.clientPlayerTeams;
import static com.mom.teammod.TeamManager.clientTeams;

public class TeamScreen extends Screen {
    private static final ResourceLocation ATLAS = ResourceLocation.fromNamespaceAndPath(TeamMod.MODID, "textures/gui/my_teams.png");
    // Иконки из твоего my_teams.png (координаты из CVAT)

    private static final ResourceLocation BACKGROUND = ResourceLocation.fromNamespaceAndPath(TeamMod.MODID, "textures/gui/team_list_background.png");
    private static final int COMPASS_U = 15, COMPASS_V = 208, COMPASS_W = 15, COMPASS_H = 15;  // иконка компаса
    private static final int TAG_U     = 31, TAG_V     = 212, TAG_W     = 28, TAG_H     = 10;   // иконка тега
    private static final int PIMP_DOT_U = 2,  PIMP_DOT_V = 211, PIMP_DOT_W = 10, PIMP_DOT_H = 10;  // маленькая точка пимпа (вкл/выкл)

    private static final int GUI_WIDTH = 256;
    private static final int GUI_HEIGHT = 170;

    private final Inventory playerInventory;
    private boolean compassVisible = false;
    private Button pimpButton;

    public TeamScreen(TeamMenu menu, Inventory playerInventory, Component title) {
        super(title);
        this.playerInventory = playerInventory;
    }

    @Override
    protected void init() {
        super.init();
        int guiX = (width - GUI_WIDTH) / 2;
        int guiY = (height - GUI_HEIGHT) / 2;

        UUID playerId = minecraft.player.getUUID();
        Set<String> playerTeams = clientPlayerTeams.getOrDefault(playerId, Set.of());
        List<String> playerTeamList = new ArrayList<>(playerTeams);

        // === Y позиции: 36, 73, 110 ===
        int[] yPositions = {36, 73, 110};

        for (int slot = 0; slot < 3; slot++) {
            int y = yPositions[slot];

            if (slot >= playerTeamList.size()) {
                addTransparentButton(guiX + 17, guiY + y, 28, 13,
                        this::openJoinList, Component.literal("Присоединиться"));
            }

            addTransparentButton(guiX + 58, guiY + y, 43, 13,
                    this::openCreateTeam, Component.literal("Создать команду"));

            if (slot < playerTeamList.size()) {
                String teamName = playerTeamList.get(slot);
                addTransparentButton(guiX + 108, guiY + y, 43, 13,
                        () -> openTeamProfile(teamName), Component.literal("Профиль"));
                addTransparentButton(guiX + 158, guiY + y, 43, 13,
                        () -> leaveTeam(teamName), Component.literal("Покинуть"));
            }
        }

        // PIMP 1 — компас
        final boolean[] compassEnabled = {false};
        addPimpButton(
                guiX + 44, guiY + 147, 14, 14,
                16, 138, 15, 14,
                COMPASS_U, COMPASS_V, COMPASS_W, COMPASS_H,
                "Вкл. / Выкл. командный компас для всех команд.",
                compassEnabled
        );

        // PIMP 2 — тег
        final boolean[] tagEnabled = {true}; // по умолчанию включён
        addPimpButton(
                guiX + 153, guiY + 147, 14, 14,
                113 - 81 - 28, 140, 28, 9,
                TAG_U, TAG_V, TAG_W, TAG_H,
                "Вкл. / Выкл. отображение тега выбранной команды.",
                tagEnabled
        );

        // Кнопка "Назад"
        addRenderableWidget(Button.builder(
                Component.translatable("gui.teammod.back_to_inventory"),
                b -> minecraft.setScreen(new InventoryScreen(minecraft.player))
        ).pos(guiX + 10, guiY - 30).size(100, 20).build());
    }

    private Button addTransparentButton(int x, int y, int w, int h, Runnable action, Component tooltip) {
        Button button = new Button(x, y, w, h, Component.empty(), b -> action.run(),
                (s) -> Component.empty()) {  // ← ИСПРАВЛЕНО
            @Override
            public void renderWidget(GuiGraphics g, int mx, int my, float pt) {
                if (this.isHovered()) {
                    g.fill(this.getX(), this.getY(), this.getX() + w, this.getY() + h, 0x30FFFFFF);
                }
            }
        };
        button.setTooltip(Tooltip.create(tooltip));
        return addRenderableWidget(button);
    }

    private void addPimpButton(
            int buttonX, int buttonY, int w, int h,
            int holderGuiX, int holderGuiY, int holderWidth, int holderHeight,
            int iconU, int iconV, int iconW, int iconH,
            String tooltipText,
            boolean[] enabled
    ) {
        Button button = new Button(buttonX, buttonY, w, h, Component.empty(), b -> {
            enabled[0] = !enabled[0];
        }, s -> Component.empty()) {

            { this.setTooltip(Tooltip.create(Component.literal(tooltipText))); }

            @Override
            public void renderWidget(GuiGraphics g, int mx, int my, float pt) {
                if (this.isHovered()) {
                    g.fill(getX(), getY(), getX() + w, getY() + h, 0x30FFFFFF);
                }

                if (enabled[0]) {
                    // Маленькая точка пимпа (вкл)
                    RenderSystem.setShaderTexture(0, ATLAS);
                    g.blit(ATLAS, getX() + 2, getY() + 2, PIMP_DOT_U, PIMP_DOT_V, PIMP_DOT_W, PIMP_DOT_H, 256, 256);

                    // Иконка (компас или тег)
                    g.blit(ATLAS, buttonX - 44 + holderGuiX, buttonY - 147 + holderGuiY,
                            iconU, iconV, iconW, iconH, 256, 256);
                }
            }
        };
        addRenderableWidget(button);
    }

    // === ДЕЙСТВИЯ ===
    private void openCreateTeam() {
        minecraft.setScreen(new CreatingTeamScreen(this));
    }

    private void openJoinList() {
        minecraft.setScreen(new TeamsListScreen());
    }

    private void openInviteList() {
    }

    private void openTeamProfile(String teamName) {
        // TODO: Получить реальные настройки из TeamManager
        TeamManager.Team team = TeamManager.getTeam(teamName);
        if (team != null) {
            minecraft.setScreen(new TeamProfileOwner(
                    null,
                    playerInventory,
                    Component.literal(teamName),
                    teamName,
                    team.getTag(),
                    true, // временно
                    true, // временно
                    team.isFriendlyFire()
            ));
        } else {
            minecraft.setScreen(new TeamProfileOwner(
                    null,
                    playerInventory,
                    Component.literal(teamName),
                    teamName,
                    "",
                    true,
                    true,
                    true
            ));
        }
    }

    private void leaveTeam(String teamName) {
        minecraft.setScreen(new LeaveTeamConfirmScreen(this, teamName));
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        renderBackground(g);
        renderBg(g, pt, mx, my);
        super.render(g, mx, my, pt);
    }

    protected void renderBg(GuiGraphics g, float pt, int mx, int my) {
        RenderSystem.setShaderTexture(0, BACKGROUND);
        int x = (width - GUI_WIDTH) / 2;
        int y = (height - GUI_HEIGHT) / 2;
        g.blit(BACKGROUND, x, y, 0, 0, GUI_WIDTH, GUI_HEIGHT, GUI_WIDTH, GUI_HEIGHT);
    }

    public void refreshLists() {
        UUID playerUUID = minecraft.player.getUUID();
        clientPlayerTeams.put(playerUUID, new HashSet<>(TeamManager.playerTeams.getOrDefault(playerUUID, Set.of())));
        clientTeams.clear();
        clientTeams.putAll(TeamManager.teams);
    }
}
package com.mom.teammod;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mom.teammod.packets.AskMyLeaderTeamsPacket;
import com.mom.teammod.packets.InvitePlayerPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.*;

public class MyTeamsListScreen extends BaseModScreen {

    private static final ResourceLocation ATLAS = ResourceLocation.fromNamespaceAndPath(TeamMod.MODID,
            "textures/gui/my_teams_list.png");

    private static final int PLASHKA_BASIC_U = 0,   PLASHKA_BASIC_V = 207,  PLASHKA_BASIC_W = 168, PLASHKA_BASIC_H = 24;
    private static final int PLASHKA_HOVER_U = 1,   PLASHKA_HOVER_V = 232,  PLASHKA_HOVER_W = 168, PLASHKA_HOVER_H = 24;
    private static final int PLASHKA_CLICKED_U = 0, PLASHKA_CLICKED_V = 183, PLASHKA_CLICKED_W = 169, PLASHKA_CLICKED_H = 24;
    private final List<TeamManager.Team> teamList = new ArrayList<>();
    private int askCooldown = 0;
    private static final int CONFIRM_U = 0, CONFIRM_V = 147, CONFIRM_W = 47, CONFIRM_H = 14;
    private List<TeamManager.Team> leaderTeams = new ArrayList<>();
    private static final int GUI_WIDTH = 256;
    private static final int GUI_HEIGHT = 147;
    private static final int VISIBLE_SLOTS = 4; // видно одновременно 4 команды (по текстуре)
    private static final int SLOT_HEIGHT = 30;
    private String targetName;
    // Реальные команды игрока
    private List<TeamManager.Team> myTeams = new ArrayList<>();
    public final Set<String> clickedTeams = new HashSet<>();
    private int scrollOffset = 0;

    private Button confirmButton;

    // новый – с именем цели
    public MyTeamsListScreen(Screen parentScreen, String targetName) {
        super(parentScreen, Component.literal(""));
        this.targetName = targetName == null ? "" : targetName;

        NetworkHandler.INSTANCE.sendToServer(new AskMyLeaderTeamsPacket());
    }

    private int left() { return (width - GUI_WIDTH) / 2; }
    private int top()  { return (height - GUI_HEIGHT) / 2; }

    @Override
    protected void init() {
        super.init();
        scrollOffset = 0;

        int x = left();
        int y = top();

        addTransparentButton(
                x + 10 + 22,
                y + GUI_HEIGHT - 20 - 9,
                30, 12,
                () -> minecraft.setScreen(parentScreen),
                Component.literal("Назад")
        );

        // Удаляем старую кнопку confirm, если она осталась от предыдущего init
        if (confirmButton != null) {
            removeWidget(confirmButton);
            confirmButton = null;
        }
        int reloadX = left() + GUI_WIDTH - 20;
        int reloadY = top() + 5;
        addRenderableWidget(new ReloadButton(reloadX, reloadY, this::refreshTeamList));
        confirmButton = new Button(
                x + GUI_WIDTH - CONFIRM_W - 20 - 22 - 32,
                y + GUI_HEIGHT - CONFIRM_H - 15 - 20 + 11 + 8,
                CONFIRM_W, CONFIRM_H,
                Component.empty(),
                btn -> sendInvite(),
                (s) -> Component.empty()
        ) {
            @Override
            public void renderWidget(GuiGraphics g, int mx, int my, float pt) {
                if (!clickedTeams.isEmpty()) {
                    g.blit(ATLAS, this.getX(), this.getY(), CONFIRM_U, CONFIRM_V, CONFIRM_W, CONFIRM_H, 256, 256);
                }
                if (this.isHovered() && !clickedTeams.isEmpty()) {
                    g.fill(this.getX(), this.getY(), this.getX() + CONFIRM_W, this.getY() + CONFIRM_H, 0x30FFFFFF);
                }
            }
        };
        confirmButton.setTooltip(Tooltip.create(Component.literal("Подтвердить выбор")));
        addRenderableWidget(confirmButton);

        refreshTeamList();
        createTeamButtons();
    }

    public void refreshTeamList() {
        UUID my = minecraft.player.getUUID();
        leaderTeams = TeamManager.getLeaderTeams(my);

        UUID target = PlayerNameCache.getUUID(targetName);
        if (target != null) {
            leaderTeams.removeIf(t -> TeamManager.alreadyInTeamOrInvited(target, t.getName()));
        }
        leaderTeams.sort(Comparator.comparing(TeamManager.Team::getName));

        /* ➜➜➜ копируем в myTeams, чтобы кнопки их увидели */
        myTeams.clear();
        myTeams.addAll(leaderTeams);
        createTeamButtons();
    }
    private void createTeamButtons() {
        this.renderables.removeIf(w -> w instanceof TeamButton);

        int baseX = left() + 45 - 24;
        int baseY = top() + 32 + 14 - 14;

        int totalTeams = myTeams.size();
        int maxVisibleIndex = Math.min(VISIBLE_SLOTS, totalTeams);

        for (int i = 0; i < maxVisibleIndex; i++) {
            int index = scrollOffset + i;
            if (index >= totalTeams) break;

            TeamManager.Team team = myTeams.get(index);
            int buttonY = baseY + i * PLASHKA_BASIC_H;

            addRenderableWidget(new TeamButton(baseX, buttonY, PLASHKA_BASIC_W, PLASHKA_BASIC_H, team));
        }
    }

    private void onTeamClicked(TeamManager.Team team) {
        String teamName = team.getName();

        if (clickedTeams.contains(teamName)) {
            clickedTeams.remove(teamName);
        } else {
            // Лимит выбора — 3 команды (это логично — игрок может быть максимум в 3 командах)
            if (clickedTeams.size() < 3) {
                clickedTeams.add(teamName);
            }
        }

        createTeamButtons(); // перерисовываем кнопки
    }

    private void sendInvite() {
        if (clickedTeams.isEmpty()) {
            return;
        }

        // Отключаем кнопку сразу — защита от двойного клика
        confirmButton.active = false;
        confirmButton.visible = false;

        String targetName = "Unknown";
        if (parentScreen instanceof OtherPlayerProfileScreen profile) {
            targetName = profile.playerName;
        }

        // Отправляем приглашения
        for (String teamName : clickedTeams) {
            NetworkHandler.INSTANCE.sendToServer(new InvitePlayerPacket(teamName, targetName));
        }

        // Закрываем экран с небольшой задержкой, чтобы пакеты точно ушли
        Minecraft.getInstance().execute(() -> {
            minecraft.setScreen(parentScreen);
        });
    }

    private Button addTransparentButton(int x, int y, int w, int h, Runnable action, Component tooltip) {
        Button button = new Button(x, y, w, h, Component.empty(), b -> action.run(), (s) -> Component.empty()) {
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

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(g);
        RenderSystem.setShaderTexture(0, ATLAS);
        g.blit(ATLAS, left(), top(), 0, 0, GUI_WIDTH, GUI_HEIGHT, 256, 256);

        super.render(g, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        int total = myTeams.size();
        if (total <= VISIBLE_SLOTS) return false;

        scrollOffset -= (int) delta;
        scrollOffset = Math.max(0, Math.min(scrollOffset, total - VISIBLE_SLOTS));
        createTeamButtons();
        return true;
    }

    private class TeamButton extends Button {
        private final TeamManager.Team team;

        public TeamButton(int x, int y, int width, int height, TeamManager.Team team) {
            super(x, y, width, height, Component.empty(), btn -> {}, DEFAULT_NARRATION);
            this.team = team;
        }

        @Override
        public void onClick(double mouseX, double mouseY) {
            super.onClick(mouseX, mouseY);
            MyTeamsListScreen.this.onTeamClicked(team);
        }

        @Override
        public void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
            boolean selected = clickedTeams.contains(team.getName());

            int u, v;
            if (selected) {
                u = PLASHKA_CLICKED_U;
                v = PLASHKA_CLICKED_V;
            } else if (this.isHovered()) {
                u = PLASHKA_HOVER_U;
                v = PLASHKA_HOVER_V;
            } else {
                u = PLASHKA_BASIC_U;
                v = PLASHKA_BASIC_V;
            }

            RenderSystem.setShaderTexture(0, ATLAS);
            g.blit(ATLAS, getX(), getY(), u, v, width, height, 256, 256);

            String displayText = team.getName();
            if (!team.getTag().isEmpty()) {
                displayText += " [" + team.getTag() + "]";
            }

            g.drawString(font, displayText, getX() + 10, getY() + 8, 0xFFFFFF, false);
        }
    }
}
package com.mom.teammod;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ImageButton;
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
    private static final ResourceLocation BACKGROUND = ResourceLocation.fromNamespaceAndPath(TeamMod.MODID, "textures/gui/team_list_background.png");

    private static final ResourceLocation PROFILE_ATLAS = ResourceLocation.fromNamespaceAndPath(TeamMod.MODID, "textures/gui/my_profile_background.png");

    // Иконки из my_teams.png (координаты из CVAT)

    private static final int COMPASS_U = 15, COMPASS_V = 208, COMPASS_W = 15, COMPASS_H = 15;  // иконка компаса
    private static final int TAG_U     = 31, TAG_V     = 212, TAG_W     = 28, TAG_H     = 10;   // иконка тега
    private static final int PIMP_DOT_U = 2,  PIMP_DOT_V = 211, PIMP_DOT_W = 10, PIMP_DOT_H = 10;  // маленькая точка пимпа (вкл/выкл)

    private static final int GUI_WIDTH = 256;
    private static final int GUI_HEIGHT = 170;

    private final Inventory playerInventory;
    private boolean compassVisible = false;
    private Button pimpButton;
    private static final int PROFILE_BTN_U = 0;    // "Профиль"
    private static final int PROFILE_BTN_V = 170;
    private static final int PROFILE_BTN_W = 44;
    private static final int PROFILE_BTN_H = 14;

    Screen parentScreen;

    private static final int LEAVE_BTN_U = 0;      // "Покинуть"
    private static final int LEAVE_BTN_V = 184;
    private static final int LEAVE_BTN_W = 37;
    private static final int LEAVE_BTN_H = 14;

    // Плашка команды (из my_teams.png)
    private static final int PLASHKA_U = 0;
    private static final int PLASHKA_V = 222;
    private static final int PLASHKA_W = 100;
    private static final int PLASHKA_H = 33;

    // Звёздочка
    private static final int ZVEZDA_U = 0;
    private static final int ZVEZDA_V = 199;
    private static final int ZVEZDA_W = 8;
    private static final int ZVEZDA_H = 6;

    // === НОВОЕ: СКРОЛЛЯЩИЙСЯ СПИСОК ПРИГЛАШЕНИЙ (из MyProfileScreen) ===
    private static final int INV_U       = 1,   INV_V       = 171, INV_W   = 23, INV_H   = 16;
    private static final int SCROLLER_U  = 25,  SCROLLER_V  = 171, SCROLLER_W = 6,  SCROLLER_H = 25;
    private static final int X_U         = 1,   X_V         = 187, X_W     = 12, X_H     = 11;
    private static final int V_U         = 13,  V_V         = 187, V_W     = 11, V_H     = 11;

    private static final int VISIBLE_SLOTS = 4;
    private final List<TeamManager.Team> teamList = new ArrayList<>();
    private int scrollOffset = 0;
    private boolean isDraggingScroller = false;
    private int lastRenderedScrollOffset = -1;
    private static final int SCROLL_TRACK_HEIGHT = 119;

    public TeamScreen(Screen parentScreen, TeamMenu menu, Inventory playerInventory, Component title) {
        super(title);
        this.parentScreen = parentScreen;
        this.playerInventory = playerInventory;
    }

    private Button addAtlasButton(int x, int y, int w, int h, int u, int v, Runnable action, Component tooltip) {
        Button btn = new Button(x, y, w, h, Component.empty(), b -> action.run(), s -> Component.empty()) {
            @Override
            public void renderWidget(GuiGraphics g, int mx, int my, float pt) {
                RenderSystem.setShaderTexture(0, ATLAS);
                g.blit(ATLAS, getX(), getY(), u, v, w, h, 256, 256);
                if (isHovered()) {
                    g.fill(getX(), getY(), getX() + w, getY() + h, 0x30FFFFFF);
                }
            }
        };
        btn.setTooltip(Tooltip.create(tooltip));
        return addRenderableWidget(btn);
    }

    @Override
    protected void init() {
        super.init();
        scrollOffset = 0;
        lastRenderedScrollOffset = -1;

        int guiX = (width - 256) / 2;
        int guiY = (height - 170) / 2;
        int baseY = guiY - 26;

        ResourceLocation unpress = ResourceLocation.fromNamespaceAndPath(TeamMod.MODID, "textures/gui/unpress.png");
        ResourceLocation press   = ResourceLocation.fromNamespaceAndPath(TeamMod.MODID, "textures/gui/press.png");

        ResourceLocation INV_ICON       = ResourceLocation.fromNamespaceAndPath(TeamMod.MODID, "textures/gui/inv_icon.png");
        ResourceLocation TEAM_LIST_ICON = ResourceLocation.fromNamespaceAndPath(TeamMod.MODID, "textures/gui/team_list_icon.png");
        ResourceLocation PROFILE_ICON   = ResourceLocation.fromNamespaceAndPath(TeamMod.MODID, "textures/gui/profile_icon.png");

        // КНОПКА ИНВЕНТАРЬ
        this.addRenderableWidget(new Button(guiX + 2, baseY, 26, 27, Component.empty(), btn -> {
            minecraft.setScreen(new InventoryScreen(minecraft.player));
        }, supplier -> supplier.get()) {  // ← ЭТО РАБОТАЕТ В 1.20.1!
            private boolean isPressed = false;

            @Override
            public void renderWidget(GuiGraphics g, int mx, int my, float pt) {
                boolean active = this.isHovered() || isPressed;
                ResourceLocation tex = active ? press : unpress;
                int h = active ? 29 : 27;
                int yOff = active ? -2 : 0;

                if (this.getHeight() != h) {
                    this.setHeight(h);
                    this.setY(baseY + yOff);
                }

                g.blit(tex, getX(), getY(), 0, 0, 26, h, 26, h);
                g.blit(INV_ICON, getX() + 5, getY() + (active ? 7 : 6), 0, 0, 16, 16, 16, 16);

                if (this.isHovered()) {
                    g.renderTooltip(font, Component.translatable("gui.teammod.button3"), mx, my);
                }
            }

            @Override
            public void onClick(double mouseX, double mouseY) {
                super.onClick(mouseX, mouseY);
                this.isPressed = true;
            }
        });

        // КНОПКА КОМАНДЫ — ЗАЖАТА
        int teamX = guiX + 2 + 26 + 52;
        this.addRenderableWidget(new Button(teamX, baseY - 2, 26, 29, Component.empty(), btn -> {
            // Уже здесь — ничего не делаем
        }, supplier -> supplier.get()) {
            @Override
            public void renderWidget(GuiGraphics g, int mx, int my, float pt) {
                g.blit(press, getX(), getY(), 0, 0, 26, 29, 26, 29);
                g.blit(TEAM_LIST_ICON, getX() + 5, getY() + 6, 0, 0, 16, 16, 16, 16);

                if (this.isHovered()) {
                    g.renderTooltip(font, Component.translatable("gui.teammod.team_tab"), mx, my);
                }
            }
        });

        // КНОПКА ПРОФИЛЬ
        int profileX = teamX + 26;
        this.addRenderableWidget(new Button(profileX, baseY, 26, 27, Component.empty(), btn -> {
            minecraft.setScreen(new MyProfileScreen(TeamScreen.this, Component.translatable("gui.teammod.profile")));
        }, supplier -> supplier.get()) {
            private boolean isPressed = false;

            @Override
            public void renderWidget(GuiGraphics g, int mx, int my, float pt) {
                boolean active = this.isHovered() || isPressed;
                ResourceLocation tex = active ? press : unpress;
                int h = active ? 29 : 27;
                int yOff = active ? -2 : 0;

                if (this.getHeight() != h) {
                    this.setHeight(h);
                    this.setY(baseY + yOff);
                }

                g.blit(tex, getX(), getY(), 0, 0, 26, h, 26, h);
                g.blit(PROFILE_ICON, getX() + 5, getY() + (active ? 7 : 6), 0, 0, 16, 16, 16, 16);

                if (this.isHovered()) {
                    g.renderTooltip(font, Component.translatable("gui.teammod.profile"), mx, my);
                }
            }

            @Override
            public void onClick(double mouseX, double mouseY) {
                super.onClick(mouseX, mouseY);
                this.isPressed = true;
            }
        });

        // ТВОЙ СТАРЫЙ КОД — НЕ ТРОГАЙ!
        UUID playerId = minecraft.player.getUUID();
        Set<String> playerTeams = clientPlayerTeams.getOrDefault(playerId, Set.of());
        List<String> playerTeamList = new ArrayList<>(playerTeams);

        int[] yPositions = {36, 73, 110};

        for (int slot = 0; slot < 3; slot++) {
            int y = yPositions[slot];

            if (slot < playerTeamList.size()) {
                String teamName = playerTeamList.get(slot);
                TeamManager.Team team = TeamManager.getTeam(teamName);

                int plashkaX = guiX + 10 - 1;
                int plashkaY = guiY + y - 5 - 5 - 1;

                addRenderableOnly(new AbstractWidget(plashkaX, plashkaY, PLASHKA_W, PLASHKA_H, Component.empty()) {
                    @Override
                    public void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
                        RenderSystem.setShaderTexture(0, ATLAS);
                        g.blit(ATLAS, getX(), getY(), PLASHKA_U, PLASHKA_V, PLASHKA_W, PLASHKA_H, 256, 256);
                        g.blit(ATLAS, getX() + PLASHKA_W - ZVEZDA_W - 4, getY() + 4,
                                ZVEZDA_U, ZVEZDA_V, ZVEZDA_W, ZVEZDA_H, 256, 256);

                        String display = teamName + (team != null && !team.getTag().isEmpty() ? "[" + team.getTag() + "]" : "");
                        int textX = getX() + PLASHKA_W / 2 - font.width(display) / 2;
                        int textY = getY() + (PLASHKA_H - 9) / 2;
                        g.drawString(font, display, textX, textY, 0xFFFFFF, false);
                    }

                    @Override public boolean isMouseOver(double mouseX, double mouseY) { return false; }
                    @Override protected void updateWidgetNarration(net.minecraft.client.gui.narration.NarrationElementOutput output) {}
                });

                addAtlasButton(guiX + 108 + 5, guiY + y, PROFILE_BTN_W, PROFILE_BTN_H,
                        PROFILE_BTN_U, PROFILE_BTN_V,
                        () -> openTeamProfile(teamName), Component.literal("Профиль"));

                addAtlasButton(guiX + 158 + 3, guiY + y, LEAVE_BTN_W, LEAVE_BTN_H,
                        LEAVE_BTN_U, LEAVE_BTN_V,
                        () -> openLeaveTeam(teamName, team),
                        Component.literal("Покинуть"));
            } else {
                addTransparentButton(guiX + 17, guiY + y, 28, 13,
                        this::openJoinList, Component.literal("Присоединиться"));

                addTransparentButton(guiX + 58, guiY + y, 43, 13,
                        this::openCreateTeam, Component.literal("Создать команду"));
            }
        }

        final boolean[] compassEnabled = {false};
        addPimpButton(guiX + 44, guiY + 147, 14, 14, 16, 138, 15, 14,
                COMPASS_U, COMPASS_V, COMPASS_W, COMPASS_H,
                "Вкл. / Выкл. командный компас для всех команд.", compassEnabled);

        final boolean[] tagEnabled = {true};
        addPimpButton(guiX + 153, guiY + 147, 14, 14, 113 - 81 - 28, 140, 28, 9,
                TAG_U, TAG_V, TAG_W, TAG_H,
                "Вкл. / Выкл. отображение тега выбранной команды.", tagEnabled);

        renderTeamList(null);
    }

    private void openLeaveTeam(String teamName, TeamManager.Team team) {
        final Screen thisScreen = this; // Сохраняем ссылку на текущий экран (TeamScreen)

        LeaveTeamScreen leaveScreen = new LeaveTeamScreen(thisScreen, teamName,
                team != null && team.getTag() != null ? team.getTag() : "") {
            @Override
            public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
                // 1. Рисуем TeamScreen (thisScreen) в замороженном состоянии
                thisScreen.render(g, mouseX, mouseY, partialTick);

                // 2. Дополнительное глубокое затемнение поверх
                g.fill(0, 0, width, height, 0xB3000000);

                // 3. Рисуем окно подтверждения
                int x = left();
                int y = top();
                g.blit(ATLAS, x, y, FON_U, FON_V, FON_W, FON_H, 256, 256);

                // 4. ТЕКСТ КОМАНДЫ
                String teamText = teamName;
                String teamTagStr = team != null ? team.getTag() : "";
                if (teamTagStr != null && !teamTagStr.isEmpty()) {
                    teamText += "[" + teamTagStr + "]";
                }

                if (font.width(teamText) > TEAM_NAME_W) {
                    teamText = font.plainSubstrByWidth(teamText, TEAM_NAME_W - 6) + "..";
                }

                int textX = x + TEAM_NAME_U + (TEAM_NAME_W - font.width(teamText)) / 2;
                int textY = y + TEAM_NAME_V + (TEAM_NAME_H - 8) / 2;
                g.drawString(font, teamText, textX, textY, 0xFFFFFF, false);

                // 5. КНОПКИ
                super.render(g, mouseX, mouseY, partialTick);
            }
        };
        minecraft.setScreen(leaveScreen);
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


    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        renderBackground(g);
        renderBg(g, pt, mx, my);
        renderScroller(g);
        super.render(g, mx, my, pt);
    }

    protected void renderBg(GuiGraphics g, float pt, int mx, int my) {
        RenderSystem.setShaderTexture(0, BACKGROUND);
        int x = (width - GUI_WIDTH) / 2;
        int y = (height - GUI_HEIGHT) / 2;
        g.blit(BACKGROUND, x, y, 0, 0, GUI_WIDTH, GUI_HEIGHT, GUI_WIDTH, GUI_HEIGHT);
    }


    public void refreshLists() {
        // НИЧЕГО НЕ ОЧИЩАЕМ! Мы работаем с теми же мапами, что и TeamSyncPacket

        // Просто удаляем старые ячейки и кнопки
        this.children().removeIf(widget ->
                widget instanceof AbstractWidget && !(widget instanceof Button && ((Button)widget).getMessage().getString().contains("Инвентарь"))
        );
        this.renderables.removeIf(widget -> widget instanceof AbstractWidget);

        // Заново создаём ТОЛЬКО ячейки команд (левые 3 слота)
        UUID playerId = minecraft.player.getUUID();
        Set<String> playerTeamsSet = TeamManager.clientPlayerTeams.getOrDefault(playerId, Set.of());
        List<String> playerTeamList = new ArrayList<>(playerTeamsSet);

        int guiX = (width - 256) / 2;
        int guiY = (height - 170) / 2;
        int[] yPositions = {36, 73, 110};

        for (int slot = 0; slot < 3; slot++) {
            int y = yPositions[slot];

            if (slot < playerTeamList.size()) {
                String teamName = playerTeamList.get(slot);
                TeamManager.Team team = TeamManager.clientTeams.get(teamName);

                int plashkaX = guiX + 10 - 1;
                int plashkaY = guiY + y - 5 - 5 - 1;

                // Плашка
                addRenderableOnly(new AbstractWidget(plashkaX, plashkaY, PLASHKA_W, PLASHKA_H, Component.empty()) {
                    @Override
                    public void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
                        RenderSystem.setShaderTexture(0, ATLAS);
                        g.blit(ATLAS, getX(), getY(), PLASHKA_U, PLASHKA_V, PLASHKA_W, PLASHKA_H, 256, 256);
                        g.blit(ATLAS, getX() + PLASHKA_W - ZVEZDA_W - 4, getY() + 4,
                                ZVEZDA_U, ZVEZDA_V, ZVEZDA_W, ZVEZDA_H, 256, 256);

                        String display = teamName + (team != null && !team.getTag().isEmpty() ? "[" + team.getTag() + "]" : "");
                        int textX = getX() + PLASHKA_W / 2 - font.width(display) / 2;
                        int textY = getY() + (PLASHKA_H - 9) / 2;
                        g.drawString(font, display, textX, textY, 0xFFFFFF, false);
                    }

                    @Override public boolean isMouseOver(double mouseX, double mouseY) { return false; }
                    @Override protected void updateWidgetNarration(net.minecraft.client.gui.narration.NarrationElementOutput output) {}
                });

                // Кнопки "Профиль" и "Покинуть"
                addAtlasButton(guiX + 108 + 5, guiY + y, PROFILE_BTN_W, PROFILE_BTN_H,
                        PROFILE_BTN_U, PROFILE_BTN_V,
                        () -> openTeamProfile(teamName), Component.literal("Профиль"));

                addAtlasButton(guiX + 158 + 3, guiY + y, LEAVE_BTN_W, LEAVE_BTN_H,
                        LEAVE_BTN_U, LEAVE_BTN_V,
                        () -> openLeaveTeam(teamName, team),
                        Component.literal("Покинуть"));
            } else {
                // Пустые слоты — кнопки "Присоединиться" и "Создать"
                addTransparentButton(guiX + 17, guiY + y, 28, 13,
                        this::openJoinList, Component.literal("Присоединиться"));

                addTransparentButton(guiX + 58, guiY + y, 43, 13,
                        this::openCreateTeam, Component.literal("Создать команду"));
            }
        }

        // Обновляем правый список приглашений
        teamList.clear();
        for (TeamManager.Team team : TeamManager.clientTeams.values()) {
            if (!playerTeamsSet.contains(team.getName())) {
                teamList.add(team);
            }
        }
        teamList.sort(Comparator.comparing(TeamManager.Team::getName));
    }

    private void renderTeamList(GuiGraphics g) {
        int baseX = (width - GUI_WIDTH) / 2;
        int baseY = (height - GUI_HEIGHT) / 2;

        int startX = baseX + 111 + 41 + 10 + 20 + 10 + 10 + 10 + 1 + (int)(8 / 0.75f);
        int startY = baseY + 30 - 8 + 7 + 1 + 2; // ← весь список на 2 пикселя ниже

        if (teamList.isEmpty()) {
            Set<String> myTeams = TeamManager.clientPlayerTeams.getOrDefault(minecraft.player.getUUID(), Collections.emptySet());
            teamList.clear();
            for (TeamManager.Team team : TeamManager.clientTeams.values()) {
                teamList.add(team);
            }
            teamList.sort(Comparator.comparing(TeamManager.Team::getName));
        }

        int maxVisible = Math.min(4, teamList.size());

        for (int i = 0; i < maxVisible; i++) {
            int index = i + scrollOffset;
            if (index >= teamList.size()) break;

            TeamManager.Team team = teamList.get(index);
            String tag = team.getTag();
            int invY = startY + i * (INV_H + X_H + 2);
            int underY = invY + INV_H;

            // INV + тег
            addRenderableWidget(new TextureButton(startX, invY, INV_W, INV_H, INV_U, INV_V, PROFILE_ATLAS, btn -> {
                System.out.println("Команда: " + team.getName());
            }) {
                @Override
                protected void renderWidget(GuiGraphics gg, int mx, int my, float pt) {
                    super.renderWidget(gg, mx, my, pt);
                    if (!tag.isEmpty()) {
                        float scale = 0.75f;
                        gg.pose().pushPose();
                        gg.pose().scale(scale, scale, scale);
                        int tx = (int) ((this.getX() + INV_W / 2 + 1.25f) / scale) - font.width(tag) / 2;
                        int ty = (int) ((invY + INV_H / 2 - 2) / scale);
                        gg.drawString(font, tag, tx + 1, ty + 1, 0x000000, false);
                        gg.drawString(font, tag, tx, ty, 0xFFFFFF, false);
                        gg.pose().popPose();
                    }
                }
            });

            // X
            addRenderableWidget(new TextureButton(startX + 5 + 20 - 24 -1 , underY, X_W, X_H, X_U, X_V, PROFILE_ATLAS, btn -> {
                System.out.println("X: " + team.getName());
            }));

            // V
            addRenderableWidget(new TextureButton(startX + 5 + 20 + X_W - 24-1, underY, V_W, V_H, V_U, V_V, PROFILE_ATLAS, btn -> {
                System.out.println("V: " + team.getName());
            }));
        }
    }

    private void renderScroller(GuiGraphics g) {
        int centerX = width / 2;
        int baseY = (height - GUI_HEIGHT) / 2;

        int scrollerX = centerX - SCROLLER_W / 2 + 92;
        int scrollerBaseY = baseY + 80 - 50;

        int offsetY = 0;
        if (teamList.size() > VISIBLE_SLOTS) {
            float ratio = (float) scrollOffset / (teamList.size() - VISIBLE_SLOTS);
            offsetY = (int) (ratio * (SCROLL_TRACK_HEIGHT - SCROLLER_H));
        }

        g.blit(PROFILE_ATLAS, scrollerX, scrollerBaseY + offsetY,
                SCROLLER_U, SCROLLER_V, SCROLLER_W, SCROLLER_H, 256, 256);
    }

    // === СКРОЛЛЕР (всё в одном блоке, без дубликатов) ===

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Проверяем клик по скроллеру
        if (teamList.size() > VISIBLE_SLOTS) {
            int centerX = width / 2;
            int baseY = (height - GUI_HEIGHT) / 2;
            int scrollerX = centerX - SCROLLER_W / 2 + 92;
            int scrollerY = baseY + 80 - 50;
            int trackHeight = SCROLL_TRACK_HEIGHT;

            if (mouseX >= scrollerX && mouseX <= scrollerX + SCROLLER_W &&
                    mouseY >= scrollerY && mouseY <= scrollerY + trackHeight) {
                isDraggingScroller = true;
                updateScrollFromMouse(mouseY);
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (isDraggingScroller) {
            updateScrollFromMouse(mouseY);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        isDraggingScroller = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaY) {
        if (teamList.size() <= VISIBLE_SLOTS) return false;

        int maxScroll = Math.max(0, teamList.size() - VISIBLE_SLOTS);
        scrollOffset -= (int) deltaY;
        scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));

        this.renderables.removeIf(w -> w instanceof TextureButton);
        renderTeamList(null);

        return true;
    }

    private void updateScrollFromMouse(double mouseY) {
        int baseY = (height - GUI_HEIGHT) / 2 + 80 - 50;
        double relativeY = mouseY - baseY;
        relativeY = Math.max(0, Math.min(relativeY, SCROLL_TRACK_HEIGHT - SCROLLER_H));

        float ratio = (float) relativeY / (SCROLL_TRACK_HEIGHT - SCROLLER_H);
        int maxOffset = Math.max(0, teamList.size() - VISIBLE_SLOTS);
        scrollOffset = (int) (ratio * maxOffset);
        scrollOffset = Math.max(0, Math.min(scrollOffset, maxOffset));

        this.renderables.removeIf(w -> w instanceof TextureButton);
        renderTeamList(null);
    }

    private static class TextureButton extends Button {
        private final ResourceLocation atlas;
        private final int u, v;

        public TextureButton(int x, int y, int width, int height, int u, int v,
                             ResourceLocation atlas, OnPress onPress) {
            super(x, y, width, height, Component.empty(), onPress, DEFAULT_NARRATION);
            this.atlas = atlas;
            this.u = u;
            this.v = v;
        }

        @Override
        protected void renderWidget(GuiGraphics g, int mx, int my, float pt) {
            // Фон из атласа
            g.blit(atlas, this.getX(), this.getY(), u, v, this.width, this.height, 256, 256);

            // Подсветка при наведении
            if (this.isHovered()) {
                g.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, 0x30FFFFFF);
            }
        }
    }

    private void onCancel() {
        if (parentScreen != null) {
            // Возвращаемся в тот экран, откуда пришли
            minecraft.setScreen(parentScreen);
        } else {
            minecraft.setScreen(null);
        }
    }
}
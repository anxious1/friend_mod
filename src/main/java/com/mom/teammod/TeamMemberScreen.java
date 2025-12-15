package com.mom.teammod;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TeamMemberScreen extends Screen {

    // АТЛАС для члена команды
    public static final ResourceLocation ATLAS = ResourceLocation.fromNamespaceAndPath(TeamMod.MODID,
            "textures/gui/team_profile_background.png");

    // Координаты из разметки (аналогичные OtherTeamProfileScreen)
    private static final int TAG_U      = 1;   // tag
    private static final int TAG_V      = 207;
    private static final int TAG_W      = 28;
    private static final int TAG_H      = 10;
    private Screen parentScreen;
    private Button leaveButton = null;
    private static final int COMPASS_U  = 1;   // compass
    private static final int COMPASS_V  = 231;
    private static final int COMPASS_W  = 15;
    private static final int COMPASS_H  = 14;

    private static final int FFON_U     = 1;   // ffon
    private static final int FFON_V     = 218;
    private static final int FFON_W     = 12;
    private static final int FFON_H     = 12;

    // Кнопка leave_team (размеры из разметки: 124.49, 138.36, 187.19, 151.57)
    private static final int LEAVE_TEAM_W = 62;  // 187.19 - 124.49 ≈ 63
    private static final int LEAVE_TEAM_H = 12;  // 151.57 - 138.36 ≈ 13

    public static final int GUI_WIDTH  = 256;
    public static final int GUI_HEIGHT = 170;

    // Смещения (такие же как в TeamProfileOwner)
    private static final int OFFSET_X = 240;
    private static final int OFFSET_Y = 141;

    // Ползунок
    private static final int SCROLL_U = 14;
    private static final int SCROLL_V = 218;
    private static final int SCROLL_W = 7;
    private static final int SCROLL_H = 12;

    private static final int ONLINE_U = 1;    // online_player
    private static final int ONLINE_V = 191;
    private static final int ONLINE_W = 78;
    private static final int ONLINE_H = 15;

    private static final int LEADER_U = 1;    // leader_metka
    private static final int LEADER_V = 246;
    private static final int LEADER_W = 32;
    private static final int LEADER_H = 7;

    private final List<Button> playerButtons = new ArrayList<>();
    private int scrollOffset = 0;
    private boolean isDraggingScroller = false;

    // Данные команды
    private final String teamName;
    private final String teamTag;

    private final UUID teamLeader;

    private static final int XP_BAR_U = 0;
    private static final int XP_BAR_V = 170;
    private static final int XP_BAR_W = 83;
    private static final int XP_BAR_H = 5;

    public TeamMemberScreen(Screen parent, String teamName, String teamTag,
                            boolean showTag, boolean showCompass, boolean friendlyFire,
                            UUID teamLeader) {
        super(Component.literal(teamName));
        this.teamName = teamName;
        this.teamTag = teamTag;
        this.teamLeader = teamLeader;
        this.parentScreen = null;
    }

    public int left() { return (width - GUI_WIDTH) / 2; }
    public int top()  { return (height - GUI_HEIGHT) / 2; }

    private int[] getOnlineAndTotalPlayers() {
        TeamManager.Team team = TeamManager.clientTeams.get(teamName);
        if (team == null) return new int[]{0, 0};
        java.util.Set<UUID> members = team.getMembers();
        int online = 0;
        for (UUID id : members) {
            if (minecraft.level.getPlayerByUUID(id) != null) online++;
        }
        return new int[]{online, members.size()};
    }

    @Override
    protected void init() {
        super.init(); // ← ВАЖНО: сначала super.init(), потом кнопки!

        // === УНИВЕРСАЛЬНЫЕ КНОПКИ НАВИГАЦИИ ===
        {
            ResourceLocation unpress = ResourceLocation.fromNamespaceAndPath(TeamMod.MODID, "textures/gui/unpress.png");
            ResourceLocation press   = ResourceLocation.fromNamespaceAndPath(TeamMod.MODID, "textures/gui/press.png");

            ResourceLocation INV_ICON       = ResourceLocation.fromNamespaceAndPath(TeamMod.MODID, "textures/gui/inv_icon.png");
            ResourceLocation TEAM_LIST_ICON = ResourceLocation.fromNamespaceAndPath(TeamMod.MODID, "textures/gui/team_list_icon.png");
            ResourceLocation PROFILE_ICON   = ResourceLocation.fromNamespaceAndPath(TeamMod.MODID, "textures/gui/profile_icon.png");

            int guiX = (width - 256) / 2;
            int baseY = (height - 170) / 2 - 26;

            // === КНОПКА ИНВЕНТАРЬ ===
            int invX = guiX + 2;
            boolean invPressed = false; // В TeamMemberScreen никогда не зажата
            this.addRenderableWidget(new ImageButton(invX, baseY, 26, 27, 0, 0, 0, unpress, button -> {
                minecraft.setScreen(new InventoryScreen(minecraft.player));
            }) {
                private boolean isPressed = false;

                @Override
                public void renderWidget(GuiGraphics g, int mx, int my, float pt) {
                    boolean active = this.isHoveredOrFocused() || isPressed;
                    ResourceLocation tex = active ? press : unpress;
                    int h = active ? 29 : 27;
                    int yOff = active ? -2 : 0;

                    if (this.getHeight() != h) {
                        this.setHeight(h);
                        this.setY(baseY + yOff);
                    }

                    g.blit(tex, getX(), getY(), 0, 0, 26, h, 26, h);
                    g.blit(INV_ICON, getX() + 5, getY() + (active ? 7 : 6), 0, 0, 16, 16, 16, 16);

                    if (this.isHoveredOrFocused()) {
                        g.renderTooltip(font, Component.translatable("gui.teammod.button3"), mx, my);
                    }
                }

                @Override
                public void onClick(double mouseX, double mouseY) {
                    super.onClick(mouseX, mouseY);
                    this.isPressed = true;
                }
            });

            // === КНОПКА КОМАНДЫ (всегда ведёт в TeamScreen) ===
            int teamX = invX + 26 + 52;
            boolean teamPressed = true; // ← В TeamMemberScreen — ты в команде → зажата!
            this.addRenderableWidget(new ImageButton(teamX, baseY - 2, 26, 29, 0, 0, 0, press, button -> {
                // Клик по зажатой — открывает твои команды (TeamScreen)
                minecraft.setScreen(new TeamScreen(
                        TeamMemberScreen.this,
                        new TeamMenu(0, minecraft.player.getInventory()),
                        minecraft.player.getInventory(),
                        Component.translatable("gui.teammod.team_tab")
                ));
            }) {
                @Override
                public void renderWidget(GuiGraphics g, int mx, int my, float pt) {
                    g.blit(press, getX(), getY(), 0, 0, 26, 29, 26, 29);
                    g.blit(TEAM_LIST_ICON, getX() + 5, getY() + 6, 0, 0, 16, 16, 16, 16);

                    if (this.isHoveredOrFocused()) {
                        g.renderTooltip(font, Component.translatable("gui.teammod.team_tab"), mx, my);
                    }
                }
            });

            // === КНОПКА ПРОФИЛЬ ===
            int profileX = teamX + 26;
            boolean profilePressed = false; // В TeamMemberScreen — профиль НЕ зажат
            this.addRenderableWidget(new ImageButton(profileX, baseY, 26, 27, 0, 0, 0, unpress, button -> {
                minecraft.setScreen(new MyProfileScreen(TeamMemberScreen.this, Component.translatable("gui.teammod.profile")));
            }) {
                private boolean isPressed = false;

                @Override
                public void renderWidget(GuiGraphics g, int mx, int my, float pt) {
                    boolean active = this.isHoveredOrFocused() || isPressed;
                    ResourceLocation tex = active ? press : unpress;
                    int h = active ? 29 : 27;
                    int yOff = active ? -2 : 0;

                    if (this.getHeight() != h) {
                        this.setHeight(h);
                        this.setY(baseY + yOff);
                    }

                    g.blit(tex, getX(), getY(), 0, 0, 26, h, 26, h);
                    g.blit(PROFILE_ICON, getX() + 5, getY() + (active ? 7 : 6), 0, 0, 16, 16, 16, 16);

                    if (this.isHoveredOrFocused()) {
                        g.renderTooltip(font, Component.translatable("gui.teammod.profile"), mx, my);
                    }
                }

                @Override
                public void onClick(double mouseX, double mouseY) {
                    super.onClick(mouseX, mouseY);
                    this.isPressed = true;
                }
            });
        }

        // === ОСТАЛЬНОЙ ТВОЙ КОД (ниже) ===
        scrollOffset = 0;

        int guiX = left();
        int guiY = top();

        addLeaveTeamButton(guiX - 4 + (67-7+9) + OFFSET_X/4, guiY - 2 + 105+1 + OFFSET_Y/4, LEAVE_TEAM_W, LEAVE_TEAM_H);
        addEmptyHintArea(guiX + 118 - 72 - 6 - 9 +4 + OFFSET_X/4 - 2, guiY + 90 - 42 - 20 +4 + OFFSET_Y/4 - 1, 9, 9);
        createPlayerButtons(guiX, guiY);
    }

    private void createPlayerButtons(int guiX, int guiY) {
        int baseX = guiX + 10;
        int baseY = guiY + 42;
        int cellX = baseX + 21 - 9;
        int slotHeight = ONLINE_H + 1;

        // === ДИНАМИЧЕСКИЙ СПИСОК УЧАСТНИКОВ ===
        TeamManager.Team team = TeamManager.clientTeams.get(teamName);
        List<UUID> members = new ArrayList<>();
        UUID ownerId = null;
        if (team != null) {
            ownerId = team.getOwner();
            members.addAll(team.getMembers());
        }

        // Владелец первый
        if (ownerId != null && members.remove(ownerId)) {
            members.add(0, ownerId);
        }

        // Очищаем старые кнопки
        playerButtons.forEach(this::removeWidget);
        playerButtons.clear();

        // Создаём кнопки для всех
        for (int i = 0; i < members.size(); i++) {
            UUID playerId = members.get(i);
            Player player = minecraft.level != null ? minecraft.level.getPlayerByUUID(playerId) : null;

            String name = player != null ? player.getName().getString() : "Неизвестно";
            boolean online = player != null;
            boolean isOwner = playerId.equals(ownerId);

            int buttonY = baseY + 20 + 4 + i * slotHeight;

            final String finalName = name;
            final UUID finalPlayerId = playerId;

            Button playerButton = new Button(cellX, buttonY, ONLINE_W, ONLINE_H,
                    Component.empty(), b -> {
                if (finalPlayerId.equals(minecraft.player.getUUID())) {
                    minecraft.setScreen(new MyProfileScreen(TeamMemberScreen.this, Component.translatable("gui.teammod.profile")));
                } else {
                    minecraft.setScreen(new OtherPlayerProfileScreen(finalPlayerId, TeamMemberScreen.this,
                            Component.literal("Профиль " + finalName)));
                }
            }, s -> Component.empty()) {
                @Override
                public void renderWidget(GuiGraphics g, int mx, int my, float pt) {
                    if (!this.visible) return;

                    int bgV = online ? ONLINE_V : 175;
                    g.blit(ATLAS, getX(), getY(), ONLINE_U, bgV, ONLINE_W, ONLINE_H, 256, 256);

                    ResourceLocation skin = minecraft.getSkinManager().getInsecureSkinLocation(
                            player != null ? player.getGameProfile() : minecraft.player.getGameProfile());

                    int headX = getX() + 3;
                    int headY = getY() + (ONLINE_H - 8) / 2;
                    g.blit(skin, headX, headY, 8, 8, 8, 8, 8, 8, 64, 64);
                    RenderSystem.enableBlend();
                    g.blit(skin, headX, headY, 8, 8, 40, 8, 8, 8, 64, 64);
                    RenderSystem.disableBlend();

                    TeamManager.Team currentTeam = TeamManager.clientTeams.get(teamName);
                    String tagPart = (currentTeam != null && currentTeam.showTag() && currentTeam.getTag() != null && !currentTeam.getTag().isEmpty())
                            ? "[" + currentTeam.getTag() + "]"
                            : "";
                    String fullText = finalName + tagPart;
                    if (font.width(fullText) > ONLINE_W - 22) {
                        fullText = font.plainSubstrByWidth(fullText, ONLINE_W - 25) + "..";
                    }
                    g.drawString(font, fullText, getX() + 14, getY() + 4, 0xFFFFFF, false);

                    if (isOwner) {
                        g.blit(ATLAS, getX() + ONLINE_W - LEADER_W - 2, getY() + ONLINE_H - LEADER_H - 1,
                                LEADER_U, LEADER_V, LEADER_W, LEADER_H, 256, 256);
                    }

                    if (isHovered()) {
                        g.fill(getX(), getY(), getX() + width, getY() + height, 0x30FFFFFF);
                    }
                }
            };

            playerButtons.add(playerButton);
            addRenderableWidget(playerButton);
        }

        updateVisibleButtons();
    }

    private void onPlayerClicked(String playerName) {
        System.out.println("Клик по игроку в своей команде: " + playerName);
    }

    private Button addTransparentButton(int x, int y, int w, int h, Runnable action, Component tooltip) {
        Button btn = new Button(x, y, w, h, Component.empty(), b -> action.run(), s -> Component.empty()) {
            @Override
            public void renderWidget(GuiGraphics g, int mx, int my, float pt) {
                if (isHovered()) {
                    g.fill(getX(), getY(), getX() + width, getY() + height, 0x30FFFFFF);
                }
            }
        };
        btn.setTooltip(Tooltip.create(tooltip));
        return addRenderableWidget(btn);
    }

    private void addLeaveTeamButton(int x, int y, int w, int h) {
        if (leaveButton != null) {
            removeWidget(leaveButton);
        }

        leaveButton = new Button(x, y, w, h, Component.empty(), b -> {
            // Открываем окно подтверждения выхода — функционал сохранён полностью
            minecraft.setScreen(new LeaveTeamScreen(
                    TeamMemberScreen.this,
                    teamName,
                    teamTag
            ));
        }, s -> Component.empty()) {
            {
                setTooltip(Tooltip.create(Component.literal("Покинуть команду")));
            }

            @Override
            public void renderWidget(GuiGraphics g, int mx, int my, float pt) {
                if (isHovered()) {
                    g.fill(getX(), getY(), getX() + width, getY() + height, 0x30FFFFFF);
                }
            }
        };

        addRenderableWidget(leaveButton);
    }

    private void addEmptyHintArea(int x, int y, int w, int h) {
        addRenderableWidget(new Button(x, y, w, h, Component.empty(), b -> {
            // Пустая область - ничего не делает при клике
        }, s -> Component.empty()) {
            {
                setTooltip(Tooltip.create(Component.literal(
                        "Доступ в команду:\n§aВкл§r — только по приглашению\n§cВыкл§r — свободный доступ")));
            }

            @Override
            public void renderWidget(GuiGraphics g, int mx, int my, float pt) {
                // Не рисуем никакой текстуры - просто пустая область
                if (isHovered()) {
                    g.fill(getX(), getY(), getX() + width, getY() + height, 0x30FFFFFF);
                }
            }
        });
    }

    public void renderElementsWithoutButtons(GuiGraphics g) {
        // Рисуем ТОЛЬКО иконки и элементы (без текста и кнопок)
        int guiX = left();
        int guiY = top();

        // На этот:
        TeamManager.Team actualTeam = TeamManager.clientTeams.get(teamName);
        if (actualTeam != null) {
            if (actualTeam.showTag()) {
                g.blit(ATLAS, guiX + 118 - 14 + OFFSET_X/4 - 2, guiY + 34 + OFFSET_Y/4 - 1, TAG_U, TAG_V, TAG_W, TAG_H, 256, 256);
            }
            if (actualTeam.showCompass()) {
                g.blit(ATLAS, guiX + 118 - 7 + OFFSET_X/4 - 2, guiY + 51 + OFFSET_Y/4 - 1, COMPASS_U, COMPASS_V, COMPASS_W, COMPASS_H, 256, 256);
            }
            if (actualTeam.isFriendlyFire()) {
                g.blit(ATLAS, guiX + 118 - 6 + OFFSET_X/4 - 2, guiY + 72 + OFFSET_Y/4 - 1, FFON_U, FFON_V, FFON_W, FFON_H, 256, 256);
            }
        }

        // ПОЛЗУНОК
        // ── ПОЛЗУНОК (только если участников больше 3) ─────────────────────────────
        int totalPlayers = playerButtons.size();
        if (totalPlayers > 3) {
            int baseX = guiX + 10;
            int baseY = guiY + 42;

            int visibleHeight = 3 * (ONLINE_H + 1); // высота видимой области (3 слота)
            int maxScroll = totalPlayers - 3;
            int scrollerOffset = maxScroll == 0 ? 0 :
                    (int)((float)scrollOffset / maxScroll * (visibleHeight - SCROLL_H));

            g.blit(ATLAS, baseX + 13 - 8, baseY + 5 + 20 + 4 + 10 + scrollerOffset,
                    SCROLL_U, SCROLL_V, SCROLL_W, SCROLL_H, 256, 256);
        }
        // XP БАР
        int xpBarX = guiX + 10 + 21 - 9 - 7;
        int xpBarY = guiY + 42 + 20 + 4 + 15 + (3 * (ONLINE_H + 1)) + 5 + 13;
        g.blit(ATLAS, xpBarX, xpBarY, XP_BAR_U, XP_BAR_V, XP_BAR_W, XP_BAR_H, 256, 256);
    }
    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        // 1. СТАНДАРТНОЕ ЗАТЕМНЕНИЕ (как в StatisticsScreen)
        this.renderBackground(g);

        // 2. РИСУЕМ НАШЕ ОКНО
        RenderSystem.setShaderTexture(0, ATLAS);
        int guiX = left();
        int guiY = top();
        g.blit(ATLAS, guiX, guiY, 0, 0, GUI_WIDTH, GUI_HEIGHT, 256, 256);

        // 3. ТЕКСТ КОМАНДЫ
        g.drawCenteredString(font, teamName, guiX + 19 + OFFSET_X/4 - 2, guiY + OFFSET_Y/4 - 2, 0xFFFFFF);

        // 4. ТЕГ КОМАНДЫ — актуальные данные
        TeamManager.Team currentTeam = TeamManager.clientTeams.get(teamName);
        if (currentTeam != null && currentTeam.showTag() && !currentTeam.getTag().isEmpty()) {
            g.drawCenteredString(font, currentTeam.getTag(), guiX + 19 + OFFSET_X/4 - 2, guiY + 26 + OFFSET_Y/4 - 15, 0xFFFFFF);
        }

        // 5. ОНЛАЙН/ВСЕГО
        int[] stats = getOnlineAndTotalPlayers();
        g.drawCenteredString(font, stats[0] + "/" + stats[1], guiX + 118 + OFFSET_X/4 - 2, guiY + 13 + OFFSET_Y/4 + 2, 0xFFFFFF);

        // 6. ИКОНКИ НАСТРОЕК — берём актуальные данные
        TeamManager.Team actualTeam = TeamManager.clientTeams.get(teamName);
        if (actualTeam != null) {
            if (actualTeam.showTag()) {
                g.blit(ATLAS, guiX + 118 - 14 + OFFSET_X/4 - 2, guiY + 34 + OFFSET_Y/4 - 1,
                        TAG_U, TAG_V, TAG_W, TAG_H, 256, 256);
            }
            if (actualTeam.showCompass()) {
                g.blit(ATLAS, guiX + 118 - 7 + OFFSET_X/4 - 2, guiY + 51 + OFFSET_Y/4 - 1,
                        COMPASS_U, COMPASS_V, COMPASS_W, COMPASS_H, 256, 256);
            }
            if (actualTeam.isFriendlyFire()) {
                g.blit(ATLAS, guiX + 118 - 6 + OFFSET_X/4 - 2, guiY + 72 + OFFSET_Y/4 - 1,
                        FFON_U, FFON_V, FFON_W, FFON_H, 256, 256);
            }
        }

        // 7. ПОЛЗУНОК
        int baseX = guiX + 10;
        int baseY = guiY + 42;
        int trackHeight = 46;
        int maxScroll = Math.max(0, 9 - 3);
        int scrollerOffset = maxScroll == 0 ? 0 :
                (int)((float)scrollOffset / maxScroll * (trackHeight - SCROLL_H));
        g.blit(ATLAS, baseX + 13 - 8, baseY + 5 + 20 + 4 + 10 + scrollerOffset,
                SCROLL_U, SCROLL_V, SCROLL_W, SCROLL_H, 256, 256);

        // 8. XP БАР
        int xpBarX = guiX + 10 + 21 - 9 - 7;
        int xpBarY = guiY + 42 + 20 + 4 + 15 + (3 * (ONLINE_H + 1)) + 5 + 13;
        g.blit(ATLAS, xpBarX, xpBarY, XP_BAR_U, XP_BAR_V, XP_BAR_W, XP_BAR_H, 256, 256);

        // 9. КНОПКИ
        super.render(g, mouseX, mouseY, partialTick);
    }

    protected void renderBg(GuiGraphics g, float pt, int mx, int my) {
        // Не рисуем родительский экран здесь
        // Просто рисуем фон TeamMemberScreen
        RenderSystem.setShaderTexture(0, ATLAS);
        int guiX = left();
        int guiY = top();
        g.blit(ATLAS, guiX, guiY, 0, 0, GUI_WIDTH, GUI_HEIGHT, 256, 256);
    }


    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaY) {
        int total = playerButtons.size();
        if (total <= 3) return false; // если ≤3 — скролл не нужен

        int maxScroll = total - 3; // сколько можно проскроллить
        scrollOffset -= (int) deltaY;
        scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));
        updateVisibleButtons();
        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int totalPlayers = playerButtons.size();
        if (totalPlayers <= 3) return super.mouseClicked(mouseX, mouseY, button);

        int trackX = left() + 21 - 9 - 9;
        int trackY = top() + 20 + 4 + 15 + 18;
        int trackHeight = 3 * (ONLINE_H + 1); // высота трека под 3 видимых слота

        if (mouseX >= trackX && mouseX <= trackX + 7 && mouseY >= trackY && mouseY <= trackY + trackHeight) {
            isDraggingScroller = true;
            updateScrollFromMouse(mouseY);
            return true;
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

    private void updateScrollFromMouse(double mouseY) {
        int trackY = top() + 20 + 4 + 15 + 18;
        int trackHeight = 3 * (ONLINE_H + 1);

        double rel = mouseY - trackY;
        rel = Math.max(0, Math.min(rel, trackHeight - SCROLL_H));

        float ratio = (float) rel / (trackHeight - SCROLL_H);
        int maxScroll = Math.max(0, playerButtons.size() - 3);
        scrollOffset = Math.round(ratio * maxScroll);
        scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));

        updateVisibleButtons();
    }

    private void updateVisibleButtons() {
        if (playerButtons.isEmpty()) return;

        int visibleSlots = 3; // сколько строк видно одновременно в текстуре — это фиксировано!
        int slotHeight = ONLINE_H + 1; // 15 + 1 = 16 пикселей на слот
        int startY = top() + 42 + 20 + 4; // начальная Y первого видимого слота

        for (int i = 0; i < playerButtons.size(); i++) {
            Button button = playerButtons.get(i);
            int visibleIndex = i - scrollOffset;

            if (visibleIndex >= 0 && visibleIndex < visibleSlots) {
                button.setY(startY + visibleIndex * slotHeight);
                button.visible = true;
            } else {
                button.visible = false;
            }
        }
    }

    @Override
    public void onClose() {
        // Просто закрываем экран, не возвращаемся к родителю
        minecraft.setScreen(null);
    }

    public void refreshFromSync() {
        int guiX = left();
        int guiY = top();

        // Перестраиваем кнопку выхода
        addLeaveTeamButton(guiX - 4 + (67-7+9) + OFFSET_X/4, guiY - 2 + 105+1 + OFFSET_Y/4, LEAVE_TEAM_W, LEAVE_TEAM_H);

        // Перестраиваем список участников
        createPlayerButtons(guiX, guiY);

        scrollOffset = 0;
        updateVisibleButtons();
    }
}
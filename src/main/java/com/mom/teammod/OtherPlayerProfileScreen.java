package com.mom.teammod;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import com.mom.teammod.MyTeamsListScreen;

import java.util.*;

public class OtherPlayerProfileScreen extends Screen {

    private static final ResourceLocation ATLAS = ResourceLocation.fromNamespaceAndPath(TeamMod.MODID,
            "textures/gui/user_profile_background.png");

    // Размеры кнопки invite_off из разметки XML
    private static final int INVITE_OFF_W = 107;  // xbr(137.26) - xtl(30.76) = 106.5 ≈ 107
    private static final int INVITE_OFF_H = 15;   // ybr(185.31) - ytl(170.71) = 14.6 ≈ 15
    private static final int INVITE_OFF_U = (int)30.76;   // xtl
    private static final int INVITE_OFF_V = (int)170.71;  // ytl

    private static final int ACCEPT_JOIN_W = 114;  // xbr(244.36) - xtl(130.56) = 113.8 ≈ 114
    private static final int ACCEPT_JOIN_H = 16;   // ybr(200.91) - ytl(185.41) = 15.5 ≈ 16
    private static final int ACCEPT_JOIN_U = (int)130.56;  // xtl
    private static final int ACCEPT_JOIN_V = (int)185.41;  // ytl

    private enum InviteButtonState {
        NONE,        // нет кнопки
        INVITE_OFF,  // текстура invite_off
        ACCEPT_JOIN  // текстура accept_join
    }

    // Остальные UV-координаты как в MyProfileScreen
    private int lastRenderedScrollOffset = -1;
    private static final int INV_U       = 1,   INV_V       = 171, INV_W   = 23, INV_H   = 16;
    private static final int SCROLLER_U  = 25,  SCROLLER_V  = 171, SCROLLER_W = 6,  SCROLLER_H = 25;
    private static final int BAR_U       = 31,  BAR_V       = 186, BAR_W   = 81, BAR_H   = 5;
    private static final int X_U         = 1,   X_V         = 187, X_W     = 12, X_H     = 11;
    private static final int V_U         = 13,  V_V         = 187, V_W     = 11, V_H     = 11;
    private static final int HUMAN_U     = 81,  HUMAN_V     = 199, HUMAN_W = 9,  HUMAN_H = 10;

    // Статусы
    private static final int ONLINE_U = 31,  ONLINE_V = 192;
    private static final int AFK_U    = 36,  AFK_V    = 192;
    private static final int OFFLINE_U = 41, OFFLINE_V = 192; // НОВОЕ: оффлайн иконка

    private static final int GUI_WIDTH  = 256;
    private static final int GUI_HEIGHT = 169;

    private static final int SKIN_FRAME_WIDTH  = 87;
    private static final int SKIN_FRAME_HEIGHT = 100;
    private static final int FRAME_MARGIN_LEFT = 15;

    private static final int VISIBLE_SLOTS = 4;
    private static final int SLOT_HEIGHT = INV_H + X_H + 4;
    private final List<TeamManager.Team> teamList = new ArrayList<>();
    private int scrollOffset = 0;

    private boolean isDraggingScroller = false;
    private static final int SCROLL_TRACK_HEIGHT = 119;
    private static final int SCROLLER_X_OFFSET = 111 + 41 + 10 + 20 + 10 + 10 + 10 + 1 + (int)(8 / 0.75f) + 23 + 5;
    private static final int SCROLLER_Y_START = 30 - 8 + 7 + 1;



    // Данные профиля
    private final UUID targetPlayerId;
    private String playerName = "Unknown";
    private OtherPlayerState.PlayerStatus playerStatus;

    private final Screen parentScreen;

    public OtherPlayerProfileScreen(UUID targetPlayerId, Screen parentScreen, Component title) {
        super(title);
        this.targetPlayerId = targetPlayerId;
        this.parentScreen = parentScreen;

        // Получаем тестовые данные
        this.playerStatus = OtherPlayerState.getStatus(targetPlayerId);
        this.playerName = playerStatus.name; // Всегда используем тестовое имя
    }

    private InviteButtonState getInviteButtonState() {
        // Привязываем состояния к UUID игроков
        if (targetPlayerId.equals(UUID.fromString("deadbeef-dead-beef-dead-beefdeadbeef"))) {
            return InviteButtonState.NONE;           // BirdMan - нет текстуры, прозрачная кнопка (1)
        } else if (targetPlayerId.equals(UUID.fromString("11111111-1111-1111-1111-111111111111"))) {
            return InviteButtonState.INVITE_OFF;     // BridMan - текстура invite_off, НЕТ кнопки (2)
        } else if (targetPlayerId.equals(UUID.fromString("22222222-2222-2222-2222-222222222222"))) {
            return InviteButtonState.ACCEPT_JOIN;    // Berdamel - текстура accept_join, ЕСТЬ кнопка (3)
        } else if (targetPlayerId.equals(UUID.fromString("33333333-3333-3333-3333-333333333333"))) {
            return InviteButtonState.NONE;           // HerobRine - нет текстуры, прозрачная кнопка (1)
        } else if (targetPlayerId.equals(UUID.fromString("44444444-4444-4444-4444-444444444444"))) {
            return InviteButtonState.INVITE_OFF;     // TOPSON - текстура invite_off, НЕТ кнопки (2)
        }
        return InviteButtonState.NONE;
    }

    @Override
    protected void init() {
        super.init();
        scrollOffset = 0;
        lastRenderedScrollOffset = -1;

        int guiX = (width - GUI_WIDTH) / 2;
        int guiY = (height - GUI_HEIGHT) / 2;

        InviteButtonState buttonState = getInviteButtonState();

        // В методе init() класса OtherPlayerProfileScreen заменить:
        if (buttonState == InviteButtonState.NONE) {
            // Прозрачная кнопка на позиции invite_off
            int buttonX = guiX + (GUI_WIDTH - INVITE_OFF_W) / 2 - 23;
            int buttonY = guiY + GUI_HEIGHT - INVITE_OFF_H - 16;

            addTransparentButton(buttonX, buttonY, INVITE_OFF_W, INVITE_OFF_H,
                    this::openMyTeamsList, // ← ИЗМЕНИТЬ
                    Component.literal("Пригласить в команду")); // ← ИЗМЕНИТЬ
        }
        // Состояние 2: текстура invite_off, НЕТ кнопки - ничего не создаем
        // Состояние 3: текстура accept_join, ЕСТЬ кнопка с текстурой
        else if (buttonState == InviteButtonState.ACCEPT_JOIN) {
            int acceptX = guiX + (GUI_WIDTH - ACCEPT_JOIN_W) / 2 - 23;
            int acceptY = guiY + GUI_HEIGHT - ACCEPT_JOIN_H - 26;

            addRenderableWidget(new TextureButton(acceptX, acceptY, ACCEPT_JOIN_W, ACCEPT_JOIN_H,
                    ACCEPT_JOIN_U, ACCEPT_JOIN_V, ATLAS, btn -> onAcceptJoinClicked()) {
                @Override
                public void renderWidget(GuiGraphics g, int mx, int my, float pt) {
                    super.renderWidget(g, mx, my, pt);
                    if (this.isHovered()) {
                        g.fill(getX(), getY(), getX() + width, getY() + height, 0x30FFFFFF);
                    }
                }
            });
        }

        // Кнопка "Подробная статистика"
        addTransparentButton(guiX + 110, guiY + 114, 86, 14,
                this::openDetailedStats,
                Component.literal("Подробная статистика"));

        renderTeamList(null);
    }

    private void onAcceptJoinClicked() {
        System.out.println("Принять заявку clicked for " + playerName);
        // TODO: Отправить пакет на сервер для принятия заявки
    }

    private void renderButtonTexture(GuiGraphics g) {
        InviteButtonState buttonState = getInviteButtonState();
        int guiX = (width - GUI_WIDTH) / 2;
        int guiY = (height - GUI_HEIGHT) / 2;

        // Состояние 2: текстура invite_off, НЕТ кнопки
        if (buttonState == InviteButtonState.INVITE_OFF) {
            int inviteX = guiX + (GUI_WIDTH - INVITE_OFF_W) / 2 - 23;
            int inviteY = guiY + GUI_HEIGHT - INVITE_OFF_H - 16;
            g.blit(ATLAS, inviteX, inviteY, INVITE_OFF_U, INVITE_OFF_V,
                    INVITE_OFF_W, INVITE_OFF_H, 256, 256);
        }
        // Состояние 1: ничего не рисуем (прозрачная кнопка рисуется через виджет)
        // Состояние 3: кнопка с текстурой рисуется через виджет TextureButton
    }

    private void onInviteOffClicked() {
        System.out.println("Отклонить приглашение clicked for " + playerName);
        // TODO: Отправить пакет на сервер для отклонения приглашения
    }

    private void openDetailedStats() {
        ClientState.hidePlayerRender = true;

        StatisticsScreen statsScreen = new StatisticsScreen(Component.literal("Подробная статистика")) {
            @Override
            public void render(GuiGraphics g, int mx, int my, float pt) {
                // 1. Рисуем затемнение ВСЕГО ЭКРАНА
                g.fill(0, 0, width, height, 0xB3000000); // ← ЗДЕСЬ!

                // 2. Рисуем текстуру фона GUI поверх затемнения
                RenderSystem.setShaderTexture(0, ATLAS);
                int guiX = (width - GUI_WIDTH) / 2;
                int guiY = (height - GUI_HEIGHT) / 2;
                g.blit(ATLAS, guiX, guiY, 0, 0, GUI_WIDTH, GUI_HEIGHT, 256, 256);

                // 3. Рисуем текстуры кнопок (если есть)
                InviteButtonState buttonState = getInviteButtonState();
                if (buttonState == InviteButtonState.INVITE_OFF) {
                    int inviteX = guiX + (GUI_WIDTH - INVITE_OFF_W) / 2 - 23;
                    int inviteY = guiY + GUI_HEIGHT - INVITE_OFF_H - 16;
                    g.blit(ATLAS, inviteX, inviteY, INVITE_OFF_U, INVITE_OFF_V,
                            INVITE_OFF_W, INVITE_OFF_H, 256, 256);
                }
                else if (buttonState == InviteButtonState.ACCEPT_JOIN) {
                    int acceptX = guiX + (GUI_WIDTH - ACCEPT_JOIN_W) / 2 - 23;
                    int acceptY = guiY + GUI_HEIGHT - ACCEPT_JOIN_H - 26;
                    g.blit(ATLAS, acceptX, acceptY, ACCEPT_JOIN_U, ACCEPT_JOIN_V,
                            ACCEPT_JOIN_W, ACCEPT_JOIN_H, 256, 256);
                }

                // 4. Теперь рисуем окно статистики поверх всего
                super.render(g, mx, my, pt); // ← Оно рисует СВОЕ окно БЕЗ затемнения
            }

            @Override
            public void onClose() {
                ClientState.hidePlayerRender = false;
                minecraft.setScreen(OtherPlayerProfileScreen.this);
            }
        };

        statsScreen.init(minecraft, width, height);
        minecraft.setScreen(statsScreen);
    }

    private void openMyTeamsList() {
        MyTeamsListScreen teamsListScreen = new MyTeamsListScreen(this) {
            @Override
            public void render(GuiGraphics g, int mx, int my, float pt) {
                // 1. Рисуем затемнение ВСЕГО ЭКРАНА
                g.fill(0, 0, width, height, 0xB3000000);

                // 2. Рисуем текстуру фона GUI поверх затемнения
                RenderSystem.setShaderTexture(0, ATLAS);
                int guiX = (width - GUI_WIDTH) / 2;
                int guiY = (height - GUI_HEIGHT) / 2;
                g.blit(ATLAS, guiX, guiY, 0, 0, GUI_WIDTH, GUI_HEIGHT, 256, 256);

                // 3. Рисуем текстуры кнопок (если есть)
                InviteButtonState buttonState = getInviteButtonState();
                if (buttonState == InviteButtonState.INVITE_OFF) {
                    int inviteX = guiX + (GUI_WIDTH - INVITE_OFF_W) / 2 - 23;
                    int inviteY = guiY + GUI_HEIGHT - INVITE_OFF_H - 16;
                    g.blit(ATLAS, inviteX, inviteY, INVITE_OFF_U, INVITE_OFF_V,
                            INVITE_OFF_W, INVITE_OFF_H, 256, 256);
                }
                else if (buttonState == InviteButtonState.ACCEPT_JOIN) {
                    int acceptX = guiX + (GUI_WIDTH - ACCEPT_JOIN_W) / 2 - 23;
                    int acceptY = guiY + GUI_HEIGHT - ACCEPT_JOIN_H - 26;
                    g.blit(ATLAS, acceptX, acceptY, ACCEPT_JOIN_U, ACCEPT_JOIN_V,
                            ACCEPT_JOIN_W, ACCEPT_JOIN_H, 256, 256);
                }

                // 4. Рисуем окно со списком команд поверх всего
                super.render(g, mx, my, pt);
            }
        };

        teamsListScreen.init(minecraft, width, height);
        minecraft.setScreen(teamsListScreen);
    }

    private Button addTransparentButton(int x, int y, int w, int h, Runnable action, Component tooltip) {
        Button btn = new Button(x, y, w, h, Component.empty(), b -> action.run(), s -> Component.empty()) {
            @Override
            public void renderWidget(GuiGraphics g, int mx, int my, float pt) {
                // НЕ рисуем текстуру! Только подсветку при наведении
                if (this.isHovered()) {
                    g.fill(getX(), getY(), getX() + width, getY() + height, 0x30FFFFFF);
                }
            }
        };
        btn.setTooltip(Tooltip.create(tooltip));
        return addRenderableWidget(btn);
    }

    private void renderAllElements(GuiGraphics g, int mx, int my) {
        renderSkin(g, mx, my);
        renderStatusIcon(g);
        renderNickname(g);
        renderHumanIconAndText(g);
        renderCenterBarsAndScroller(g);
        renderButtonTexture(g); // ← Добавить эту строку
    }

    private void renderStatusIcon(GuiGraphics g) {
        int u;
        if (!playerStatus.online) {
            u = OFFLINE_U; // Оффлайн
        } else if (playerStatus.afk) {
            u = AFK_U; // AFK
        } else {
            u = ONLINE_U; // Онлайн
        }

        int baseX = (width - GUI_WIDTH) / 2;
        int baseY = (height - GUI_HEIGHT) / 2;
        int drawX = baseX + 88 - 2 + 1 - 2;
        int drawY = baseY + 38 - 3 + 5 - 2;
        g.blit(ATLAS, drawX, drawY, u, ONLINE_V, 5, 5, 256, 256);
    }

    private void renderSkin(GuiGraphics g, int mx, int my) {
        if (ClientState.hidePlayerRender) {
            return;
        }

        int frameX = (width - GUI_WIDTH) / 2 + FRAME_MARGIN_LEFT;
        int frameY = (height - GUI_HEIGHT) / 2 + (GUI_HEIGHT - SKIN_FRAME_HEIGHT) / 2 - 13;
        int playerX = frameX + SKIN_FRAME_WIDTH / 2;
        int playerY = frameY + SKIN_FRAME_HEIGHT - 10;

        // Всегда рисуем скин текущего игрока (minecraft.player) для наглядности
        if (minecraft.player != null) {
            InventoryScreen.renderEntityInInventoryFollowsMouse(
                    g, playerX, playerY, 38,
                    (float) playerX - mx,
                    (float) (frameY + SKIN_FRAME_HEIGHT / 2) - my,
                    minecraft.player);
        }
    }

    private void renderNickname(GuiGraphics g) {
        drawScaledCenteredString(g, playerName, 0.75f,
                (height - GUI_HEIGHT) / 2 + 30 + 2, 0xFFFFFF);
    }

    private void drawScaledCenteredString(GuiGraphics g, String text, float scale, int y, int color) {
        int x = getPlayerNicknameX(text);
        g.pose().pushPose();
        g.pose().scale(scale, scale, scale);
        g.drawString(font, text, (int)(x / scale), (int)(y / scale), color, false);
        g.pose().popPose();
    }

    private int getHumanTextX(String text) {
        int guiLeft = (width - GUI_WIDTH) / 2;
        float zoneLeft = 111.90f;
        float zoneRight = 195.50f;
        float zoneWidth = zoneRight - zoneLeft;
        int textWidth = font.width(text);
        float offsetX = (zoneWidth - textWidth) / 2.0f;
        return (int) (guiLeft + zoneLeft + offsetX);
    }

    private int getPlayerNicknameX(String displayName) {
        int guiLeft = (width - GUI_WIDTH) / 2;
        float zoneLeft = 111.90f;
        float zoneRight = 195.50f;
        float zoneWidth = zoneRight - zoneLeft;
        float scale = 0.75f;
        int scaledTextWidth = (int) (font.width(displayName) * scale);
        float offsetX = (zoneWidth - scaledTextWidth) / 2.0f;
        return (int) (guiLeft + zoneLeft + offsetX);
    }

    private void renderHumanIconAndText(GuiGraphics g) {
        int baseX = (width - GUI_WIDTH) / 2;
        int baseY = (height - GUI_HEIGHT) / 2;

        // Иконка Human
        g.blit(ATLAS, baseX + 111 + 41 - 74 - 20 - 20 - 18, baseY + 96 + 123 - 194 + 5 + 5,
                HUMAN_U, HUMAN_V, HUMAN_W, HUMAN_H, 256, 256);

        int textX = getHumanTextX("Human");
        int textY = baseY + 96 + 2;
        g.drawString(font, "Human", textX, textY, 0xFFFFFF, false);
    }

    // Остальные методы копируются из MyProfileScreen без изменений

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
            g.blit(atlas, this.getX(), this.getY(), u, v, this.width, this.height, 256, 256);
            if (this.isHovered()) {
                g.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, 0x30FFFFFF);
            }
        }
    }

    private void renderTeamList(GuiGraphics g) {
        int baseX = (width - GUI_WIDTH) / 2;
        int baseY = (height - GUI_HEIGHT) / 2;

        int startX = baseX + 111 + 41 + 10 + 20 + 10 + 10 + 10 + 1 + (int)(8 / 0.75f);
        int startY = baseY + 30 - 8 + 7 + 1 + 2;

        // ВСЁ, ЧТО НИЖЕ - ПОЛНАЯ КОПИЯ ИЗ MyProfileScreen
        // Убираем старую логику и берем список команд текущего игрока

        if (teamList.isEmpty()) {
            // Берем команды ТЕКУЩЕГО игрока (minecraft.player), а не targetPlayerId!
            Set<String> myTeams = TeamManager.clientPlayerTeams.getOrDefault(
                    minecraft.player.getUUID(), Collections.emptySet());
            teamList.clear();
            for (TeamManager.Team team : TeamManager.clientTeams.values()) {
                if (myTeams.contains(team.getName())) {
                    teamList.add(team);
                }
            }
            teamList.sort(Comparator.comparing(TeamManager.Team::getName));
        }

        int currentY = startY;
        int maxVisible = Math.min(VISIBLE_SLOTS, teamList.size());

        for (int i = 0; i < maxVisible; i++) {
            int index = i + scrollOffset;
            if (index >= teamList.size()) break;

            TeamManager.Team team = teamList.get(index);
            String tag = team.getTag();
            int invY = currentY + i * (INV_H + X_H + 2);
            int underY = invY + INV_H;

            // INV + тег
            addRenderableWidget(new TextureButton(startX, invY, INV_W, INV_H, INV_U, INV_V, ATLAS, btn -> {
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
            addRenderableWidget(new TextureButton(startX + 5 + 20 - 24 -1 , underY, X_W, X_H, X_U, X_V, ATLAS, btn -> {
                System.out.println("X: " + team.getName());
            }));

            // V
            addRenderableWidget(new TextureButton(startX + 5 + 20 + X_W - 24-1, underY, V_W, V_H, V_U, V_V, ATLAS, btn -> {
                System.out.println("V: " + team.getName());
            }));
        }
    }

    private void renderCenterBarsAndScroller(GuiGraphics g) {
        renderProgressBar1(g);
        renderProgressBar2(g);
        renderScroller(g);
    }

    private void renderProgressBar1(GuiGraphics g) {
        int centerX = width / 2;
        int baseY = (height - GUI_HEIGHT) / 2;
        int x = centerX - BAR_W / 2 + 24;
        int y = baseY + 110 - 34;
        int fillPercent = 25;
        int fillWidth = (int) (BAR_W * fillPercent / 100.0);
        g.blit(ATLAS, x, y, BAR_U, BAR_V, fillWidth, BAR_H, 256, 256);
    }

    private void renderProgressBar2(GuiGraphics g) {
        int centerX = width / 2;
        int baseY = (height - GUI_HEIGHT) / 2;
        int x = centerX - BAR_W / 2 + 24;
        int y = baseY + 118 - 65;
        int fillPercent = 75;
        int fillWidth = (int) (BAR_W * fillPercent / 100.0);
        g.blit(ATLAS, x, y, BAR_U, BAR_V, fillWidth, BAR_H, 256, 256);
    }

    private void renderScroller(GuiGraphics g) {
        int centerX = width / 2;
        int baseY = (height - GUI_HEIGHT) / 2;
        int scrollerX = centerX - SCROLLER_W / 2 + 92;
        int scrollerBaseY = baseY + 80 - 50;
        int offsetY = 0;
        if (teamList.size() > VISIBLE_SLOTS) {
            float ratio = (float) scrollOffset / (teamList.size() - VISIBLE_SLOTS);
            offsetY = (int) (ratio * (119 - SCROLLER_H));
        }
        g.blit(ATLAS, scrollerX, scrollerBaseY + offsetY,
                SCROLLER_U, SCROLLER_V, SCROLLER_W, SCROLLER_H, 256, 256);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        // Стандартный рендер фона (как в MyProfileScreen)
        renderBackground(g);

        // Текстура GUI и все элементы
        renderBg(g, pt, mx, my);
        renderAllElements(g, mx, my);
        super.render(g, mx, my, pt);
    }

    protected void renderBg(GuiGraphics g, float pt, int mx, int my) {
        RenderSystem.setShaderTexture(0, ATLAS);
        int x = (width - GUI_WIDTH) / 2;
        int y = (height - GUI_HEIGHT) / 2;
        g.blit(ATLAS, x, y, 0, 0, GUI_WIDTH, GUI_HEIGHT, 256, 256);
    }

    // Методы скроллинга (аналогично MyProfileScreen)
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (teamList.size() > VISIBLE_SLOTS) {
            int scrollerX = width / 2 - SCROLLER_W / 2 + 92;
            int scrollerY = (height - GUI_HEIGHT) / 2 + 80 - 50;
            int trackHeight = 119;

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
        relativeY = Math.max(0, Math.min(relativeY, 119 - SCROLLER_H));

        float ratio = (float) relativeY / (119 - SCROLLER_H);
        int maxOffset = Math.max(0, teamList.size() - VISIBLE_SLOTS);
        scrollOffset = (int) (ratio * maxOffset);
        scrollOffset = Math.max(0, Math.min(scrollOffset, maxOffset));

        this.renderables.removeIf(w -> w instanceof TextureButton);
        renderTeamList(null);
    }

    @Override
    public void onClose() {
        ClientState.hidePlayerRender = false; // Сбрасываем флаг
        minecraft.setScreen(parentScreen);
    }
}
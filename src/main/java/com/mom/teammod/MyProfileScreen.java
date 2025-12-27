package com.mom.teammod;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mom.teammod.packets.RespondInvitationPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.stats.Stats;
import net.minecraft.stats.StatsCounter;
import net.minecraft.world.entity.player.Player;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

public class MyProfileScreen extends Screen {

    private static final ResourceLocation ATLAS = ResourceLocation.fromNamespaceAndPath(TeamMod.MODID,
            "textures/gui/my_profile_background.png");

    // координаты из CVAT-разметки
    private int lastRenderedScrollOffset = -1; // ← НОВОЕ поле!
    private static final int INV_U       = 1,   INV_V       = 171, INV_W   = 23, INV_H   = 16;
    private static final int SCROLLER_U  = 25,  SCROLLER_V  = 171, SCROLLER_W = 6,  SCROLLER_H = 25;
    private static final int BAR_U       = 31,  BAR_V       = 186, BAR_W   = 81, BAR_H   = 5;
    private static final int X_U         = 1,   X_V         = 187, X_W     = 12, X_H     = 11;
    private static final int V_U         = 13,  V_V         = 187, V_W     = 11, V_H     = 11;
    private static final int HUMAN_U     = 81,  HUMAN_V     = 199, HUMAN_W = 9,  HUMAN_H = 10;

    Screen parentScreen;

    private static final int ONLINE_U = 31,  ONLINE_V = 192;
    private static final int AFK_U    = 36,  AFK_V    = 192;

    private static final int GUI_WIDTH  = 256;
    private static final int GUI_HEIGHT = 169;

    private static final int SKIN_FRAME_WIDTH  = 87;
    private static final int SKIN_FRAME_HEIGHT = 100;
    private static final int FRAME_MARGIN_LEFT = 15;

    private static final long AFK_THRESHOLD = 10_000L;
    private final AtomicLong lastInputTime = new AtomicLong(System.currentTimeMillis());

    private static final int VISIBLE_SLOTS = 4;
    private static final int SLOT_HEIGHT = INV_H + X_H + 4; // 31 пиксель на ячейку
    private final List<TeamManager.Team> teamList = new ArrayList<>();
    private int scrollOffset = 0;

    private boolean isDraggingScroller = false;
    private static final int SCROLL_TRACK_HEIGHT = 119;  // высота трека (от верха до низа)
    private static final int SCROLLER_X_OFFSET = 111 + 41 + 10 + 20 + 10 + 10 + 10 + 1 + (int)(8 / 0.75f) + 23 + 5;
    private static final int SCROLLER_Y_START = 30 - 8 + 7 + 1;

    private int playerKills = 0;
    private int deaths      = 0;
    private long playTime   = 0;
    private int bossKills   = 0;

    public MyProfileScreen(Screen parentScreen, Component title) {
        super(title);
        this.parentScreen = parentScreen;
    }

    @Override
    protected void init() {
        super.init();
        scrollOffset = 0;
        lastRenderedScrollOffset = -1;

        int guiX = (width - GUI_WIDTH) / 2;
        int guiY = (height - GUI_HEIGHT) / 2;

        // === ТЕКСТУРЫ И ИКОНКИ (один раз объявляем) ===
        ResourceLocation unpress = ResourceLocation.fromNamespaceAndPath(TeamMod.MODID, "textures/gui/unpress.png");
        ResourceLocation press   = ResourceLocation.fromNamespaceAndPath(TeamMod.MODID, "textures/gui/press.png");

        ResourceLocation INV_ICON       = ResourceLocation.fromNamespaceAndPath(TeamMod.MODID, "textures/gui/inv_icon.png");
        ResourceLocation TEAM_LIST_ICON = ResourceLocation.fromNamespaceAndPath(TeamMod.MODID, "textures/gui/team_list_icon.png");
        ResourceLocation PROFILE_ICON   = ResourceLocation.fromNamespaceAndPath(TeamMod.MODID, "textures/gui/profile_icon.png");

        int baseY = guiY - 26;  // Та же базовая высота, что и в инвентаре

        // === КНОПКА 3 — ИНВЕНТАРЬ (активная, не зажата) ===
        int invButtonX = guiX + 2;
        this.addRenderableWidget(new ImageButton(invButtonX, baseY, 26, 27, 0, 0, 0, unpress, button -> {
            minecraft.setScreen(new InventoryScreen(minecraft.player));
        }) {
            private boolean isPressed = false;

            @Override
            public void renderWidget(GuiGraphics g, int mx, int my, float pt) {
                boolean hoveredOrPressed = this.isHoveredOrFocused() || isPressed;
                ResourceLocation tex = hoveredOrPressed ? press : unpress;
                int h = hoveredOrPressed ? 29 : 27;

                if (this.getHeight() != h) {
                    this.setHeight(h);
                    this.setY(hoveredOrPressed ? baseY - 2 : baseY);
                }

                g.blit(tex, this.getX(), this.getY(), 0, 0, 26, h, 26, h);
                g.blit(INV_ICON, this.getX() + 5, this.getY() + (hoveredOrPressed ? 7 : 6), 0, 0, 16, 16, 16, 16);

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

        // === КНОПКА 1 — КОМАНДЫ ===
        int teamButtonX = invButtonX + 26 + 52;
        this.addRenderableWidget(new ImageButton(teamButtonX, baseY, 26, 27, 0, 0, 0, unpress, button -> {
            Player player = minecraft.player;
            if (player != null) {
                minecraft.setScreen(new TeamScreen(
                        MyProfileScreen.this,
                        new TeamMenu(0, player.getInventory()),
                        player.getInventory(),
                        Component.translatable("gui.teammod.team_tab")
                ));
            }
        }) {
            private boolean isPressed = false;

            @Override
            public void renderWidget(GuiGraphics g, int mx, int my, float pt) {
                boolean hoveredOrPressed = this.isHoveredOrFocused() || isPressed;
                ResourceLocation tex = hoveredOrPressed ? press : unpress;
                int h = hoveredOrPressed ? 29 : 27;

                if (this.getHeight() != h) {
                    this.setHeight(h);
                    this.setY(hoveredOrPressed ? baseY - 2 : baseY);
                }

                g.blit(tex, this.getX(), this.getY(), 0, 0, 26, h, 26, h);
                g.blit(TEAM_LIST_ICON, this.getX() + 5, this.getY() + (hoveredOrPressed ? 7 : 6), 0, 0, 16, 16, 16, 16);

                if (this.isHoveredOrFocused()) {
                    g.renderTooltip(font, Component.translatable("gui.teammod.team_tab"), mx, my);
                }
            }

            @Override
            public void onClick(double mouseX, double mouseY) {
                super.onClick(mouseX, mouseY);
                this.isPressed = true;
            }
        });

        // === КНОПКА 2 — ПРОФИЛЬ (всегда зажата) ===
        int profileButtonX = teamButtonX + 26;
        this.addRenderableWidget(new ImageButton(profileButtonX, baseY - 2, 26, 29, 0, 0, 0, press, button -> {}) {
            @Override
            public void renderWidget(GuiGraphics g, int mx, int my, float pt) {
                g.blit(press, this.getX(), this.getY(), 0, 0, 26, 29, 26, 29);
                g.blit(PROFILE_ICON, this.getX() + 5, this.getY() + 6, 0, 0, 16, 16, 16, 16);

                if (this.isHoveredOrFocused()) {
                    g.renderTooltip(font, Component.translatable("gui.teammod.profile"), mx, my);
                }
            }
        });

        // Твоя старая кнопка статистики (оставляем)
        addTransparentButton(guiX + 110, guiY + 114, 86, 14,
                this::openDetailedStats,
                Component.literal("Подробная статистика"));

        renderTeamList(null);
    }

    private void openDetailedStats() {
        ClientState.hidePlayerRender = true;

        StatisticsScreen statsScreen = new StatisticsScreen(Component.literal("Подробная статистика"),minecraft.player.getUUID()) {
            @Override
            public void render(GuiGraphics g, int mx, int my, float pt) {
                // 1. Рисуем затемнение ВСЕГО ЭКРАНА
                g.fill(0, 0, width, height, 0xB3000000); // ← ЗДЕСЬ!

                // 2. Рисуем текстуру фона GUI поверх затемнения
                RenderSystem.setShaderTexture(0, ATLAS);
                int guiX = (width - GUI_WIDTH) / 2;
                int guiY = (height - GUI_HEIGHT) / 2;
                g.blit(ATLAS, guiX, guiY, 0, 0, GUI_WIDTH, GUI_HEIGHT, 256, 256);

                // 3. Рисуем все элементы профиля
                MyProfileScreen.this.renderAllElements(g, mx, my); // ← скин не рисуется из-за флага

                // 4. Теперь рисуем окно статистики поверх всего
                super.render(g, mx, my, pt); // ← Оно рисует СВОЕ окно БЕЗ затемнения
            }

            @Override
            public void onClose() {
                ClientState.hidePlayerRender = false; // ← ВКЛЮЧАЕМ СКИН ОБРАТНО
                minecraft.setScreen(parentScreen); // Возврат на предыдущий экран
            }
        };

        statsScreen.init(minecraft, width, height);
        minecraft.setScreen(statsScreen);
    }

    private Button addTransparentButton(int x, int y, int w, int h, Runnable action, Component tooltip) {
        Button btn = new Button(x, y, w, h, Component.empty(), b -> action.run(), s -> Component.empty()) {
            @Override
            public void renderWidget(GuiGraphics g, int mx, int my, float pt) {
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
        renderAFKStatus(g);
        renderNickname(g);
        renderHumanIconAndText(g);
        renderCenterBarsAndScroller(g); // ← НОВОЕ: бары и скроллер по центру
    }

    private void renderAFKStatus(GuiGraphics g) {
        boolean isAfk = System.currentTimeMillis() - lastInputTime.get() >= AFK_THRESHOLD;
        int u = isAfk ? AFK_U : ONLINE_U;
        int baseX = (width - GUI_WIDTH) / 2;
        int baseY = (height - GUI_HEIGHT) / 2;
        int drawX = baseX + 88 - 2 + 1 - 2;
        int drawY = baseY + 38 - 3 + 5 - 2;
        g.blit(ATLAS, drawX, drawY, u, ONLINE_V, 5, 5, 256, 256);
    }

    private void renderSkin(GuiGraphics g, int mx, int my) {
        // ← НОВАЯ ПРОВЕРКА — если флаг включён, НЕ рисуем скин вообще
        if (ClientState.hidePlayerRender) {
            return;
        }

        int frameX = (width - GUI_WIDTH) / 2 + FRAME_MARGIN_LEFT;
        int frameY = (height - GUI_HEIGHT) / 2 + (GUI_HEIGHT - SKIN_FRAME_HEIGHT) / 2 - 13;
        int playerX = frameX + SKIN_FRAME_WIDTH / 2;
        int playerY = frameY + SKIN_FRAME_HEIGHT - 10;

        InventoryScreen.renderEntityInInventoryFollowsMouse(
                g, playerX, playerY, 38,
                (float) playerX - mx,
                (float) (frameY + SKIN_FRAME_HEIGHT / 2) - my,
                minecraft.player);
    }

    private void renderNickname(GuiGraphics g) {
        String displayName = minecraft.player.getDisplayName().getString();
        drawScaledCenteredString(g, displayName, 0.75f,
                (height - GUI_HEIGHT) / 2 + 30 + 2, 0xFFFFFF);
    }

    // Универсальная функция — будет везде работать идеально
    private void drawScaledCenteredString(GuiGraphics g, String text, float scale, int y, int color) {
        int x = getPlayerNicknameX(text);  // реальная координата по центру зоны

        g.pose().pushPose();
        g.pose().scale(scale, scale, scale);
        g.drawString(font, text, (int)(x / scale), (int)(y / scale), color, false);
        g.pose().popPose();
    }

    // Универсальное центрирование текста по зоне из атласа (110.90 → 194.50)
    private int getHumanTextX(String text) {
        int guiLeft = (width - GUI_WIDTH) / 2;
        float zoneLeft = 111.90f;
        float zoneRight = 195.50f;
        float zoneWidth = zoneRight - zoneLeft; // 83.6 пикселей
        int textWidth = font.width(text);
        float offsetX = (zoneWidth - textWidth) / 2.0f;
        return (int) (guiLeft + zoneLeft + offsetX);
    }

    // Универсальное центрирование ника (с тегом или без) в той же зоне, что и "Human"
    private int getPlayerNicknameX(String displayName) {
        int guiLeft = (width - GUI_WIDTH) / 2;
        float zoneLeft = 111.90f;   // как в getHumanTextX — уже со сдвигом +1
        float zoneRight = 195.50f;
        float zoneWidth = zoneRight - zoneLeft; // 83.6

        // Учитываем масштаб 0.75f
        float scale = 0.75f;
        int scaledTextWidth = (int) (font.width(displayName) * scale);

        float offsetX = (zoneWidth - scaledTextWidth) / 2.0f;
        return (int) (guiLeft + zoneLeft + offsetX);
    }

    private void renderHumanIconAndText(GuiGraphics g) {
        int baseX = (width - GUI_WIDTH) / 2;
        int baseY = (height - GUI_HEIGHT) / 2;

        // Иконка Human — оставляем твои финальные координаты
        g.blit(ATLAS, baseX + 111 + 41 - 74 - 20 - 20 - 18, baseY + 96 + 123 - 194 + 5 + 5,
                HUMAN_U, HUMAN_V, HUMAN_W, HUMAN_H, 256, 256);

        // УНИВЕРСАЛЬНОЕ центрирование по зоне 110.90 → 194.50
        int textX = getHumanTextX("Human");
        int textY = baseY + 96 + 2;
        g.drawString(font, "Human", textX, textY, 0xFFFFFF, false);
    }

    // Простая кнопка с текстурой из атласа
    public static class TextureButton extends Button {
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

    // 2. MyProfileScreen.java — метод renderTeamList
    private void renderTeamList(GuiGraphics g) {
        // Удаляем старый код, где заполняется teamList всеми командами

        // Заменяем на правильный список приглашений
        this.renderables.removeIf(w -> w instanceof TextureButton);

        UUID myId = minecraft.player.getUUID();
        List<TeamManager.Team> invitations = new ArrayList<>();
        for (TeamManager.Team team : TeamManager.clientTeams.values()) {
            if (team.getInvited().contains(myId)) {
                invitations.add(team);
            }
        }

        invitations.sort(new Comparator<TeamManager.Team>() {
            @Override
            public int compare(TeamManager.Team t1, TeamManager.Team t2) {
                return t1.getName().compareTo(t2.getName());
            }
        });

        int baseX = (width - GUI_WIDTH) / 2;
        int baseY = (height - GUI_HEIGHT) / 2;
        int startX = baseX + 111 + 41 + 10 + 20 + 10 + 10 + 10 + 1 + (int)(8 / 0.75f);
        int startY = baseY + 30 - 8 + 7 + 1 + 2;

        int maxVisible = Math.min(4, invitations.size());
        for (int i = 0; i < maxVisible; i++) {
            int index = i + scrollOffset;
            if (index >= invitations.size()) break;

            TeamManager.Team team = invitations.get(index);
            String tag = team.getTag();
            int invY = startY + i * (INV_H + X_H + 2);
            int underY = invY + INV_H;

            // INV + тег
            addRenderableWidget(new TextureButton(startX, invY, INV_W, INV_H, INV_U, INV_V, ATLAS, btn -> {
                // Открываем просмотр чужой команды
                minecraft.setScreen(new OtherTeamProfileScreen(
                        MyProfileScreen.this,
                        team.getName(),
                        team.getTag(),
                        team.showTag(),
                        team.showCompass(),
                        team.isFriendlyFire(),
                        team.getOwner()
                ));
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

            // X — отклонить
            addRenderableWidget(new TextureButton(startX + 35, underY, X_W, X_H, X_U, X_V, ATLAS, btn -> {
                NetworkHandler.INSTANCE.sendToServer(new RespondInvitationPacket(team.getName(), false));
            }));

            // V — принять
            addRenderableWidget(new TextureButton(startX + 60, underY, V_W, V_H, V_U, V_V, ATLAS, btn -> {
                NetworkHandler.INSTANCE.sendToServer(new RespondInvitationPacket(team.getName(), true));
            }));
        }
    }

    private void renderCenterBarsAndScroller(GuiGraphics g) {
        renderProgressBar1(g);  // Первый бар — 25%
        renderProgressBar2(g);  // Второй бар — 75%
        renderScroller(g);      // Ползунок (остаётся как был)
    }

    // Верхний бар — теперь квесты
    private void renderProgressBar1(GuiGraphics g) {
        int centerX = width / 2;
        int baseY = (height - GUI_HEIGHT) / 2;
        int x = centerX - BAR_W / 2 + 24;
        int y = baseY + 110 - 34;

        UUID playerUUID = minecraft.player.getUUID();

        int fillPercent = FTBQuestsStats.getQuestProgressPercent(playerUUID);

        int fillWidth = (int) (BAR_W * fillPercent / 100.0);

        g.blit(ATLAS, x, y, BAR_U, BAR_V, fillWidth, BAR_H, 256, 256);
    }

    // Нижний бар — теперь уровень
    private void renderProgressBar2(GuiGraphics g) {
        int centerX = width / 2;
        int baseY = (height - GUI_HEIGHT) / 2;
        int x = centerX - BAR_W / 2 + 24;
        int y = baseY + 118 - 65;

        UUID playerUUID = minecraft.player.getUUID();

        int fillPercent = SkillTreeStats.getOverallProgressPercent(playerUUID);

        int fillWidth = (int) (BAR_W * fillPercent / 100.0);

        g.blit(ATLAS, x, y, BAR_U, BAR_V, fillWidth, BAR_H, 256, 256);
    }

    // Ползунок (оставляем как был — он уже идеально работает)
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
        renderBackground(g);
        renderBg(g, pt, mx, my);

        if (minecraft.player != null) {
            StatsCounter stats = minecraft.player.getStats();
            deaths      = stats.getValue(Stats.CUSTOM.get(Stats.DEATHS));
            playerKills = stats.getValue(Stats.CUSTOM.get(Stats.PLAYER_KILLS));
            playTime    = stats.getValue(Stats.CUSTOM.get(Stats.PLAY_TIME));
            ProfileManager.Profile profile = ProfileManager.getClientProfile(minecraft.player.getUUID());
            bossKills   = profile.getCustomStats().getOrDefault("boss_kills", 0);
        }

        renderAllElements(g, mx, my);
        super.render(g, mx, my, pt);
    }

    protected void renderBg(GuiGraphics g, float pt, int mx, int my) {
        RenderSystem.setShaderTexture(0, ATLAS);
        int x = (width - GUI_WIDTH) / 2;
        int y = (height - GUI_HEIGHT) / 2;
        g.blit(ATLAS, x, y, 0, 0, GUI_WIDTH, GUI_HEIGHT, 256, 256);
    }


    // === AFK + СКРОЛЛЕР (всё в одном блоке, без дубликатов) ===

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Проверяем клик по скроллеру
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

        lastInputTime.set(System.currentTimeMillis());
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
    public boolean keyPressed(int key, int scan, int mod) {
        lastInputTime.set(System.currentTimeMillis());
        return super.keyPressed(key, scan, mod);
    }

    @Override
    public boolean charTyped(char c, int mod) {
        lastInputTime.set(System.currentTimeMillis());
        return super.charTyped(c, mod);
    }

    @Override
    public void mouseMoved(double mx, double my) {
        lastInputTime.set(System.currentTimeMillis());
        super.mouseMoved(mx, my);
    }

    @Override
    public void onClose() {
        ClientState.hidePlayerRender = false; // ← ВАЖНО! Сбрасываем при выходе в инвентарь
        minecraft.setScreen(new InventoryScreen(minecraft.player));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
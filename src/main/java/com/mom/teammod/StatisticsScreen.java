package com.mom.teammod;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.stats.Stat;
import net.minecraft.stats.Stats;
import net.minecraft.stats.StatsCounter;
import net.minecraft.core.Registry;
import com.mod.raidportals.RaidPortalsSavedData;

import java.util.Arrays;
import java.util.UUID;

public class StatisticsScreen extends BaseModScreen {

    private static final ResourceLocation ATLAS = ResourceLocation.fromNamespaceAndPath(TeamMod.MODID,
            "textures/gui/statistics_background.png");

    private static final int GUI_WIDTH  = 220;   // было 217 → стало 220
    private static final int GUI_HEIGHT = 118;   // было 105 → стало 145
    private int left() { return (width - GUI_WIDTH) / 2; }
    private int top()  { return (height - GUI_HEIGHT) / 2; }
    UUID statsOwnerUUID;
    // Список всех 8 строк статистики (статично, для теста)
    private int scrollOffset = 0;
    private boolean isDraggingScroller = false;

    private int statUpdateCounter = 0;
    private String[] lastStats = new String[0];

    // Параметры скроллера
    private static final int VISIBLE_LINES = 4;
    private static final int LINE_HEIGHT = 15;
    private static final int TOTAL_VISIBLE_HEIGHT = VISIBLE_LINES * LINE_HEIGHT; // 60 пикселей
    private static final int SCROLLER_HEIGHT = 27;        // 144.52 - 118.52 = 26
    private static final int SCROLL_TRACK_HEIGHT = 60;

    public StatisticsScreen(Screen parentScreen, Component title, UUID statsOwnerUUID) {
        super(parentScreen, title);
        this.statsOwnerUUID = statsOwnerUUID;
    }

    @Override
    protected void init() {
        super.init();
        scrollOffset = 0;

        // Убрали всю ванильную хрень — теперь статы из профиля
        System.out.println("[StatisticsScreen] init() вызван. Статистика берётся из профиля.");

        addRenderableWidget(new Button(left() + 86, top() + 89, 47, 14, Component.empty(),
                b -> {
                    ClientState.hidePlayerRender = false;
                    minecraft.setScreen(parentScreen); // ← вернётся в OtherPlayerProfileScreen
                },
                (narration) -> Component.empty())
        {
            @Override
            protected void renderWidget(GuiGraphics g, int mx, int my, float pt) {
                g.blit(ATLAS, getX(), getY(), 86, 89, 47, 14, 256, 256);
                if (isHovered()) {
                    g.fill(getX(), getY(), getX() + width, getY() + height, 0x40FFFFFF);
                }
            }
        });

        System.out.println("[StatisticsScreen] init() завершён.");
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(g);

        int x = left();
        int y = top();

        g.blit(ATLAS, x, y, 0, 0, GUI_WIDTH, GUI_HEIGHT, 256, 256);

        String[] stats = getPlayerStats();

        int textX = x + 15 + 7;
        int textStartY = y + 15 + 10 + 3;

        for (int i = 0; i < VISIBLE_LINES; i++) {
            int index = i + scrollOffset;
            if (index >= stats.length) break;

            g.drawString(font, stats[index], textX, textStartY + i * LINE_HEIGHT, 0xFFFFFF, false);
        }

        renderScroller(g, x, y);

        super.render(g, mouseX, mouseY, partialTick);
    }

    private void renderScroller(GuiGraphics g, int guiX, int guiY) {
        String[] stats = getPlayerStats(); // Теперь динамически

        int scrollerX = guiX + 75 - 63;
        int trackTopY = guiY + 45 - 23;

        int scrollerY = trackTopY;
        if (stats.length > VISIBLE_LINES) {
            float ratio = (float) scrollOffset / (stats.length - VISIBLE_LINES);
            int travel = SCROLL_TRACK_HEIGHT - SCROLLER_HEIGHT;
            scrollerY += (int) (ratio * travel);
        }

        g.blit(ATLAS, scrollerX, scrollerY, 0, 118, 6, 27, 256, 256);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaY) {
        String[] stats = getPlayerStats();
        if (stats.length <= VISIBLE_LINES) return false;

        int maxScroll = stats.length - VISIBLE_LINES;
        scrollOffset -= (int) deltaY;
        scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));
        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        String[] stats = getPlayerStats();
        if (stats.length <= VISIBLE_LINES) return super.mouseClicked(mouseX, mouseY, button);

        int guiX = left();
        int guiY = top();
        int scrollerX = guiX + 75 - 63;
        int trackTopY = guiY + 45 - 23;
        int trackHeight = SCROLL_TRACK_HEIGHT;

        if (mouseX >= scrollerX && mouseX <= scrollerX + 7 &&
                mouseY >= trackTopY && mouseY <= trackTopY + trackHeight) {
            isDraggingScroller = true;
            updateScrollFromMouse(mouseY, guiY);
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (isDraggingScroller) {
            updateScrollFromMouse(mouseY, (height - GUI_HEIGHT) / 2);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        isDraggingScroller = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private void updateScrollFromMouse(double mouseY, int guiY) {
        String[] stats = getPlayerStats();
        int trackTopY = guiY + 45 - 23;
        double relativeY = mouseY - trackTopY;
        relativeY = Math.max(0, Math.min(relativeY, SCROLL_TRACK_HEIGHT - SCROLLER_HEIGHT));

        float ratio = (float) relativeY / (SCROLL_TRACK_HEIGHT - SCROLLER_HEIGHT);
        int maxScroll = Math.max(0, stats.length - VISIBLE_LINES);
        scrollOffset = Math.round(ratio * maxScroll);
        scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));
    }

    @Override
    public void tick() {
        super.tick();

        if (++statUpdateCounter >= 60) { // Обновление каждые 3 секунды
            statUpdateCounter = 0;
            String[] currentStats = getPlayerStats();

            if (!Arrays.equals(currentStats, lastStats)) {
                lastStats = currentStats;
                // Принудительно перерисовываем
                if (minecraft.screen == this) {
                    init(); // Переинициализируем для обновления данных
                }
            }
        }
    }

    @Override
    public void onClose() {
        ClientState.hidePlayerRender = false;
        minecraft.setScreen(parentScreen); // вернёт в OtherPlayerProfileScreen
    }

    private String[] getPlayerStats() {
        System.out.println("[StatisticsScreen] getPlayerStats() вызван для UUID: " + statsOwnerUUID);

        ProfileManager.Profile profile = ProfileManager.getClientProfile(statsOwnerUUID);
        if (profile == null) {
            return new String[]{
                    "Смерти: ...",
                    "Убито мобов: ...",
                    "Время в игре: ...",
                    "Открыто сундуков: ...",
                    "Пройдено: ..."
            };
        }

        // Время: сохранённое + текущая сессия
        int savedTicks = profile.getPlayTimeTicks();
        long sessionMillis = profile.getCurrentSessionMillis();
        int sessionTicks = (int)(sessionMillis / 50);
        int completedQuests = FTBQuestsStats.getCompletedQuests(statsOwnerUUID);
        int totalQuests = FTBQuestsStats.getTotalQuests();
        int completedChapters = FTBQuestsStats.getCompletedChapters(statsOwnerUUID);
        int totalChapters = FTBQuestsStats.getTotalChapters();

        // ✅ БЕЗОПАСНО: арены из кэша (синхронизированы с сервера)
        ClientPlayerCache.PortalData pd = ClientPlayerCache.getPortalData(statsOwnerUUID);
        int tier1 = pd.tier1;
        int tier2 = pd.tier2;
        int tier3 = pd.tier3;

        int totalTicks = savedTicks + sessionTicks;
        int totalMinutes = totalTicks / 1200;
        int hours = totalMinutes / 60;
        int minutes = totalMinutes % 60;
        String playTimeStr = hours + "ч " + String.format("%02d", minutes) + "м";

        long distanceCm = profile.getDistanceCm();
        double distanceKm = distanceCm / 100000.0;
        String distanceStr = String.format("%.1f км", distanceKm);

        System.out.println("[StatisticsScreen] Загруженные значения для " + statsOwnerUUID + ":");
        System.out.println("  Смерти: " + profile.getDeaths());
        System.out.println("  Убито мобов: " + profile.getMobsKilled());
        System.out.println("  Время в игре: " + playTimeStr);
        System.out.println("  Расстояние: " + distanceStr);

        return new String[]{
                "Смерти: " + profile.getDeaths(),
                "Убито мобов: " + profile.getMobsKilled(),
                "Время в игре: " + playTimeStr,
                "Уровень персонажа: " + SkillTreeStats.getLevel(statsOwnerUUID),
                "Квесты: " + completedQuests + "/" + totalQuests,
                "Главы: " + completedChapters + "/" + totalChapters,
                "Арены тир 1: " + tier1,
                "Арены тир 2: " + tier2,
                "Арены тир 3: " + tier3,
                "Пройдено: " + distanceStr
        };
    }
}
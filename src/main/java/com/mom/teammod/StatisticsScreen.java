package com.mom.teammod;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.stats.Stats;

public class StatisticsScreen extends Screen {

    private static final ResourceLocation ATLAS = ResourceLocation.fromNamespaceAndPath(TeamMod.MODID,
            "textures/gui/statistics_background.png");

    private static final int GUI_WIDTH  = 220;   // было 217 → стало 220
    private static final int GUI_HEIGHT = 118;   // было 105 → стало 145
    private int left() { return (width - GUI_WIDTH) / 2; }
    private int top()  { return (height - GUI_HEIGHT) / 2; }

    // Список всех 8 строк статистики (статично, для теста)
    private final String[] statsLines = {
            "Deaths: 127",
            "Mobs Killed: 3841",
            "Bosses Killed: 8",
            "Blocks Mined: 15672",
            "Distance Traveled: 842 km",
            "Play Time: 127h 34m",
            "Diamonds Found: 284",
            "Nether Portals: 12"
    };

    private int scrollOffset = 0;
    private boolean isDraggingScroller = false;

    // Параметры скроллера
    private static final int VISIBLE_LINES = 4;
    private static final int LINE_HEIGHT = 15;
    private static final int TOTAL_VISIBLE_HEIGHT = VISIBLE_LINES * LINE_HEIGHT; // 60 пикселей
    private static final int SCROLLER_HEIGHT = 27;        // 144.52 - 118.52 = 26
    private static final int SCROLL_TRACK_HEIGHT = 60;

    public StatisticsScreen(Component title) {
        super(title);
    }

    @Override
    protected void init() {
        super.init();
        scrollOffset = 0;

        int x = (width - GUI_WIDTH) / 2;
        int y = (height - GUI_HEIGHT) / 2;

        // Добавь после всех кнопок в init():
        addRenderableWidget(new Button(left() + 86, top() + 89, 47, 14, Component.empty(),
                b -> {
                    ClientState.hidePlayerRender = false;
                    minecraft.setScreen(new MyProfileScreen(
                            StatisticsScreen.this,  // ← текущий экран (this)
                            Component.translatable("gui.teammod.profile")
                    ));
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
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        // 1. ТОЧНО как в AchievementPickerScreen1
        StatisticsScreen.this.renderBackground(g); // ← ВАНИЛЬНОЕ затемнение!

        // 2. Остальной код как был
        int x = (width - GUI_WIDTH) / 2;
        int y = (height - GUI_HEIGHT) / 2;

        // Фон окна статистики
        g.blit(ATLAS, x, y, 0, 0, GUI_WIDTH, GUI_HEIGHT, 256, 256);

        // Рисуем текст (сдвинут на 3 пикселя вниз)
        int textX = x + 15 + 7;
        int textStartY = y + 15 + 10 + 3;

        for (int i = 0; i < VISIBLE_LINES; i++) {
            int index = i + scrollOffset;
            if (index >= statsLines.length) break;

            g.drawString(font, statsLines[index], textX, textStartY + i * LINE_HEIGHT, 0xFFFFFF, false);
        }

        // Рисуем ползунок
        renderScroller(g, x, y);

        super.render(g, mouseX, mouseY, partialTick);
    }

    private void renderScroller(GuiGraphics g, int guiX, int guiY) {
        int scrollerX = guiX + 75 - 63;
        int trackTopY = guiY + 45 - 23;
        int trackBottomY = trackTopY + SCROLL_TRACK_HEIGHT;


        int scrollerY = trackTopY;
        if (statsLines.length > VISIBLE_LINES) {
            float ratio = (float) scrollOffset / (statsLines.length - VISIBLE_LINES);
            int travel = SCROLL_TRACK_HEIGHT - SCROLLER_HEIGHT;
            scrollerY += (int) (ratio * travel);
        }

        g.blit(ATLAS, scrollerX, scrollerY, 0, 118, 6, 27, 256, 256);
    }

    // === Скролл колёсиком ===
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaY) {
        if (statsLines.length <= VISIBLE_LINES) return false;

        int maxScroll = statsLines.length - VISIBLE_LINES;
        scrollOffset -= (int) deltaY;
        scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));
        return true;
    }

    // === Перетаскивание ползунка ===
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (statsLines.length <= VISIBLE_LINES) return super.mouseClicked(mouseX, mouseY, button);

        int guiX = (width - GUI_WIDTH) / 2;
        int guiY = (height - GUI_HEIGHT) / 2;
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
        int trackTopY = guiY + 45 - 23;
        double relativeY = mouseY - trackTopY;
        relativeY = Math.max(0, Math.min(relativeY, SCROLL_TRACK_HEIGHT - SCROLLER_HEIGHT));

        float ratio = (float) relativeY / (SCROLL_TRACK_HEIGHT - SCROLLER_HEIGHT);
        int maxScroll = Math.max(0, statsLines.length - VISIBLE_LINES);
        scrollOffset = Math.round(ratio * maxScroll);
        scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));
    }

    @Override
    public void onClose() {
        ClientState.hidePlayerRender = false;
        // Возвращаемся в MyProfileScreen (он в стеке)
        if (minecraft.screen instanceof MyProfileScreen profileScreen) {
            minecraft.setScreen(profileScreen);
        } else {
            minecraft.setScreen(null);
        }
    }
}
package com.mom.teammod;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public class ColorPickerScreen extends BaseModScreen {

    private static final ResourceLocation ATLAS = ResourceLocation.fromNamespaceAndPath(TeamMod.MODID,
            "textures/gui/list_of_colours.png");


    // НОВОЕ: HSV-пикер (тестовый быстрый вариант)
    private int selectedColor = 0xFFFFFFFF; // текущий цвет (ARGB)

    private float hue = 0f;           // 0-360
    private float saturation = 1f;    // 0-1
    private float value = 1f;         // 0-1

    private boolean draggingSquare = false;
    private boolean draggingHue = false;

    // Координаты внутри твоего GUI (146x105)
    private static final int SQUARE_X = 10;
    private static final int SQUARE_Y = 15;
    private static final int SQUARE_SIZE = 80;

    private static final int HUE_BAR_X = SQUARE_X + SQUARE_SIZE + 10;
    private static final int HUE_BAR_Y = SQUARE_Y;
    private static final int HUE_BAR_W = 15;

    private static final int PREVIEW_X = 10;
    private static final int PREVIEW_Y = SQUARE_Y + SQUARE_SIZE + 10;
    private static final int PREVIEW_SIZE = 40;

    private static final int GUI_WIDTH = 146;
    private static final int GUI_HEIGHT = 105;

    // ---------- КНОПКА CONFIRM ----------
    private static final int CONFIRM_X = 8 + 121 - 45-1;   // -45 (влево на ширину)
    private static final int CONFIRM_Y = 106 - 43 + 12;  // +12 (вниз на высоту)
    private static final int CONFIRM_W = 46;             // +1 пиксель ширина
    private static final int CONFIRM_H = 12;
    private static final int CONFIRM_U = 7;
    private static final int CONFIRM_V = 106;

    // ---------- ПОЛЗУНОК ----------
    private static final int SCROLLER_X = 11+1;
    private static final int SCROLLER_BASE_Y = 24;      // на 1 пиксель выше
    private static final int SCROLLER_W = 7;
    private static final int SCROLLER_H = 12;  // ← теперь 42, как в атласе (второй scroller)
    private static final int SCROLLER_U = 11;
    private static final int SCROLLER_V = 23;

    private static final int SCROLL_TRACK_HEIGHT = 42;  // трек — ровно 42 пикселя

    private static final int VISIBLE_ITEMS = 6;
    private static final int TOTAL_ITEMS = 32;

    private int scrollOffset = 0;
    private boolean isDraggingScroller = false;

    public ColorPickerScreen(Screen parentScreen) { super(parentScreen, Component.literal("")); }

    @Override
    protected void init() {
        super.init();
        scrollOffset = 0;

        int guiX = (width - GUI_WIDTH) / 2;
        int guiY = (height - GUI_HEIGHT) / 2;

        addTransparentButton(guiX + CONFIRM_X, guiY + CONFIRM_Y, CONFIRM_W, CONFIRM_H,
                () -> this.onClose(), Component.literal("Готово"));
        // Кнопка "Отмена" — левее "Готово" на 23 пикселя, полностью прозрачная, такой же размер
        addRenderableWidget(new Button(guiX + CONFIRM_X - 23 - 23 - 11 - 7, guiY + CONFIRM_Y, CONFIRM_W - 5, CONFIRM_H,
                Component.empty(),
                button -> this.onClose(),
                (narration) -> Component.empty())
        {
            @Override
            protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
                // Полностью прозрачная — ничего не рисуем
                if (this.isHovered()) {
                    g.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, 0x30FFFFFF);
                }
            }
        });
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(g); // ← ванильный фон уже с затемнением
        // или вручную:
        // g.fill(0, 0, width, height, 0xB3000000);

        int x = (width - GUI_WIDTH) / 2;
        int y = (height - GUI_HEIGHT) / 2;
        g.blit(ATLAS, x, y, 0, 0, GUI_WIDTH, GUI_HEIGHT, 256, 256);
        renderColorPicker(g);
        renderScroller(g, x, y);
        super.render(g, mouseX, mouseY, partialTick);
    }

    private void renderScroller(GuiGraphics g, int guiX, int guiY) {
        int scrollerX = guiX + SCROLLER_X;
        int baseY = guiY + SCROLLER_BASE_Y;

        int offsetY = 0;
        if (TOTAL_ITEMS > VISIBLE_ITEMS) {
            float ratio = (float) scrollOffset / (TOTAL_ITEMS - VISIBLE_ITEMS);
            int travel = SCROLL_TRACK_HEIGHT - SCROLLER_H; // 42 - 12 = 30 пикселей хода
            offsetY = (int) (ratio * travel);
        }

        int scrollerY = baseY + offsetY;

        g.blit(ATLAS, scrollerX, scrollerY, 1, 106, 7, 12, 256, 256);}

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaY) {
        int guiX = (width - GUI_WIDTH) / 2;
        int guiY = (height - GUI_HEIGHT) / 2;
        int sqX = guiX + SQUARE_X;
        int sqY = guiY + SQUARE_Y;

        // НОВОЕ: колёсико над квадратом меняет Value (яркость)
        if (mouseX >= sqX && mouseX < sqX + SQUARE_SIZE && mouseY >= sqY && mouseY < sqY + SQUARE_SIZE) {
            value = Mth.clamp(value + (float) deltaY * 0.1f, 0f, 1f);
            updateColor();
            return true;
        }

        // ТВОЙ СТАРЫЙ СКРОЛЛ СПИСКА
        if (TOTAL_ITEMS <= VISIBLE_ITEMS) return false;

        int max = TOTAL_ITEMS - VISIBLE_ITEMS;
        scrollOffset -= (int) deltaY;
        scrollOffset = Math.max(0, Math.min(scrollOffset, max));

        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int guiX = (width - GUI_WIDTH) / 2;
        int guiY = (height - GUI_HEIGHT) / 2;

        // НОВОЕ: клик по квадрату Saturation/Value
        int sqX = guiX + SQUARE_X;
        int sqY = guiY + SQUARE_Y;
        if (mouseX >= sqX && mouseX < sqX + SQUARE_SIZE && mouseY >= sqY && mouseY < sqY + SQUARE_SIZE) {
            draggingSquare = true;
            saturation = (float) ((mouseX - sqX) / SQUARE_SIZE);
            value = 1f - (float) ((mouseY - sqY) / SQUARE_SIZE);
            saturation = Mth.clamp(saturation, 0f, 1f);
            value = Mth.clamp(value, 0f, 1f);
            updateColor();
            return true;
        }

        // НОВОЕ: клик по Hue-бару
        int hueX = guiX + HUE_BAR_X;
        int hueY = sqY;
        if (mouseX >= hueX && mouseX < hueX + HUE_BAR_W && mouseY >= hueY && mouseY < hueY + SQUARE_SIZE) {
            draggingHue = true;
            hue = (float) ((mouseY - hueY) / SQUARE_SIZE * 360f);
            hue = Mth.clamp(hue, 0f, 360f);
            updateColor();
            return true;
        }

        // ТВОЙ СТАРЫЙ КОД СКРОЛЛЕРА
        int trackX = guiX + SCROLLER_X;
        int trackTop = guiY + SCROLLER_BASE_Y;
        int trackBottom = trackTop + SCROLL_TRACK_HEIGHT;

        if (mouseX >= trackX && mouseX <= trackX + SCROLLER_W &&
                mouseY >= trackTop && mouseY <= trackBottom) {
            isDraggingScroller = true;
            updateScrollFromMouse(mouseY, guiY);
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        int guiX = (width - GUI_WIDTH) / 2;
        int guiY = (height - GUI_HEIGHT) / 2;

        // НОВОЕ: драг по квадрату
        if (draggingSquare) {
            int sqX = guiX + SQUARE_X;
            int sqY = guiY + SQUARE_Y;
            saturation = Mth.clamp((float) ((mouseX - sqX) / SQUARE_SIZE), 0f, 1f);
            value = Mth.clamp(1f - (float) ((mouseY - sqY) / SQUARE_SIZE), 0f, 1f);
            updateColor();
            return true;
        }

        // НОВОЕ: драг по Hue
        if (draggingHue) {
            int hueY = guiY + SQUARE_Y;
            hue = Mth.clamp((float) ((mouseY - hueY) / SQUARE_SIZE * 360f), 0f, 360f);
            updateColor();
            return true;
        }

        // ТВОЙ СТАРЫЙ ДРАГ СКРОЛЛЕРА
        if (isDraggingScroller) {
            updateScrollFromMouse(mouseY, (height - GUI_HEIGHT) / 2);
            return true;
        }

        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        draggingSquare = false;
        draggingHue = false;
        isDraggingScroller = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private void updateScrollFromMouse(double mouseY, int guiY) {
        int trackTop = guiY + SCROLLER_BASE_Y;
        double rel = mouseY - trackTop;
        rel = Math.max(0, Math.min(rel, 42 - 12));  // → максимум 30 пикселей хода

        float ratio = (float) (rel / (SCROLL_TRACK_HEIGHT - SCROLLER_H));
        int maxScroll = Math.max(0, TOTAL_ITEMS - VISIBLE_ITEMS);
        scrollOffset = Math.round(ratio * maxScroll);
        scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void addTransparentButton(int x, int y, int w, int h, Runnable action, Component tooltip) {
        Button btn = new Button(x, y, w, h, Component.empty(), b -> action.run(), s -> Component.empty()) {
            @Override
            public void renderWidget(GuiGraphics g, int mx, int my, float pt) {
                g.blit(ATLAS, this.getX(), this.getY(), 7, 106, 46, 12, 256, 256);  // строго по разметке +1px ширина
                if (this.isHovered()) {
                    g.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, 0x30FFFFFF);
                }
            }
        };
        btn.setTooltip(Tooltip.create(tooltip));
        this.addRenderableWidget(btn);
    }
    private void updateColor() {
        int rgb = java.awt.Color.HSBtoRGB(hue / 360f, saturation, value);
        selectedColor = rgb | 0xFF000000;
    }
    private void renderColorPicker(GuiGraphics g) {
        int guiX = (width - GUI_WIDTH) / 2;
        int guiY = (height - GUI_HEIGHT) / 2;

        // 1. Квадратик Saturation/Value
        int sqX = guiX + SQUARE_X;
        int sqY = guiY + SQUARE_Y;

        for (int x = 0; x < SQUARE_SIZE; x++) {
            for (int y = 0; y < SQUARE_SIZE; y++) {
                float s = x / (float) SQUARE_SIZE;
                float v = 1f - y / (float) SQUARE_SIZE;
                int rgb = java.awt.Color.HSBtoRGB(hue / 360f, s, v);
                g.fill(sqX + x, sqY + y, sqX + x + 1, sqY + y + 1, rgb | 0xFF000000);
            }
        }

        // Крестик на квадрате
        int crossX = sqX + (int)(saturation * SQUARE_SIZE);
        int crossY = sqY + (int)((1f - value) * SQUARE_SIZE);
        g.fill(crossX - 6, crossY, crossX + 6, crossY + 1, 0xFFFFFFFF);
        g.fill(crossX, crossY - 6, crossX + 1, crossY + 6, 0xFFFFFFFF);

        // 2. Hue-бар
        int hueX = guiX + HUE_BAR_X;
        int hueY = sqY;

        for (int y = 0; y < SQUARE_SIZE; y++) {
            float h = y / (float) SQUARE_SIZE * 360f;
            int rgb = java.awt.Color.HSBtoRGB(h / 360f, 1f, 1f);
            g.fill(hueX, hueY + y, hueX + HUE_BAR_W, hueY + y + 1, rgb | 0xFF000000);
        }

        // Ползунок Hue
        int sliderY = hueY + (int)(hue / 360f * SQUARE_SIZE);
        g.fill(hueX - 2, sliderY - 3, hueX + HUE_BAR_W + 2, sliderY + 3, 0xFFFFFFFF);

        // 3. Кубик-превью
        int prevX = guiX + PREVIEW_X;
        int prevY = guiY + PREVIEW_Y;
        g.fill(prevX, prevY, prevX + PREVIEW_SIZE, prevY + PREVIEW_SIZE, selectedColor);

        // Рамка вокруг кубика
        g.fill(prevX - 1, prevY - 1, prevX + PREVIEW_SIZE + 1, prevY, 0xFFFFFFFF);
        g.fill(prevX - 1, prevY + PREVIEW_SIZE, prevX + PREVIEW_SIZE + 1, prevY + PREVIEW_SIZE + 1, 0xFFFFFFFF);
        g.fill(prevX - 1, prevY, prevX, prevY + PREVIEW_SIZE, 0xFFFFFFFF);
        g.fill(prevX + PREVIEW_SIZE, prevY, prevX + PREVIEW_SIZE + 1, prevY + PREVIEW_SIZE, 0xFFFFFFFF);
    }
}
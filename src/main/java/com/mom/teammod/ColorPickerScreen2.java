package com.mom.teammod;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class ColorPickerScreen2 extends BaseModScreen {

    private static final ResourceLocation ATLAS = ResourceLocation.fromNamespaceAndPath(TeamMod.MODID,
            "textures/gui/list_of_colours.png");

    private static final int GUI_WIDTH = 146;
    private static final int GUI_HEIGHT = 105;

    // КНОПКА CONFIRM
    private static final int CONFIRM_X = 8 + 121 - 45 - 1;
    private static final int CONFIRM_Y = 106 - 43 + 12;
    private static final int CONFIRM_W = 46;
    private static final int CONFIRM_H = 12;

    // ПОЛЗУНОК
    private static final int SCROLLER_X = 11 + 1;
    private static final int SCROLLER_BASE_Y = 24;
    private static final int SCROLLER_W = 7;
    private static final int SCROLLER_H = 12;
    private static final int SCROLL_TRACK_HEIGHT = 42;

    private static final int VISIBLE_ITEMS = 6;
    private static final int TOTAL_ITEMS = 32;

    private int scrollOffset = 0;
    private boolean isDraggingScroller = false;

    public ColorPickerScreen2(Screen parentScreen) { super(parentScreen, Component.literal("")); }

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
        renderScroller(g, x, y);
        super.render(g, mouseX, mouseY, partialTick);
    }

    private void renderScroller(GuiGraphics g, int guiX, int guiY) {
        int scrollerX = guiX + SCROLLER_X;
        int baseY = guiY + SCROLLER_BASE_Y;

        int offsetY = 0;
        if (TOTAL_ITEMS > VISIBLE_ITEMS) {
            float ratio = (float) scrollOffset / (TOTAL_ITEMS - VISIBLE_ITEMS);
            int travel = SCROLL_TRACK_HEIGHT - SCROLLER_H;
            offsetY = (int) (ratio * travel);
        }

        int scrollerY = baseY + offsetY;

        g.blit(ATLAS, scrollerX, scrollerY, 1, 106, 7, 12, 256, 256);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaY) {
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
        int trackTop = guiY + SCROLLER_BASE_Y;
        double rel = mouseY - trackTop;
        rel = Math.max(0, Math.min(rel, 42 - 12));

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
                g.blit(ATLAS, this.getX(), this.getY(), 7, 106, 46, 12, 256, 256);
                if (this.isHovered()) {
                    g.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, 0x30FFFFFF);
                }
            }
        };
        btn.setTooltip(Tooltip.create(tooltip));
        this.addRenderableWidget(btn);
    }
}
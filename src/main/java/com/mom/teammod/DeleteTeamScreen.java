package com.mom.teammod;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mom.teammod.packets.DeleteTeamPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class DeleteTeamScreen extends Screen {
    private static final ResourceLocation ATLAS = ResourceLocation.fromNamespaceAndPath(TeamMod.MODID,
            "textures/gui/delete_team.png");

    // Координаты из разметки
    private static final int FON_U = 0;
    private static final int FON_V = 0;
    private static final int FON_W = 147;  // 146.66 - 0.29 ≈ 147
    private static final int FON_H = 100;  // 100.57 - 0.13 ≈ 100

    // Область для названия команды
    private static final int NAME_U = 12;   // 11.69 округляем до 12
    private static final int NAME_V = 21;   // 20.89 округляем до 21
    private static final int NAME_W = 122;  // 133.29 - 11.69 ≈ 122
    private static final int NAME_H = 8;    // 29.10 - 20.89 ≈ 8

    // Поле ввода
    private static final int INPUT_U = 11;  // 11.22 округляем до 11
    private static final int INPUT_V = 53;  // 52.59 округляем до 53
    private static final int INPUT_W = 123;   // было 122
    private static final int INPUT_H = 9;   // 61.15 - 52.59 ≈ 9

    // Кнопки
    private static final int CANCEL_W = 41;  // 59.94 - 18.74 ≈ 41
    private static final int CANCEL_H = 12;  // 80.54 - 68.64 ≈ 12
    private static final int CONFIRM_W = 46; // 46.71 - 0.41 ≈ 46
    private static final int CONFIRM_H = 13; // 113.85 - 100.65 ≈ 13

    // Подтверждающая кнопка (координаты для текстуры)
    private static final int CONFIRM_BTN_U = 0;   // 0.41 округляем до 0
    private static final int CONFIRM_BTN_V = 101; // 100.65 округляем до 101

    private final Screen parentScreen;
    private final String teamName;
    private final String teamTag;
    private EditBox inputField;
    private Button confirmButton;

    public DeleteTeamScreen(Screen parentScreen, String teamName, String teamTag) {
        super(Component.literal("Удалить команду"));
        this.parentScreen = parentScreen;
        this.teamName = teamName;
        this.teamTag = teamTag;
    }

    private int left() { return (width - FON_W) / 2; }
    private int top()  { return (height - FON_H) / 2; }

    @Override
    protected void init() {
        super.init();

        int guiX = left();
        int guiY = top();

        int inputX = guiX + INPUT_U;   // теперь не сдвигаем – ширина уже 123
        int inputY = guiY + INPUT_V;

        inputField = new EditBox(font, inputX, inputY, INPUT_W, INPUT_H, Component.empty()) {
            @Override
            public void renderWidget(GuiGraphics g, int mx, int my, float pt) {
                if (!this.isVisible()) return;

                String text = this.getValue();
                int tw = font.width(text);
                int max = this.width - 8;          // небольшие отступы
                if (tw > max) text = font.plainSubstrByWidth(text, max);

                int tx = this.getX() + (this.width - font.width(text)) / 2;
                int ty = this.getY() + (this.height - 8) / 2;
                g.drawString(font, text, tx, ty, 0xFFFFFF, false);

                // мигающий курсор
                if (this.isFocused()) {
                    int curX = tx + font.width(text);
                    g.fill(curX, ty - 1, curX + 1, ty + 9, 0xFFD0D0D0);
                }
            }
        };

        inputField.setBordered(false);
        inputField.setMaxLength(50);
        inputField.setTextColor(0xFFFFFF);
        inputField.setResponder(text -> updateConfirmButton());
        addRenderableWidget(inputField);

        // Кнопка "Отмена" (прозрачная)
        addRenderableWidget(new Button(guiX + 19, guiY + 69, CANCEL_W, CANCEL_H,
                Component.empty(), b -> onCancel(), s -> Component.empty()) {
            {
                setTooltip(Tooltip.create(Component.literal("Отмена")));
            }

            @Override
            public void renderWidget(GuiGraphics g, int mx, int my, float pt) {
                if (isHovered()) {
                    g.fill(getX(), getY(), getX() + width, getY() + height, 0x30FFFFFF);
                }
            }
        });

        // Кнопка "Подтвердить" (изначально невидимая/неактивная)
        confirmButton = new Button(guiX + 83, guiY + 69, CONFIRM_W, CONFIRM_H,
                Component.empty(), b -> onConfirm(), s -> Component.empty()) {
            {
                setTooltip(Tooltip.create(Component.literal("Подтвердить удаление")));
            }

            @Override
            public void renderWidget(GuiGraphics g, int mx, int my, float pt) {
                // Рисуем текстуру только если поле ввода правильно заполнено
                if (isInputValid()) {
                    RenderSystem.setShaderTexture(0, ATLAS);
                    g.blit(ATLAS, getX(), getY(), CONFIRM_BTN_U, CONFIRM_BTN_V, CONFIRM_W, CONFIRM_H, 256, 256);
                }

                if (isHovered() && isInputValid()) {
                    g.fill(getX(), getY(), getX() + width, getY() + height, 0x30FFFFFF);
                }
            }
        };
        confirmButton.active = false;
        addRenderableWidget(confirmButton);
    }

    private void updateConfirmButton() {
        boolean valid = isInputValid();
        confirmButton.active = valid;
        confirmButton.visible = valid;
    }

    private boolean isInputValid() {
        // достаточно ввести СВОЙ ник (без тега)
        String expected = minecraft.player.getGameProfile().getName();
        return inputField.getValue().trim().equalsIgnoreCase(expected);
    }

    private void onCancel() {
        minecraft.setScreen(parentScreen);
    }

    private void onConfirm() {
        if (isInputValid()) {
            NetworkHandler.INSTANCE.sendToServer(new DeleteTeamPacket(teamName));
            minecraft.setScreen(parentScreen); // возвращаемся в TeamScreen
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        // 1. Рисуем CustomizationScreen в замороженном состоянии (ТОЛЬКО фон)
        if (this.parentScreen != null && this.parentScreen instanceof CustomizationScreen customization) {
            // Рисуем только фон CustomizationScreen, БЕЗ текста
            RenderSystem.setShaderTexture(0, CustomizationScreen.ATLAS);
            int customX = (width - CustomizationScreen.GUI_WIDTH) / 2;
            int customY = (height - CustomizationScreen.GUI_HEIGHT) / 2;
            g.blit(CustomizationScreen.ATLAS, customX, customY, 0, 0,
                    CustomizationScreen.GUI_WIDTH, CustomizationScreen.GUI_HEIGHT, 256, 256);
            // Текст команды НЕ рисуем!
        }

        // 2. Глубокое затемнение поверх
        this.renderBackground(g);

        // 3. Рисуем окно подтверждения удаления
        int x = left();
        int y = top();
        g.blit(ATLAS, x, y, FON_U, FON_V, FON_W, FON_H, 256, 256);

        // 4. ТЕКСТ КОМАНДЫ В ОКНЕ ПОДТВЕРЖДЕНИЯ (для сравнения)
        String teamText = teamName;
        if (teamTag != null && !teamTag.isEmpty()) {
            teamText += "[" + teamTag + "]";
        }

        if (font.width(teamText) > NAME_W) {
            teamText = font.plainSubstrByWidth(teamText, NAME_W - 6) + "..";
        }

        int textX = x + NAME_U + (NAME_W - font.width(teamText)) / 2;
        int textY = y + NAME_V + (NAME_H - 8) / 2;
        g.drawString(font, teamText, textX, textY, 0xFFFFFF, false);

        // 5. КНОПКИ и поле ввода
        super.render(g, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
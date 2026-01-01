package com.mom.teammod;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mom.teammod.packets.LeaveTeamPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class LeaveTeamScreen extends Screen {
    private static final ResourceLocation ATLAS = ResourceLocation.fromNamespaceAndPath(TeamMod.MODID,
            "textures/gui/leave_team.png");

    // Координаты из разметки
    public static final int FON_U = 0;
    public static final int FON_V = 0;
    public static final int FON_W = 147;  // 146.80 округляем до 147
    public static final int FON_H = 66;   // 65.80 округляем до 66

    // Кнопки (только для размеров - кнопки прозрачные)
    private static final int CANCEL_W = 39;  // 58.91 - 19.91 ≈ 39
    private static final int CANCEL_H = 11;  // 46.32 - 35.82 ≈ 11
    private static final int CONFIRM_W = 42; // 127.81 - 85.41 ≈ 42
    private static final int CONFIRM_H = 10; // 45.72 - 36.02 ≈ 10

    // Область для названия команды
    public static final int TEAM_NAME_U = 12;  // 11.58 округляем до 12
    public static final int TEAM_NAME_V = 21;  // 21.04 округляем до 21
    public static final int TEAM_NAME_W = 122; // 133.77 - 11.58 ≈ 122
    public static final int TEAM_NAME_H = 8;   // 28.67 - 21.04 ≈ 8

    private final Screen parentScreen;
    private final String teamName;
    private final String teamTag;

    public LeaveTeamScreen(Screen parentScreen, String teamName, String teamTag) {
        super(Component.literal("Покинуть команду"));
        this.parentScreen = parentScreen;
        this.teamName = teamName;
        this.teamTag = teamTag;
    }

    public int left() { return (width - FON_W) / 2; }
    public int top()  { return (height - FON_H) / 2; }

    @Override
    protected void init() {
        super.init();

        int guiX = left();
        int guiY = top();

        // Кнопка "Отмена" (прозрачная)
        addRenderableWidget(new Button(guiX + 20, guiY + 36, CANCEL_W, CANCEL_H,
                Component.empty(), b -> onCancel(), s -> Component.empty()) {
            {
                setTooltip(Tooltip.create(Component.literal("Отмена")));
            }

            @Override
            public void renderWidget(GuiGraphics g, int mx, int my, float pt) {
                // Прозрачная кнопка - только подсветка при наведении
                if (isHovered()) {
                    g.fill(getX(), getY(), getX() + width, getY() + height, 0x30FFFFFF);
                }
            }
        });

        // Кнопка "Подтвердить" (прозрачная)
        addRenderableWidget(new Button(guiX + 85, guiY + 36, CONFIRM_W, CONFIRM_H,
                Component.empty(), b -> onConfirm(), s -> Component.empty()) {
            {
                setTooltip(Tooltip.create(Component.literal("Подтвердить выход")));
            }

            @Override
            public void renderWidget(GuiGraphics g, int mx, int my, float pt) {
                // Прозрачная кнопка - только подсветка при наведении
                if (isHovered()) {
                    g.fill(getX(), getY(), getX() + width, getY() + height, 0x30FFFFFF);
                }
            }
        });
    }


    private void onConfirm() {
        NetworkHandler.INSTANCE.sendToServer(new LeaveTeamPacket(teamName));
        TeamScreen.returnToTeamScreen();
    }

    private void onCancel() {
        minecraft.setScreen(parentScreen);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        // Рисуем родительский экран (TeamMemberScreen) полностью
        if (parentScreen != null) {
            parentScreen.render(g, mouseX, mouseY, partialTick);
        }

        // Дополнительное затемнение
        g.fill(0, 0, width, height, 0xB3000000);

        // Рисуем своё окно
        int x = left();
        int y = top();
        g.blit(ATLAS, x, y, FON_U, FON_V, FON_W, FON_H, 256, 256);

        // Текст команды
        String teamText = teamName + (teamTag != null && !teamTag.isEmpty() ? "[" + teamTag + "]" : "");
        if (font.width(teamText) > TEAM_NAME_W) {
            teamText = font.plainSubstrByWidth(teamText, TEAM_NAME_W - 6) + "..";
        }
        int textX = x + TEAM_NAME_U + (TEAM_NAME_W - font.width(teamText)) / 2;
        int textY = y + TEAM_NAME_V + (TEAM_NAME_H - 8) / 2;
        g.drawString(font, teamText, textX, textY, 0xFFFFFF, false);

        super.render(g, mouseX, mouseY, partialTick); // кнопки
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) { // ESC
            onCancel();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
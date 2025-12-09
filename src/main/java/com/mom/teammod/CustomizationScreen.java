package com.mom.teammod;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mom.teammod.packets.DeleteTeamPacket;
import com.mom.teammod.packets.TeamSyncPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class CustomizationScreen extends Screen {

    public static final ResourceLocation ATLAS = ResourceLocation.fromNamespaceAndPath(TeamMod.MODID, "textures/gui/customization_background.png");

    // Пимпочка и кнопка "Готово"
    private static final int PIMP_U = 15, PIMP_V = 198, PIMP_W = 10, PIMP_H = 10;
    private static final int CONFIRM_U = 1, CONFIRM_V = 210, CONFIRM_W = 45, CONFIRM_H = 12;

    // Иконки тогглов
    private static final int TAG_U = 1,      TAG_V = 171,     TAG_W = 28,  TAG_H = 10;
    private static final int COMPASS_U = 1,  COMPASS_V = 182, COMPASS_W = 17, COMPASS_H = 14;
    private static final int FF_U = 1,       FF_V = 197,      FF_W = 12,   FF_H = 12;

    public static final int GUI_WIDTH = 256;
    public static final int GUI_HEIGHT = 170;

    private final Screen parent;
    private final TeamManager.Team team;

    // Исходное состояние
    private final boolean originalShowTag;
    private final boolean originalShowCompass;
    private final boolean originalFriendlyFire;

    // Текущее состояние
    private boolean showTag;
    private boolean showCompass;
    private boolean friendlyFire;

    private boolean isModified = false;
    private Button confirmButton;

    public CustomizationScreen(Screen parent, TeamManager.Team team) {
        super(Component.literal("Настройка команды"));
        this.parent = parent;
        this.team = team;

        this.originalShowTag = team.showTag();
        this.originalShowCompass = team.showCompass();
        this.originalFriendlyFire = team.isFriendlyFire();

        this.showTag = originalShowTag;
        this.showCompass = originalShowCompass;
        this.friendlyFire = originalFriendlyFire;
    }

    private int left() { return (width - GUI_WIDTH) / 2; }
    private int top()  { return (height - GUI_HEIGHT) / 2; }

    @Override
    protected void init() {
        super.init();
        int guiX = left();
        int guiY = top();

        // === КНОПКИ ВЫБОРА ЦВЕТА/ФИГУРЫ (clr1, clr2, shape) ===
        addTransparentButton(guiX + 18, guiY + 97, 32, 30, this::openColorPicker1, Component.literal("Цвет 1"));
        addTransparentButton(guiX + 58, guiY + 97, 32, 30, this::openColorPicker2, Component.literal("Цвет 2"));
        addTransparentButton(guiX + 98, guiY + 97, 32, 30, this::openLogoPicker, Component.literal("Фигура"));
        // === КНОПКА УДАЛЕНИЯ КОМАНДЫ (delete_team) ===
        addTransparentButton(guiX + 100 - 71, guiY + 100 - 39 + 90 - 12, 69, 12, this::deleteTeam, Component.literal("Удалить команду"));

        // === ТОГГЛЫ (FF → Compass → Tag) ===
        addToggle(guiX + 145, guiY + 106, 0, FF_U,      FF_V,      FF_W,      FF_H,      "Переключить режим дружественного огня");
        addToggle(guiX + 145, guiY + 76,  1, COMPASS_U, COMPASS_V, COMPASS_W, COMPASS_H, "Переключить отображение участников на компасе");
        addToggle(guiX + 145, guiY + 46,  2, TAG_U,     TAG_V,     TAG_W,     TAG_H,     "Переключить отображение тега команды");

        // 4 КНОПКИ ДОСТИЖЕНИЙ СПРАВА (25×25)
        addTransparentButton(guiX + 230 - 9, guiY - 10 - 11+8+5+22+12+2+4+2-1,  23, 23, this::openAchivPicker1, Component.literal("Достижение 1"));
        addTransparentButton(guiX + 230 - 9, guiY + 20 - 11+8+5+22+12+2+3+2-1,  23, 23, this::openAchivPicker2, Component.literal("Достижение 2"));
        addTransparentButton(guiX + 230 - 9, guiY + 50 - 11+8+5+22+12+2+2+2-1,  23, 23, this::openAchivPicker3, Component.literal("Достижение 3"));
        addTransparentButton(guiX + 230 - 9, guiY + 80 - 11+8+5+22+12+2+1+2-1, 23, 23, this::openAchivPicker4, Component.literal("Достижение 4"));

    }

    private void openColorPicker1() {
        ColorPickerScreen picker = new ColorPickerScreen() {
            @Override
            public void render(GuiGraphics g, int mx, int my, float pt) {
                // 1. Рисуем только фон мира (как в StatisticsScreen)
                CustomizationScreen.this.renderBackground(g);

                // 2. Рисуем ТОЛЬКО текстуру фона GUI — БЕЗ ТЕКСТА
                RenderSystem.setShaderTexture(0, ATLAS);
                int guiX = (width - 256) / 2;
                int guiY = (height - 170) / 2;
                g.blit(ATLAS, guiX, guiY, 0, 0, 256, 170, 256, 256);

                // ← ВСЁ. Текста нет. Никакого drawString. Полная тишина.

                // 3. Рисуем сам ColorPicker
                super.render(g, mx, my, pt);
            }

            @Override
            public void onClose() {
                minecraft.setScreen(CustomizationScreen.this);
            }
        };

        picker.init(minecraft, width, height);
        minecraft.setScreen(picker);
    }

    private void openColorPicker2() {
        ColorPickerScreen2 picker = new ColorPickerScreen2() {
            @Override
            public void render(GuiGraphics g, int mx, int my, float pt) {
                CustomizationScreen.this.renderBackground(g);
                RenderSystem.setShaderTexture(0, ATLAS);
                int guiX = (width - 256) / 2;
                int guiY = (height - 170) / 2;
                g.blit(ATLAS, guiX, guiY, 0, 0, 256, 170, 256, 256);
                super.render(g, mx, my, pt);
            }

            @Override
            public void onClose() {
                minecraft.setScreen(CustomizationScreen.this);
            }
        };

        picker.init(minecraft, width, height);
        minecraft.setScreen(picker);
    }

    private void openLogoPicker() {
        LogoPickerScreen picker = new LogoPickerScreen() {
            @Override
            public void render(GuiGraphics g, int mx, int my, float pt) {
                CustomizationScreen.this.renderBackground(g);
                RenderSystem.setShaderTexture(0, ATLAS);
                int guiX = (width - 256) / 2;
                int guiY = (height - 170) / 2;
                g.blit(ATLAS, guiX, guiY, 0, 0, 256, 170, 256, 256);
                super.render(g, mx, my, pt);
            }

            @Override
            public void onClose() {
                minecraft.setScreen(CustomizationScreen.this);
            }
        };

        picker.init(minecraft, width, height);
        minecraft.setScreen(picker);
    }

    private void openAchivPicker1() {
        AchievementPickerScreen1 picker = new AchievementPickerScreen1() {
            @Override
            public void render(GuiGraphics g, int mx, int my, float pt) {
                CustomizationScreen.this.renderBackground(g);
                RenderSystem.setShaderTexture(0, ATLAS);
                int guiX = (width - 256) / 2;
                int guiY = (height - 170) / 2;
                g.blit(ATLAS, guiX, guiY, 0, 0, 256, 170, 256, 256);
                super.render(g, mx, my, pt);
            }

            @Override
            public void onClose() {
                minecraft.setScreen(CustomizationScreen.this);
            }
        };
        picker.init(minecraft, width, height);
        minecraft.setScreen(picker);
    }

    private void openAchivPicker2() {
        AchievementPickerScreen2 picker = new AchievementPickerScreen2() {
            @Override public void render(GuiGraphics g, int mx, int my, float pt) {
                CustomizationScreen.this.renderBackground(g);
                RenderSystem.setShaderTexture(0, ATLAS);
                int guiX = (width - 256) / 2;
                int guiY = (height - 170) / 2;
                g.blit(ATLAS, guiX, guiY, 0, 0, 256, 170, 256, 256);
                super.render(g, mx, my, pt);
            }
            @Override public void onClose() { minecraft.setScreen(CustomizationScreen.this); }
        };
        picker.init(minecraft, width, height);
        minecraft.setScreen(picker);
    }

    private void openAchivPicker3() {
        AchievementPickerScreen3 picker = new AchievementPickerScreen3() {
            @Override public void render(GuiGraphics g, int mx, int my, float pt) {
                CustomizationScreen.this.renderBackground(g);
                RenderSystem.setShaderTexture(0, ATLAS);
                int guiX = (width - 256) / 2;
                int guiY = (height - 170) / 2;
                g.blit(ATLAS, guiX, guiY, 0, 0, 256, 170, 256, 256);
                super.render(g, mx, my, pt);
            }
            @Override public void onClose() { minecraft.setScreen(CustomizationScreen.this); }
        };
        picker.init(minecraft, width, height);
        minecraft.setScreen(picker);
    }

    private void openAchivPicker4() {
        AchievementPickerScreen4 picker = new AchievementPickerScreen4() {
            @Override public void render(GuiGraphics g, int mx, int my, float pt) {
                CustomizationScreen.this.renderBackground(g);
                RenderSystem.setShaderTexture(0, ATLAS);
                int guiX = (width - 256) / 2;
                int guiY = (height - 170) / 2;
                g.blit(ATLAS, guiX, guiY, 0, 0, 256, 170, 256, 256);
                super.render(g, mx, my, pt);
            }
            @Override public void onClose() { minecraft.setScreen(CustomizationScreen.this); }
        };
        picker.init(minecraft, width, height);
        minecraft.setScreen(picker);
    }

    private Button addTransparentButton(int x, int y, int w, int h, Runnable action, Component tooltip) {
        Button btn = new Button(x, y, w, h, Component.empty(), b -> action.run(), s -> Component.empty()) {
            @Override
            public void renderWidget(GuiGraphics g, int mx, int my, float pt) {
                if (isHovered()) g.fill(getX(), getY(), getX() + width, getY() + height, 0x30FFFFFF);
            }
        };
        btn.setTooltip(Tooltip.create(tooltip));
        return addRenderableWidget(btn);
    }

    private void addToggle(int x, int y, int index, int iconU, int iconV, int iconW, int iconH, String tooltip) {
        addRenderableWidget(new Button(x, y, 18, 18, Component.empty(), b -> {
            setState(index, !getState(index));
            checkModified();
        }, s -> Component.empty()) {
            { setTooltip(Tooltip.create(Component.literal(tooltip))); }

            @Override
            public void renderWidget(GuiGraphics g, int mx, int my, float pt) {
                if (isHovered()) g.fill(getX(), getY(), getX() + width, getY() + height, 0x30FFFFFF);

                if (getState(index)) {
                    RenderSystem.setShaderTexture(0, ATLAS);
                    g.blit(ATLAS, getX() + 4, getY() + 4, PIMP_U, PIMP_V, PIMP_W, PIMP_H, 256, 256);

                    int iconX = switch (index) {
                        case 0 -> getX() + 20 + 19; // FF
                        case 1 -> getX() + 20 + 8;  // Compass
                        case 2 -> getX() + 20 + 2;  // Tag
                        default -> getX() + 20;
                    };
                    int iconY = getY() + (18 - iconH) / 2 + (index == 0 ? -1 : 0);

                    g.blit(ATLAS, iconX, iconY, iconU, iconV, iconW, iconH, 256, 256);
                }
            }
        });
    }

    private boolean getState(int index) {
        return switch (index) {
            case 0 -> friendlyFire;
            case 1 -> showCompass;
            case 2 -> showTag;
            default -> false;
        };
    }

    private void setState(int index, boolean value) {
        switch (index) {
            case 0 -> friendlyFire = value;
            case 1 -> showCompass = value;
            case 2 -> showTag = value;
        }
    }

    private void checkModified() {
        boolean modified = showTag != originalShowTag ||
                showCompass != originalShowCompass ||
                friendlyFire != originalFriendlyFire;

        if (modified != isModified) {
            isModified = modified;
            updateConfirmButton();
        }
    }

    private void updateConfirmButton() {
        if (confirmButton != null) {
            removeWidget(confirmButton);
            confirmButton = null;
        }
        if (isModified) {
            int guiX = left();
            int guiY = top();
            confirmButton = new Button(guiX + 147, guiY + 139, CONFIRM_W, CONFIRM_H, Component.empty(), b -> applyChanges(), s -> Component.empty()) {
                @Override
                public void renderWidget(GuiGraphics g, int mx, int my, float pt) {
                    if (isHovered()) g.fill(getX(), getY(), getX() + width, getY() + height, 0x30FFFFFF);
                    RenderSystem.setShaderTexture(0, ATLAS);
                    g.blit(ATLAS, getX(), getY(), CONFIRM_U, CONFIRM_V, CONFIRM_W, CONFIRM_H, 256, 256);
                }
            };
            confirmButton.setTooltip(Tooltip.create(Component.literal("Применить изменения")));
            addRenderableWidget(confirmButton);
        }
    }

    private void applyChanges() {
        if (team != null) {
            team.setShowTag(showTag);
            team.setShowCompass(showCompass);
            team.setFriendlyFire(friendlyFire);
            NetworkHandler.INSTANCE.sendToServer(new TeamSyncPacket(team.getName()));
        }
        minecraft.setScreen(parent);
    }

    private void deleteTeam() {
        minecraft.setScreen(new DeleteTeamScreen(
                CustomizationScreen.this,
                team.getName(),
                team.getTag()
        ));
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);
        RenderSystem.setShaderTexture(0, ATLAS);
        g.blit(ATLAS, left(), top(), 0, 0, GUI_WIDTH, GUI_HEIGHT, 256, 256);

        // Текст: имя команды и тег (если включён)
        g.drawCenteredString(font, team.getName(), left() + 97, top() + 50, 0xFFFFFF);
        if (showTag && !team.getTag().isEmpty()) {
            g.drawCenteredString(font, team.getTag(), left() + 97, top() + 63, 0xFFFFFF);
        }

        super.render(g, mouseX, mouseY, partialTick);
    }

    @Override
    public void resize(Minecraft minecraft, int width, int height) {
        this.init(minecraft, width, height);
        // Ничего сохранять не нужно — всё пересоздаётся в init()
        // Кнопки, тогглы и превьюшки — всё привязано к новым координатам
    }
}
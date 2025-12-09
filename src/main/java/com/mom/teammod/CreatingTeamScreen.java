package com.mom.teammod;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mom.teammod.packets.CreateTeamPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class CreatingTeamScreen extends Screen {
    private static final ResourceLocation ATLAS = ResourceLocation.fromNamespaceAndPath(TeamMod.MODID, "textures/gui/create_team.png");

    // Точные координаты из атласа create_team.png (256×256)
    // Pimp (маленькая пимпочка — кнопка)
    private static final int PIMP_U = 15;
    private static final int PIMP_V = 198;
    private static final int PIMP_W = 10;
    private static final int PIMP_H = 10;
    private static final int BG_U          = 0;   // фон
    private static final int BG_V          = 0;
    private static final int BG_W          = 256;
    private static final int BG_H          = 170;

    private static final int CONFIRM_U     = 1;   // кнопка "Готово"
    private static final int CONFIRM_V     = 210;
    private static final int CONFIRM_W     = 45;
    private static final int CONFIRM_H     = 12;

    private static final int KN_TAG_U      = 1;   // иконка тега (верхняя кнопка)
    private static final int KN_TAG_V      = 171;
    private static final int KN_TAG_W      = 28;
    private static final int KN_TAG_H      = 10;

    private static final int KN_COMPASS_U  = 1;   // иконка компаса (средняя)
    private static final int KN_COMPASS_V  = 182;
    private static final int KN_COMPASS_W  = 17;
    private static final int KN_COMPASS_H  = 14;

    private static final int KN_FF_U       = 1;   // иконка FF (нижняя)
    private static final int KN_FF_V       = 197;
    private static final int KN_FF_W       = 12;
    private static final int KN_FF_H       = 12;

    private static final int SLASH_U       = 173; // перечёркивание
    private static final int SLASH_V       = 51;
    private static final int SLASH_W       = 17;
    private static final int SLASH_H       = 10;

    private static final int FFOFF_U       = 184; // крестик для выключенного тега
    private static final int FFOFF_V       = 108;
    private static final int FFOFF_W       = 13;
    private static final int FFOFF_H       = 13;

    private static final int GUI_WIDTH = 256;
    private static final int GUI_HEIGHT = 170;

    private final Screen parent;
    private EditBox nameBox, tagBox;
    private Button confirmBtn;

    // ПРОВЕРЬ ЭТИ НАСТРОЙКИ - они должны соответствовать:
    private boolean showTag = true;        // knopik1 - отображение тега команды
    private boolean showCompass = true;    // knopik2 - отображение компаса
    private boolean friendlyFire = true;   // knopik3 - дружественный огонь

    public CreatingTeamScreen(Screen parent) {
        super(Component.literal("Создать команду"));
        this.parent = parent;
    }

    private int left() { return (width - GUI_WIDTH) / 2; }
    private int top() { return (height - GUI_HEIGHT) / 2; }

    @Override
    protected void init() {
        super.init();

        int guiX = left();
        int guiY = top();

        // ПОЛЯ ВВОДА
        tagBox = new EditBox(font, guiX + 21, guiY + 142, 25, 12, Component.literal("")) {
            {
                setMaxLength(3);
                setHint(Component.literal(""));
                setBordered(false);
                setTextColor(0xFFFFFF);
                setResponder(s -> updateAll());
            }
        };
        tagBox.setTooltip(Tooltip.create(Component.literal("Тег должен быть длиной 3 символа!")));
        addRenderableWidget(tagBox);

        nameBox = new EditBox(font, guiX + 50, guiY + 142, 83, 12, Component.literal("")) {
            {
                setMaxLength(8);
                setHint(Component.literal(""));
                setBordered(false);
                setTextColor(0xFFFFFF);
                setResponder(s -> updateAll());
            }
        };
        nameBox.setTooltip(Tooltip.create(Component.literal("Название команды должно быть не короче 3 и не длиннее 8 символов!")));
        addRenderableWidget(nameBox);

        // КНОПКИ ЦВЕТОВ
        addTransparentButton(guiX + 18, guiY + 97, 32, 30, this::openColor1, Component.literal("Цвет 1"));
        addTransparentButton(guiX + 58, guiY + 97, 32, 30, this::openColor2, Component.literal("Цвет 2"));
        addTransparentButton(guiX + 98, guiY + 97, 32, 30, this::openShape, Component.literal("Фигура"));

        // ТОГГЛЫ - ВАЖНО: ПРОВЕРЬ ПОРЯДОК НАСТРОЕК
        // knopik1 (106) — теперь дружественный огонь
        addToggleButton(guiX + 145, guiY + 106, 18, 18, 0, KN_FF_U, KN_FF_V, KN_FF_W, KN_FF_H, "Переключить режим дружественного огня");

        // knopik2 (76) — компас
        addToggleButton(guiX + 145, guiY + 76, 18, 18, 1, KN_COMPASS_U, KN_COMPASS_V, KN_COMPASS_W, KN_COMPASS_H, "Переключить отображение участников на командном компасе");

        // knopik3 (46) — тег
        addToggleButton(guiX + 145, guiY + 46, 18, 18, 2, KN_TAG_U, KN_TAG_V, KN_TAG_W, KN_TAG_H, "Переключить отображение тега команды");
        updateAll();
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

    private void addToggleButton(int x, int y, int w, int h, int idx, int iconU, int iconV, int iconW, int iconH, String tip) {
        addRenderableWidget(new Button(x, y, w, h, Component.empty(), b -> {
            if (idx == 0) friendlyFire = !friendlyFire;
            if (idx == 1) showCompass = !showCompass;
            if (idx == 2) showTag = !showTag;
            updateAll();
        }, s -> Component.empty()) {
            {
                setTooltip(Tooltip.create(Component.literal(tip)));
            }

            @Override
            public void renderWidget(GuiGraphics g, int mx, int my, float pt) {
                if (isHovered()) {
                    g.fill(getX(), getY(), getX() + width, getY() + height, 0x30FFFFFF);
                }

                boolean enabled = (idx == 0 && friendlyFire) || (idx == 1 && showCompass) || (idx == 2 && showTag);

                if (enabled) {
                    // 1. Рисуем пимпочку (кнопка включена)
                    RenderSystem.setShaderTexture(0, ATLAS);
                    g.blit(ATLAS, getX() + 4, getY() + 4, PIMP_U, PIMP_V, PIMP_W, PIMP_H, 256, 256);

                    // 2. Рисуем иконку справа от пимпочки — с индивидуальной подгонкой
                    int iconX;
                    int iconY = getY() + (h - iconH) / 2; // по центру по вертикали (остаётся)

                    if (idx == 0) { // FF (нижняя кнопка)
                        iconX = getX() + 20 + 19; // +19 вправо
                        iconY -= 1;               // -1 вверх
                    } else if (idx == 1) { // Компас (средняя)
                        iconX = getX() + 20 + 8;  // +8 вправо
                    } else { // Тег (верхняя)
                        iconX = getX() + 20 + 2;  // +1 вправо
                    }

                    g.blit(ATLAS, iconX, iconY, iconU, iconV, iconW, iconH, 256, 256);
                }
                // Если выключено — ничего не рисуем
            }
        });
    }

    private void openColor1() {
        ColorPickerScreen picker = new ColorPickerScreen() {
            @Override
            public void render(GuiGraphics g, int mx, int my, float pt) {
                CreatingTeamScreen.this.renderBackground(g);
                RenderSystem.setShaderTexture(0, ATLAS);
                int guiX = (width - 256) / 2;
                int guiY = (height - 170) / 2;
                g.blit(ATLAS, guiX, guiY, 0, 0, 256, 170, 256, 256);
                super.render(g, mx, my, pt);
            }

            @Override
            public void onClose() {
                minecraft.setScreen(CreatingTeamScreen.this);
            }
        };
        picker.init(minecraft, width, height);
        minecraft.setScreen(picker);
    }

    private void openColor2() {
        ColorPickerScreen2 picker = new ColorPickerScreen2() {
            @Override
            public void render(GuiGraphics g, int mx, int my, float pt) {
                CreatingTeamScreen.this.renderBackground(g);
                RenderSystem.setShaderTexture(0, ATLAS);
                int guiX = (width - 256) / 2;
                int guiY = (height - 170) / 2;
                g.blit(ATLAS, guiX, guiY, 0, 0, 256, 170, 256, 256);
                super.render(g, mx, my, pt);
            }

            @Override
            public void onClose() {
                minecraft.setScreen(CreatingTeamScreen.this);
            }
        };
        picker.init(minecraft, width, height);
        minecraft.setScreen(picker);
    }

    private void openShape() {
        LogoPickerScreen picker = new LogoPickerScreen() {
            @Override
            public void render(GuiGraphics g, int mx, int my, float pt) {
                CreatingTeamScreen.this.renderBackground(g);
                RenderSystem.setShaderTexture(0, ATLAS);
                int guiX = (width - 256) / 2;
                int guiY = (height - 170) / 2;
                g.blit(ATLAS, guiX, guiY, 0, 0, 256, 170, 256, 256);
                super.render(g, mx, my, pt);
            }

            @Override
            public void onClose() {
                minecraft.setScreen(CreatingTeamScreen.this);
            }
        };
        picker.init(minecraft, width, height);
        minecraft.setScreen(picker);
    }

    private void updateConfirm() {
        if (confirmBtn != null) {
            removeWidget(confirmBtn);
            confirmBtn = null;
        }

        String name = nameBox.getValue().trim();
        String tag = tagBox.getValue().trim();

        // Должно быть: имя 3–8 символов, тег ровно 3
        if (name.length() >= 3 && name.length() <= 8 && tag.length() == 3) {
            int guiX = left();
            int guiY = top();

            confirmBtn = new Button(guiX + 147, guiY + 139, 45, 12, Component.empty(), b -> createTeam(), s -> Component.empty()) {
                @Override
                public void renderWidget(GuiGraphics g, int mx, int my, float pt) {
                    if (isHovered()) g.fill(getX(), getY(), getX() + width, getY() + height, 0x30FFFFFF);
                    RenderSystem.setShaderTexture(0, ATLAS);
                    g.blit(ATLAS, getX(), getY(), CONFIRM_U, CONFIRM_V, CONFIRM_W, CONFIRM_H, 256, 256);
                }
            };
            confirmBtn.setTooltip(Tooltip.create(Component.literal("закончить создание команды")));
            addRenderableWidget(confirmBtn);
        }
    }

    private void createTeam() {
        String teamName = nameBox.getValue().trim();
        NetworkHandler.INSTANCE.sendToServer(new CreateTeamPacket(teamName));
        minecraft.setScreen(parent);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        renderBackground(g);

        RenderSystem.setShaderTexture(0, ATLAS);
        int guiX = left();
        int guiY = top();

        // Только фон
        g.blit(ATLAS, guiX, guiY, BG_U, BG_V, GUI_WIDTH, GUI_HEIGHT, 256, 256);

        // Превью ника
        String nameText = nameBox.getValue();
        if (!nameText.isEmpty()) {
            g.drawCenteredString(font, nameText, guiX + 97, guiY + 50, 0xFFFFFF);
        }

        // Превью тега — посимвольно
        String tagText = tagBox.getValue();
        if (!tagText.isEmpty()) {
            g.drawCenteredString(font, tagText, guiX + 97, guiY + 63, 0xFFFFFF);
        }

        super.render(g, mx, my, pt);
    }

    private void updateAll() {
        updateConfirm();
    }

    @Override
    public boolean keyPressed(int k, int s, int m) {
        if (k == 257) {
            if (nameBox.isFocused()) {
                nameBox.setFocused(false);
                return true;
            }
            if (tagBox.isFocused()) {
                tagBox.setFocused(false);
                return true;
            }
        }

        if (nameBox.isFocused() || tagBox.isFocused()) {
            if (nameBox.keyPressed(k, s, m) || tagBox.keyPressed(k, s, m)) {
                updateAll();
                return true;
            }
        }
        return super.keyPressed(k, s, m);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int b) {
        nameBox.mouseClicked(mx, my, b);
        tagBox.mouseClicked(mx, my, b);
        return super.mouseClicked(mx, my, b);
    }

    @Override
    public void resize(Minecraft minecraft, int width, int height) {
        String savedName = this.nameBox != null ? this.nameBox.getValue() : "";
        String savedTag  = this.tagBox  != null ? this.tagBox.getValue()  : "";

        this.init(minecraft, width, height);

        // Восстанавливаем текст в полях ввода
        if (this.nameBox != null) this.nameBox.setValue(savedName);
        if (this.tagBox  != null) this.tagBox.setValue(savedTag);

        updateAll(); // обновляем кнопку "Готово"
    }
}
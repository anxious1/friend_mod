package com.mom.teammod;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mom.teammod.packets.DeleteTeamPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import org.checkerframework.checker.signature.qual.Identifier;


import java.util.Set;
import java.util.UUID;

public class TeamProfileOwner extends Screen {

    // ОДИН ЕДИНСТВЕННЫЙ АТЛАС
    private static final ResourceLocation ATLAS = ResourceLocation.fromNamespaceAndPath(TeamMod.MODID,
            "textures/gui/team_profile_owner_background.png");

    // Координаты из твоих XML-файлов (округлённые)
    private static final int TAG_U      = 1;   // tag
    private static final int TAG_V      = 207;
    private static final int TAG_W      = 28;
    private static final int TAG_H      = 10;

    private static final int COMPASS_U  = 1;   // compass
    private static final int COMPASS_V  = 231;
    private static final int COMPASS_W  = 15;
    private static final int COMPASS_H  = 14;

    private static final int FFON_U     = 1;   // ffon
    private static final int FFON_V     = 218;
    private static final int FFON_W     = 12;
    private static final int FFON_H     = 12;

    private static final int ZAMOK_U    = 30;  // замок (из правки)
    private static final int ZAMOK_V    = 207;
    private static final int ZAMOK_W    = 9;
    private static final int ZAMOK_H    = 9;

    private static final int GUI_WIDTH  = 256;
    private static final int GUI_HEIGHT = 170;

    // Твои старые смещения — оставлены 1 в 1
    private static final int OFFSET_X = 240;
    private static final int OFFSET_Y = 141;

    private final String teamName;
    private final String teamTag;
    private final boolean showTag;
    private final boolean showCompass;
    private final boolean friendlyFire;

    // Добавь эти константы в начало класса (после других)
    private static final int SCROLL_U = 14;   // scroll из разметки
    private static final int SCROLL_V = 218;
    private static final int SCROLL_W = 7;
    private static final int SCROLL_H = 12;

    private static final int ONLINE_U = 1;    // online_player
    private static final int ONLINE_V = 191;
    private static final int ONLINE_W = 78;
    private static final int ONLINE_H = 15;

    private static final int LEADER_U = 1;    // leader_metka
    private static final int LEADER_V = 246;
    private static final int LEADER_W = 32;
    private static final int LEADER_H = 7;
    private final Button[] playerButtons = new Button[9];

    private int scrollOffset = 0;
    private boolean isDraggingScroller = false;

    // Новое: состояние замка (по умолчанию закрыт — только по приглашению)
    private boolean inviteOnly = true;

    public TeamProfileOwner(TeamMenu menu, Inventory playerInventory, Component title,
                            String teamName, String teamTag,
                            boolean showTag, boolean showCompass, boolean friendlyFire) {
        super(title);
        this.teamName = teamName;
        this.teamTag = teamTag;
        this.showTag = showTag;
        this.showCompass = showCompass;
        this.friendlyFire = friendlyFire;
    }

    private int left() { return (width - GUI_WIDTH) / 2; }
    private int top()  { return (height - GUI_HEIGHT) / 2; }

    private int[] getOnlineAndTotalPlayers() {
        TeamManager.Team team = TeamManager.clientTeams.get(teamName);
        if (team == null) return new int[]{0, 0};
        Set<UUID> members = team.getMembers();
        int online = 0;
        for (UUID id : members) {
            if (minecraft.level.getPlayerByUUID(id) != null) online++;
        }
        return new int[]{online, members.size()};
    }

    @Override
    protected void init() {
        super.init();
        scrollOffset = 0;

        int guiX = left();
        int guiY = top();

        // Твои старые кнопки
        addRenderableWidget(Button.builder(
                Component.translatable("gui.teammod.back_to_inventory"),
                b -> minecraft.setScreen(new InventoryScreen(minecraft.player))
        ).pos(guiX + 10 + OFFSET_X/4, guiY - 30 + OFFSET_Y/4).size(100, 20).build());

        addTransparentButton(guiX - 2 - 2 + OFFSET_X/4, guiY + 31 - 1 + OFFSET_Y/4, 27, 11,
                this::openPlayersList, Component.literal("Список участников"));

        addTransparentButton(guiX + (67-7+9) + OFFSET_X/4, guiY + 105+1 + OFFSET_Y/4, 56, 11,
                this::openCustomization, Component.empty());

        addToggleLockButton(guiX + 118 - 72 - 6 - 9 +4 + OFFSET_X/4 - 2, guiY + 90 - 42 - 20 +4 + OFFSET_Y/4 - 1, 9, 9);

        // ── СОЗДАЁМ 9 ОТДЕЛЬНЫХ КНОПОК-ЯЧЕЕК (КАЖДАЯ НА СВОЁМ МЕСТЕ) ─────────────────────
        int baseX = guiX + 10;
        int baseY = guiY + 42;
        int cellX = baseX + 21 - 9;
        int cellY = baseY + 20 + 4 + 15;

        Player owner = minecraft.player;
        String[] testNames = {"Alex", "Notch", "Jeb", "Dinnerbone", "Grum", "Herobrine", "Steve", "Creeper"};
        boolean[] testOnline = {true, true, true, true, false, false, false, false};

        for (int i = 0; i < 9; i++) {
            final int index = i;
            boolean isOwner = (i == 0);
            String name = isOwner ? owner.getName().getString() : testNames[i - 1];
            boolean online = isOwner || testOnline[i - 1];

            int buttonY = cellY + i * (ONLINE_H + 1);

            Button playerButton = new Button(cellX, buttonY, ONLINE_W, ONLINE_H,
                    Component.empty(), b -> System.out.println("Клик по игроку: " + name), s -> Component.empty()) {
                @Override
                public void renderWidget(GuiGraphics g, int mx, int my, float pt) {
                    if (!this.visible) return;

                    int bgV = online ? ONLINE_V : 175;
                    g.blit(ATLAS, getX(), getY(), ONLINE_U, bgV, ONLINE_W, ONLINE_H, 256, 256);

                    ResourceLocation skin = minecraft.getSkinManager().getInsecureSkinLocation(owner.getGameProfile());
                    int headX = getX() + 3;
                    int headY = getY() + (ONLINE_H - 8) / 2;
                    g.blit(skin, headX, headY, 8, 8, 8, 8, 8, 8, 64, 64);
                    RenderSystem.enableBlend();
                    g.blit(skin, headX, headY, 8, 8, 40, 8, 8, 8, 64, 64);
                    RenderSystem.disableBlend();

                    String tagPart = (showTag && teamTag != null && !teamTag.isEmpty()) ? "[" + teamTag + "]" : "";
                    String fullText = name + tagPart;
                    if (font.width(fullText) > ONLINE_W - 22) {
                        fullText = font.plainSubstrByWidth(fullText, ONLINE_W - 25) + "..";
                    }
                    g.drawString(font, fullText, getX() + 14, getY() + 4, 0xFFFFFF, false);
                    if (isOwner) {
                        g.blit(ATLAS, getX() + ONLINE_W - LEADER_W - 2, getY() + ONLINE_H - LEADER_H - 1,
                                LEADER_U, LEADER_V, LEADER_W, LEADER_H, 256, 256);
                    }

                    if (isHovered()) {
                        g.fill(getX(), getY(), getX() + width, getY() + height, 0x30FFFFFF);
                    }
                }
            };

            updateVisibleButtons();
            playerButton.visible = (i < 3); // изначально видны первые 3
            playerButtons[i] = playerButton;
            addRenderableWidget(playerButton);
        }
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

    private void addToggleLockButton(int x, int y, int w, int h) {
        addRenderableWidget(new Button(x, y, w, h, Component.empty(), b -> {
            inviteOnly = !inviteOnly;
            // TODO: потом подключишь пакет SetInviteOnlyPacket
        }, s -> Component.empty()) {
            {
                setTooltip(Tooltip.create(Component.literal(
                        "Доступ в команду:\n§aВкл§r — только по приглашению\n§cВыкл§r — свободный доступ")));
            }

            @Override
            public void renderWidget(GuiGraphics g, int mx, int my, float pt) {
                if (isHovered()) {
                    g.fill(getX(), getY(), getX() + width, getY() + height, 0x30FFFFFF);
                }

                if (inviteOnly) {
                    RenderSystem.setShaderTexture(0, ATLAS);
                    // Рисуем текстуру точно в границах кнопки, без смещения
                    g.blit(ATLAS, getX(), getY(), ZAMOK_U, ZAMOK_V, ZAMOK_W, ZAMOK_H, 256, 256);
                }
                // если выключен — ничего не рисуется
            }
        });
    }

    private void deleteTeam() {
        if (minecraft.player != null) {
            TeamManager.Team team = TeamManager.clientTeams.get(teamName);
            if (team != null && team.getOwner().equals(minecraft.player.getUUID())) {
                NetworkHandler.INSTANCE.sendToServer(new DeleteTeamPacket(teamName));
            }
        }
        minecraft.setScreen(new InventoryScreen(minecraft.player));
    }

    private void openPlayersList() {
        minecraft.setScreen(new PlayersListScreen(this, teamName));
    }

    private void openCustomization() {
        System.out.println("=== ОТКРЫТИЕ КАСТОМИЗАЦИИ ===");
        System.out.println("teamName = " + teamName);
        System.out.println("clientTeams содержит: " + TeamManager.clientTeams.keySet());

        TeamManager.Team team = TeamManager.clientTeams.get(teamName);
        if (team == null) {
            System.out.println("ОШИБКА: команда не найдена на клиенте!");
            return;
        }

        System.out.println("Команда найдена! showTag = " + team.showTag() + ", showCompass = " + team.showCompass());
        System.out.println("Открываем CustomizationScreen...");

        try {
            minecraft.setScreen(new CustomizationScreen(this, team));
            System.out.println("Экран успешно открыт!");
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("ОШИБКА при создании CustomizationScreen!");
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);
        renderBg(g, partialTick, mouseX, mouseY);

        int guiX = left();
        int guiY = top();

        // ── Твой старый код (оставляем без изменений) ─────────────────────────────
        g.drawCenteredString(font, teamName, guiX + 19 + OFFSET_X/4 - 2, guiY + OFFSET_Y/4 - 2, 0xFFFFFF);

        if (teamTag != null && !teamTag.isEmpty() && showTag) {
            g.drawCenteredString(font, teamTag, guiX + 19 + OFFSET_X/4 - 2, guiY + 26 + OFFSET_Y/4 - 15, 0xFFFFFF);
        }

        int[] stats = getOnlineAndTotalPlayers();
        g.drawCenteredString(font, stats[0] + "/" + stats[1], guiX + 118 + OFFSET_X/4 - 2, guiY + 13 + OFFSET_Y/4 + 2, 0xFFFFFF);

        RenderSystem.setShaderTexture(0, ATLAS);
        if (showTag)      g.blit(ATLAS, guiX + 118 - 14 + OFFSET_X/4 - 2, guiY + 34 + OFFSET_Y/4 - 1, TAG_U,    TAG_V,    TAG_W,    TAG_H,    256, 256);
        if (showCompass)  g.blit(ATLAS, guiX + 118 - 7  + OFFSET_X/4 - 2, guiY + 51 + OFFSET_Y/4 - 1, COMPASS_U, COMPASS_V, COMPASS_W, COMPASS_H, 256, 256);
        if (friendlyFire) g.blit(ATLAS, guiX + 118 - 6 + OFFSET_X/4 - 2, guiY + 72 + OFFSET_Y/4 - 1, FFON_U,    FFON_V,    FFON_W,    FFON_H,    256, 256);

        // ── ПОЛЗУНОК (единственное, что рисуем вручную) ─────────────────────────────
        int baseX = guiX + 10;
        int baseY = guiY + 42;

        int trackHeight = 46;
        int maxScroll = Math.max(0, 9 - 3);
        int scrollerOffset = maxScroll == 0 ? 0 :
                (int)((float)scrollOffset / maxScroll * (trackHeight - SCROLL_H));

        g.blit(ATLAS, baseX + 13 - 8, baseY + 5 + 20 + 4 + 10 + scrollerOffset,
                SCROLL_U, SCROLL_V, SCROLL_W, SCROLL_H, 256, 256);

        // ── ВСЁ ОСТАЛЬНОЕ РИСУЮТ КНОПКИ САМИ (super.render) ───────────────────────
        super.render(g, mouseX, mouseY, partialTick);
    }

    protected void renderBg(GuiGraphics g, float pt, int mx, int my) {
        RenderSystem.setShaderTexture(0, ATLAS);
        g.blit(ATLAS, left(), top(), 0, 0, GUI_WIDTH, GUI_HEIGHT, 256, 256);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaY) {
        if (9 <= 3) return false;

        int oldOffset = scrollOffset;
        scrollOffset -= (int) deltaY;
        scrollOffset = Math.max(0, Math.min(scrollOffset, 9 - 3));

        if (oldOffset != scrollOffset) {
            updateVisibleButtons();
        }
        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int trackX = left() + 21 - 9 - 9;
        int trackY = top() + 20 + 4 + 15 + 18;

        if (mouseX >= trackX && mouseX <= trackX + 7 && mouseY >= trackY && mouseY <= trackY + 50) {
            isDraggingScroller = true;
            updateScrollFromMouse(mouseY);
            return true;
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

    private void updateScrollFromMouse(double mouseY) {
        int trackY = top() + 20 + 4 + 15 + 18;
        double rel = mouseY - trackY;
        rel = Math.max(0, Math.min(rel, 50 - 12));

        float ratio = (float) rel / (50 - 12);
        int maxScroll = 9 - 3;
        int newOffset = Math.round(ratio * maxScroll);
        newOffset = Math.max(0, Math.min(newOffset, maxScroll));

        if (scrollOffset != newOffset) {
            scrollOffset = newOffset;
            updateVisibleButtons();
        }
    }

    private void updateVisibleButtons() {
        int baseY = top() + 42 + 20 + 4 + 15;  // это твоя cellY (точно та же координата, что в init())

        for (int i = 0; i < 9; i++) {
            if (playerButtons[i] != null) {
                // Считаем, на каком месте в видимой области должен быть этот игрок
                int visibleIndex = i - scrollOffset; // от -scrollOffset до 8-scrollOffset

                if (visibleIndex >= 0 && visibleIndex < 3) {
                    // Эта кнопка должна быть видима — ставим её в правильный слот (0, 1 или 2)
                    playerButtons[i].setY(baseY + visibleIndex * (ONLINE_H + 1));
                    playerButtons[i].visible = true;
                } else {
                    // Скрываем все остальные (можно даже не двигать, но лучше спрятать)
                    playerButtons[i].visible = false;
                }
            }
        }
    }
}
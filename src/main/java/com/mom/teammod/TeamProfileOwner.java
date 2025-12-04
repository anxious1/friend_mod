package com.mom.teammod;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mom.teammod.packets.DeleteTeamPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;

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
        int guiX = left();
        int guiY = top();

        // Кнопки — 100% как у тебя было
        addRenderableWidget(Button.builder(
                Component.translatable("gui.teammod.back_to_inventory"),
                b -> minecraft.setScreen(new InventoryScreen(minecraft.player))
        ).pos(guiX + 10 + OFFSET_X/4, guiY - 30 + OFFSET_Y/4).size(100, 20).build());

        addTransparentButton(guiX - 2 - 2 + OFFSET_X/4, guiY + 31 - 1 + OFFSET_Y/4, 27, 11,
                this::openPlayersList, Component.literal("Список участников"));

        addTransparentButton(guiX + (67-7+9) + OFFSET_X/4, guiY + 105+1 + OFFSET_Y/4,  56, 11,
                this::openCustomization, Component.literal("Кастомизация"));

        // НОВАЯ КНОПКА-ЗАМОК (по твоим правилам: прозрачная при выключении)
        addToggleLockButton(guiX + 118 - 72 - 6 - 9 +4 + OFFSET_X/4 - 2, guiY + 90 - 42 - 20 +4 + OFFSET_Y/4 - 1, 9, 9);
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
        // 100% твой старый код — без единого изменения
        ResourceLocation bg = ResourceLocation.fromNamespaceAndPath(TeamMod.MODID, "textures/gui/players_list_background.png");
        minecraft.setScreen(new Screen(Component.literal("Список участников")) {
            @Override protected void init() {
                int x = (width - 209) / 2;
                int y = (height - 170) / 2;
                addRenderableWidget(Button.builder(Component.literal("Назад"), b -> minecraft.setScreen(TeamProfileOwner.this))
                        .pos(x + 10, y + 140).size(100, 20).build());

                TeamManager.Team team = TeamManager.clientTeams.get(teamName);
                if (team != null) {
                    int memberY = y + 20;
                    for (UUID memberId : team.getMembers()) {
                        Player p = minecraft.level.getPlayerByUUID(memberId);
                        String name = p != null ? p.getName().getString() : "???";
                        String status = p != null ? "§aOnline" : "§cOffline";
                        boolean owner = memberId.equals(team.getOwner());
                        addRenderableWidget(Button.builder(
                                Component.literal((owner ? name + " §6[Owner]" : name) + " " + status),
                                b -> { if (p != null && !memberId.equals(minecraft.player.getUUID())) {
                                    minecraft.setScreen(new OtherProfileScreen(memberId, this, Component.literal("Профиль " + name)));
                                }}
                        ).pos(x + 20, memberY).size(169, 20).build());
                        memberY += 25;
                    }
                }
            }

            @Override
            public void render(GuiGraphics g, int mx, int my, float pt) {
                TeamProfileOwner.this.renderBackground(g);
                TeamProfileOwner.this.renderBg(g, pt, mx, my);
                g.blit(bg, (width - 209)/2, (height - 170)/2, 0, 0, 209, 170);
                g.drawCenteredString(font, "Участники " + teamName, width / 2, (height - 170)/2 + 10, 0xFFFFFF);
                super.render(g, mx, my, pt);
            }

            @Override public void onClose() { minecraft.setScreen(TeamProfileOwner.this); }
            @Override public boolean isPauseScreen() { return false; }
        });
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

        // ВСЁ — 1 в 1 как у тебя было, только текстуры из атласа
        g.drawCenteredString(font, teamName, guiX + 19 + OFFSET_X/4 - 2, guiY + OFFSET_Y/4 - 2, 0xFFFFFF);

        if (teamTag != null && !teamTag.isEmpty() && showTag) {
            g.drawCenteredString(font, teamTag, guiX + 19 + OFFSET_X/4 - 2, guiY + 26 + OFFSET_Y/4 - 15, 0xFFFFFF);
        }

        int[] stats = getOnlineAndTotalPlayers();
        g.drawCenteredString(font, stats[0] + "/" + stats[1], guiX + 118 + OFFSET_X/4 - 2, guiY + 13 + OFFSET_Y/4 + 2, 0xFFFFFF);

        RenderSystem.setShaderTexture(0, ATLAS);

        if (showTag) {
            g.blit(ATLAS, guiX + 118 - 14 + OFFSET_X/4 - 2, guiY + 34 + OFFSET_Y/4 - 1,
                    TAG_U, TAG_V, TAG_W, TAG_H, 256, 256);
        }
        if (showCompass) {
            g.blit(ATLAS, guiX + 118 - 7 + OFFSET_X/4 - 2, guiY + 51 + OFFSET_Y/4 - 1,
                    COMPASS_U, COMPASS_V, COMPASS_W, COMPASS_H, 256, 256);
        }
        if (friendlyFire) {
            g.blit(ATLAS, guiX + 118 - 6 + OFFSET_X/4 - 2, guiY + 72 + OFFSET_Y/4 - 1,
                    FFON_U, FFON_V, FFON_W, FFON_H, 256, 256);
        }

        // Замок рисуется только в кнопке (в addToggleLockButton)

        super.render(g, mouseX, mouseY, partialTick);
    }

    protected void renderBg(GuiGraphics g, float pt, int mx, int my) {
        RenderSystem.setShaderTexture(0, ATLAS);
        g.blit(ATLAS, left(), top(), 0, 0, GUI_WIDTH, GUI_HEIGHT, 256, 256);
    }
}
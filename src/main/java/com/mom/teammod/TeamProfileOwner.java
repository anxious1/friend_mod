package com.mom.teammod;

import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mom.teammod.packets.DeleteTeamPacket;
import com.mom.teammod.packets.RequestProfilePacket;
import com.mom.teammod.packets.SetInviteOnlyPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.checkerframework.checker.signature.qual.Identifier;


import java.util.*;

public class TeamProfileOwner extends BaseModScreen {

    // ОДИН ЕДИНСТВЕННЫЙ АТЛАС
    private static final ResourceLocation ATLAS = ResourceLocation.fromNamespaceAndPath(TeamMod.MODID,
            "textures/gui/team_profile_owner_background.png");
    private static final int TEAM_BAR_U = 0;
    private static final int TEAM_BAR_V = 170;
    private static final int TEAM_BAR_W = 81;
    private static final int TEAM_BAR_H = 5;
    private final Map<UUID, Button> uuidToButton = new HashMap<>();
    // Координаты из твоих XML-файлов (округлённые)
    private static final int TAG_U      = 1;   // tag
    private static final int TAG_V      = 207;
    private static final int TAG_W      = 28;
    private static final int TAG_H      = 10;
    private int nameCheckTick = 0;
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

    public final String teamName;
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
    private final List<Button> playerButtons = new ArrayList<>();
    /* --------------- ХЕЛПЕРЫ ДЛЯ КЕША --------------- */
    /* внутри хелпера getNameSafe() */
    private String getNameSafe(UUID id) {
        GameProfile gp = ClientPlayerCache.getGameProfile(id);
        if (gp == null || "Unknown".equals(gp.getName())) {
            // Запрашиваем у сервера
            NetworkHandler.INSTANCE.sendToServer(new RequestProfilePacket(id));
            return "Loading..."; // или вернуть временно "Unknown", но не хранить это
        }
        return gp.getName();
    }
    private boolean isOnline(UUID id) {
        return ClientPlayerCache.isOnline(id);
    }
    private GameProfile getProfileSafe(UUID id){
        return ClientPlayerCache.getGameProfile(id);
    }
    private int scrollOffset = 0;
    private boolean isDraggingScroller = false;

    // Новое: состояние замка (по умолчанию закрыт — только по приглашению)
    private boolean inviteOnly = true;

    public TeamProfileOwner(Screen parentScreen, TeamMenu menu, Inventory playerInventory, Component title,
                            String teamName, String teamTag, boolean showTag, boolean showCompass, boolean friendlyFire) {
        super(parentScreen, title);
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
        long online = team.getMembers()
                .stream()
                .filter(ClientPlayerCache::isOnline)
                .count();
        return new int[]{(int) online, team.getMembers().size()};
    }

    @Override
    protected void init() {
        super.init();
        scrollOffset = 0;
        int guiX = left();
        int guiY = top();
        int baseU = guiY - 26;

        ResourceLocation unpress = ResourceLocation.fromNamespaceAndPath(TeamMod.MODID, "textures/gui/unpress.png");
        ResourceLocation press   = ResourceLocation.fromNamespaceAndPath(TeamMod.MODID, "textures/gui/press.png");

        ResourceLocation INV_ICON       = ResourceLocation.fromNamespaceAndPath(TeamMod.MODID, "textures/gui/inv_icon.png");
        ResourceLocation TEAM_LIST_ICON = ResourceLocation.fromNamespaceAndPath(TeamMod.MODID, "textures/gui/team_list_icon.png");
        ResourceLocation PROFILE_ICON   = ResourceLocation.fromNamespaceAndPath(TeamMod.MODID, "textures/gui/profile_icon.png");

// === КНОПКА ИНВЕНТАРЬ ===
        this.addRenderableWidget(new ImageButton(guiX + 2, baseU, 26, 27, 0, 0, 0, unpress, button -> {
            minecraft.setScreen(new InventoryScreen(minecraft.player));
        }) {
            private boolean isPressed = false;
            @Override public void renderWidget(GuiGraphics g, int mx, int my, float pt) {
                boolean active = this.isHovered() || isPressed;
                ResourceLocation tex = active ? press : unpress;
                int h = active ? 29 : 27;
                int yOff = active ? -2 : 0;
                if (this.getHeight() != h) { this.setHeight(h); this.setY(baseU + yOff); }
                g.blit(tex, getX(), getY(), 0, 0, 26, h, 26, h);
                g.blit(INV_ICON, getX() + 5, getY() + (active ? 7 : 6), 0, 0, 16, 16, 16, 16);
                if (this.isHovered()) g.renderTooltip(font, Component.translatable("gui.teammod.inventory"), mx, my);
            }
            @Override public void onClick(double mx, double my) { super.onClick(mx, my); this.isPressed = true; }
        });

// === КНОПКА КОМАНДЫ — ЗАЖАТА ===
        int teamX = guiX + 2 + 26 + 52;
        this.addRenderableWidget(new ImageButton(teamX, baseU - 2, 26, 29, 0, 0, 0, press, btn -> {}) {
            @Override public void renderWidget(GuiGraphics g, int mx, int my, float pt) {
                g.blit(press, getX(), getY(), 0, 0, 26, 29, 26, 29);
                g.blit(TEAM_LIST_ICON, getX() + 5, getY() + 6, 0, 0, 16, 16, 16, 16);
                if (this.isHovered()) g.renderTooltip(font, Component.translatable("gui.teammod.team_tab"), mx, my);
            }
        });

// === КНОПКА ПРОФИЛЬ ===
        int profileX = teamX + 26;
        this.addRenderableWidget(new ImageButton(profileX, baseU, 26, 27, 0, 0, 0, unpress, button -> {
            minecraft.setScreen(new MyProfileScreen(this, Component.translatable("gui.teammod.profile")));
        }) {
            private boolean isPressed = false;
            @Override public void renderWidget(GuiGraphics g, int mx, int my, float pt) {
                boolean active = this.isHovered() || isPressed;
                ResourceLocation tex = active ? press : unpress;
                int h = active ? 29 : 27;
                int yOff = active ? -2 : 0;
                if (this.getHeight() != h) { this.setHeight(h); this.setY(baseU + yOff); }
                g.blit(tex, getX(), getY(), 0, 0, 26, h, 26, h);
                g.blit(PROFILE_ICON, getX() + 5, getY() + (active ? 7 : 6), 0, 0, 16, 16, 16, 16);
                if (this.isHovered()) g.renderTooltip(font, Component.translatable("gui.teammod.profile"), mx, my);
            }
            @Override public void onClick(double mx, double my) { super.onClick(mx, my); this.isPressed = true; }
        });

        addTransparentButton(guiX - 2 - 2 + OFFSET_X/4, guiY + 31 - 1 + OFFSET_Y/4, 27, 11,
                this::openPlayersList, Component.literal("Список участников"));

        addTransparentButton(guiX + (67-7+9) + OFFSET_X/4 - 5, guiY + 105+1 + OFFSET_Y/4 - 1, 56, 11,
                this::openCustomization, Component.empty());

        addToggleLockButton(guiX + 118 - 72 - 6 - 9 +4 + OFFSET_X/4 - 2, guiY + 90 - 42 - 20 +4 + OFFSET_Y/4 - 1, 9, 9);

        // === ДИНАМИЧЕСКИЙ СПИСОК УЧАСТНИКОВ (БЕЗ ЛИМИТА) ===
        TeamManager.Team team = TeamManager.clientTeams.get(teamName);
        List<UUID> members = new ArrayList<>();
        UUID ownerId = null;
        if (team != null) {
            ownerId = team.getOwner();
            members.addAll(team.getMembers());
        }

        // Владелец всегда первый
        if (ownerId != null && members.remove(ownerId)) {
            members.add(0, ownerId);
        }

        int baseX = guiX + 10;
        int baseY = guiY + 42 + 14;
        int cellX = baseX + 21 - 9;
        int slotHeight = ONLINE_H + 1; // высота одного слота

        // Очищаем старые кнопки
        playerButtons.forEach(this::removeWidget);
        playerButtons.clear();

        // Создаём кнопки для всех участников
        for (int i = 0; i < members.size(); i++) {
            UUID playerId = members.get(i);

            String name = getNameSafe(playerId);
            if ("Loading...".equals(name)) {
                ClientPlayerCache.loadQueue.offer(playerId);
            }
            boolean online   = isOnline(playerId);             // ONLINE / AFK / OFFLINE
            boolean isOwner  = playerId.equals(ownerId);

            int buttonY = baseY + 20 + 4 + i * slotHeight;

            final UUID finalPlayerId = playerId;
            final String finalName   = name;

            Button playerButton = new Button(cellX, buttonY, ONLINE_W, ONLINE_H,
                    Component.empty(), b -> {
                if (finalPlayerId.equals(minecraft.player.getUUID())) {
                    minecraft.setScreen(new MyProfileScreen(TeamProfileOwner.this,
                            Component.translatable("gui.teammod.profile")));
                } else {
                    minecraft.setScreen(new OtherPlayerProfileScreen(TeamProfileOwner.this,
                            finalPlayerId, Component.literal("Профиль " + finalName)));
                }
            }, s -> Component.empty()) {

                @Override
                public void renderWidget(GuiGraphics g, int mx, int my, float pt) {
                    if (!this.visible) return;

                    /* фон слота */
                    int bgV = online ? ONLINE_V : 175;           // 175 – ваша «оффлайн»-секция в атласе
                    g.blit(ATLAS, getX(), getY(), ONLINE_U, bgV, ONLINE_W, ONLINE_H, 256, 256);

                    /* голова */
                    ResourceLocation skin = minecraft.getSkinManager()
                            .getInsecureSkinLocation(getProfileSafe(finalPlayerId));
                    int headX = getX() + 3;
                    int headY = getY() + (ONLINE_H - 8) / 2;
                    g.blit(skin, headX, headY, 8, 8, 8, 8, 8, 8, 64, 64);
                    RenderSystem.enableBlend();
                    g.blit(skin, headX, headY, 40, 8, 8, 8, 64, 64);
                    RenderSystem.disableBlend();

                    /* ник + тег */
                    String tagPart = (showTag && teamTag != null && !teamTag.isEmpty())
                            ? "[" + teamTag + "]" : "";
                    String fullText = finalName + tagPart;
                    if (font.width(fullText) > ONLINE_W - 22) {
                        fullText = font.plainSubstrByWidth(fullText, ONLINE_W - 25) + "..";
                    }
                    g.drawString(font, fullText, getX() + 14, getY() + 4, 0xFFFFFF, false);

                    /* корона у лидера */
                    if (isOwner) {
                        g.blit(ATLAS, getX() + ONLINE_W - LEADER_W - 2,
                                getY() + ONLINE_H - LEADER_H - 1,
                                LEADER_U, LEADER_V, LEADER_W, LEADER_H, 256, 256);
                    }

                    /* ховер */
                    if (isHovered()) {
                        g.fill(getX(), getY(), getX() + width, getY() + height, 0x30FFFFFF);
                    }
                }
            };

            playerButtons.add(playerButton);
            addRenderableWidget(playerButton);
            playerButton.setTooltip(Tooltip.create(
                    Component.translatable("gui.teammod.member.view_profile")));
            uuidToButton.put(finalPlayerId, playerButton);
        }
        updateVisibleButtons();
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
            TeamManager.Team team = TeamManager.clientTeams.get(teamName);
            if (team != null && team.getOwner().equals(minecraft.player.getUUID())) {
                boolean newState = !team.isInviteOnly();
                team.setInviteOnly(newState); // локально
                NetworkHandler.INSTANCE.sendToServer(new SetInviteOnlyPacket(teamName, newState));
            }
        }, s -> Component.empty()) {
            {
                setTooltip(Tooltip.create(Component.literal(
                        "Режим вступления:\n§aОткрытая§r — любой может вступить\n§cПриглашения§r — только по приглашению")));
            }

            @Override
            public void renderWidget(GuiGraphics g, int mx, int my, float pt) {
                TeamManager.Team team = TeamManager.clientTeams.get(teamName);
                if (team != null && team.isInviteOnly()) {
                    RenderSystem.setShaderTexture(0, ATLAS);
                    g.blit(ATLAS, getX(), getY(), ZAMOK_U, ZAMOK_V, ZAMOK_W, ZAMOK_H, 256, 256);
                }
                // если открытая — ничего не рисуем
                if (isHovered()) {
                    g.fill(getX(), getY(), getX() + width, getY() + height, 0x30FFFFFF);
                }
            }
        });
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
        this.renderBackground(g);
        renderBg(g, partialTick, mouseX, mouseY);

        int guiX = left();
        int guiY = top();

        // Название команды
        g.drawCenteredString(font, teamName, guiX + 19 + OFFSET_X/4 - 2, guiY + OFFSET_Y/4 - 2, 0xFFFFFF);

        // Тег (только если включён)
        TeamManager.Team actualTeam = TeamManager.clientTeams.get(teamName);
        if (actualTeam != null && actualTeam.showTag() && !actualTeam.getTag().isEmpty()) {
            g.drawCenteredString(font, actualTeam.getTag(), guiX + 19 + OFFSET_X/4 - 2, guiY + 26 + OFFSET_Y/4 - 15, 0xFFFFFF);
        }

        // Онлайн/всего
        int[] stats = getOnlineAndTotalPlayers();
        g.drawCenteredString(font, stats[0] + "/" + stats[1], guiX + 118 + OFFSET_X/4 - 2, guiY + 13 + OFFSET_Y/4 + 2, 0xFFFFFF);

        RenderSystem.setShaderTexture(0, ATLAS);

        // Иконки — только если включены в актуальной команде
        if (actualTeam != null) {
            if (actualTeam.showTag()) {
                g.blit(ATLAS, guiX + 118 - 14 + OFFSET_X/4 - 2, guiY + 34 + OFFSET_Y/4 - 1, TAG_U, TAG_V, TAG_W, TAG_H, 256, 256);
            }
            if (actualTeam.showCompass()) {
                g.blit(ATLAS, guiX + 118 - 7 + OFFSET_X/4 - 2, guiY + 51 + OFFSET_Y/4 - 1, COMPASS_U, COMPASS_V, COMPASS_W, COMPASS_H, 256, 256);
            }
            if (actualTeam.isFriendlyFire()) {
                g.blit(ATLAS, guiX + 118 - 6 + OFFSET_X/4 - 2, guiY + 72 + OFFSET_Y/4 - 1, FFON_U, FFON_V, FFON_W, FFON_H, 256, 256);
            }
        }

        // ==== ЕДИНЫЙ КРАСИВЫЙ ПРОГРЕСС (как в TeamMemberScreen) ====
        int xpBarX = guiX + 10 + 21 - 9 - 7;
        int xpBarY = guiY + 42 + 20 + 4 + 15 + (3 * (ONLINE_H + 1)) + 5 + 13;

        int avgProgress = TeamQuestHelper.getTeamAverageQuestProgress(teamName);
        int fillWidth   = (int)(TEAM_BAR_W * avgProgress / 100.0);

        // фон
        g.blit(ATLAS, xpBarX, xpBarY, TEAM_BAR_U, TEAM_BAR_V, TEAM_BAR_W, TEAM_BAR_H, 256, 256);
        // заполнение
        g.blit(ATLAS, xpBarX, xpBarY, TEAM_BAR_U, TEAM_BAR_V + TEAM_BAR_H, fillWidth, TEAM_BAR_H, 256, 256);

        // тултип: всегда одно число – avgProgress
        if (mouseX >= xpBarX && mouseX <= xpBarX + TEAM_BAR_W &&
                mouseY >= xpBarY && mouseY <= xpBarY + TEAM_BAR_H) {
            g.renderTooltip(font,
                    Component.translatable("gui.teammod.tooltip.team_quests", avgProgress),
                    mouseX, mouseY);
        }
        super.render(g, mouseX, mouseY, partialTick);
    }

    protected void renderBg(GuiGraphics g, float pt, int mx, int my) {
        RenderSystem.setShaderTexture(0, ATLAS);
        g.blit(ATLAS, left(), top(), 0, 0, GUI_WIDTH, GUI_HEIGHT, 256, 256);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaY) {
        int total = playerButtons.size();
        if (total <= 3) return false; // если ≤3 — скролл не нужен

        int maxScroll = total - 3; // сколько можно проскроллить
        scrollOffset -= (int) deltaY;
        scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));
        updateVisibleButtons();
        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int totalPlayers = playerButtons.size();
        if (totalPlayers <= 3) return super.mouseClicked(mouseX, mouseY, button);

        int trackX = left() + 21 - 9 - 9;
        int trackY = top() + 20 + 4 + 15 + 18;
        int trackHeight = 3 * (ONLINE_H + 1); // высота трека под 3 видимых слота

        if (mouseX >= trackX && mouseX <= trackX + 7 && mouseY >= trackY && mouseY <= trackY + trackHeight) {
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
        int trackHeight = 3 * (ONLINE_H + 1);

        double rel = mouseY - trackY;
        rel = Math.max(0, Math.min(rel, trackHeight - SCROLL_H));

        float ratio = (float) rel / (trackHeight - SCROLL_H);
        int maxScroll = Math.max(0, playerButtons.size() - 3);
        scrollOffset = Math.round(ratio * maxScroll);
        scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));

        updateVisibleButtons();
    }

    private void updateVisibleButtons() {
        if (playerButtons.isEmpty()) return;

        int visibleSlots = 3; // сколько строк видно одновременно в текстуре — это фиксировано!
        int slotHeight = ONLINE_H + 1; // 15 + 1 = 16 пикселей на слот
        int startY = top() + 42 + 20 + 4 + 14; // начальная Y первого видимого слота

        for (int i = 0; i < playerButtons.size(); i++) {
            Button button = playerButtons.get(i);
            int visibleIndex = i - scrollOffset;

            if (visibleIndex >= 0 && visibleIndex < visibleSlots) {
                button.setY(startY + visibleIndex * slotHeight);
                button.visible = true;
            } else {
                button.visible = false;
            }
        }
    }

    public void refreshFromSync() {
        updateVisibleButtons();
    }

    private int getTeamAverageQuestProgress() {
        TeamManager.Team team = TeamManager.getTeam(teamName);
        if (team == null) return 0;

        int totalCompleted = 0;
        int totalQuests = 0;
        int memberCount = 0;

        for (UUID memberUUID : team.getMembers()) {
            int completed = FTBQuestsStats.getCompletedQuests(memberUUID);
            int total = FTBQuestsStats.getTotalQuests();

            totalCompleted += completed;
            totalQuests += total;
            memberCount++;
        }

        if (memberCount == 0 || totalQuests == 0) return 0;

        return (totalCompleted * 100) / totalQuests;
    }

    @Override
    public void tick() {
        super.tick();
        if (++nameCheckTick >= 10) {
            nameCheckTick = 0;

            for (Map.Entry<UUID, Button> entry : uuidToButton.entrySet()) {
                UUID uuid = entry.getKey();
                Button btn = entry.getValue();
                String oldName = btn.getMessage().getString();
                String newName = getNameSafe(uuid);

                if (!newName.equals(oldName) && !"Loading...".equals(newName)) {
                    btn.setMessage(Component.literal(newName));
                }
            }
        }
    }

    private List<UUID> getAllPlayerUUIDs() {
        TeamManager.Team team = TeamManager.clientTeams.get(teamName);
        return team != null ? new ArrayList<>(team.getMembers()) : List.of();
    }
}
package com.mom.teammod;

import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mom.teammod.packets.JoinTeamPacket;
import com.mom.teammod.packets.RequestProfilePacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import java.util.*;

public class OtherTeamProfileScreen extends BaseModScreen {

    // АТЛАС для чужой команды
    private static final ResourceLocation ATLAS = ResourceLocation.fromNamespaceAndPath(TeamMod.MODID,
            "textures/gui/other_profile_background.png");

    // Координаты из разметки (аналогичные TeamProfileOwner)
    private static final int TAG_U      = 1;   // tag
    private static final int TAG_V      = 207;
    private static final int TAG_W      = 28;
    private static final int TAG_H      = 10;
    private Button joinButton = null; // Кнопка Join или прозрачная область
    private static final int COMPASS_U  = 1;   // compass
    private static final int COMPASS_V  = 231;
    private static final int COMPASS_W  = 15;
    private static final int COMPASS_H  = 14;
    private int nameCheckTick = 0;
    private static final int FFON_U     = 1;   // ffon
    private static final int FFON_V     = 218;
    private static final int FFON_W     = 12;
    private static final int FFON_H     = 12;
    private final Map<UUID, Button> uuidToButton = new HashMap<>();
    // Новая текстура кнопки join_team
    private static final int JOIN_TEAM_U    = 30;  // join_team из разметки (29.61 округляем до 30)
    private static final int JOIN_TEAM_V    = 206; // 206.25 округляем до 206
    private static final int JOIN_TEAM_W    = 50;  // 79.41 - 29.61 ≈ 50
    private static final int JOIN_TEAM_H    = 9;   // 215.55 - 206.25 ≈ 9

    private static final int GUI_WIDTH  = 256;
    private static final int GUI_HEIGHT = 170;


    // Смещения (такие же как в TeamProfileOwner)
    private static final int OFFSET_X = 240;
    private static final int OFFSET_Y = 141;

    // Ползунок
    private static final int SCROLL_U = 14;
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
    private static final int ZAMOK_U = 158;   // xtl
    private static final int ZAMOK_V = 170;   // ytl
    private static final int ZAMOK_W = 9;     // xbr - xtl
    private static final int ZAMOK_H = 11;
    private final List<Button> playerButtons = new ArrayList<>();
    private int scrollOffset = 0;
    private boolean isDraggingScroller = false;

    // Данные команды
    private final String teamName;
    private final String teamTag;
    private final boolean showTag;
    private final boolean showCompass;
    private final boolean friendlyFire;
    private final UUID teamLeader;

    private static final int XP_BAR_U = 0;
    private static final int XP_BAR_V = 170;
    private static final int XP_BAR_W = 83;  // 82.89 округляем до 83
    private static final int XP_BAR_H = 5;   // 174.55 - 170.05 = 4.5, округляем до 5



    public OtherTeamProfileScreen(Screen parentScreen, String teamName, String teamTag,
                                  boolean showTag, boolean showCompass, boolean friendlyFire, UUID teamLeader) {
        super(parentScreen, Component.literal(teamName));
        this.teamName = teamName;
        this.teamTag = teamTag;
        this.showTag = showTag;
        this.showCompass = showCompass;
        this.friendlyFire = friendlyFire;
        this.teamLeader = teamLeader;
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
        super.init(); // ← Сначала super.init()!
        int guiX = left();
        int baseY = top() - 26;

        ResourceLocation unpress = ResourceLocation.fromNamespaceAndPath(TeamMod.MODID, "textures/gui/unpress.png");
        ResourceLocation press   = ResourceLocation.fromNamespaceAndPath(TeamMod.MODID, "textures/gui/press.png");

        ResourceLocation INV_ICON       = ResourceLocation.fromNamespaceAndPath(TeamMod.MODID, "textures/gui/inv_icon.png");
        ResourceLocation TEAM_LIST_ICON = ResourceLocation.fromNamespaceAndPath(TeamMod.MODID, "textures/gui/team_list_icon.png");
        ResourceLocation PROFILE_ICON   = ResourceLocation.fromNamespaceAndPath(TeamMod.MODID, "textures/gui/profile_icon.png");

// === КНОПКА ИНВЕНТАРЬ ===
        this.addRenderableWidget(new ImageButton(guiX + 2, baseY, 26, 27, 0, 0, 0, unpress, button -> {
            minecraft.setScreen(new InventoryScreen(minecraft.player));
        }) {
            private boolean isPressed = false;
            @Override public void renderWidget(GuiGraphics g, int mx, int my, float pt) {
                boolean active = this.isHovered() || isPressed;
                ResourceLocation tex = active ? press : unpress;
                int h = active ? 29 : 27;
                int yOff = active ? -2 : 0;
                if (this.getHeight() != h) { this.setHeight(h); this.setY(baseY + yOff); }
                g.blit(tex, getX(), getY(), 0, 0, 26, h, 26, h);
                g.blit(INV_ICON, getX() + 5, getY() + (active ? 7 : 6), 0, 0, 16, 16, 16, 16);
                if (this.isHovered()) g.renderTooltip(font, Component.translatable("gui.teammod.inventory"), mx, my);
            }
            @Override public void onClick(double mx, double my) { super.onClick(mx, my); this.isPressed = true; }
        });

// === КНОПКА КОМАНДЫ — ЗАЖАТА ===
        int teamX = guiX + 2 + 26 + 52;
        this.addRenderableWidget(new ImageButton(teamX, baseY - 2, 26, 29, 0, 0, 0, press, btn -> {}) {
            @Override public void renderWidget(GuiGraphics g, int mx, int my, float pt) {
                g.blit(press, getX(), getY(), 0, 0, 26, 29, 26, 29);
                g.blit(TEAM_LIST_ICON, getX() + 5, getY() + 6, 0, 0, 16, 16, 16, 16);
                if (this.isHovered()) g.renderTooltip(font, Component.translatable("gui.teammod.team_tab"), mx, my);
            }
        });

// === КНОПКА ПРОФИЛЬ ===
        int profileX = teamX + 26;
        this.addRenderableWidget(new ImageButton(profileX, baseY, 26, 27, 0, 0, 0, unpress, button -> {
            minecraft.setScreen(new MyProfileScreen(this, Component.translatable("gui.teammod.profile")));
        }) {
            private boolean isPressed = false;
            @Override public void renderWidget(GuiGraphics g, int mx, int my, float pt) {
                boolean active = this.isHovered() || isPressed;
                ResourceLocation tex = active ? press : unpress;
                int h = active ? 29 : 27;
                int yOff = active ? -2 : 0;
                if (this.getHeight() != h) { this.setHeight(h); this.setY(baseY + yOff); }
                g.blit(tex, getX(), getY(), 0, 0, 26, h, 26, h);
                g.blit(PROFILE_ICON, getX() + 5, getY() + (active ? 7 : 6), 0, 0, 16, 16, 16, 16);
                if (this.isHovered()) g.renderTooltip(font, Component.translatable("gui.teammod.profile"), mx, my);
            }
            @Override public void onClick(double mx, double my) { super.onClick(mx, my); this.isPressed = true; }
        });
        scrollOffset = 0;

        int guiY = top();

        addJoinTeamButton(guiX + (67-7+9) + OFFSET_X/4, guiY + 105+1 + OFFSET_Y/4, 56, 11);
        createPlayerButtons(guiX, guiY);
    }

    private void createPlayerButtons(int guiX, int guiY) {
        int baseX = guiX + 10;
        int baseY = guiY + 42 + 14;
        int cellX = baseX + 21 - 9;
        int slotHeight = ONLINE_H + 1;

        TeamManager.Team team = TeamManager.clientTeams.get(teamName);
        List<UUID> members = new ArrayList<>();
        UUID ownerId = null;
        if (team != null) {
            ownerId = team.getOwner();
            members.addAll(team.getMembers());
        }

        if (ownerId != null && members.remove(ownerId)) {
            members.add(0, ownerId);
        }

        playerButtons.forEach(this::removeWidget);
        playerButtons.clear();

        for (int i = 0; i < members.size(); i++) {
            UUID playerId = members.get(i);
            String name = getNameSafe(playerId);
            if ("Loading...".equals(name)) {
                ClientPlayerCache.loadQueue.offer(playerId);
            }
            boolean online = isOnline(playerId);
            boolean isOwner = playerId.equals(ownerId);

            int buttonY = baseY + 20 + 4 + i * slotHeight;

            final UUID finalPlayerId = playerId;
            final String finalName = name;

            Button playerButton = new Button(cellX, buttonY, ONLINE_W, ONLINE_H,
                    Component.empty(), b -> {
                minecraft.setScreen(new OtherPlayerProfileScreen(this, finalPlayerId, Component.literal("Профиль " + finalName)));
            }, s -> Component.empty()) {

                @Override
                public void renderWidget(GuiGraphics g, int mx, int my, float pt) {
                    if (!this.visible) return;

                    int bgV = online ? ONLINE_V : 175;
                    g.blit(ATLAS, getX(), getY(), ONLINE_U, bgV, ONLINE_W, ONLINE_H, 256, 256);

                    ResourceLocation skin = minecraft.getSkinManager().getInsecureSkinLocation(getProfileSafe(finalPlayerId));
                    int headX = getX() + 3;
                    int headY = getY() + (ONLINE_H - 8) / 2;
                    g.blit(skin, headX, headY, 8, 8, 8, 8, 64, 64);
                    RenderSystem.enableBlend();
                    g.blit(skin, headX, headY, 40, 8, 8, 8, 64, 64);
                    RenderSystem.disableBlend();

                    String tagPart = (showTag && teamTag != null && !teamTag.isEmpty()) ? "[" + teamTag + "]" : "";
                    String fullText = finalName + tagPart;
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

            uuidToButton.put(finalPlayerId, playerButton);
            playerButton.setTooltip(Tooltip.create(Component.translatable("gui.teammod.member.view_profile")));
            playerButtons.add(playerButton);
            addRenderableWidget(playerButton);
        }

        updateVisibleButtons();
    }

    private Button addTransparentButton(int x, int y, int w, int h, Runnable action, Component tooltip) {
        Button btn = new Button(x, y, w, h, Component.empty(), b -> action.run(), s -> Component.empty()) {
            @Override
            public void renderWidget(GuiGraphics g, int mx, int my, float pt) {
                if (isHovered()) {
                    g.fill(getX(), getY(), getX() + width, getY() + height, 0x30FFFFFF);
                }
            }
        };
        btn.setTooltip(Tooltip.create(tooltip));
        return addRenderableWidget(btn);
    }

    // Старый метод полностью
    private void addJoinTeamButton(int x, int y, int w, int h) {
        if (joinButton != null) removeWidget(joinButton);

        TeamManager.Team team = TeamManager.clientTeams.get(teamName);
        if (team == null) return;

        int newX = x - 3;              // ← 3 px левее
        int newW = JOIN_TEAM_W;
        int newH = JOIN_TEAM_H + 2;    // ← на 2 px выше

        joinButton = new Button(newX, y, newW, newH, Component.empty(), b -> {
            if (!team.isInviteOnly()) {
                NetworkHandler.INSTANCE.sendToServer(new JoinTeamPacket(teamName));
            }
        }, s -> Component.empty()) {

            { setTooltip(Tooltip.create(
                    team.isInviteOnly()
                            ? Component.literal("§cКоманда закрыта\nВступить можно только по приглашению лидера")
                            : Component.literal("Вступить в команду"))); }

            @Override
            public void renderWidget(GuiGraphics g, int mx, int my, float pt) {
                /* ---------- кнопка ---------- */
                if (team.isInviteOnly()) {
                    // закрытая — рисуем текстуру
                    RenderSystem.setShaderTexture(0, ATLAS);
                    g.blit(ATLAS, getX() + 5 , getY() - 1, JOIN_TEAM_U, JOIN_TEAM_V, JOIN_TEAM_W, JOIN_TEAM_H, 256, 256);
                } else {
                    // открытая — только ховер
                    if (isHovered()) {
                        g.fill(getX() + 4, getY() - 2 , getX() + width + 6, getY() + height -1, 0x30FFFFFF);
                    }
                }

                /* ---------- замочек ---------- */
                if (team.isInviteOnly()) {
                    int lockX = getX() + width + 2;   // правее кнопки на 2 px
                    int lockY = getY();
                    int lockW = ZAMOK_W;
                    int lockH = height;               // та же высота, что и кнопка
                    RenderSystem.setShaderTexture(0, ATLAS);
                    g.blit(ATLAS, lockX + 7, lockY -2, ZAMOK_U, ZAMOK_V, lockW, lockH, 256, 256);
                }
            }
        };

        addRenderableWidget(joinButton);
    }


    public void refreshFromSync() {
        int guiX = left();
        int guiY = top();

        // Перестраиваем кнопку Join
        addJoinTeamButton(guiX + (67-7+9) + OFFSET_X/4, guiY + 105+1 + OFFSET_Y/4, 56, 11);

        // Перестраиваем список участников
        createPlayerButtons(guiX, guiY);

        // Сбрасываем скролл
        scrollOffset = 0;
        updateVisibleButtons();
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(g);
        renderBg(g, partialTick, mouseX, mouseY);

        int guiX = left();
        int guiY = top();

        // Название команды
        g.drawCenteredString(font, teamName, guiX + 19 + OFFSET_X/4 - 2, guiY + OFFSET_Y/4 - 2, 0xFFFFFF);

        // Тег команды (если отображается)
        if (teamTag != null && !teamTag.isEmpty() && showTag) {
            g.drawCenteredString(font, teamTag, guiX + 19 + OFFSET_X/4 - 2, guiY + 26 + OFFSET_Y/4 - 15, 0xFFFFFF);
        }

        // Онлайн/всего участников
        int[] stats = getOnlineAndTotalPlayers();
        g.drawCenteredString(font, stats[0] + "/" + stats[1], guiX + 118 + OFFSET_X/4 - 2, guiY + 13 + OFFSET_Y/4 + 2, 0xFFFFFF);

        // Иконки настроек команды
        RenderSystem.setShaderTexture(0, ATLAS);
        TeamManager.Team actualTeam = TeamManager.clientTeams.get(teamName);
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

        // ── ПОЛЗУНОК (только если участников больше 3) ─────────────────────────────
        int totalPlayers = playerButtons.size();
        if (totalPlayers > 3) {
            int baseX = guiX + 10;
            int baseY = guiY + 42;

            int visibleHeight = 3 * (ONLINE_H + 1); // высота видимой области (3 слота)
            int maxScroll = totalPlayers - 3;
            int scrollerOffset = maxScroll == 0 ? 0 :
                    (int)((float)scrollOffset / maxScroll * (visibleHeight - SCROLL_H));

            g.blit(ATLAS, baseX + 13 - 8, baseY + 5 + 20 + 4 + 10 + scrollerOffset,
                    SCROLL_U, SCROLL_V, SCROLL_W, SCROLL_H, 256, 256);
        }

        // ==== КОМАНДНЫЙ ПРОГРЕСС (средний % квестов) ====
        int xpBarX = guiX + 10 + 21 - 9 - 7;
        int xpBarY = guiY + 42 + 20 + 4 + 15 + (3 * (ONLINE_H + 1)) + 5 + 13;

        int avgProgress = TeamQuestHelper.getTeamAverageQuestProgress(teamName);
        int fillWidth   = (int)(XP_BAR_W * avgProgress / 100.0);

        // фон
        g.blit(ATLAS, xpBarX, xpBarY, XP_BAR_U, XP_BAR_V, XP_BAR_W, XP_BAR_H, 256, 256);
        // заполнение
        g.blit(ATLAS, xpBarX, xpBarY, XP_BAR_U, XP_BAR_V + XP_BAR_H, fillWidth, XP_BAR_H, 256, 256);

        // тултип: всегда одно число – avgProgress
        if (mouseX >= xpBarX && mouseX <= xpBarX + XP_BAR_W &&
                mouseY >= xpBarY && mouseY <= xpBarY + XP_BAR_H) {
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
    private String getNameSafe(UUID id) {
        GameProfile gp = ClientPlayerCache.getGameProfile(id);
        if (gp == null || "Unknown".equals(gp.getName())) {
            NetworkHandler.INSTANCE.sendToServer(new RequestProfilePacket(id));
            return "Loading...";
        }
        return gp.getName();
    }

    private boolean isOnline(UUID id) {
        return ClientPlayerCache.isOnline(id);
    }

    private GameProfile getProfileSafe(UUID id) {
        return ClientPlayerCache.getGameProfile(id);
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
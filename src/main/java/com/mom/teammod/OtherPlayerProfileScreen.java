package com.mom.teammod;

import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mom.teammod.packets.InvitePlayerPacket;
import com.mom.teammod.packets.RequestProfilePacket;
import com.mom.teammod.packets.RespondInvitationPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.simpleraces.network.SimpleracesModVariables;

import java.util.Comparator;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

public class OtherPlayerProfileScreen extends BaseModScreen {

    private static final ResourceLocation ATLAS = ResourceLocation.fromNamespaceAndPath(TeamMod.MODID,
            "textures/gui/user_profile_background.png");

    // Размеры кнопки invite_off из разметки XML
    private static final int INVITE_OFF_W = 107;  // xbr(137.26) - xtl(30.76) = 106.5 ≈ 107
    private static final int INVITE_OFF_H = 15;   // ybr(185.31) - ytl(170.71) = 14.6 ≈ 15
    private static final int INVITE_OFF_U = (int)30.76;   // xtl
    private static final int INVITE_OFF_V = (int)170.71;  // ytl
    private static final long AFK_THRESHOLD = 10_000L; // 10 секунд без ввода = AFK
    private final AtomicLong lastInputTime = new AtomicLong(System.currentTimeMillis());
    private static final int ACCEPT_JOIN_W = 114;  // xbr(244.36) - xtl(130.56) = 113.8 ≈ 114
    private static final int ACCEPT_JOIN_H = 16;   // ybr(200.91) - ytl(185.41) = 15.5 ≈ 16
    private static final int ACCEPT_JOIN_U = (int)130.56;  // xtl
    private static final int ACCEPT_JOIN_V = (int)185.41;  // ytl

    private ItemStack[] lastSeenEquipment = new ItemStack[4]; // 0-3 для слотов брони
    private boolean hasSeenPlayer = false;

    private enum InviteButtonState {
        NONE,        // нет кнопки
        INVITE_OFF,  // текстура invite_off
        ACCEPT_JOIN  // текстура accept_join
    }

    // Остальные UV-координаты как в MyProfileScreen
    private int lastRenderedScrollOffset = -1;
    private static final int INV_U       = 1,   INV_V       = 171, INV_W   = 23, INV_H   = 16;
    private static final int SCROLLER_U  = 25,  SCROLLER_V  = 171, SCROLLER_W = 6,  SCROLLER_H = 25;
    private static final int BAR_U       = 31,  BAR_V       = 186, BAR_W   = 81, BAR_H   = 5;
    private static final int X_U         = 1,   X_V         = 187, X_W     = 12, X_H     = 11;
    private static final int V_U         = 13,  V_V         = 187, V_W     = 11, V_H     = 11;
    private static final int HUMAN_U     = 81,  HUMAN_V     = 199, HUMAN_W = 9,  HUMAN_H = 10;

    // Статусы
    private static final int ONLINE_U = 31,  ONLINE_V = 192;
    private static final int AFK_U    = 36,  AFK_V    = 192;
    private static final int OFFLINE_U = 41, OFFLINE_V = 192; // НОВОЕ: оффлайн иконка

    private static final int GUI_WIDTH  = 256;
    private static final int GUI_HEIGHT = 169;

    private static final int SKIN_FRAME_WIDTH  = 87;
    private static final int SKIN_FRAME_HEIGHT = 100;
    private static final int FRAME_MARGIN_LEFT = 15;

    private static final int VISIBLE_SLOTS = 4;
    private static final int SLOT_HEIGHT = INV_H + X_H + 4;
    private final List<TeamManager.Team> teamList = new ArrayList<>();
    private int scrollOffset = 0;

    private boolean isDraggingScroller = false;
    private static final int SCROLL_TRACK_HEIGHT = 119;
    private static final int SCROLLER_X_OFFSET = 111 + 41 + 10 + 20 + 10 + 10 + 10 + 1 + (int)(8 / 0.75f) + 23 + 5;
    private static final int SCROLLER_Y_START = 30 - 8 + 7 + 1;



    // Данные профиля
    private final UUID targetPlayerId;
    public String playerName = "Unknown";
    private OtherPlayerState.PlayerStatus playerStatus;

    private final Screen parentScreen;

    public OtherPlayerProfileScreen(Screen parentScreen, UUID targetPlayerId, Component title) {
        super(parentScreen, title);
        this.targetPlayerId = targetPlayerId;
        this.parentScreen = parentScreen;

        // Получаем тестовые данные
        ProfileManager.Profile profile = ProfileManager.getClientProfile(targetPlayerId);
        NetworkHandler.INSTANCE.sendToServer(new RequestProfilePacket(targetPlayerId));
    }

    private InviteButtonState getInviteButtonState() {
        // Привязываем состояния к UUID игроков
        if (targetPlayerId.equals(UUID.fromString("deadbeef-dead-beef-dead-beefdeadbeef"))) {
            return InviteButtonState.NONE;           // BirdMan - нет текстуры, прозрачная кнопка (1)
        } else if (targetPlayerId.equals(UUID.fromString("11111111-1111-1111-1111-111111111111"))) {
            return InviteButtonState.INVITE_OFF;     // BridMan - текстура invite_off, НЕТ кнопки (2)
        } else if (targetPlayerId.equals(UUID.fromString("22222222-2222-2222-2222-222222222222"))) {
            return InviteButtonState.ACCEPT_JOIN;    // Berdamel - текстура accept_join, ЕСТЬ кнопка (3)
        } else if (targetPlayerId.equals(UUID.fromString("33333333-3333-3333-3333-333333333333"))) {
            return InviteButtonState.NONE;           // HerobRine - нет текстуры, прозрачная кнопка (1)
        } else if (targetPlayerId.equals(UUID.fromString("44444444-4444-4444-4444-444444444444"))) {
            return InviteButtonState.INVITE_OFF;     // TOPSON - текстура invite_off, НЕТ кнопки (2)
        }
        return InviteButtonState.NONE;
    }

    @Override
    public void tick() {
        super.tick();
        ProfileManager.Profile prof = ProfileManager.getClientProfile(targetPlayerId);
        if ("Unknown".equals(prof.getGameProfile().getName())) {
            // ещё не пришло – повторно запросим (на всякий случай)
            NetworkHandler.INSTANCE.sendToServer(new RequestProfilePacket(targetPlayerId));
        } else {
            // обновляем имя и перерисовываем
            if ("Unknown".equals(playerName)) {
                playerName = prof.getGameProfile().getName();
            }
        }
    }

    @Override
    protected void init() {
        super.init();
        updatePlayerEquipment(targetPlayerId);
        scrollOffset = 0;
        lastRenderedScrollOffset = -1;

        // === УНИВЕРСАЛЬНЫЕ КНОПКИ НАВИГАЦИИ ===
        {
            ResourceLocation unpress = ResourceLocation.fromNamespaceAndPath(TeamMod.MODID, "textures/gui/unpress.png");
            ResourceLocation press   = ResourceLocation.fromNamespaceAndPath(TeamMod.MODID, "textures/gui/press.png");

            ResourceLocation INV_ICON       = ResourceLocation.fromNamespaceAndPath(TeamMod.MODID, "textures/gui/inv_icon.png");
            ResourceLocation TEAM_LIST_ICON = ResourceLocation.fromNamespaceAndPath(TeamMod.MODID, "textures/gui/team_list_icon.png");
            ResourceLocation PROFILE_ICON   = ResourceLocation.fromNamespaceAndPath(TeamMod.MODID, "textures/gui/profile_icon.png");

            int guiX  = (width  - 256) / 2;
            int baseY = (height - 170) / 2 - 26;

            // КНОПКА ИНВЕНТАРЬ
            int invX = guiX + 2;
            this.addRenderableWidget(new ImageButton(invX, baseY, 26, 27, 0, 0, 0, unpress, b -> minecraft.setScreen(new InventoryScreen(minecraft.player))) {
                private boolean isPressed = false;
                @Override public void renderWidget(GuiGraphics g, int mx, int my, float pt) {
                    boolean active = this.isHoveredOrFocused() || isPressed;
                    ResourceLocation tex = active ? press : unpress;
                    int h = active ? 29 : 27;
                    int yOff = active ? -2 : 0;
                    if (this.getHeight() != h) { this.setHeight(h); this.setY(baseY + yOff); }
                    g.blit(tex, getX(), getY(), 0, 0, 26, h, 26, h);
                    g.blit(INV_ICON, getX() + 5, getY() + (active ? 7 : 6), 0, 0, 16, 16, 16, 16);
                    if (this.isHoveredOrFocused()) g.renderTooltip(font, Component.translatable("gui.teammod.button3"), mx, my);
                }
                @Override public void onClick(double mx, double my) { super.onClick(mx, my); this.isPressed = true; }
            });

            // КНОПКА КОМАНДЫ (зажата)
            int teamX = invX + 26 + 52;
            this.addRenderableWidget(new ImageButton(teamX, baseY - 2, 26, 29, 0, 0, 0, press, b -> {
                minecraft.setScreen(new TeamScreen(
                        OtherPlayerProfileScreen.this,
                        null,
                        minecraft.player.getInventory(),
                        Component.translatable("gui.teammod.team_tab")
                ));
            }) {
                @Override public void renderWidget(GuiGraphics g, int mx, int my, float pt) {
                    g.blit(press, getX(), getY(), 0, 0, 26, 29, 26, 29);
                    g.blit(TEAM_LIST_ICON, getX() + 5, getY() + 6, 0, 0, 16, 16, 16, 16);
                    if (this.isHoveredOrFocused()) g.renderTooltip(font, Component.translatable("gui.teammod.team_tab"), mx, my);
                }
            });

            // КНОПКА ПРОФИЛЬ
            int profileX = teamX + 26;
            this.addRenderableWidget(new ImageButton(profileX, baseY, 26, 27, 0, 0, 0, unpress, b -> {
                minecraft.setScreen(new MyProfileScreen(OtherPlayerProfileScreen.this, Component.translatable("gui.teammod.profile")));
            }) {
                private boolean isPressed = false;
                @Override public void renderWidget(GuiGraphics g, int mx, int my, float pt) {
                    boolean active = this.isHoveredOrFocused() || isPressed;
                    ResourceLocation tex = active ? press : unpress;
                    int h = active ? 29 : 27;
                    int yOff = active ? -2 : 0;
                    if (this.getHeight() != h) { this.setHeight(h); this.setY(baseY + yOff); }
                    g.blit(tex, getX(), getY(), 0, 0, 26, h, 26, h);
                    g.blit(PROFILE_ICON, getX() + 5, getY() + (active ? 7 : 6), 0, 0, 16, 16, 16, 16);
                    if (this.isHoveredOrFocused()) g.renderTooltip(font, Component.translatable("gui.teammod.profile"), mx, my);
                }
                @Override public void onClick(double mx, double my) { super.onClick(mx, my); this.isPressed = true; }
            });
        }

        int guiX = (width - GUI_WIDTH) / 2;
        int guiY = (height - GUI_HEIGHT) / 2;

        // === КНОПКА "ПРИГЛАСИТЬ" / "ПРИНЯТЬ ЗАЯВКУ" ===
        int buttonX = guiX + (GUI_WIDTH - INVITE_OFF_W) / 2 - 23;
        int buttonY = guiY + GUI_HEIGHT - INVITE_OFF_H - 16;

        TeamManager.Team team = TeamManager.clientTeams.get(playerName); // playerName — это имя команды

        if (team != null && team.getInvited().contains(minecraft.player.getUUID())) {
            // Мы получили приглашение в эту команду → показываем "Принять заявку"
            addRenderableWidget(new TextureButton(buttonX, buttonY, ACCEPT_JOIN_W, ACCEPT_JOIN_H,
                    ACCEPT_JOIN_U, ACCEPT_JOIN_V, ATLAS, b -> {
                NetworkHandler.INSTANCE.sendToServer(new RespondInvitationPacket(playerName, true));
            }) {
                @Override public void renderWidget(GuiGraphics g, int mx, int my, float pt) {
                    super.renderWidget(g, mx, my, pt);
                    if (isHovered()) g.fill(getX(), getY(), getX() + width, getY() + height, 0x30FFFFFF);
                }
            });
        } else {
            // Обычная кнопка "Пригласить в команду"
            addTransparentButton(buttonX, buttonY, INVITE_OFF_W, INVITE_OFF_H,
                    this::openMyTeamsList,
                    Component.literal("Пригласить в команду"));
        }

        // Кнопка статистики
        addTransparentButton(guiX + 110, guiY + 114, 86, 14,
                this::openDetailedStats,
                Component.literal("Подробная статистика"));

        // Реальное имя игрока (если это профиль игрока)
        Player realPlayer = minecraft.level.getPlayerByUUID(targetPlayerId);
        if (realPlayer != null) {
            this.playerName = realPlayer.getName().getString();
        }

        renderTeamList(null);
    }

    private void onAcceptJoinClicked() {
        NetworkHandler.INSTANCE.sendToServer(new RespondInvitationPacket(playerName, true));
    }

    private void renderButtonTexture(GuiGraphics g) {
        InviteButtonState buttonState = getInviteButtonState();
        int guiX = (width - GUI_WIDTH) / 2;
        int guiY = (height - GUI_HEIGHT) / 2;

        // Состояние 2: текстура invite_off, НЕТ кнопки
        if (buttonState == InviteButtonState.INVITE_OFF) {
            int inviteX = guiX + (GUI_WIDTH - INVITE_OFF_W) / 2 - 23;
            int inviteY = guiY + GUI_HEIGHT - INVITE_OFF_H - 16;
            g.blit(ATLAS, inviteX, inviteY, INVITE_OFF_U, INVITE_OFF_V,
                    INVITE_OFF_W, INVITE_OFF_H, 256, 256);
        }
        // Состояние 1: ничего не рисуем (прозрачная кнопка рисуется через виджет)
        // Состояние 3: кнопка с текстурой рисуется через виджет TextureButton
    }

    private void onInviteOffClicked() {
        NetworkHandler.INSTANCE.sendToServer(new RespondInvitationPacket(playerName, false));
    }

    private void openDetailedStats() {
        ClientState.hidePlayerRender = true;

        StatisticsScreen statsScreen = new StatisticsScreen(this,Component.literal("Подробная статистика"), targetPlayerId) {
            @Override
            public void render(GuiGraphics g, int mx, int my, float pt) {
                this.renderBackground(g); // затемнение

                // Фон профиля
                RenderSystem.setShaderTexture(0, ATLAS);
                int guiX = (width - GUI_WIDTH) / 2;
                int guiY = (height - GUI_HEIGHT) / 2;
                g.blit(ATLAS, guiX, guiY, 0, 0, GUI_WIDTH, GUI_HEIGHT, 256, 256);

                // Навигация статично (инвентарь unpress, команды press, профиль unpress)
                ResourceLocation unpress = ResourceLocation.fromNamespaceAndPath(TeamMod.MODID, "textures/gui/unpress.png");
                ResourceLocation press = ResourceLocation.fromNamespaceAndPath(TeamMod.MODID, "textures/gui/press.png");
                ResourceLocation INV_ICON = ResourceLocation.fromNamespaceAndPath(TeamMod.MODID, "textures/gui/inv_icon.png");
                ResourceLocation TEAM_LIST_ICON = ResourceLocation.fromNamespaceAndPath(TeamMod.MODID, "textures/gui/team_list_icon.png");
                ResourceLocation PROFILE_ICON = ResourceLocation.fromNamespaceAndPath(TeamMod.MODID, "textures/gui/profile_icon.png");

                int baseY = guiY - 26;
                int navX = guiX + 2;

                g.blit(unpress, navX, baseY, 0, 0, 26, 27, 26, 27);
                g.blit(INV_ICON, navX + 5, baseY + 6, 0, 0, 16, 16, 16, 16);

                g.blit(press, navX + 78, baseY - 2, 0, 0, 26, 29, 26, 29);
                g.blit(TEAM_LIST_ICON, navX + 83, baseY + 4, 0, 0, 16, 16, 16, 16);

                g.blit(unpress, navX + 104, baseY, 0, 0, 26, 27, 26, 27);
                g.blit(PROFILE_ICON, navX + 109, baseY + 6, 0, 0, 16, 16, 16, 16);

                // Список приглашений статично
                int startX = guiX + 111 + 41 + 10 + 20 + 10 + 10 + 10 + 1 + (int)(8 / 0.75f);
                int startY = guiY + 30 - 8 + 7 + 1 + 2;

                UUID myId = minecraft.player.getUUID();
                List<TeamManager.Team> invitations = new ArrayList<>();
                for (TeamManager.Team team : TeamManager.clientTeams.values()) {
                    if (team.getInvited().contains(myId)) invitations.add(team);
                }
                invitations.sort(Comparator.comparing(TeamManager.Team::getName));

                int visible = Math.min(4, invitations.size());
                for (int i = 0; i < visible; i++) {
                    int index = i + OtherPlayerProfileScreen.this.scrollOffset;
                    if (index >= invitations.size()) break;

                    int invY = startY + i * (INV_H + X_H + 2);
                    int underY = invY + INV_H;

                    g.blit(ATLAS, startX, invY, INV_U, INV_V, INV_W, INV_H, 256, 256);
                    g.blit(ATLAS, startX + 35, underY, X_U, X_V, X_W, X_H, 256, 256);
                    g.blit(ATLAS, startX + 60, underY, V_U, V_V, V_W, V_H, 256, 256);
                }

                // Ползунок приглашений
                int scrollerX = width / 2 - SCROLLER_W / 2 + 92;
                int scrollerBaseY = guiY + 80 - 50;
                int offsetY = 0;
                if (invitations.size() > VISIBLE_SLOTS) {
                    float ratio = (float) OtherPlayerProfileScreen.this.scrollOffset / (invitations.size() - VISIBLE_SLOTS);
                    offsetY = (int) (ratio * (119 - SCROLLER_H));
                }
                g.blit(ATLAS, scrollerX, scrollerBaseY + offsetY, SCROLLER_U, SCROLLER_V, SCROLLER_W, SCROLLER_H, 256, 256);

                // Окно статистики поверх
                super.render(g, mx, my, pt);
            }

            @Override
            public void onClose() {
                ClientState.hidePlayerRender = false;
                minecraft.setScreen(OtherPlayerProfileScreen.this.parentScreen);
            }
        };

        statsScreen.init(minecraft, width, height);
        minecraft.setScreen(statsScreen);
    }
    private void openMyTeamsList() {
        MyTeamsListScreen teamsListScreen = new com.mom.teammod.MyTeamsListScreen(this) {
            @Override
            public void render(GuiGraphics g, int mx, int my, float pt) {
                // 1. Рисуем затемнение ВСЕГО ЭКРАНА
                g.fill(0, 0, width, height, 0xB3000000);

                // 2. Рисуем текстуру фона GUI поверх затемнения
                RenderSystem.setShaderTexture(0, ATLAS);
                int guiX = (width - GUI_WIDTH) / 2;
                int guiY = (height - GUI_HEIGHT) / 2;
                g.blit(ATLAS, guiX, guiY, 0, 0, GUI_WIDTH, GUI_HEIGHT, 256, 256);

                // 3. Рисуем текстуры кнопок (если есть)
                InviteButtonState buttonState = getInviteButtonState();
                if (buttonState == InviteButtonState.INVITE_OFF) {
                    int inviteX = guiX + (GUI_WIDTH - INVITE_OFF_W) / 2 - 23;
                    int inviteY = guiY + GUI_HEIGHT - INVITE_OFF_H - 16;
                    g.blit(ATLAS, inviteX, inviteY, INVITE_OFF_U, INVITE_OFF_V,
                            INVITE_OFF_W, INVITE_OFF_H, 256, 256);
                }
                else if (buttonState == InviteButtonState.ACCEPT_JOIN) {
                    int acceptX = guiX + (GUI_WIDTH - ACCEPT_JOIN_W) / 2 - 23;
                    int acceptY = guiY + GUI_HEIGHT - ACCEPT_JOIN_H - 26;
                    g.blit(ATLAS, acceptX, acceptY, ACCEPT_JOIN_U, ACCEPT_JOIN_V,
                            ACCEPT_JOIN_W, ACCEPT_JOIN_H, 256, 256);
                }

                // 4. Рисуем окно со списком команд поверх всего
                super.render(g, mx, my, pt);
            }
        };

        teamsListScreen.init(minecraft, width, height);
        minecraft.setScreen(teamsListScreen);
    }

    private Button addTransparentButton(int x, int y, int w, int h, Runnable action, Component tooltip) {
        Button btn = new Button(x, y, w, h, Component.empty(), b -> action.run(), s -> Component.empty()) {
            @Override
            public void renderWidget(GuiGraphics g, int mx, int my, float pt) {
                // НЕ рисуем текстуру! Только подсветку при наведении
                if (this.isHovered()) {
                    g.fill(getX(), getY(), getX() + width, getY() + height, 0x30FFFFFF);
                }
            }
        };
        btn.setTooltip(Tooltip.create(tooltip));
        return addRenderableWidget(btn);
    }

    private void renderAllElements(GuiGraphics g, int mx, int my) {
        renderSkin(g, mx, my);
        renderStatusIcon(g);
        renderNickname(g);
        renderHumanIconAndText(g);
        renderCenterBarsAndScroller(g,mx,my);
        renderButtonTexture(g); // ← Добавить эту строку
    }

    private void renderStatusIcon(GuiGraphics g) {
        byte st = ClientPlayerCache.getRawStatus(targetPlayerId);
        int u = switch (st) {
            case 1  -> ONLINE_U;
            case 2  -> AFK_U;
            default -> OFFLINE_U;
        };

        int baseX = (width - GUI_WIDTH) / 2;
        int baseY = (height - GUI_HEIGHT) / 2;
        int drawX = baseX + 88 - 2 + 1 - 2;
        int drawY = baseY + 38 - 3 + 5 - 2;

        g.blit(ATLAS, drawX, drawY, u, ONLINE_V, 5, 5, 256, 256);
    }

    private void renderSkin(GuiGraphics g, int mx, int my) {
        int frameX = (width - GUI_WIDTH) / 2 + FRAME_MARGIN_LEFT;
        int frameY = (height - GUI_HEIGHT) / 2 + (GUI_HEIGHT - SKIN_FRAME_HEIGHT) / 2 - 13;
        int playerX = frameX + SKIN_FRAME_WIDTH / 2;
        int playerY = frameY + SKIN_FRAME_HEIGHT - 10;

        Player realPlayer = minecraft.level.getPlayerByUUID(targetPlayerId);
        ProfileManager.Profile profile = ProfileManager.getClientProfile(targetPlayerId);

        /* 1. Игрок онлайн – рисуем живого, обновляем кэш */
        if (realPlayer != null && realPlayer.isAlive()) {
            ItemStack[] equipment = new ItemStack[4];
            for (int i = 0; i < 4; i++) {
                equipment[i] = realPlayer.getItemBySlot(EquipmentSlot.byTypeAndIndex(EquipmentSlot.Type.ARMOR, i)).copy();
            }
            ClientPlayerCache.onPlayerSeen(targetPlayerId, realPlayer.getGameProfile(), equipment);
            profile.setGameProfile(realPlayer.getGameProfile());
            profile.setLastEquipment(equipment);

            InventoryScreen.renderEntityInInventoryFollowsMouse(
                    g, playerX, playerY, 38,
                    (float) playerX - mx,
                    (float) (frameY + SKIN_FRAME_HEIGHT / 2) - my,
                    realPlayer);
            return;
        }

        /* 2. Оффлайн, но уже встречали – берём данные из кэша */
        if (ClientPlayerCache.hasMet(targetPlayerId)) {
            GameProfile gp = ClientPlayerCache.getGameProfile(targetPlayerId);
            ItemStack[] equipment = ClientPlayerCache.getLastEquipment(targetPlayerId);

            Player temp = new RemotePlayer(minecraft.level, gp);
            for (int i = 0; i < 4; i++) {
                temp.setItemSlot(EquipmentSlot.byTypeAndIndex(EquipmentSlot.Type.ARMOR, i), equipment[i]);
            }

            InventoryScreen.renderEntityInInventoryFollowsMouse(
                    g, playerX, playerY, 38,
                    (float) playerX - mx,
                    (float) (frameY + SKIN_FRAME_HEIGHT / 2) - my,
                    temp);
            return;
        }

        /* 3. Никогда не встречались – рисуем default-скин, но ник берём из профиля */
        GameProfile unknown = new GameProfile(targetPlayerId, profile.getGameProfile().getName());
        Player temp = new RemotePlayer(minecraft.level, unknown);

        InventoryScreen.renderEntityInInventoryFollowsMouse(
                g, playerX, playerY, 38,
                (float) playerX - mx,
                (float) (frameY + SKIN_FRAME_HEIGHT / 2) - my,
                temp);
    }

    private void renderNickname(GuiGraphics g) {
        drawScaledCenteredString(g, playerName, 0.75f,
                (height - GUI_HEIGHT) / 2 + 30 + 2, 0xFFFFFF);
    }

    private void drawScaledCenteredString(GuiGraphics g, String text, float scale, int y, int color) {
        int x = getPlayerNicknameX(text);
        g.pose().pushPose();
        g.pose().scale(scale, scale, scale);
        g.drawString(font, text, (int)(x / scale), (int)(y / scale), color, false);
        g.pose().popPose();
    }

    private int getHumanTextX(String text) {
        int guiLeft = (width - GUI_WIDTH) / 2;
        float zoneLeft = 111.90f;
        float zoneRight = 195.50f;
        float zoneWidth = zoneRight - zoneLeft;
        int textWidth = font.width(text);
        float offsetX = (zoneWidth - textWidth) / 2.0f;
        return (int) (guiLeft + zoneLeft + offsetX);
    }

    private int getPlayerNicknameX(String displayName) {
        int guiLeft = (width - GUI_WIDTH) / 2;
        float zoneLeft = 111.90f;
        float zoneRight = 195.50f;
        float zoneWidth = zoneRight - zoneLeft;
        float scale = 0.75f;
        int scaledTextWidth = (int) (font.width(displayName) * scale);
        float offsetX = (zoneWidth - scaledTextWidth) / 2.0f;
        return (int) (guiLeft + zoneLeft + offsetX);
    }

    private void renderHumanIconAndText(GuiGraphics g) {
        int baseX = (width - GUI_WIDTH) / 2;
        int baseY = (height - GUI_HEIGHT) / 2;

        ProfileManager.Profile profile = ProfileManager.getClientProfile(targetPlayerId);
        String raceLabel = profile.getRace().isEmpty() ? "Human" : profile.getRace();
        int raceU = switch (raceLabel) {
            case "aracha"   -> 1;
            case "dragon"   -> 11;
            case "dwarf"    -> 21;
            case "elf"      -> 31;
            case "fairy"    -> 41;
            case "halfdead" -> 51;
            case "merfolk"  -> 61;
            case "orc"      -> 71;
            case "serpentin"-> 81;
            case "werewolf" -> 91;
            default         -> HUMAN_U;
        };

        // рисуем иконку и текст
        g.blit(ATLAS, baseX + 111 + 41 - 74 - 20 - 20 - 18,
                baseY + 96 + 123 - 194 + 5 + 5,
                raceU, HUMAN_V, HUMAN_W, HUMAN_H, 256, 256);

        int textX = getHumanTextX(raceLabel);
        int textY = baseY + 96 + 2;
        g.drawString(font, raceLabel, textX, textY, 0xFFFFFF, false);
    }

    // Остальные методы копируются из MyProfileScreen без изменений

    private static class TextureButton extends Button {
        private final ResourceLocation atlas;
        private final int u, v;

        public TextureButton(int x, int y, int width, int height, int u, int v,
                             ResourceLocation atlas, OnPress onPress) {
            super(x, y, width, height, Component.empty(), onPress, DEFAULT_NARRATION);
            this.atlas = atlas;
            this.u = u;
            this.v = v;
        }

        @Override
        protected void renderWidget(GuiGraphics g, int mx, int my, float pt) {
            g.blit(atlas, this.getX(), this.getY(), u, v, this.width, this.height, 256, 256);
            if (this.isHovered()) {
                g.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, 0x30FFFFFF);
            }
        }
    }

    // 3. OtherPlayerProfileScreen.java — метод renderTeamList
    private void renderTeamList(GuiGraphics g) {
        // Полностью заменяем на правильную логику приглашений для текущего игрока
        this.renderables.removeIf(w -> w instanceof TextureButton);

        UUID myId = minecraft.player.getUUID();
        List<TeamManager.Team> invitations = new ArrayList<>();
        for (TeamManager.Team team : TeamManager.clientTeams.values()) {
            if (team.getInvited().contains(myId)) {
                invitations.add(team);
            }
        }

        invitations.sort(new Comparator<TeamManager.Team>() {
            @Override
            public int compare(TeamManager.Team t1, TeamManager.Team t2) {
                return t1.getName().compareTo(t2.getName());
            }
        });

        int baseX = (width - GUI_WIDTH) / 2;
        int baseY = (height - GUI_HEIGHT) / 2;
        int startX = baseX + 111 + 41 + 10 + 20 + 10 + 10 + 10 + 1 + (int)(8 / 0.75f);
        int startY = baseY + 30 - 8 + 7 + 1 + 2;

        int maxVisible = Math.min(4, invitations.size());
        for (int i = 0; i < maxVisible; i++) {
            int index = i + scrollOffset;
            if (index >= invitations.size()) break;

            TeamManager.Team team = invitations.get(index);
            int slotY = startY + i * (INV_H + X_H + 2);

            addRenderableWidget(new TextureButton(startX, slotY, INV_W, INV_H, INV_U, INV_V, ATLAS, b -> {
                minecraft.setScreen(new OtherTeamProfileScreen(
                        OtherPlayerProfileScreen.this,
                        team.getName(),
                        team.getTag(),
                        team.showTag(),
                        team.showCompass(),
                        team.isFriendlyFire(),
                        team.getOwner()
                ));
            }));

            addRenderableWidget(new TextureButton(startX + 35, slotY + INV_H + 2, X_W, X_H, X_U, X_V, ATLAS, b -> {
                NetworkHandler.INSTANCE.sendToServer(new RespondInvitationPacket(team.getName(), false));
            }));

            addRenderableWidget(new TextureButton(startX + 60, slotY + INV_H + 2, V_W, V_H, V_U, V_V, ATLAS, b -> {
                NetworkHandler.INSTANCE.sendToServer(new RespondInvitationPacket(team.getName(), true));
            }));
        }
    }

    private void renderCenterBarsAndScroller(GuiGraphics g, int mouseX, int mouseY) {
        renderProgressBar1(g, mouseX, mouseY);
        renderProgressBar2(g, mouseX, mouseY);
        renderScroller(g);
    }
    private void renderProgressBar1(GuiGraphics g, int mouseX, int mouseY) {
        int centerX = width / 2;
        int baseY = (height - GUI_HEIGHT) / 2;
        int x = centerX - BAR_W / 2 + 24;
        int y = baseY + 110 - 34;

        UUID playerUUID = targetPlayerId;
        int completedQuests = FTBQuestsStats.getCompletedQuests(playerUUID);
        int totalQuests = FTBQuestsStats.getTotalQuests();

        int fillPercent = totalQuests > 0 ? (completedQuests * 100) / totalQuests : 0;
        int fillWidth = (int) (BAR_W * fillPercent / 100.0);

        g.blit(ATLAS, x, y, BAR_U, BAR_V, fillWidth, BAR_H, 256, 256);

        if (mouseX >= x && mouseX <= x + BAR_W && mouseY >= y && mouseY <= y + BAR_H) {
            g.renderTooltip(font,
                    Component.translatable("gui.teammod.tooltip.quests", completedQuests, totalQuests),
                    mouseX, mouseY);
        }
    }

    private void renderProgressBar2(GuiGraphics g, int mouseX, int mouseY) {
        int centerX = width / 2;
        int baseY = (height - GUI_HEIGHT) / 2;
        int x = centerX - BAR_W / 2 + 24;
        int y = baseY + 118 - 65;

        UUID playerUUID = targetPlayerId;
        int level = SkillTreeStats.getLevel(playerUUID);
        int currentExp = (int) SkillTreeStats.getCurrentExp(playerUUID);
        int expForNext = SkillTreeStats.getNextLevelCost(playerUUID);

        int fillPercent = expForNext > 0 ? (currentExp * 100) / expForNext : 100;
        int fillWidth = (int) (BAR_W * fillPercent / 100.0);

        g.blit(ATLAS, x, y, BAR_U, BAR_V, fillWidth, BAR_H, 256, 256);

        if (mouseX >= x && mouseX <= x + BAR_W && mouseY >= y && mouseY <= y + BAR_H) {
            if (expForNext > 0) {
                g.renderTooltip(font,
                        Component.translatable("gui.teammod.tooltip.level_progress", currentExp, expForNext, level),
                        mouseX, mouseY);
            } else {
                g.renderTooltip(font,
                        Component.translatable("gui.teammod.tooltip.max_level", level),
                        mouseX, mouseY);
            }
        }
    }

    private void renderScroller(GuiGraphics g) {
        int centerX = width / 2;
        int baseY = (height - GUI_HEIGHT) / 2;
        int scrollerX = centerX - SCROLLER_W / 2 + 92;
        int scrollerBaseY = baseY + 80 - 50;
        int offsetY = 0;
        if (teamList.size() > VISIBLE_SLOTS) {
            float ratio = (float) scrollOffset / (teamList.size() - VISIBLE_SLOTS);
            offsetY = (int) (ratio * (119 - SCROLLER_H));
        }
        g.blit(ATLAS, scrollerX, scrollerBaseY + offsetY,
                SCROLLER_U, SCROLLER_V, SCROLLER_W, SCROLLER_H, 256, 256);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        // Стандартный рендер фона (как в MyProfileScreen)
        this.renderBackground(g);

        // Текстура GUI и все элементы
        renderBg(g, pt, mx, my);
        renderAllElements(g, mx, my);
        super.render(g, mx, my, pt);
    }

    protected void renderBg(GuiGraphics g, float pt, int mx, int my) {
        RenderSystem.setShaderTexture(0, ATLAS);
        int x = (width - GUI_WIDTH) / 2;
        int y = (height - GUI_HEIGHT) / 2;
        g.blit(ATLAS, x, y, 0, 0, GUI_WIDTH, GUI_HEIGHT, 256, 256);
    }

    // Методы скроллинга (аналогично MyProfileScreen)
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (teamList.size() > VISIBLE_SLOTS) {
            int scrollerX = width / 2 - SCROLLER_W / 2 + 92;
            int scrollerY = (height - GUI_HEIGHT) / 2 + 80 - 50;
            int trackHeight = 119;

            if (mouseX >= scrollerX && mouseX <= scrollerX + SCROLLER_W &&
                    mouseY >= scrollerY && mouseY <= scrollerY + trackHeight) {
                isDraggingScroller = true;
                updateScrollFromMouse(mouseY);
                return true;
            }
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

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaY) {
        if (teamList.size() <= VISIBLE_SLOTS) return false;

        int maxScroll = Math.max(0, teamList.size() - VISIBLE_SLOTS);
        scrollOffset -= (int) deltaY;
        scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));

        this.renderables.removeIf(w -> w instanceof TextureButton);
        renderTeamList(null);

        return true;
    }

    private void updateScrollFromMouse(double mouseY) {
        int baseY = (height - GUI_HEIGHT) / 2 + 80 - 50;
        double relativeY = mouseY - baseY;
        relativeY = Math.max(0, Math.min(relativeY, 119 - SCROLLER_H));

        float ratio = (float) relativeY / (119 - SCROLLER_H);
        int maxOffset = Math.max(0, teamList.size() - VISIBLE_SLOTS);
        scrollOffset = (int) (ratio * maxOffset);
        scrollOffset = Math.max(0, Math.min(scrollOffset, maxOffset));

        this.renderables.removeIf(w -> w instanceof TextureButton);
        renderTeamList(null);
    }

    @Override
    public void onClose() {
        ClientState.hidePlayerRender = false; // Сбрасываем флаг
        minecraft.setScreen(parentScreen);
    }

    @Override
    public boolean keyPressed(int key, int scan, int mod) {
        ClientPlayerCache.updateInputTime();
        return super.keyPressed(key, scan, mod);
    }


    @Override
    public boolean charTyped(char c, int mod) {
        lastInputTime.set(System.currentTimeMillis());
        return super.charTyped(c, mod);
    }

    @Override
    public void mouseMoved(double mx, double my) {
        lastInputTime.set(System.currentTimeMillis());
        super.mouseMoved(mx, my);
    }

    // Метод для обновления экипировки при видимости игрока
    private void updatePlayerEquipment(UUID playerUUID) {
        Player targetPlayer = minecraft.level.getPlayerByUUID(playerUUID);
        if (targetPlayer != null) {
            // Сохраняем текущую экипировку
            for (int i = 0; i < 4; i++) {
                lastSeenEquipment[i] = targetPlayer.getItemBySlot(EquipmentSlot.byTypeAndIndex(EquipmentSlot.Type.ARMOR, i)).copy();
            }
            hasSeenPlayer = true;
        }
    }
}
package com.mom.teammod;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mom.teammod.packets.KickPlayerPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class PlayersListScreen extends BaseModScreen {

    private static final ResourceLocation ATLAS = ResourceLocation.fromNamespaceAndPath(TeamMod.MODID,
            "textures/gui/players_list.png");

    // === Твои точные UV (с твоими правками) ===
    private boolean kickPending = false;  // флаг: был ли кик
    private UUID pendingKickPlayer = null; // кого кикнули (для пакета потом)
    private static final int PLASHKA_ONLINE_U  = 1;
    private static final int PLASHKA_ONLINE_V  = 232;
    private static final int PLASHKA_ONLINE_W  = 167;
    private static final int PLASHKA_ONLINE_H  = 23;
    private static final int PLASHKA_OFFLINE_U = 1;
    private static final int PLASHKA_OFFLINE_V = 208;
    private static final int PLASHKA_OFFLINE_W = 167;  // оставляем 168
    private static final int PLASHKA_OFFLINE_H = 23;
    private TeamProfileOwner teamProfileOwner;
    private static final int ZVEZDA_U = 1;
    private static final int ZVEZDA_V = 194;
    private static final int ZVEZDA_W = 7;
    private static final int ZVEZDA_H = 6;

    private static final int LEADER_U = 1;
    private static final int LEADER_V = 201;
    private static final int LEADER_W = 31;
    private static final int LEADER_H = 6;

    private static final int KICK_U = 33;   // +1 слева
    private static final int KICK_V = 201;  // +1 сверху
    private static final int KICK_W = 19;   // -2 по ширине
    private static final int KICK_H = 6;

    private static final int SCROLL_U = 47;
    private static final int SCROLL_V = 171;
    private static final int SCROLL_W = 6;
    private static final int SCROLL_H = 11;
    private static final int SCROLLBAR_HEIGHT = 80;

    private static final int GUI_WIDTH = 256;
    private static final int GUI_HEIGHT = 170;
    private static final int VISIBLE_SLOTS = 3;
    private static final int SLOT_HEIGHT = 28;

    private Screen parent;
    private final String teamName;
    private final List<PlayerEntry> players = new ArrayList<>();

    private int scrollOffset = 0;
    private boolean isDraggingScrollbar = false;

    private EditBox searchBox;
    private final List<PlayerEntry> allPlayers = new ArrayList<>();
    private List<PlayerEntry> filteredPlayers = new ArrayList<>();


    public PlayersListScreen(Screen parentScreen, String teamName) {
        super(parentScreen, Component.literal(""));
        this.parent = parent;
        this.teamName = teamName;
        this.teamProfileOwner = teamProfileOwner;
    }

    private int left() { return (width - GUI_WIDTH) / 2; }
    private int top()  { return (height - GUI_HEIGHT) / 2; }

    @Override
    protected void init() {
        super.init();

        // Поле поиска — поднято на 9 пикселей, как в TeamsListScreen
        searchBox = new EditBox(font, left() + 45 - 21, top() + 32 + 14 - 9 - 6, 165, 8, Component.literal(""));
        searchBox.setBordered(false);
        searchBox.setMaxLength(20);
        searchBox.setTextColor(0xFFFFFF);
        searchBox.setResponder(this::onSearchChanged);
        addRenderableWidget(searchBox);
        int reloadX = left() + GUI_WIDTH - 20;
        int reloadY = top() + 5;
        addRenderableWidget(new ReloadButton(reloadX, reloadY, this::refreshFromSync));
        // Замени свою кнопку "Назад" на эту:
        addRenderableWidget(new Button(left() + 10 + 22, top() + 179 - 38, 30, 12, Component.empty(),
                button -> this.onClose(),
                (supplier) -> Component.literal("Назад"))  // ← вот так правильно
        {
            @Override
            protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
                if (this.isHovered()) {
                    g.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, 0x30FFFFFF);
                }
            }
        });

        Button confirmButton = new Button(left() + 2, top() + 172, 43, 10, Component.empty(), b -> {
            b.visible = false;
        }, narration -> Component.empty()) {
            @Override
            protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
                // Показываем кнопку ТОЛЬКО если количество игроков в allPlayers меньше, чем было изначально
                TeamManager.Team team = TeamManager.clientTeams.get(teamName);
                int realCount = team != null ? team.getMembers().size() : 0;
                boolean listChanged = allPlayers.size() < realCount;

                if (!listChanged) {
                    this.visible = false;
                    return;
                }

                this.visible = true;
                g.blit(ATLAS, getX(), getY(), 2, 172, 43, 10, 256, 256);
                if (this.isHovered()) {
                    g.fill(getX(), getY(), getX() + width, getY() + height, 0x40FFFFFF);
                }
            }
        };
        confirmButton.visible = false;
        addRenderableWidget(confirmButton);

        refreshPlayerList();
    }

    private void refreshPlayerList() {
        allPlayers.clear();
        TeamManager.Team team = TeamManager.clientTeams.get(teamName);
        if (team == null) return;

        UUID ownerId = team.getOwner();
        for (UUID memberId : team.getMembers()) {
            Player p = minecraft.level != null ? minecraft.level.getPlayerByUUID(memberId) : null;
            boolean online = p != null;
            String name = p != null ? p.getName().getString() : "Неизвестно";
            allPlayers.add(new PlayerEntry(memberId, name, online, memberId.equals(ownerId)));
        }

        applySearchFilter(); // обновляем отображение
    }

    public void refreshFromSync() {
        refreshPlayerList();
        repositionSlots();
    }


    private void repositionSlots() {
        // ШАГ 1: Удаляем ВСЕ PlayerSlotWidget И ВСЕ вложенные nameButton
        List<AbstractWidget> toRemove = new ArrayList<>();
        for (var widget : this.renderables) {
            if (widget instanceof PlayerSlotWidget slot) {
                if (slot.nameButton != null) {
                    toRemove.add(slot.nameButton);
                }
                toRemove.add(slot);
            } else if (widget instanceof Button btn && btn.getMessage().getString().isEmpty() == false) {
                // Дополнительно ловим любые кнопки с именами (на всякий случай)
                String text = btn.getMessage().getString();
                if (text.equals(text.trim()) && !text.isEmpty() && Character.isLetter(text.charAt(0))) {
                    toRemove.add(btn);
                }
            }
        }
        this.renderables.removeAll(toRemove);

        int baseX = left() + 45 - 21;
        int baseY = top() + 32 + 14;

        int startIndex = scrollOffset;
        int endIndex = Math.min(startIndex + VISIBLE_SLOTS, filteredPlayers.size());

        for (int i = startIndex; i < endIndex; i++) {
            PlayerEntry entry = filteredPlayers.get(i);
            int slotY = baseY + (i - startIndex) * SLOT_HEIGHT;

            addRenderableWidget(new PlayerSlotWidget(baseX, slotY, entry));
        }
    }

    private void onSearchChanged(String text) {
        applySearchFilter();
    }

    private void applySearchFilter() {
        String query = searchBox.getValue().toLowerCase().trim();

        if (query.isEmpty()) {
            filteredPlayers = new ArrayList<>(allPlayers);
        } else {
            filteredPlayers = allPlayers.stream()
                    .filter(entry -> entry.name.toLowerCase().contains(query))
                    .collect(Collectors.toList());
        }

        scrollOffset = Math.max(0, Math.min(scrollOffset, Math.max(0, filteredPlayers.size() - VISIBLE_SLOTS)));
        repositionSlots();

        // Обновляем все кнопки имен
        for (var widget : this.renderables) {
            if (widget instanceof PlayerSlotWidget slotWidget) {
                slotWidget.updateNameButton();
            }
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float pt) {
        this.renderBackground(g);

        RenderSystem.setShaderTexture(0, ATLAS);
        g.blit(ATLAS, left(), top(), 0, 0, GUI_WIDTH, GUI_HEIGHT, 256, 256);
        // Скроллер — всегда виден
        int scrollX = left() + 45 - 21 - 7 - 1 + 1;  // +1 вправо (как ты просил)
        int scrollY = top() + 32 + 14 - 1 + 1;       // +3 вниз

        int offsetY = 0;
        if (filteredPlayers.size() > VISIBLE_SLOTS) {
            float ratio = (float) scrollOffset / (filteredPlayers.size() - VISIBLE_SLOTS);
            offsetY = (int) (ratio * (SCROLLBAR_HEIGHT - SCROLL_H));
        }
        g.blit(ATLAS, scrollX, scrollY + offsetY, SCROLL_U, SCROLL_V, SCROLL_W, SCROLL_H, 256, 256);

        super.render(g, mouseX, mouseY, pt);
    }

    // ======================= СКРОЛЛ =======================
    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        if (filteredPlayers.size() <= VISIBLE_SLOTS) return false;  // ← filteredPlayers!
        scrollOffset = Math.max(0, Math.min(scrollOffset - (int)delta, filteredPlayers.size() - VISIBLE_SLOTS));
        repositionSlots();
        return true;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        if (filteredPlayers.size() > VISIBLE_SLOTS) {  // ← filteredPlayers!
            int sx = left() + 45 - 21 - 7 - 1 + 1;
            int sy = top() + 32 + 14 - 1 + 1;
            if (mx >= sx && mx <= sx + 8 && my >= sy && my <= sy + SCROLLBAR_HEIGHT) {
                isDraggingScrollbar = true;
                return true;
            }
        }
        return super.mouseClicked(mx, my, btn);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int btn, double dx, double dy) {
        if (isDraggingScrollbar && filteredPlayers.size() > VISIBLE_SLOTS) {  // ← filteredPlayers!
            int sy = top() + 32 + 14 - 1 + 1;
            double rel = my - sy;
            double ratio = rel / (SCROLLBAR_HEIGHT - SCROLL_H);
            scrollOffset = (int)(ratio * (filteredPlayers.size() - VISIBLE_SLOTS));
            scrollOffset = Math.max(0, Math.min(scrollOffset, filteredPlayers.size() - VISIBLE_SLOTS));
            repositionSlots();
            return true;
        }
        return super.mouseDragged(mx, my, btn, dx, dy);
    }

    @Override public boolean mouseReleased(double mx, double my, int btn) {
        isDraggingScrollbar = false;
        return super.mouseReleased(mx, my, btn);
    }

    private class PlayerSlotWidget extends AbstractWidget {
        private final PlayerEntry entry;
        private Button nameButton;
        private int nameButtonWidth; // Ширина кнопки имени

        PlayerSlotWidget(int x, int y, PlayerEntry entry) {
            super(x, y, 167, entry.online ? 23 : 24, Component.empty());
            this.entry = entry;
            updateNameButton();
        }

        private void updateNameButton() {
            // Вычисляем ширину имени
            String displayName = entry.name;
            int textWidth = font.width(displayName);

            // Максимальная ширина для имени (чтобы не залезало на другие элементы)
            int maxWidth = width - 34 - 20; // 34 отступ слева, 20 для кнопки кика и отступов

            if (textWidth > maxWidth) {
                // Укорачиваем имя с многоточием
                displayName = font.plainSubstrByWidth(entry.name, maxWidth - 4) + "..";
                textWidth = font.width(displayName);
            }

            this.nameButtonWidth = textWidth;

            // Обновляем или создаем кнопку
            if (this.nameButton != null) {
                // Обновляем существующую кнопку
                this.nameButton.setWidth(nameButtonWidth);
                this.nameButton.setX(getX() + 34);
                this.nameButton.setY(getY() + 8);
            } else {
                // Создаем новую кнопку
                String finalDisplayName = displayName;
                this.nameButton = new Button(
                        getX() + 34, getY() + 8, // Позиция
                        nameButtonWidth, 8, // Ширина по размеру текста, высота
                        Component.literal(finalDisplayName),
                        btn -> onClickName(),
                        (supplier) -> Component.literal("Профиль " + entry.name)
                ) {
                    @Override
                    public void renderWidget(GuiGraphics g, int mx, int my, float pt) {
                        // Кастомный рендер для текстовой кнопки
                        if (this.isHovered()) {
                            g.fill(this.getX() - 1, this.getY() - 1,
                                    this.getX() + this.width + 1,
                                    this.getY() + this.height + 1,
                                    0x30FFFFFF);
                        }
                        // Рисуем имя
                        g.drawString(font, finalDisplayName, getX(), getY(), 0xFFFFFF, false);
                    }

                    @Override
                    public boolean isMouseOver(double mx, double my) {
                        // Проверяем наведение именно на область кнопки
                        return this.visible &&
                                mx >= (double)this.getX() &&
                                my >= (double)this.getY() &&
                                mx < (double)(this.getX() + this.width) &&
                                my < (double)(this.getY() + this.height);
                    }
                };

                // ВАЖНО: Добавляем кнопку в систему виджетов!
                PlayersListScreen.this.addRenderableWidget(this.nameButton);
            }
        }

        @Override
        public void setX(int x) {
            super.setX(x);
            if (nameButton != null) {
                nameButton.setX(x + 34);
            }
        }

        @Override
        public void setY(int y) {
            super.setY(y);
            if (nameButton != null) {
                nameButton.setY(y + 8);
            }
        }

        @Override
        protected void renderWidget(GuiGraphics g, int mx, int my, float pt) {
            RenderSystem.setShaderTexture(0, ATLAS);

            int u = entry.online ? PLASHKA_ONLINE_U : PLASHKA_OFFLINE_U;
            int v = entry.online ? PLASHKA_ONLINE_V : PLASHKA_OFFLINE_V;
            int w = entry.online ? PLASHKA_ONLINE_W : PLASHKA_OFFLINE_W;
            int h = entry.online ? PLASHKA_ONLINE_H : PLASHKA_OFFLINE_H;

            g.blit(ATLAS, getX(), getY(), u, v, w, h, 256, 256);

            // === ГОЛОВА ===
            int headSize = 20;
            int headX = getX() + 7;
            int headY = getY() + 2;

            Player player = minecraft.level != null ? minecraft.level.getPlayerByUUID(entry.id) : null;
            ResourceLocation skin = player != null
                    ? minecraft.getSkinManager().getInsecureSkinLocation(player.getGameProfile())
                    : minecraft.getSkinManager().getInsecureSkinLocation(minecraft.player.getGameProfile());

            g.blit(skin, headX, headY, headSize, headSize, 8, 8, 8, 8, 64, 64);
            RenderSystem.enableBlend();
            g.blit(skin, headX, headY, headSize, headSize, 40, 8, 8, 8, 64, 64);
            RenderSystem.disableBlend();

            // === РЕНДЕР КНОПКИ ИМЕНИ ===
            if (nameButton != null) {
                nameButton.render(g, mx, my, pt);
            }

            // Leader
            if (entry.isOwner) {
                int lx = getX() + width - LEADER_W - 14;
                int ly = getY() + h - LEADER_H - 2;
                g.blit(ATLAS, lx, ly, LEADER_U, LEADER_V, LEADER_W, LEADER_H, 256, 256);
            }

            // Звёздочка
            if (entry.id.equals(minecraft.player.getUUID())) {
                int starX = getX() + width - ZVEZDA_W - 6;
                int starY = getY() + 4;
                g.blit(ATLAS, starX, starY, ZVEZDA_U, ZVEZDA_V, ZVEZDA_W, ZVEZDA_H, 256, 256);
            }

            // Kick - позиция зависит от ширины имени
            boolean isLeader = TeamManager.clientTeams.get(teamName) != null &&
                    TeamManager.clientTeams.get(teamName).getOwner().equals(minecraft.player.getUUID());

            if (isLeader && !entry.id.equals(minecraft.player.getUUID())) {
                int kickX = getX() + 34 + nameButtonWidth + 3; // Отступ от конца имени
                int kickY = getY() + (h - KICK_H) / 2;

                // Проверка наведения для подсветки
                boolean kickHovered = mx >= kickX && mx <= kickX + KICK_W &&
                        my >= kickY && my <= kickY + KICK_H;

                if (kickHovered) {
                    g.fill(kickX - 2, kickY - 1, kickX + KICK_W + 2, kickY + KICK_H + 1, 0xAAFF4444);
                }
                g.blit(ATLAS, kickX, kickY, KICK_U, KICK_V, KICK_W, KICK_H, 256, 256);
            }
        }

        @Override
        public void onClick(double mx, double my) {
            if (nameButton != null && nameButton.isMouseOver(mx, my)) {
                nameButton.onClick(mx, my);
                return;
            }

            boolean isLeader = TeamManager.clientTeams.get(teamName) != null &&
                    TeamManager.clientTeams.get(teamName).getOwner().equals(minecraft.player.getUUID());

            if (isLeader && !entry.id.equals(minecraft.player.getUUID())) {
                int kickX = getX() + 34 + nameButtonWidth + 3;
                int kickY = getY() + (height - KICK_H) / 2;

                if (mx >= kickX && mx <= kickX + KICK_W && my >= kickY && my <= kickY + KICK_H) {
                    // Отправляем кик сразу
                    NetworkHandler.INSTANCE.sendToServer(new KickPlayerPacket(teamName, entry.id));

                    // Сразу убираем из локального списка — визуально исчезает
                    PlayersListScreen.this.allPlayers.removeIf(e -> e.id.equals(entry.id));
                    PlayersListScreen.this.applySearchFilter();

                    // Зажигаем кнопку "Подтвердить" — она уже есть в init()
                    // Ничего не делаем — она сама загорится в render() по флагу kickPending
                    // но мы его убрали → см. ниже
                    return;
                }
            }
        }

        private void onClickName() {
            if (entry.id.equals(minecraft.player.getUUID())) {
                // Открываем свой профиль, передаём текущий экран как parent
                minecraft.setScreen(new MyProfileScreen(
                        PlayersListScreen.this,  // ← ВОТ ЭТО ГЛАВНОЕ!
                        Component.translatable("gui.teammod.profile")
                ));
            } else {
                // Чужой профиль — всё ок, ты уже передаёшь parent
                minecraft.setScreen(new OtherPlayerProfileScreen(PlayersListScreen.this, entry.id, Component.literal("Профиль " + entry.name)));
            }
        }

        @Override
        protected void updateWidgetNarration(net.minecraft.client.gui.narration.NarrationElementOutput narration) {
            this.defaultButtonNarrationText(narration);
        }

        @Override
        public boolean isMouseOver(double mx, double my) {
            // Проверяем наведение на весь виджет или его части
            return super.isMouseOver(mx, my) ||
                    (nameButton != null && nameButton.isMouseOver(mx, my));
        }
    }

    private static class PlayerEntry {
        final UUID id;
        final String name;
        final boolean online;
        final boolean isOwner;

        PlayerEntry(UUID id, String name, boolean online, boolean isOwner) {
            this.id = id;
            this.name = name;
            this.online = online;
            this.isOwner = isOwner;
        }
    }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    public void onClose() {
        if (teamProfileOwner != null) minecraft.setScreen(teamProfileOwner);
        else super.onClose();                 // fallback
    }
}
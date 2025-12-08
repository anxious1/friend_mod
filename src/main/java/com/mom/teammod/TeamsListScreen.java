package com.mom.teammod;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.*;
import java.util.stream.Collectors;

public class TeamsListScreen extends Screen {

    private static final ResourceLocation ATLAS = ResourceLocation.fromNamespaceAndPath(TeamMod.MODID, "textures/gui/team_list.png");

    // Ячейки
    private static final int AVAIL_U = 1,   AVAIL_V = 232, AVAIL_W = 167, AVAIL_H = 23;
    private static final int UNAVAIL_U = 0, UNAVAIL_V = 207, UNAVAIL_W = 168, UNAVAIL_H = 25;
    private static final int REQUEST_U = 0, REQUEST_V = 200, REQUEST_W = 43, REQUEST_H = 7;

    // Ползунок из разметки (8×12)
    private static final int SCROLL_U = 81, SCROLL_V = 195, SCROLL_W = 8, SCROLL_H = 12;

    // Зона скроллбара (80×8)
    private static final int SCROLLBAR_HEIGHT = 80;
    private static final int SCROLLBAR_WIDTH = 8;

    // Цифры
    private static final int[] DIGIT_U = {45, 55, 64, 72};
    private static final int DIGIT_V = 193;
    private static final int DIGIT_W = 8, DIGIT_H = 14;

    private static final int GUI_WIDTH = 256;
    private static final int GUI_HEIGHT = 170;
    private static final int VISIBLE_SLOTS = 3;
    private static final int SLOT_HEIGHT = 28;

    private final List<TeamEntry> allTeams = new ArrayList<>();
    private List<TeamEntry> filteredTeams = new ArrayList<>();

    private int scrollOffset = 0;
    private boolean isDraggingScrollbar = false;
    private EditBox searchBox;

    public TeamsListScreen() {
        super(Component.literal(""));
    }

    private int left() { return (width - GUI_WIDTH) / 2; }
    private int top()  { return (height - GUI_HEIGHT) / 2; }

    @Override
    protected void init() {
        super.init();

        int x = left();
        int y = top();

        addTransparentButton(
                left() + 10 + 22,
                top() + 179 - 38,
                30, 12,
                () -> minecraft.setScreen(new TeamScreen(
                TeamsListScreen.this,                                      // ← parentScreen
                new TeamMenu(0, minecraft.player.getInventory()),         // ← создаём меню
                minecraft.player.getInventory(),
                Component.translatable("gui.teammod.team_tab"))),
                Component.literal("Назад")
        );

        // Поле поиска — поднято на 9 пикселей
        searchBox = new EditBox(font, x + 45 - 21, y + 32 + 14 - 9 - 6 , 165, 8, Component.literal(""));
        searchBox.setBordered(false);
        searchBox.setMaxLength(20);
        searchBox.setTextColor(0xFFFFFF);
        searchBox.setResponder(this::onSearchChanged);
        addRenderableWidget(searchBox);

        refreshTeamList();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) { // 256 = клавиша ESC
            minecraft.setScreen(new TeamScreen(
                    TeamsListScreen.this,
                    new TeamMenu(0, minecraft.player.getInventory()),
                    minecraft.player.getInventory(),
                    Component.translatable("gui.teammod.team_tab")
            ));
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private Button addTransparentButton(int x, int y, int w, int h, Runnable action, Component tooltip) {
        Button button = new Button(x, y, w, h, Component.empty(), b -> action.run(), (s) -> Component.empty()) {
            @Override
            public void renderWidget(GuiGraphics g, int mx, int my, float pt) {
                if (this.isHovered()) {
                    g.fill(this.getX(), this.getY(), this.getX() + w, this.getY() + h, 0x30FFFFFF);
                }
            }
        };
        button.setTooltip(Tooltip.create(tooltip));
        return addRenderableWidget(button);
    }

    private void refreshTeamList() {
        Set<String> myTeams = TeamManager.clientPlayerTeams.getOrDefault(minecraft.player.getUUID(), Collections.emptySet());

        // Обновляем только если список изменился
        boolean changed = allTeams.size() != TeamManager.clientTeams.size();
        if (!changed) {
            for (TeamEntry entry : allTeams) {
                if (!myTeams.contains(entry.team.getName()) != !entry.isMember) {
                    changed = true;
                    break;
                }
            }
        }

        if (changed) {
            allTeams.clear();
            for (TeamManager.Team team : TeamManager.clientTeams.values()) {
                allTeams.add(new TeamEntry(team, myTeams.contains(team.getName())));
            }
            allTeams.sort(Comparator.comparing(t -> t.team.getName()));
            applySearchFilter();
        }
    }

    private void applySearchFilter() {
        String query = searchBox.getValue().toLowerCase().trim();

        if (query.isEmpty()) {
            filteredTeams = new ArrayList<>(allTeams);
        } else {
            filteredTeams = allTeams.stream()
                    .filter(entry -> {
                        String name = entry.team.getName().toLowerCase();
                        String tag = entry.team.getTag().toLowerCase();
                        return name.contains(query) || tag.contains(query) || (query.length() == 3 && tag.equals(query));
                    })
                    .collect(Collectors.toList());
        }

        // Сохраняем позицию скролла при фильтрации
        scrollOffset = Math.max(0, Math.min(scrollOffset, Math.max(0, filteredTeams.size() - VISIBLE_SLOTS)));
        repositionSlots();
    }

    private void onSearchChanged(String text) {
        applySearchFilter();
    }

    private void repositionSlots() {
        // Удаляем только виджеты TeamSlotWidget
        this.renderables.removeIf(w -> w instanceof TeamSlotWidget);

        int baseX = left() + 45 - 21;
        int baseY = top() + 32 + 14;

        for (int i = 0; i < VISIBLE_SLOTS; i++) {
            int index = scrollOffset + i;
            if (index >= filteredTeams.size()) break;

            TeamEntry entry = filteredTeams.get(index);
            int y = baseY + i * (SLOT_HEIGHT - 2);
            addRenderableWidget(new TeamSlotWidget(baseX, y, entry));
        }
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        renderBackground(g);
        RenderSystem.setShaderTexture(0, ATLAS);
        g.blit(ATLAS, left(), top(), 0, 0, GUI_WIDTH, GUI_HEIGHT, 256, 256);

        // Цифры
        int count = TeamManager.clientPlayerTeams.getOrDefault(minecraft.player.getUUID(), Collections.emptySet()).size();
        if (count > 0 && count <= 3) {
            g.blit(ATLAS, left() + 45 + 100, top() + 193 - 54, DIGIT_U[count], DIGIT_V, DIGIT_W, DIGIT_H, 256, 256);
        }

        // Скроллбар — только ползунок 8×12
        if (filteredTeams.size() > VISIBLE_SLOTS) {
            int scrollX = left() + 45 - 21 - 7 - 1;
            int scrollY = top() + 32 + 14 - 1;

            int totalHeight = SCROLLBAR_HEIGHT;
            int handleHeight = SCROLL_H;
            int travel = totalHeight - handleHeight;

            float ratio = (float) scrollOffset / Math.max(1, filteredTeams.size() - VISIBLE_SLOTS);
            int offsetY = (int) (ratio * travel);

            g.blit(ATLAS, scrollX, scrollY + offsetY, SCROLL_U, SCROLL_V, SCROLL_W, handleHeight, 256, 256);
        }

        super.render(g, mx, my, pt);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (filteredTeams.size() <= VISIBLE_SLOTS) return false;

        scrollOffset -= (int) delta;
        scrollOffset = Math.max(0, Math.min(scrollOffset, filteredTeams.size() - VISIBLE_SLOTS));
        repositionSlots();
        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (filteredTeams.size() > VISIBLE_SLOTS) {
            int scrollX = left() + 45 - 21 - 7 - 1;
            int scrollY = top() + 32 + 14 - 1;
            int totalHeight = SCROLLBAR_HEIGHT;

            if (mouseX >= scrollX && mouseX <= scrollX + SCROLLBAR_WIDTH &&
                    mouseY >= scrollY && mouseY <= scrollY + totalHeight) {
                isDraggingScrollbar = true;
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (isDraggingScrollbar && filteredTeams.size() > VISIBLE_SLOTS) {
            int scrollY = top() + 32 + 14 - 1;
            int totalHeight = SCROLLBAR_HEIGHT;
            int handleHeight = SCROLL_H;
            int travel = totalHeight - handleHeight;

            double relativeY = mouseY - scrollY;
            double ratio = relativeY / travel;
            scrollOffset = (int) (ratio * (filteredTeams.size() - VISIBLE_SLOTS));
            scrollOffset = Math.max(0, Math.min(scrollOffset, filteredTeams.size() - VISIBLE_SLOTS));
            repositionSlots();
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        isDraggingScrollbar = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public void tick() {
        refreshTeamList(); // Оставляем — но теперь он не сбрасывает скролл
    }

    private class TeamSlotWidget extends Button {
        private final TeamEntry entry;

        public TeamSlotWidget(int x, int y, TeamEntry entry) {
            super(x, y, 167, 23, Component.empty(), btn -> {
                // При клике открываем профиль команды
                openTeamProfile(entry.team);
            }, DEFAULT_NARRATION);
            this.entry = entry;
        }

        @Override
        protected void renderWidget(GuiGraphics g, int mx, int my, float pt) {
            // Убрать проверку на isMember и full - кнопка всегда кликабельна для просмотра
            int u, v, h;
            if (this.isHovered()) {
                // Наведение - avail (даже для своих команд)
                u = AVAIL_U;
                v = AVAIL_V;
                h = AVAIL_H;
            } else {
                // Базовое состояние - unavail
                u = UNAVAIL_U;
                v = UNAVAIL_V;
                h = UNAVAIL_H;
            }

            RenderSystem.setShaderTexture(0, ATLAS);
            g.blit(ATLAS, getX(), getY(), u, v, width, h, 256, 256);

            String text = entry.team.getName();
            if (!entry.team.getTag().isEmpty()) text += "[" + entry.team.getTag() + "]";
            g.drawString(font, text, getX() + 8, getY() + 8, 0xFFFFFF, false);

            // Показываем "request" только при наведении на НЕ свою команду
            if (this.isHovered() && !entry.isMember) {
                boolean full = TeamManager.clientPlayerTeams.getOrDefault(minecraft.player.getUUID(), Collections.emptySet()).size() >= 3;
                if (!full) {
                    g.blit(ATLAS, getX() + width - REQUEST_W - 5, getY() + height - REQUEST_H - 3,
                            REQUEST_U, REQUEST_V, REQUEST_W, REQUEST_H, 256, 256);
                }
            }
        }

        @Override
        public void updateWidgetNarration(net.minecraft.client.gui.narration.NarrationElementOutput output) {}
    }

    // Добавить этот метод в основной класс TeamsListScreen:
    private void openTeamProfile(TeamManager.Team team) {
        if (team != null) {
            // Проверяем, является ли игрок участником этой команды
            boolean isMember = TeamManager.clientPlayerTeams.getOrDefault(minecraft.player.getUUID(), Collections.emptySet())
                    .contains(team.getName());

            if (isMember) {
                // Если игрок - участник, открываем его профиль команды (TeamProfileOwner)
                minecraft.setScreen(new TeamProfileOwner(
                        null, // TeamMenu может быть null
                        minecraft.player.getInventory(),
                        Component.literal(team.getName()),
                        team.getName(),
                        team.getTag(),
                        team.showTag(),
                        team.showCompass(),
                        team.isFriendlyFire()
                ));
            } else {
                // Если игрок НЕ участник, открываем просмотр чужой команды (OtherTeamProfileScreen)
                minecraft.setScreen(new OtherTeamProfileScreen(
                        TeamsListScreen.this, // parent screen
                        team.getName(),
                        team.getTag(),
                        team.showTag(),
                        team.showCompass(),
                        team.isFriendlyFire(),
                        team.getOwner() // передаем UUID владельца
                ));
            }
        }
    }

    private static class TeamEntry {
        final TeamManager.Team team;
        final boolean isMember;

        TeamEntry(TeamManager.Team team, boolean isMember) {
            this.team = team;
            this.isMember = isMember;
        }
    }
}
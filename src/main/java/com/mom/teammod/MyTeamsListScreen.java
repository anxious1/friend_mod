package com.mom.teammod;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.*;

public class MyTeamsListScreen extends Screen {

    private static final ResourceLocation ATLAS = ResourceLocation.fromNamespaceAndPath(TeamMod.MODID,
            "textures/gui/my_teams_list.png");

    // Исправить UV-координаты согласно разметке XML
    private static final int PLASHKA_BASIC_U = 0,   PLASHKA_BASIC_V = 207,  PLASHKA_BASIC_W = 168, PLASHKA_BASIC_H = 24;
    private static final int PLASHKA_HOVER_U = 1,   PLASHKA_HOVER_V = 232,  PLASHKA_HOVER_W = 168, PLASHKA_HOVER_H = 24; // ← изменить на 24
    private static final int PLASHKA_CLICKED_U = 0, PLASHKA_CLICKED_V = 183, PLASHKA_CLICKED_W = 169, PLASHKA_CLICKED_H = 24;

    // Кнопка подтверждения
    private static final int CONFIRM_U = 0, CONFIRM_V = 147, CONFIRM_W = 47, CONFIRM_H = 14;

    private static final int GUI_WIDTH = 256;
    private static final int GUI_HEIGHT = 147;
    private static final int VISIBLE_SLOTS = 4;
    private static final int SLOT_HEIGHT = 30;

    private static class TestTeam {
        final String name;
        final String tag;

        TestTeam(String name, String tag) {
            this.name = name;
            this.tag = tag;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            TestTeam testTeam = (TestTeam) obj;
            return Objects.equals(name, testTeam.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name);
        }
    }

    private final List<TestTeam> teamList = Arrays.asList(
            new TestTeam("Охотники за головами", "ОГ"),
            new TestTeam("Строители мира", "СМ"),
            new TestTeam("Ночные рейдеры", "НР")
    );

    private final Set<String> clickedTeams = new HashSet<>();
    private int scrollOffset = 0;

    private final Screen parentScreen;
    private Button confirmButton;

    public MyTeamsListScreen(Screen parentScreen) {
        super(Component.literal(""));
        this.parentScreen = parentScreen;
    }

    private int left() { return (width - GUI_WIDTH) / 2; }
    private int top()  { return (height - GUI_HEIGHT) / 2; }

    @Override
    protected void init() {
        super.init();

        int x = left();
        int y = top();

        // Кнопка "Назад"
        addTransparentButton(
                x + 10 + 22,
                y + GUI_HEIGHT - 20 - 9, // ← поднять на 20 пикселей
                30, 12,
                () -> minecraft.setScreen(parentScreen),
                Component.literal("Назад")
        );

        confirmButton = new Button(
                x + GUI_WIDTH - CONFIRM_W - 20 - 22 - 32,
                y + GUI_HEIGHT - CONFIRM_H - 15 - 20 + 11 + 8,
                CONFIRM_W, CONFIRM_H,
                Component.empty(),
                btn -> sendInvite(),
                (s) -> Component.empty()
        ) {
            @Override
            public void renderWidget(GuiGraphics g, int mx, int my, float pt) {
                // Рисуем текстуру только если выбрана хотя бы одна команда
                if (!clickedTeams.isEmpty()) {
                    g.blit(ATLAS, this.getX(), this.getY(), CONFIRM_U, CONFIRM_V, CONFIRM_W, CONFIRM_H, 256, 256);
                }

                // Подсветка при наведении (только если есть текстурка)
                if (this.isHovered() && !clickedTeams.isEmpty()) {
                    g.fill(this.getX(), this.getY(), this.getX() + CONFIRM_W, this.getY() + CONFIRM_H, 0x30FFFFFF);
                }
            }
        };

        confirmButton.setTooltip(Tooltip.create(Component.literal("Подтвердить выбор")));
        addRenderableWidget(confirmButton);

        createTeamButtons();
    }

    private void createTeamButtons() {
        this.renderables.removeIf(w -> w instanceof TeamButton);

        int baseX = left() + 45 - 24;
        int baseY = top() + 32 + 14 - 14;

        for (int i = 0; i < VISIBLE_SLOTS; i++) {
            int index = scrollOffset + i;
            if (index >= teamList.size()) break;

            TestTeam team = teamList.get(index);
            int buttonY = baseY + i * (PLASHKA_BASIC_H);

            // УБЕРИТЕ лямбду здесь - просто передайте null или метод onClick сам вызовет onTeamClicked
            addRenderableWidget(new TeamButton(
                    baseX, buttonY,
                    PLASHKA_BASIC_W, PLASHKA_BASIC_H,
                    team,
                    btn -> {} // Пустой обработчик, так как onClick уже вызывает onTeamClicked
            ));
        }
    }

    private void onTeamClicked(TestTeam team) {
        System.out.println("Клик по команде: " + team.name + " (было выбрано: " + clickedTeams.contains(team.name) + ")");

        // Если команда уже выбрана - снимаем выбор
        if (clickedTeams.contains(team.name)) {
            clickedTeams.remove(team.name);
            System.out.println("Снят выбор с команды: " + team.name);
        } else {
            // Если можно выбрать еще (максимум 3)
            if (clickedTeams.size() < 3) {
                clickedTeams.add(team.name);
                System.out.println("Выбрана команда: " + team.name);
            } else {
                System.out.println("Достигнут лимит выбора (максимум 3 команды)");
            }
        }

        // Обновляем все кнопки
        updateTeamButtons();
    }

    private void updateTeamButtons() {
        for (var widget : renderables) {
            if (widget instanceof TeamButton teamButton) {
                // Кнопка активна, если её команда выбрана
                teamButton.active = clickedTeams.contains(teamButton.team.name);
                // Принудительно запрашиваем перерисовку кнопки
                teamButton.setFocused(false); // Сбрасываем фокус для обновления отображения
            }
        }
    }

    private void sendInvite() {
        if (!clickedTeams.isEmpty()) {
            System.out.println("Отправка приглашений в команды: " + clickedTeams);
            // TODO: Отправить пакеты на сервер для приглашения игрока во все выбранные команды
            // for (String teamName : clickedTeams) {
            //     NetworkHandler.INSTANCE.sendToServer(new InvitePlayerPacket(targetPlayerId, teamName));
            // }
            minecraft.setScreen(parentScreen);
        }
    }

    private Button addTransparentButton(int x, int y, int w, int h, Runnable action, Component tooltip) {
        Button button = new Button(x, y, w, h, Component.empty(), b -> action.run(), (s) -> Component.empty()) {
            @Override
            public void renderWidget(GuiGraphics g, int mx, int my, float pt) {
                // Подсветка при наведении
                if (this.isHovered()) {
                    g.fill(this.getX(), this.getY(), this.getX() + w, this.getY() + h, 0x30FFFFFF);
                }
            }
        };
        button.setTooltip(Tooltip.create(tooltip));
        return addRenderableWidget(button);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);
        RenderSystem.setShaderTexture(0, ATLAS);
        g.blit(ATLAS, left(), top(), 0, 0, GUI_WIDTH, GUI_HEIGHT, 256, 256);

        super.render(g, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) { // ESC
            minecraft.setScreen(parentScreen);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (teamList.size() <= VISIBLE_SLOTS) return false;

        scrollOffset -= (int) delta;
        scrollOffset = Math.max(0, Math.min(scrollOffset, teamList.size() - VISIBLE_SLOTS));
        createTeamButtons();
        return true;
    }

    private class TeamButton extends Button {
        private final TestTeam team;
        private boolean active = false;

        public TeamButton(int x, int y, int width, int height, TestTeam team, Button.OnPress onPress) {
            super(x, y, width, height, Component.empty(), onPress, DEFAULT_NARRATION);
            this.team = team;
            this.active = clickedTeams.contains(team.name);
        }

        @Override
        public void onClick(double mouseX, double mouseY) {
            super.onClick(mouseX, mouseY);
            // Вызываем обработчик клика по команде
            MyTeamsListScreen.this.onTeamClicked(team);
        }

        @Override
        public void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
            // Обновляем состояние active каждый кадр на случай, если изменилось через updateTeamButtons
            this.active = clickedTeams.contains(team.name);

            int u, v;

            if (this.active) {
                u = PLASHKA_CLICKED_U;
                v = PLASHKA_CLICKED_V;
            } else if (this.isHovered()) {
                u = PLASHKA_HOVER_U;
                v = PLASHKA_HOVER_V;
            } else {
                u = PLASHKA_BASIC_U;
                v = PLASHKA_BASIC_V;
            }

            RenderSystem.setShaderTexture(0, ATLAS);
            g.blit(ATLAS,
                    this.getX(),
                    this.getY(),
                    u, v,
                    this.width,
                    this.height,
                    256, 256);

            // Текст команды
            String displayText = team.name;
            if (team.tag != null && !team.tag.isEmpty()) {
                displayText += " [" + team.tag + "]";
            }

            int textColor = 0xFFFFFF;
            g.drawString(font, displayText, this.getX() + 10, this.getY() + 8, textColor, false);
        }
    }

    @Override
    public void resize(Minecraft minecraft, int width, int height) {
        int savedScroll = this.scrollOffset;
        Set<String> savedClicked = new HashSet<>(this.clickedTeams); // сохраняем выбор

        this.init(minecraft, width, height);

        this.scrollOffset = savedScroll;
        this.clickedTeams.clear();
        this.clickedTeams.addAll(savedClicked);

        // Пересоздаём кнопки команд с новым выбором
        createTeamButtons();
    }
}
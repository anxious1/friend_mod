package com.mom.teammod;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Set;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = TeamMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = net.minecraftforge.api.distmarker.Dist.CLIENT)
public class ChatHook {

    // Popup для игрока
    private static class PlayerChatPopup extends Screen {
        private final UUID targetUUID;
        private final String playerName;
        private final Screen parent;

        protected PlayerChatPopup(UUID targetUUID, String playerName, Screen parent) {
            super(Component.empty());
            this.targetUUID = targetUUID;
            this.playerName = playerName;
            this.parent = parent;
        }

        @Override
        protected void init() {
            int centerX = width / 2;
            int centerY = height / 2;

            addRenderableWidget(Button.builder(Component.literal("Открыть профиль"), b -> {
                if (targetUUID.equals(minecraft.player.getUUID())) {
                    minecraft.setScreen(new MyProfileScreen(parent, Component.translatable("gui.teammod.profile")));
                } else {
                    minecraft.setScreen(new OtherPlayerProfileScreen(targetUUID, parent, Component.literal("Профиль " + playerName)));
                }
            }).pos(centerX - 100, centerY - 30).size(200, 20).build());

            addRenderableWidget(Button.builder(Component.literal("Написать личное сообщение"), b -> {
                minecraft.setScreen(new ChatScreen("/msg " + playerName + " "));
            }).pos(centerX - 100, centerY + 10).size(200, 20).build());
        }

        @Override
        public void render(GuiGraphics g, int mx, int my, float pt) {
            this.renderBackground(g);
            super.render(g, mx, my, pt);
        }

        @Override
        public void onClose() {
            minecraft.setScreen(parent);
        }

        @Override
        public boolean shouldCloseOnEsc() {
            return true;
        }
    }

    // Popup для команды
    private static class TeamChatPopup extends Screen {
        private final String teamName;
        private final Screen parent;

        protected TeamChatPopup(String teamName, Screen parent) {
            super(Component.empty());
            this.teamName = teamName;
            this.parent = parent;
        }

        @Override
        protected void init() {
            int centerX = width / 2;
            int centerY = height / 2;

            addRenderableWidget(Button.builder(Component.literal("Открыть профиль команды \"" + teamName + "\""), b -> {
                TeamManager.Team team = TeamManager.clientTeams.get(teamName);
                if (team != null) {
                    UUID myUUID = minecraft.player.getUUID();
                    boolean isOwner = team.getOwner().equals(myUUID);
                    boolean isMember = team.getMembers().contains(myUUID);

                    if (isOwner) {
                        minecraft.setScreen(new TeamProfileOwner(null, minecraft.player.getInventory(), Component.literal(teamName), teamName, team.getTag(), team.showTag(), team.showCompass(), team.isFriendlyFire()));
                    } else if (isMember) {
                        minecraft.setScreen(new TeamMemberScreen(parent, teamName, team.getTag(), team.showTag(), team.showCompass(), team.isFriendlyFire(), team.getOwner()));
                    } else {
                        minecraft.setScreen(new OtherTeamProfileScreen(parent, teamName, team.getTag(), team.showTag(), team.showCompass(), team.isFriendlyFire(), team.getOwner()));
                    }
                }
            }).pos(centerX - 150, centerY).size(300, 20).build());
        }

        @Override
        public void render(GuiGraphics g, int mx, int my, float pt) {
            this.renderBackground(g);
            super.render(g, mx, my, pt);
        }

        @Override
        public void onClose() {
            minecraft.setScreen(parent);
        }

        @Override
        public boolean shouldCloseOnEsc() {
            return true;
        }
    }

    // Ник игрока
    private static Component makePlayerNick(String playerName, UUID playerUUID, boolean isMe) {
        int nickColor = isMe ? 0x55FF55 : 0xFFFFFF;

        return Component.literal(playerName)
                .withStyle(Style.EMPTY
                        .withColor(nickColor)
                        .withBold(true)
                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/teammod_open_profile " + playerUUID))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("§7Клик — открыть профиль игрока"))));
    }

    // Тег команды
    private static Component makeTeamTag(String tag, String teamName, boolean isTeammate) {
        if (tag.isEmpty()) return Component.literal("");

        int tagColor = isTeammate ? 0xFFAA00 : 0xAAAAAA;

        return Component.literal("[" + tag + "]")
                .withStyle(Style.EMPTY
                        .withColor(tagColor)
                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/teammod_open_team " + teamName))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("§7Клик — открыть профиль команды"))));
    }

    @SubscribeEvent
    public static void onChatReceived(ClientChatReceivedEvent event) {
        Component original = event.getMessage();
        String text = original.getString();

        if (!text.matches("^<[^>]+> .+$")) return;

        int start = text.indexOf('<') + 1;
        int end = text.indexOf('>');
        if (start <= 0 || end <= start) return;

        String nick = text.substring(start, end);
        String rest = text.substring(end + 2);

        Player sender = Minecraft.getInstance().level.players().stream()
                .filter(p -> p.getName().getString().equals(nick))
                .findFirst()
                .orElse(null);

        if (sender == null) return;

        UUID senderUUID = sender.getUUID();
        UUID myUUID = Minecraft.getInstance().player.getUUID();
        boolean isMe = myUUID.equals(senderUUID);

        // Проверяем, союзник ли (хотя бы по одной команде)
        boolean isTeammate = TeamManager.clientPlayerTeams.getOrDefault(myUUID, Set.of())
                .stream()
                .anyMatch(teamName -> {
                    TeamManager.Team team = TeamManager.clientTeams.get(teamName);
                    return team != null && team.getMembers().contains(senderUUID);
                });

        // Ищем первую команду с тегом у отправителя
        String teamTag = "";
        String teamNameForTag = "";
        for (TeamManager.Team team : TeamManager.clientTeams.values()) {
            if (team.getMembers().contains(senderUUID) && team.showTag() && !team.getTag().isEmpty()) {
                teamTag = team.getTag();
                teamNameForTag = team.getName();
                break;
            }
        }

        Component message = Component.literal("<")
                .append(makePlayerNick(nick, senderUUID, isMe))
                .append(makeTeamTag(teamTag, teamNameForTag, isTeammate))
                .append("> ")
                .append(Component.literal(rest));

        event.setMessage(message);
    }

    @SubscribeEvent
    public static void onChatMouseClick(ScreenEvent.MouseButtonPressed.Pre event) {
        if (!(event.getScreen() instanceof ChatScreen)) return;

        Minecraft mc = Minecraft.getInstance();
        double mouseX = event.getMouseX();
        double mouseY = event.getMouseY();

        Style style = mc.gui.getChat().getClickedComponentStyleAt(mouseX, mouseY);
        if (style == null || style.getClickEvent() == null) return;

        ClickEvent click = style.getClickEvent();
        String value = click.getValue();

        if (value.startsWith("/teammod_open_profile ")) {
            String uuidStr = value.substring("/teammod_open_profile ".length());
            try {
                UUID uuid = UUID.fromString(uuidStr);
                Player player = mc.level.getPlayerByUUID(uuid);
                String name = player != null ? player.getName().getString() : "Игрок";

                if (uuid.equals(mc.player.getUUID())) {
                    mc.setScreen(new MyProfileScreen(event.getScreen(), Component.translatable("gui.teammod.profile")));
                } else {
                    mc.setScreen(new OtherPlayerProfileScreen(uuid, event.getScreen(), Component.literal("Профиль " + name)));
                }
                event.setCanceled(true);
            } catch (Exception ignored) {}
        } else if (value.startsWith("/teammod_open_team ")) {
            String teamName = value.substring("/teammod_open_team ".length());

            TeamManager.Team team = TeamManager.clientTeams.get(teamName);
            if (team != null) {
                UUID myUUID = mc.player.getUUID();
                boolean isOwner = team.getOwner().equals(myUUID);
                boolean isMember = team.getMembers().contains(myUUID);

                if (isOwner) {
                    mc.setScreen(new TeamProfileOwner(null, mc.player.getInventory(),
                            Component.literal(teamName), teamName, team.getTag(),
                            team.showTag(), team.showCompass(), team.isFriendlyFire()));
                } else if (isMember) {
                    mc.setScreen(new TeamMemberScreen(event.getScreen(), teamName, team.getTag(),
                            team.showTag(), team.showCompass(), team.isFriendlyFire(), team.getOwner()));
                } else {
                    mc.setScreen(new OtherTeamProfileScreen(event.getScreen(), teamName, team.getTag(),
                            team.showTag(), team.showCompass(), team.isFriendlyFire(), team.getOwner()));
                }
            }
            event.setCanceled(true);
        }
    }
}
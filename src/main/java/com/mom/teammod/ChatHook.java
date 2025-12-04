package com.mom.teammod;

import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.UUID;

@Mod.EventBusSubscriber(modid = TeamMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = net.minecraftforge.api.distmarker.Dist.CLIENT)
public class ChatHook {
    @SubscribeEvent
    public static void onChatRender(ScreenEvent.Render.Pre event) {
        if (event.getScreen() instanceof ChatScreen) {
            // TODO: Перебрать chat components, найти ники/теги, добавить hover для стиля (bold, brighter/white)
            // Пример: component.setStyle(Style.EMPTY.withBold(true).withColor(0xFFFFFF));
        }
    }

    @SubscribeEvent
    public static void onChatMouseClick(ScreenEvent.MouseButtonPressed.Pre event) {
        if (event.getScreen() instanceof ChatScreen) {
            // Определить клик на ник, открыть popup
            // double mouseX = event.getMouseX(), mouseY = event.getMouseY();
            // Найти component под мышью, если ник -> openChatPopup(nick, uuid)
        }
    }

    private static void openChatPopup(String nick, UUID uuid) {
        // Popup окошко (мини-screen или widgets)
        // Button 1: View Profile -> new OtherProfileScreen(uuid, current, ...)
        // Button 2: Send PM -> input box, send PrivateMessagePacket(minecraft.player.getUUID(), uuid, message)
        // Button 3: Team (если релевантно, напр. invite)
    }
}
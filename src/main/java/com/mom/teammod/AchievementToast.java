package com.mom.teammod;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastComponent;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class AchievementToast implements Toast {
    private static final ResourceLocation POSITIVE_TEXTURE = ResourceLocation.fromNamespaceAndPath(TeamMod.MODID, "textures/gui/toast_positive.png");
    private static final ResourceLocation NEGATIVE_TEXTURE = ResourceLocation.fromNamespaceAndPath(TeamMod.MODID, "textures/gui/toast_negative.png");

    private final Component title;
    private final Component description;
    private final ItemStack icon;
    private final boolean isPositive; // используем флаг вместо FrameType

    public AchievementToast(Component title, Component description, ItemStack icon, boolean isPositive) {
        this.title = title;
        this.description = description;
        this.icon = icon;
        this.isPositive = isPositive;
    }

    public static void show(String title, String description, String iconItemName, boolean isPositive) {
        ItemStack icon = ItemStack.EMPTY;
        if (iconItemName != null && !iconItemName.isEmpty()) {
            try {
                icon = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(iconItemName.toLowerCase())));
            } catch (Exception ignored) {}
        }

        Minecraft.getInstance().getToasts().addToast(new AchievementToast(
                Component.literal(title),
                Component.literal(description),
                icon,
                isPositive
        ));
    }

    @Override
    public Visibility render(GuiGraphics g, ToastComponent toastComponent, long time) {
        ResourceLocation texture = isPositive ? POSITIVE_TEXTURE : NEGATIVE_TEXTURE;

        // Рисуем твою кастомную текстуру (160x32)
        g.blit(texture, 0, 0, 0, 0, this.width(), this.height(), this.width(), this.height());

        int textColor = isPositive ? 0x80FF20 : 0xFF5555; // зелёный или красный текст

        g.drawString(toastComponent.getMinecraft().font, title, 30, 7, textColor);
        g.drawString(toastComponent.getMinecraft().font, description, 30, 18, 0xFFFFFF);

        if (!icon.isEmpty()) {
            g.renderItem(icon, 8, 8);
        }

        return time >= 5000L ? Visibility.HIDE : Visibility.SHOW;
    }
}
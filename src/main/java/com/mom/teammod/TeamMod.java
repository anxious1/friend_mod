package com.mom.teammod;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

@Mod(TeamMod.MODID)
public class TeamMod {
    public static final String MODID = "teammod";
    private static final Logger LOGGER = LogUtils.getLogger();

    public TeamMod() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        modEventBus.addListener(this::commonSetup);

        Items.ITEMS.register(modEventBus);
        TeamRecipes.SERIALIZERS.register(modEventBus);
        TeamMenu.MENUS.register(modEventBus);
        MinecraftForge.EVENT_BUS.register(new BossKillHandler());

        NetworkHandler.register();

        MinecraftForge.EVENT_BUS.register(this);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("TeamMod: Common setup started - Recipes registered!");
    }

    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            LOGGER.info("TeamMod: Client setup started");
        }
    }

    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = net.minecraftforge.api.distmarker.Dist.CLIENT)
    public static class ClientForgeEvents {
        @SubscribeEvent
        public static void onScreenInit(ScreenEvent.Init.Post event) {
            if (event.getScreen() instanceof InventoryScreen inventoryScreen) {
                int x = (inventoryScreen.width - 176) / 2 + 48;
                int y = (inventoryScreen.height - 166) / 2 - 15;

                ResourceLocation teamUnpress = ResourceLocation.fromNamespaceAndPath(MODID, "textures/gui/unpress.png");
                ResourceLocation teamPress = ResourceLocation.fromNamespaceAndPath(MODID, "textures/gui/press.png");
                ResourceLocation profileUnpress = ResourceLocation.fromNamespaceAndPath(MODID, "textures/gui/unpress_profile.png");
                ResourceLocation profilePress = ResourceLocation.fromNamespaceAndPath(MODID, "textures/gui/press_profile.png");

                // КНОПКА КОМАНД
                event.addListener(new ImageButton(x, y, 26, 29, 0, 0, 0, teamUnpress, button -> {
                    Player player = inventoryScreen.getMinecraft().player;
                    if (player != null) {
                        inventoryScreen.getMinecraft().setScreen(new TeamScreen(
                                new TeamMenu(0, player.getInventory()),
                                player.getInventory(),
                                Component.translatable("gui.teammod.team_tab")
                        ));
                    }
                }) {
                    @Override
                    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
                        ResourceLocation tex = this.isHoveredOrFocused() ? teamPress : teamUnpress;
                        RenderSystem.setShaderTexture(0, tex);
                        guiGraphics.blit(tex, this.getX(), this.getY(), 0, 0, 26, 29, 26, 29);
                        if (this.isHoveredOrFocused()) {
                            guiGraphics.renderTooltip(Minecraft.getInstance().font,
                                    Component.translatable("gui.teammod.team_tab"), mouseX, mouseY);
                        }
                    }
                });

                // КНОПКА ПРОФИЛЯ
                event.addListener(new ImageButton(x + 28, y, 26, 29, 0, 0, 0, profileUnpress, button -> {
                    Player player = inventoryScreen.getMinecraft().player;
                    if (player != null) {
                        inventoryScreen.getMinecraft().setScreen(new MyProfileScreen(
                                Component.translatable("gui.teammod.profile")
                        ));
                    }
                }) {
                    @Override
                    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
                        ResourceLocation tex = this.isHoveredOrFocused() ? profilePress : profileUnpress;
                        RenderSystem.setShaderTexture(0, tex);
                        guiGraphics.blit(tex, this.getX(), this.getY(), 0, 0, 26, 29, 26, 29);
                        if (this.isHoveredOrFocused()) {
                            guiGraphics.renderTooltip(Minecraft.getInstance().font,
                                    Component.translatable("gui.teammod.profile"), mouseX, mouseY);
                        }
                    }
                });
            }
        }
    }
}
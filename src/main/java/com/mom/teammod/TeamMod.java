package com.mom.teammod;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
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

    private static final ResourceLocation INV_ICON = ResourceLocation.fromNamespaceAndPath(MODID, "textures/gui/inv_icon.png");
    private static final ResourceLocation TEAM_LIST_ICON = ResourceLocation.fromNamespaceAndPath(MODID, "textures/gui/team_list_icon.png");
    private static final ResourceLocation PROFILE_ICON = ResourceLocation.fromNamespaceAndPath(MODID, "textures/gui/profile_icon.png");

    public TeamMod() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        modEventBus.addListener(this::commonSetup);

        Items.ITEMS.register(modEventBus);
        TeamRecipes.SERIALIZERS.register(modEventBus);
        TeamMenu.MENUS.register(modEventBus);
        MinecraftForge.EVENT_BUS.register(new BossKillHandler());
        MinecraftForge.EVENT_BUS.register(TeamMod.class);
        NetworkHandler.register();
        MinecraftForge.EVENT_BUS.register(TeamManager.class);
        MinecraftForge.EVENT_BUS.register(this);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("TeamMod: Common setup started - Recipes registered!");
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        // Это событие срабатывает ПОСЛЕ полной загрузки мира
        // overworld() уже существует, всё безопасно
        TeamWorldData.get(event.getServer().overworld());
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
            if (!(event.getScreen() instanceof InventoryScreen inventoryScreen)) return;

            int guiLeft = (inventoryScreen.width - 176) / 2;
            int guiTop = (inventoryScreen.height - 166) / 2;

            // Текстуры кнопок
            ResourceLocation teamUnpress = ResourceLocation.fromNamespaceAndPath(MODID, "textures/gui/unpress.png");
            ResourceLocation teamPress = ResourceLocation.fromNamespaceAndPath(MODID, "textures/gui/press.png");
            ResourceLocation profileUnpress = ResourceLocation.fromNamespaceAndPath(MODID, "textures/gui/unpress.png");
            ResourceLocation profilePress = ResourceLocation.fromNamespaceAndPath(MODID, "textures/gui/press.png");

            // Иконки 16x16
            ResourceLocation INV_ICON         = ResourceLocation.fromNamespaceAndPath(MODID, "textures/gui/inv_icon.png");
            ResourceLocation TEAM_LIST_ICON   = ResourceLocation.fromNamespaceAndPath(MODID, "textures/gui/team_list_icon.png");
            ResourceLocation PROFILE_ICON     = ResourceLocation.fromNamespaceAndPath(MODID, "textures/gui/profile_icon.png");

            // Базовая Y-позиция (от незажатой кнопки)
            int baseY = guiTop - 26;

            // === КНОПКА 3 — ИНВЕНТАРЬ (всегда зажата) ===
            int button3X = guiLeft + 2;
            event.addListener(new ImageButton(button3X, baseY - 2, 26, 29, 0, 0, 0, teamPress, button -> {}) {
                @Override
                public void renderWidget(GuiGraphics g, int mx, int my, float pt) {
                    g.blit(teamPress, this.getX(), this.getY(), 0, 0, 26, 29, 26, 29);
                    g.blit(INV_ICON, this.getX() + 5, this.getY() + 6, 0, 0, 16, 16, 16, 16);

                    if (this.isHoveredOrFocused()) {
                        g.renderTooltip(Minecraft.getInstance().font,
                                Component.translatable("gui.teammod.button3"), mx, my);
                    }
                }
            });

            // === КНОПКА 1 — КОМАНДЫ ===
            int button1X = button3X + 26 + 52;
            event.addListener(new ImageButton(button1X, baseY, 26, 27, 0, 0, 0, teamUnpress, button -> {
                Player player = inventoryScreen.getMinecraft().player;
                if (player != null) {
                    inventoryScreen.getMinecraft().setScreen(new TeamScreen(
                            inventoryScreen,
                            new TeamMenu(0, player.getInventory()),
                            player.getInventory(),
                            Component.translatable("gui.teammod.team_tab")
                    ));
                }
            }) {
                private boolean isPressed = false;

                @Override
                public void renderWidget(GuiGraphics g, int mx, int my, float pt) {
                    boolean pressed = this.isPressed || this.isHoveredOrFocused();
                    ResourceLocation tex = pressed ? teamPress : teamUnpress;
                    int height = pressed ? 29 : 27;

                    if (this.getHeight() != height) {
                        this.setHeight(height);
                        this.setY(pressed ? baseY - 2 : baseY);
                    }

                    g.blit(tex, this.getX(), this.getY(), 0, 0, 26, height, 26, height);
                    g.blit(TEAM_LIST_ICON, this.getX() + 5, this.getY() + (pressed ? 7 : 6), 0, 0, 16, 16, 16, 16);

                    if (this.isHoveredOrFocused()) {
                        g.renderTooltip(Minecraft.getInstance().font,
                                Component.translatable("gui.teammod.team_tab"), mx, my);
                    }
                }

                @Override
                public void onClick(double mouseX, double mouseY) {
                    super.onClick(mouseX, mouseY);
                    this.isPressed = true;
                }
            });

            // === КНОПКА 2 — ПРОФИЛЬ ===
            int button2X = button1X + 26;
            event.addListener(new ImageButton(button2X, baseY, 26, 27, 0, 0, 0, profileUnpress, button -> {
                Player player = inventoryScreen.getMinecraft().player;
                if (player != null) {
                    inventoryScreen.getMinecraft().setScreen(new MyProfileScreen(
                            inventoryScreen,
                            Component.translatable("gui.teammod.profile")
                    ));
                }
            }) {
                private boolean isPressed = false;

                @Override
                public void renderWidget(GuiGraphics g, int mx, int my, float pt) {
                    boolean pressed = this.isPressed || this.isHoveredOrFocused();
                    ResourceLocation tex = pressed ? profilePress : profileUnpress;
                    int height = pressed ? 29 : 27;

                    if (this.getHeight() != height) {
                        this.setHeight(height);
                        this.setY(pressed ? baseY - 2 : baseY);
                    }

                    g.blit(tex, this.getX(), this.getY(), 0, 0, 26, height, 26, height);
                    g.blit(PROFILE_ICON, this.getX() + 5, this.getY() + (pressed ? 7 : 6), 0, 0, 16, 16, 16, 16);

                    if (this.isHoveredOrFocused()) {
                        g.renderTooltip(Minecraft.getInstance().font,
                                Component.translatable("gui.teammod.profile"), mx, my);
                    }
                }

                @Override
                public void onClick(double mouseX, double mouseY) {
                    super.onClick(mouseX, mouseY);
                    this.isPressed = true;
                }
            });
        }
    }
}
package com.mom.teammod;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mom.teammod.items.TeamCompassItem;
import com.mom.teammod.packets.PlayerActivityPacket;
import com.mom.teammod.packets.RequestProfilePacket;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
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

import java.util.Collections;
import java.util.Set;
import java.util.UUID;

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

            // === COMPASS (это НЕ тикер, это property override) ===
            ItemProperties.register(Items.TEAM_COMPASS.get(), ResourceLocation.fromNamespaceAndPath(MODID, "angle"), (stack, level, entity, seed) -> {
                if (entity == null || !(entity instanceof Player player)) {
                    return spinningAngle();
                }

                ProfileManager.Profile holderProfile = ProfileManager.getClientProfile(player.getUUID());
                if (!holderProfile.isShowOnCompass()) return spinningAngle();

                CompoundTag tag = stack.getTag();
                if (tag == null || !tag.hasUUID("TrackedPlayer")) return spinningAngle();

                UUID tracked = tag.getUUID("TrackedPlayer");
                Player target = level == null ? null : level.getPlayerByUUID(tracked);

                if (target == null || !player.level().dimension().equals(target.level().dimension()) || player.distanceTo(target) > 5000) {
                    return spinningAngle();
                }

                ProfileManager.Profile targetProfile = ProfileManager.getClientProfile(tracked);
                if (!targetProfile.isShowOnCompass()) return spinningAngle();

                Set<String> myTeams = TeamManager.clientPlayerTeams.getOrDefault(player.getUUID(), Collections.emptySet());
                Set<String> targetTeams = TeamManager.clientPlayerTeams.getOrDefault(tracked, Collections.emptySet());
                boolean visible = myTeams.stream()
                        .anyMatch(t -> targetTeams.contains(t) && TeamManager.clientTeams.get(t) != null && TeamManager.clientTeams.get(t).showCompass());

                if (!visible) return spinningAngle();

                double dx = target.getX() - player.getX();
                double dz = target.getZ() - player.getZ();

                double angleToTarget = Math.atan2(dz, dx) + Math.PI / 2;   // +90°
                double playerYaw = Math.toRadians(player.getYRot());

                double angle = angleToTarget - playerYaw + Math.PI;
                angle = (angle / (2 * Math.PI)) + 0.5;
                if (angle < 0) angle += 1;
                if (angle >= 1) angle -= 1;

                int frame = (int) Math.round(angle * 32) % 32;
                return frame / 32.0f;
            });

            // === Тикеры: 1) AFK input  2) "видел рядом => запомнить экипу" ===
            // Автозапрос профиля должен быть ТОЛЬКО в ClientForgeEvents.onClientTick (как у тебя ниже)
            MinecraftForge.EVENT_BUS.register(new Object() {

                // ===== ТИКЕР 1: сброс AFK при любом вводе (для себя) =====
                @SubscribeEvent
                public void onClientTickAFK(TickEvent.ClientTickEvent e) {
                    if (e.phase != TickEvent.Phase.END) return;
                    Minecraft mc = Minecraft.getInstance();
                    if (mc.player == null) return;

                    boolean isMoving =
                            mc.player.input.leftImpulse != 0 ||
                                    mc.player.input.forwardImpulse != 0 ||
                                    mc.player.input.jumping ||
                                    mc.player.input.shiftKeyDown ||
                                    mc.player.xxa != 0 || mc.player.zza != 0;

                    if (isMoving) {
                        ClientPlayerCache.updateInputTime();
                        NetworkHandler.INSTANCE.sendToServer(new PlayerActivityPacket());
                    }
                }

                // ===== ТИКЕР 2: кеш игроков + последняя "замеченная" экипировка =====
                @SubscribeEvent
                public void onClientTickCache(TickEvent.ClientTickEvent e) {
                    if (e.phase != TickEvent.Phase.END) return;

                    Minecraft mc = Minecraft.getInstance();
                    if (mc.level == null || mc.player == null) return;

                    // Раз в 10 тиков (~2 раза в секунду), чтобы не грузить клиент на больших онлайнах
                    if ((mc.level.getGameTime() % 10L) != 0L) return;

                    for (Player player : mc.level.players()) {
                        if (player == mc.player) continue;

                        UUID uuid = player.getUUID();
                        ClientPlayerCache.CacheEntry entry = ClientPlayerCache.getOrCreate(uuid);

                        // Статус (оставляю твою текущую механику как есть)
                        long timeSinceLastInput = System.currentTimeMillis() - ClientPlayerCache.lastInputTime;
                        entry.status = (timeSinceLastInput > 10_000)
                                ? ClientPlayerCache.PlayerStatus.AFK
                                : ClientPlayerCache.PlayerStatus.ONLINE;

                        // Экипировка — снимаем "последний раз виденную" в радиусе
                        // 3 чанка ≈ 48 блоков (по прямой). Для "круга" по диагонали можно оставить твою формулу.
                        if (player.distanceTo(mc.player) <= 48.0f) {
                            ItemStack[] eq = new ItemStack[4];
                            for (int i = 0; i < 4; i++) {
                                eq[i] = player.getItemBySlot(EquipmentSlot.byTypeAndIndex(EquipmentSlot.Type.ARMOR, i)).copy();
                            }
                            ClientPlayerCache.onPlayerSeen(uuid, player.getGameProfile(), eq);
                        }
                    }
                }

            });
        }



        private static float spinningAngle() {
            long time = System.currentTimeMillis() % 1280L; // 1.28 сек на оборот
            int frame = (int)(time * 32 / 1280);
            return frame / 32.0f;
        }

    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = net.minecraftforge.api.distmarker.Dist.CLIENT)
    public static class ClientForgeEvents {
            private static int profileLoadCooldown = 0;

        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent e) {
            if (e.phase != TickEvent.Phase.END) return;
            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null) return;

            if (--profileLoadCooldown > 0) return;
            profileLoadCooldown = 20;

            UUID uuid = ClientPlayerCache.loadQueue.poll();
            if (uuid == null) return;

            ClientPlayerCache.CacheEntry entry = ClientPlayerCache.getOrCreate(uuid);

            long attempts = 0;
            if (!entry.hasMet && entry.lastSeenTime < 0) {
                attempts = -entry.lastSeenTime;
            }

            if (attempts >= 5) {
                LOGGER.warn("[AUTO-LOAD] лимит 5 попыток для {}, больше не запрашиваем", uuid);
                return;
            }

            if (!entry.hasMet) {
                entry.lastSeenTime = -(attempts + 1);
            }

            LOGGER.info("[AUTO-LOAD] запрос профиля ({}/5) {}", (attempts + 1), uuid);
            NetworkHandler.INSTANCE.sendToServer(new RequestProfilePacket(uuid));
        }

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
        @SubscribeEvent
        public static void onClientLogin(net.minecraftforge.client.event.ClientPlayerNetworkEvent.LoggingIn event) {
            ClientPlayerCache.loadFromDisk();
        }

        @SubscribeEvent
        public static void onClientLogout(net.minecraftforge.client.event.ClientPlayerNetworkEvent.LoggingOut event) {
            ClientPlayerCache.saveToDisk();
        }

    }
}
    @SubscribeEvent
    public static void onServerStarting(ServerStartingEvent e) {
        PlayerNameCache.rebuild();

        TeamWorldData.get(e.getServer().overworld())
                .getNameMap()
                .forEach((uuid, name) -> {
                    if (name == null) return;
                    String n = name.trim();
                    if (n.isEmpty()) return;

                    PlayerNameCache.NAME_UUID.put(n.toLowerCase(), uuid);
                    PlayerNameCache.UUID_NAME.put(uuid, n);
                });
    }
    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer sp)) return;

        PlayerNameCache.onLogin(sp);

        ServerLevel storageLevel = TeamWorldData.storageLevel(sp.getServer());
        TeamWorldData data = TeamWorldData.get(storageLevel);

        String name = sp.getGameProfile() != null ? sp.getGameProfile().getName() : null;
        if (name != null && !name.isBlank()) {
            data.putName(sp.getUUID(), name);
            data.setDirty(true);
            storageLevel.getDataStorage().save();
        }
    }

}
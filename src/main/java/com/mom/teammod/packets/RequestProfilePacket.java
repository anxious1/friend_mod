package com.mom.teammod.packets;

import com.mojang.authlib.GameProfile;
import com.mom.teammod.NetworkHandler;
import com.mom.teammod.PlayerNameCache;
import com.mom.teammod.ProfileManager;
import com.mom.teammod.TeamWorldData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

public class RequestProfilePacket {
    private final UUID targetUUID;   // убери static

    public RequestProfilePacket(UUID targetUUID) {
        this.targetUUID = targetUUID;
    }

    public static void encode(RequestProfilePacket pkt, FriendlyByteBuf buf) {
        buf.writeUUID(pkt.targetUUID);
    }

    public static RequestProfilePacket decode(FriendlyByteBuf buf) {
        return new RequestProfilePacket(buf.readUUID());
    }

    public static void handle(RequestProfilePacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer sender = ctx.get().getSender();
            if (sender == null) return;

            ServerLevel level = sender.serverLevel();
            TeamWorldData data = TeamWorldData.get(level);
            ProfileManager.Profile profile = data.getPlayerProfiles().get(pkt.targetUUID);

            if (profile == null) {
                // 1. Пытаемся взять GameProfile из кеша сервера
                Optional<GameProfile> opt = ServerLifecycleHooks.getCurrentServer()
                        .getProfileCache()
                        .get(pkt.targetUUID);
                GameProfile gp = opt.orElse(null);
                if (gp == null || gp.getName() == null || gp.getName().isEmpty()) {
                    // 2. Нет в кеше – смотрим онлайн-игрока
                    ServerPlayer online = sender.getServer().getPlayerList().getPlayer(pkt.targetUUID);
                    if (online != null) gp = online.getGameProfile();
                }
                // 3. Если и онлайн-игрока нет – просто выходим, не шлём пакет
                if (gp == null || gp.getName() == null || gp.getName().isEmpty()) {
                    System.out.println("[SERVER] GameProfile для " + pkt.targetUUID + " не найден – пакет не отправляем");
                    return;
                }
                // 4. Создаём профиль с корректным GameProfile
                profile = new ProfileManager.Profile(pkt.targetUUID);
                profile.setGameProfile(gp);
                // экипировка
                ItemStack[] eq = new ItemStack[4];
                for (int i = 0; i < 4; i++)
                    eq[i] = gp.getProperties().containsKey("textures") ? ItemStack.EMPTY   // оффлайн, экипировки нет
                            : sender.getServer().getPlayerList().getPlayer(pkt.targetUUID) != null
                            ? sender.getServer().getPlayerList().getPlayer(pkt.targetUUID)
                            .getItemBySlot(EquipmentSlot.byTypeAndIndex(EquipmentSlot.Type.ARMOR, i)).copy()
                            : ItemStack.EMPTY;
                profile.setLastEquipment(eq);

                data.getPlayerProfiles().put(pkt.targetUUID, profile);
                data.setDirty(true);
                level.getDataStorage().save();   // сразу на диск
            }

            NetworkHandler.INSTANCE.send(
                    PacketDistributor.PLAYER.with(() -> sender),
                    new ProfileSyncPacket(pkt.targetUUID, profile)
            );
        });
        ctx.get().setPacketHandled(true);
    }
}
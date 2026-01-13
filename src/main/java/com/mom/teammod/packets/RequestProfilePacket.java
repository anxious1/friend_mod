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
import net.simpleraces.network.SimpleracesModVariables;

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

            ServerLevel storageLevel = TeamWorldData.storageLevel(sender.getServer());
            TeamWorldData data = TeamWorldData.get(storageLevel);

            // Берём/создаём профиль ВСЕГДА
            ProfileManager.Profile profile = data.getPlayerProfiles().get(pkt.targetUUID);
            if (profile == null) {
                profile = data.getOrCreateProfile(pkt.targetUUID);
            }

            // Если игрок онлайн — ОБНОВЛЯЕМ ДАННЫЕ ПРЯМО СЕЙЧАС (чтобы не было "голый" после выхода из радиуса)
            ServerPlayer online = sender.getServer().getPlayerList().getPlayer(pkt.targetUUID);
            if (online != null) {
                // имя/геймпрофиль
                GameProfile gp = online.getGameProfile();
                if (gp != null && gp.getName() != null && !gp.getName().isBlank()) {
                    data.putName(pkt.targetUUID, gp.getName());
                }
                if (gp != null) {
                    profile.setGameProfile(gp);
                }

                // экипировка
                ItemStack[] eq = new ItemStack[4];
                for (int i = 0; i < 4; i++) {
                    eq[i] = online.getItemBySlot(EquipmentSlot.byTypeAndIndex(EquipmentSlot.Type.ARMOR, i)).copy();
                }
                profile.setLastEquipment(eq);

                // раса (на всякий) — если капа уже готова
                var cap = online.getCapability(SimpleracesModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(null);
                if (cap != null && cap.selected) {
                    String race = cap.aracha    ? "arachna"
                            : cap.dragon    ? "dragon"
                            : cap.dwarf     ? "dwarf"
                            : cap.elf       ? "elf"
                            : cap.fairy     ? "fairy"
                            : cap.halfdead  ? "halfdead"
                            : cap.merfolk   ? "merfolk"
                            : cap.orc       ? "orc"
                            : cap.serpentin ? "serpentin"
                            : cap.werewolf  ? "werewolf"
                            : "human";
                    profile.setRace(race);
                }

                // сохраняем обновлённый профиль в мир (чтобы дальше оффлайн/далеко показывал последнюю замеченную экипу)
                data.getPlayerProfiles().put(pkt.targetUUID, profile);
                data.setDirty(true);
                storageLevel.getDataStorage().save();
            } else {
                // оффлайн — подстрахуемся GameProfile
                // 1) имя из nameMap
                String savedName = data.getNameMap().get(pkt.targetUUID);

                // 2) profileCache сервера
                Optional<GameProfile> opt = sender.getServer().getProfileCache().get(pkt.targetUUID);
                GameProfile gp = opt.orElse(null);
                if (gp != null && gp.getName() != null && !gp.getName().isBlank()) {
                    data.putName(pkt.targetUUID, gp.getName());
                    savedName = gp.getName();
                }

                // 3) финальный fallback
                if (gp == null) {
                    String finalName = (savedName != null && !savedName.isBlank()) ? savedName : "Unknown";
                    gp = new GameProfile(pkt.targetUUID, finalName);
                }
                profile.setGameProfile(gp);

                // ВАЖНО: lastEquipment оффлайн НЕ ТРОГАЕМ (должна остаться "последняя замеченная")
            }

            // отправляем отправителю
            NetworkHandler.INSTANCE.send(
                    PacketDistributor.PLAYER.with(() -> sender),
                    new ProfileSyncPacket(pkt.targetUUID, profile)
            );
        });
        ctx.get().setPacketHandled(true);
    }


}
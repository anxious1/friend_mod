package com.mom.teammod.packets;

import com.mom.teammod.*;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public class ProfileSyncPacket {
    private UUID playerUUID;
    private CompoundTag profileData;

    public ProfileSyncPacket(UUID playerUUID, ProfileManager.Profile profile) {
        this.playerUUID = playerUUID;
        this.profileData = profile != null ? profile.serializeNBT() : new CompoundTag();
    }

    public static void encode(ProfileSyncPacket pkt, FriendlyByteBuf buf) {
        buf.writeUUID(pkt.playerUUID);
        buf.writeNbt(pkt.profileData);
    }

    public static ProfileSyncPacket decode(FriendlyByteBuf buf) {
        UUID uuid = buf.readUUID();
        CompoundTag data = buf.readNbt();
        if (data == null) data = new CompoundTag();
        ProfileSyncPacket pkt = new ProfileSyncPacket(uuid, null);
        pkt.profileData = data;
        return pkt;
    }

    public static void handle(ProfileSyncPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ProfileManager.Profile profile = ProfileManager.getClientProfile(pkt.playerUUID);
            profile.deserializeNBT(pkt.profileData);

            // Обновляем клиентский кэш профиля (важно: чтобы не было "раса не выбрана" и т.п.)
            ClientPlayerCache.updateFromProfile(pkt.playerUUID, profile);

            String receivedName = null;
            if (profile.getGameProfile() != null) {
                receivedName = profile.getGameProfile().getName();
            }

            // Если имя норм — кладём в кеш имён
            if (receivedName != null && !receivedName.isBlank() && !"Unknown".equalsIgnoreCase(receivedName) && !"Loading...".equalsIgnoreCase(receivedName)) {
                ClientPlayerNameCache.put(pkt.playerUUID, receivedName);
                return;
            }

            // Иначе: НЕ спамим сервер отсюда, а ставим UUID в очередь дозагрузки (лимит сделаем в client tick)
            ClientPlayerCache.loadQueue.offer(pkt.playerUUID);
        });
        ctx.get().setPacketHandled(true);
    }

}
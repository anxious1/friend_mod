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
            System.out.println("[CLIENT] ProfileSyncPacket для " + pkt.playerUUID);
            System.out.println("[CLIENT]  NBT name = " + pkt.profileData.getString("name"));

            ProfileManager.Profile profile = ProfileManager.getClientProfile(pkt.playerUUID);
            profile.deserializeNBT(pkt.profileData);

            String receivedName = profile.getGameProfile().getName();
            if (receivedName == null || receivedName.isEmpty() || "Unknown".equals(receivedName)) {
                System.out.println("[CLIENT] имя пустое/Unknown – не кладём в кеш, запрашиваем заново");
                NetworkHandler.INSTANCE.sendToServer(new RequestProfilePacket(pkt.playerUUID));
                return;
            }
            System.out.println("[CLIENT] кладём в кеш: " + receivedName);
            ClientPlayerNameCache.put(pkt.playerUUID, receivedName);
        });
        ctx.get().setPacketHandled(true);
    }
}
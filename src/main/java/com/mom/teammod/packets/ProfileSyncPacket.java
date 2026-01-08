package com.mom.teammod.packets;

import com.mom.teammod.ClientPlayerCache;
import com.mom.teammod.ProfileManager;
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
            ClientPlayerCache.updateFromProfile(pkt.playerUUID, profile);
            if (pkt.profileData != null && !pkt.profileData.isEmpty()) {
                profile.deserializeNBT(pkt.profileData);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
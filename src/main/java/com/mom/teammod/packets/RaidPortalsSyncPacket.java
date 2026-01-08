package com.mom.teammod.packets;

import com.mod.raidportals.RaidPortalsSavedData;
import com.mom.teammod.ClientPlayerCache;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class RaidPortalsSyncPacket {
    private final int tier1;
    private final int tier2;
    private final int tier3;

    public RaidPortalsSyncPacket(int tier1, int tier2, int tier3) {
        this.tier1 = tier1;
        this.tier2 = tier2;
        this.tier3 = tier3;
    }

    public static void encode(RaidPortalsSyncPacket pkt, FriendlyByteBuf buf) {
        buf.writeInt(pkt.tier1);
        buf.writeInt(pkt.tier2);
        buf.writeInt(pkt.tier3);
    }

    public static RaidPortalsSyncPacket decode(FriendlyByteBuf buf) {
        return new RaidPortalsSyncPacket(buf.readInt(), buf.readInt(), buf.readInt());
    }

    public static void handle(RaidPortalsSyncPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            // Сохраняем на клиенте в ClientPlayerCache
            ClientPlayerCache.setPortalData(ctx.get().getSender().getUUID(), pkt.tier1, pkt.tier2, pkt.tier3);
        });
        ctx.get().setPacketHandled(true);
    }
}
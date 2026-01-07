package com.mom.teammod.packets;

import com.mom.teammod.ClientPlayerCache;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public class PlayerStatusPacket {
    private final UUID playerId;
    private final byte status;   // 0-offline 1-online 2-afk

    public PlayerStatusPacket(UUID playerId, byte status) {
        this.playerId = playerId;
        this.status = status;
    }

    public static void encode(PlayerStatusPacket pkt, FriendlyByteBuf buf) {
        buf.writeUUID(pkt.playerId);
        buf.writeByte(pkt.status);
    }

    public static PlayerStatusPacket decode(FriendlyByteBuf buf) {
        return new PlayerStatusPacket(buf.readUUID(), buf.readByte());
    }

    public static void handle(PlayerStatusPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ClientPlayerCache.setStatus(pkt.playerId, pkt.status);
        });
        ctx.get().setPacketHandled(true);
    }
}
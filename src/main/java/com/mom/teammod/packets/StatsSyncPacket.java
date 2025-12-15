package com.mom.teammod.packets;

import com.mom.teammod.PlayerStatsData;
import com.mom.teammod.TeamManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public class StatsSyncPacket {
    private final UUID playerUUID;
    private final PlayerStatsData stats;

    public StatsSyncPacket(UUID playerUUID, PlayerStatsData stats) {
        this.playerUUID = playerUUID;
        this.stats = stats;
    }

    public static void encode(StatsSyncPacket pkt, FriendlyByteBuf buf) {
        buf.writeUUID(pkt.playerUUID);
        buf.writeNbt(pkt.stats.serialize());
    }

    public static StatsSyncPacket decode(FriendlyByteBuf buf) {
        UUID uuid = buf.readUUID();
        PlayerStatsData data = new PlayerStatsData();
        data.deserialize(buf.readNbt());
        return new StatsSyncPacket(uuid, data);
    }

    public static void handle(StatsSyncPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            TeamManager.clientPlayerStats.put(pkt.playerUUID, pkt.stats); // клиентское хранилище
        });
        ctx.get().setPacketHandled(true);
    }
}
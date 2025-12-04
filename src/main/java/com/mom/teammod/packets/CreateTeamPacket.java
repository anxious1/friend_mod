package com.mom.teammod.packets;

import com.mom.teammod.NetworkHandler;
import com.mom.teammod.TeamManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class CreateTeamPacket {
    private final String teamName;

    public CreateTeamPacket(String teamName) {
        this.teamName = teamName;
    }

    public static void encode(CreateTeamPacket pkt, FriendlyByteBuf buf) {
        buf.writeUtf(pkt.teamName);
    }

    public static CreateTeamPacket decode(FriendlyByteBuf buf) {
        return new CreateTeamPacket(buf.readUtf(32767));
    }

    public static void handle(CreateTeamPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (TeamManager.createTeam(pkt.teamName, player)) {
                // Ничего не отправляем через reply() — createTeam уже сам отправил пакет
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
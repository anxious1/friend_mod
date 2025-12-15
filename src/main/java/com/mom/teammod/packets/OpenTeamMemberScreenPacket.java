package com.mom.teammod.packets;

import com.mom.teammod.TeamManager;
import com.mom.teammod.TeamMemberScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class OpenTeamMemberScreenPacket {
    private final String teamName;

    public OpenTeamMemberScreenPacket(String teamName) {
        this.teamName = teamName;
    }

    public static void encode(OpenTeamMemberScreenPacket pkt, FriendlyByteBuf buf) {
        buf.writeUtf(pkt.teamName);
    }

    public static OpenTeamMemberScreenPacket decode(FriendlyByteBuf buf) {
        return new OpenTeamMemberScreenPacket(buf.readUtf(32767));
    }

    public static void handle(OpenTeamMemberScreenPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            Minecraft.getInstance().execute(() -> {
                TeamManager.Team team = TeamManager.clientTeams.get(pkt.teamName);
                if (team != null) {
                    Minecraft.getInstance().setScreen(new TeamMemberScreen(
                            null,
                            pkt.teamName,
                            team.getTag(),
                            team.showTag(),
                            team.showCompass(),
                            team.isFriendlyFire(),
                            team.getOwner()
                    ));
                }
            });
        });
        ctx.get().setPacketHandled(true);
    }
}
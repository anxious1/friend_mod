package com.mom.teammod.packets;

import com.mom.teammod.NetworkHandler;
import com.mom.teammod.TeamManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class UpdateTeamSettingsPacket {

    private final String teamName;
    private final boolean showTag;
    private final boolean showCompass;
    private final boolean friendlyFire;

    public UpdateTeamSettingsPacket(String teamName, boolean showTag, boolean showCompass, boolean friendlyFire) {
        this.teamName = teamName;
        this.showTag = showTag;
        this.showCompass = showCompass;
        this.friendlyFire = friendlyFire;
    }

    public static void encode(UpdateTeamSettingsPacket msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.teamName);
        buf.writeBoolean(msg.showTag);
        buf.writeBoolean(msg.showCompass);
        buf.writeBoolean(msg.friendlyFire);
    }

    public static UpdateTeamSettingsPacket decode(FriendlyByteBuf buf) {
        return new UpdateTeamSettingsPacket(
                buf.readUtf(32767),
                buf.readBoolean(),
                buf.readBoolean(),
                buf.readBoolean()
        );
    }

    public static void handle(UpdateTeamSettingsPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            var sender = ctx.get().getSender();
            if (sender == null) return;

            TeamManager.Team team = TeamManager.getServerTeam(msg.teamName);
            if (team == null || !team.getOwner().equals(sender.getUUID())) {
                return;
            }

            // Меняем данные только на сервере
            team.setShowTag(msg.showTag);
            team.setShowCompass(msg.showCompass);
            team.setFriendlyFire(msg.friendlyFire);

            var data = TeamManager.getData();
            if (data != null) {
                data.setDirty(true);
            }

            // Рассылаем всем актуальные данные
            TeamManager.syncTeamToAll(msg.teamName);
        });
        ctx.get().setPacketHandled(true);
    }
}
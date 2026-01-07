package com.mom.teammod.packets;

import com.mom.teammod.LastActivityTracker;
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
            LastActivityTracker.update(ctx.get().getSender().getUUID());
            var sender = ctx.get().getSender();
            if (sender == null) return;

            TeamManager.Team team = TeamManager.getServerTeam(msg.teamName);
            if (team == null || !team.getOwner().equals(sender.getUUID())) {
                return;
            }

            // Меняем данные только на сервере
            boolean oldShowTag = team.showTag();
            boolean oldShowCompass = team.showCompass();
            boolean oldFriendlyFire = team.isFriendlyFire();

            team.setShowTag(msg.showTag);
            team.setShowCompass(msg.showCompass);
            team.setFriendlyFire(msg.friendlyFire);

            var data = TeamManager.getData();
            if (data != null) {
                data.setDirty(true);
            }

            // Рассылаем всем актуальные данные
            TeamManager.syncTeamToAll(msg.teamName);

            // === УВЕДОМЛЕНИЯ ===
            // Тег
            if (msg.showTag != oldShowTag) {
                String title = msg.showTag ? "Отображение тега включено" : "Отображение тега выключено";
                String desc = "В команде §b" + msg.teamName + " " + (msg.showTag ? "включено" : "выключено") + " отображение тега";
                TeamManager.sendAchievementToTeam(msg.teamName, title, desc, "NAME_TAG", msg.showTag);
            }

            // Компас
            if (msg.showCompass != oldShowCompass) {
                String title = msg.showCompass ? "Компас включён" : "Компас выключен";
                String desc = "В команде §b" + msg.teamName + " " + (msg.showCompass ? "включён" : "выключен") + " командный компас";
                TeamManager.sendAchievementToTeam(msg.teamName, title, desc, "COMPASS", msg.showCompass);
            }

            // Дружественный огонь
            if (msg.friendlyFire != oldFriendlyFire) {
                String title = msg.friendlyFire ? "Дружественный огонь включён" : "Дружественный огонь выключен";
                String desc = "В команде §b" + msg.teamName + " " + (msg.friendlyFire ? "включён" : "выключен") + " дружественный огонь";
                String icon = msg.friendlyFire ? "IRON_SWORD" : "SHIELD";
                boolean positive = !msg.friendlyFire; // выключение FF — позитивно
                TeamManager.sendAchievementToTeam(msg.teamName, title, desc, icon, positive);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
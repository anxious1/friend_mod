package com.mom.teammod.packets;

import com.mom.teammod.NetworkHandler;
import com.mom.teammod.TeamManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class CreateTeamPacket {

    private final String teamName;
    private final String tag;
    private final boolean friendlyFire;
    private final boolean showTag;
    private final boolean showCompass;

    // Конструктор — вызывается на клиенте
    public CreateTeamPacket(String teamName, String tag, boolean friendlyFire, boolean showTag, boolean showCompass) {
        this.teamName = teamName;
        this.tag = tag;
        this.friendlyFire = friendlyFire;
        this.showTag = showTag;
        this.showCompass = showCompass;
    }

    // Сериализация (отправка на сервер)
    public static void encode(CreateTeamPacket pkt, FriendlyByteBuf buf) {
        buf.writeUtf(pkt.teamName);
        buf.writeUtf(pkt.tag);
        buf.writeBoolean(pkt.friendlyFire);
        buf.writeBoolean(pkt.showTag);
        buf.writeBoolean(pkt.showCompass);
    }

    // Десериализация (чтение на сервере)
    public static CreateTeamPacket decode(FriendlyByteBuf buf) {
        String name = buf.readUtf(32767);
        String tag = buf.readUtf(32767);
        boolean ff = buf.readBoolean();
        boolean showTag = buf.readBoolean();
        boolean showCompass = buf.readBoolean();
        return new CreateTeamPacket(name, tag, ff, showTag, showCompass);
    }

    // Обработка на сервере
    public static void handle(CreateTeamPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();

            boolean success = TeamManager.createTeam(
                    pkt.teamName,
                    pkt.tag,
                    pkt.friendlyFire,
                    pkt.showTag,
                    pkt.showCompass,
                    player
            );

            if (success) {
                player.sendSystemMessage(Component.literal("§aКоманда §f" + pkt.teamName + "§a успешно создана!"));
            } else {
                player.sendSystemMessage(Component.literal("§cНе удалось создать команду. Проверьте имя и лимит (макс. 3)."));
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
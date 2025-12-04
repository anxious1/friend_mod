package com.mom.teammod;

import com.mom.teammod.packets.*;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class NetworkHandler {
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(TeamMod.MODID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private static int id = 0;

    public static void register() {
        registerMessage(CreateTeamPacket.class, CreateTeamPacket::encode, CreateTeamPacket::decode, CreateTeamPacket::handle);
        registerMessage(InvitePlayerPacket.class, InvitePlayerPacket::encode, InvitePlayerPacket::decode, InvitePlayerPacket::handle);
        registerMessage(AcceptInvitationPacket.class, AcceptInvitationPacket::encode, AcceptInvitationPacket::decode, AcceptInvitationPacket::handle);
        registerMessage(DeclineInvitationPacket.class, DeclineInvitationPacket::encode, DeclineInvitationPacket::decode, DeclineInvitationPacket::handle);
        registerMessage(LeaveTeamPacket.class, LeaveTeamPacket::encode, LeaveTeamPacket::decode, LeaveTeamPacket::handle);
        registerMessage(KickPlayerPacket.class, KickPlayerPacket::encode, KickPlayerPacket::decode, KickPlayerPacket::handle);
        registerMessage(TransferOwnershipPacket.class, TransferOwnershipPacket::encode, TransferOwnershipPacket::decode, TransferOwnershipPacket::handle);
        registerMessage(SetFriendlyFirePacket.class, SetFriendlyFirePacket::encode, SetFriendlyFirePacket::decode, SetFriendlyFirePacket::handle);
        registerMessage(SendChatPacket.class, SendChatPacket::encode, SendChatPacket::decode, SendChatPacket::handle);
        registerMessage(TeamSyncPacket.class, TeamSyncPacket::encode, TeamSyncPacket::new, TeamSyncPacket::handle);
        registerMessage(ProfileSyncPacket.class, ProfileSyncPacket::encode, ProfileSyncPacket::decode, ProfileSyncPacket::handle);
        registerMessage(PrivateMessagePacket.class, PrivateMessagePacket::encode, PrivateMessagePacket::decode, PrivateMessagePacket::handle);
        registerMessage(UpdateProfilePacket.class, UpdateProfilePacket::encode, UpdateProfilePacket::decode, UpdateProfilePacket::handle);
        registerMessage(DeleteTeamPacket.class, DeleteTeamPacket::encode, DeleteTeamPacket::decode, DeleteTeamPacket::handle);
    }

    private static <M> void registerMessage(
            Class<M> messageType,
            BiConsumer<M, FriendlyByteBuf> encoder,
            Function<FriendlyByteBuf, M> decoder,
            BiConsumer<M, Supplier<NetworkEvent.Context>> messageConsumer) {
        INSTANCE.registerMessage(id++, messageType, encoder, decoder, messageConsumer);
    }
}
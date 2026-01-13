package com.mom.teammod;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Collection;

@Mod.EventBusSubscriber(modid = TeamMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class AdminTeamCommands {

    private AdminTeamCommands() {}

    private static final SuggestionProvider<CommandSourceStack> SUGGEST_TEAMS = (ctx, builder) -> {
        TeamWorldData data = TeamManager.getData();
        if (data == null) return builder.buildFuture();
        Collection<String> names = data.getTeams().keySet();
        return SharedSuggestionProvider.suggest(names, builder);
    };

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> d = event.getDispatcher();

        // /teammodadmin_delete <team>
        d.register(Commands.literal("teammodadmin_delete")
                .requires(src -> src.hasPermission(2))
                .then(Commands.argument("team", StringArgumentType.word())
                        .suggests(SUGGEST_TEAMS)
                        .executes(ctx -> {
                            final String team = StringArgumentType.getString(ctx, "team");
                            return TeamManager.adminDeleteTeam(team, ctx.getSource()) ? 1 : 0;
                        }))
        );

        // /teammodadmin_rename <old> <new>
        d.register(Commands.literal("teammodadmin_rename")
                .requires(src -> src.hasPermission(2))
                .then(Commands.argument("old", StringArgumentType.word())
                        .suggests(SUGGEST_TEAMS)
                        .then(Commands.argument("new", StringArgumentType.word())
                                .executes(ctx -> {
                                    final String oldName = StringArgumentType.getString(ctx, "old");
                                    final String newName = StringArgumentType.getString(ctx, "new");
                                    return TeamManager.adminRenameTeam(oldName, newName, ctx.getSource()) ? 1 : 0;
                                })))
        );

        // /teammodadmin_settag <team> <tag>
        d.register(Commands.literal("teammodadmin_settag")
                .requires(src -> src.hasPermission(2))
                .then(Commands.argument("team", StringArgumentType.word())
                        .suggests(SUGGEST_TEAMS)
                        .then(Commands.argument("tag", StringArgumentType.word())
                                .executes(ctx -> {
                                    final String team = StringArgumentType.getString(ctx, "team");
                                    final String tag = StringArgumentType.getString(ctx, "tag");
                                    return TeamManager.adminSetTeamTag(team, tag, ctx.getSource()) ? 1 : 0;
                                })))
        );
    }
}

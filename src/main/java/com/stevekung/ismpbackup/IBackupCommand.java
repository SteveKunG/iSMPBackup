package com.stevekung.ismpbackup;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

public class IBackupCommand
{
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher)
    {
        dispatcher.register(Commands.literal("ibackup")
                .requires(commandSourceStack -> commandSourceStack.hasPermission(2))
                .then(Commands.argument("name", StringArgumentType.greedyString())
                        .executes(commandContext -> backup(commandContext, StringArgumentType.getString(commandContext, "name"))))
                .executes(commandContext -> backup(commandContext, "latest")));
    }

    private static int backup(CommandContext<CommandSourceStack> commandContext, String name)
    {
        var server = commandContext.getSource().getServer();
        BackupUtils.EXECUTOR.execute(() -> BackupUtils.backup(server, name));
        return 1;
    }
}
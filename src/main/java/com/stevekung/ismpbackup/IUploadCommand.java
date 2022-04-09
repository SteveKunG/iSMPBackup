package com.stevekung.ismpbackup;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

public class IUploadCommand
{
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher)
    {
        dispatcher.register(Commands.literal("iupload")
                .requires(commandSourceStack -> commandSourceStack.hasPermission(2))
                .then(Commands.argument("name", StringArgumentType.greedyString())
                        .executes(commandContext -> upload(commandContext, StringArgumentType.getString(commandContext, "name")))));
    }

    private static int upload(CommandContext<CommandSourceStack> commandContext, String name)
    {
        var server = commandContext.getSource().getServer();
        BackupUtils.upload(server, BackupUtils.BACKUP_FILE);
        return 1;
    }
}
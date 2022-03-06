package com.stevekung.ismpbackup;

import com.mojang.brigadier.CommandDispatcher;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

public class IBackupCommand
{
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher)
    {
        dispatcher.register(Commands.literal("ibackup").requires(commandSourceStack -> commandSourceStack.hasPermission(2)).executes(commandContext ->
        {
            var server = commandContext.getSource().getServer();
            BackupUtils.upload(server, BackupUtils.backup(server, true));
            return 1;
        }));
    }
}
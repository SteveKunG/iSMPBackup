package com.stevekung.ismpbackup;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.TextComponent;

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
        var serverPath = server.getServerDirectory().toPath();
        var file = serverPath.resolve(name + ".zip").toFile();

        if (file.exists())
        {
            BackupUtils.upload(server, file, false);
            return 1;
        }
        else
        {
            commandContext.getSource().sendFailure(new TextComponent("File '" + name + ".zip' not found, try again!"));
            return 1;
        }
    }
}
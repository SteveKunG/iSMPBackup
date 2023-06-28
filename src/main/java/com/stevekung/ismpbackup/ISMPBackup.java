package com.stevekung.ismpbackup;

import java.io.File;
import java.io.IOException;
import java.time.DayOfWeek;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.loader.api.FabricLoader;

public class ISMPBackup implements DedicatedServerModInitializer
{
    public static final BackupConfig CONFIG = AutoConfig.register(BackupConfig.class, GsonConfigSerializer::new).getConfig();
    public static final Logger LOGGER = LogUtils.getLogger();
    private static boolean backupStarted;

    @Override
    public void onInitializeServer()
    {
        CommandRegistrationCallback.EVENT.register((dispatcher, context, selection) ->
        {
            IBackupCommand.register(dispatcher);
            IUploadCommand.register(dispatcher);
        });

        ServerLifecycleEvents.SERVER_STARTING.register(server ->
        {
            DriveAPI.CREDENTIALS = new File(FabricLoader.getInstance().getConfigDir().toFile(), "ismpbackup/credentials.json");

            if (!DriveAPI.CREDENTIALS.exists())
            {
                throw new RuntimeException("Couldn't find 'credentials.json' in the 'config/ismpbackup' folder!");
            }

            try
            {
                DriveAPI.init();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        });

        ServerTickEvents.START_SERVER_TICK.register(server ->
        {
            var zdt = ZonedDateTime.now(ZoneId.of("Asia/Bangkok"));
            var localTime = zdt.toLocalTime();

            if (!backupStarted && zdt.getDayOfWeek() == DayOfWeek.SATURDAY && localTime.getHour() == 0 && localTime.getMinute() == 0 && localTime.getSecond() == 0)
            {
                LOGGER.info("Backup started");
                backupStarted = true;

                BackupUtils.EXECUTOR.execute(() ->
                {
                    var file = CompletableFuture.supplyAsync(() -> BackupUtils.backup(server, "date"), BackupUtils.EXECUTOR).join();
                    backupStarted = false;
                    LOGGER.info("Backup finished");
                    BackupUtils.upload(server, file, true);
                });
            }
        });

        ServerLifecycleEvents.SERVER_STOPPING.register(server -> BackupUtils.EXECUTOR.shutdownNow());
    }
}
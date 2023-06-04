package com.stevekung.ismpbackup;

import java.io.File;
import java.io.IOException;
import java.time.DayOfWeek;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;

public class ISMPBackup implements DedicatedServerModInitializer
{
    public static final BackupConfig CONFIG = AutoConfig.register(BackupConfig.class, GsonConfigSerializer::new).getConfig();
    private static final ScheduledExecutorService BACKUP_SCHEDULE = Executors.newScheduledThreadPool(1);

    @Override
    public void onInitializeServer()
    {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
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

        ServerLifecycleEvents.SERVER_STARTED.register(server ->
        {
            BACKUP_SCHEDULE.scheduleAtFixedRate(() ->
            {
                var zdt = ZonedDateTime.now(ZoneId.of("Asia/Bangkok"));
                var saturday = zdt.getDayOfWeek() == DayOfWeek.SATURDAY;
                var midnight = zdt.getHour() == 0;

                if (saturday && midnight && zdt.getMinute() >= 0 && zdt.getMinute() <= 10)
                {
                    var file = CompletableFuture.supplyAsync(() -> BackupUtils.backup(server, "date"), BackupUtils.EXECUTOR).join();
                    BackupUtils.upload(server, file, true);
                }
            }, 5L, TimeUnit.SECONDS.convert(1, TimeUnit.HOURS), TimeUnit.SECONDS);
        });

        ServerLifecycleEvents.SERVER_STOPPING.register(server ->
        {
            BACKUP_SCHEDULE.shutdownNow();
            BackupUtils.EXECUTOR.shutdownNow();
        });
    }
}
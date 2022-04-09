package com.stevekung.ismpbackup;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.concurrent.ExecutorService;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.slf4j.Logger;

import com.google.api.client.http.FileContent;
import com.mojang.logging.LogUtils;

import net.minecraft.ChatFormatting;
import net.minecraft.FileUtil;
import net.minecraft.Util;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.StringUtil;

public class BackupUtils
{
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("MM-dd-yy");
    public static final ExecutorService BACKUP_EXECUTOR = Util.makeExecutor("iSMPBackup");
    private static final ExecutorService UPLOAD_EXECUTOR = Util.makeExecutor("iSMPBackupUpload");
    public static File BACKUP_FILE;

    public static File backup(MinecraftServer server, String name)
    {
        var levelId = "iSMP";
        var fileName = "iSMP_" + (!StringUtil.isNullOrEmpty(name) && !name.equals("date") ? name : LocalDateTime.now().format(FORMATTER));
        var serverPath = server.getServerDirectory().toPath();
        var levelPath = serverPath.resolve(levelId);

        try
        {
            Files.createDirectories(Files.exists(serverPath) ? serverPath.toRealPath() : serverPath);
            var backupFile = serverPath.resolve(FileUtil.findAvailableName(serverPath, fileName, ".zip"));

            LOGGER.info("Starting map backup: {}", backupFile.getFileName());

            try (var zipOutputStream = new ZipOutputStream(new BufferedOutputStream(Files.newOutputStream(backupFile)));)
            {
                Files.walkFileTree(levelPath, new SimpleFileVisitor<>()
                {
                    @Override
                    public FileVisitResult visitFile(Path path, BasicFileAttributes basicFileAttributes) throws IOException
                    {
                        if (path.endsWith("session.lock"))
                        {
                            return FileVisitResult.CONTINUE;
                        }
                        var string = Paths.get(levelId).resolve(levelPath.relativize(path)).toString().replace('\\', '/');
                        LOGGER.info("Zipping file: {}", string);
                        var zipEntry = new ZipEntry(string);
                        zipOutputStream.putNextEntry(zipEntry);
                        com.google.common.io.Files.asByteSource(path.toFile()).copyTo(zipOutputStream);
                        zipOutputStream.closeEntry();
                        return FileVisitResult.CONTINUE;
                    }
                });
            }
            LOGGER.info("Successfully created map backup: {}", backupFile.getFileName());
            return backupFile.toFile();
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    public static void upload(MinecraftServer server, File toUpload)
    {
        UPLOAD_EXECUTOR.execute(() ->
        {
            LOGGER.info("Start uploading map backup: {}", toUpload.getName());
            var mapBackupFolderId = "1f6BrIKWkqCMJ-iobaolP3wWD42pdaqR3";
            var fileMetadata = new com.google.api.services.drive.model.File();
            fileMetadata.setName(toUpload.getName());
            fileMetadata.setParents(Collections.singletonList(mapBackupFolderId));
            var mediaContent = new FileContent(null, toUpload);

            try
            {
                DriveAPI.DRIVE.files().create(fileMetadata, mediaContent).setFields("id, parents").execute();
                var component = new TextComponent("[Backup] ").setStyle(Style.EMPTY.applyFormats(ChatFormatting.YELLOW, ChatFormatting.BOLD)).append(new TextComponent(toUpload.getName() + " has been uploaded to iSMP Drive!").setStyle(Style.EMPTY.withBold(false).withColor(ChatFormatting.WHITE)));
                server.getPlayerList().broadcastMessage(component, ChatType.SYSTEM, Util.NIL_UUID);
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        });
    }
}
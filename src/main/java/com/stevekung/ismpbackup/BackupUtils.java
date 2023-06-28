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

import com.google.api.client.googleapis.media.MediaHttpUploader;
import com.google.api.client.googleapis.media.MediaHttpUploaderProgressListener;
import com.google.api.client.http.FileContent;

import net.minecraft.ChatFormatting;
import net.minecraft.FileUtil;
import net.minecraft.Util;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.server.MinecraftServer;

public class BackupUtils
{
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    public static final ExecutorService EXECUTOR = Util.makeExecutor("iSMPBackup");

    public static File backup(MinecraftServer server, String name)
    {
        var levelId = "iSMP";
        var fileName = "iSMP_" + (name.equals("date") ? LocalDateTime.now().format(FORMATTER) : name);
        var serverPath = server.getServerDirectory().toPath();
        var levelPath = serverPath.resolve(levelId);

        server.saveEverything(true, true, true);

        try
        {
            Files.createDirectories(Files.exists(serverPath) ? serverPath.toRealPath() : serverPath);
            var backupFile = serverPath.resolve(FileUtil.findAvailableName(serverPath, fileName, ".zip"));
            ISMPBackup.LOGGER.info("Starting backup task: {}", backupFile.getFileName());

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
                        ISMPBackup.LOGGER.info("Zipping file: {}", string);
                        var zipEntry = new ZipEntry(string);
                        zipOutputStream.putNextEntry(zipEntry);
                        com.google.common.io.Files.asByteSource(path.toFile()).copyTo(zipOutputStream);
                        zipOutputStream.closeEntry();
                        return FileVisitResult.CONTINUE;
                    }
                });
                ISMPBackup.LOGGER.info("Backup successfully created at: {}", backupFile.getFileName());
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
            return backupFile.toFile();
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    public static void upload(MinecraftServer server, File toUpload, boolean delete)
    {
        if (!ISMPBackup.CONFIG.autoUpload)
        {
            return;
        }

        EXECUTOR.execute(() ->
        {
            var fileName = toUpload.getName();
            ISMPBackup.LOGGER.info("Starting upload backup: {}", fileName);
            var mapBackupFolderId = "1f6BrIKWkqCMJ-iobaolP3wWD42pdaqR3";
            var fileMetadata = new com.google.api.services.drive.model.File();
            fileMetadata.setName(fileName);
            fileMetadata.setParents(Collections.singletonList(mapBackupFolderId));
            var mediaContent = new FileContent(null, toUpload);

            try
            {
                var toDriveFile = DriveAPI.DRIVE.files().create(fileMetadata, mediaContent).setFields("id, parents");
                var uploader = toDriveFile.getMediaHttpUploader();

                uploader.setDirectUploadEnabled(false);
                uploader.setProgressListener(new FileUploadProgressListener(fileName));

                toDriveFile.execute();
                var component = Component.literal("[Backup] ").withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD).append(Component.literal(fileName + " has been uploaded to iSMP Drive!").setStyle(Style.EMPTY.withBold(false).withColor(ChatFormatting.WHITE)));
                server.getPlayerList().broadcastSystemMessage(component, true);

                if (delete)
                {
                    toUpload.delete();
                }
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        });
    }

    static class FileUploadProgressListener implements MediaHttpUploaderProgressListener
    {
        private final String fileName;

        FileUploadProgressListener(String fileName)
        {
            this.fileName = fileName;
        }

        @Override
        public void progressChanged(MediaHttpUploader mediaHttpUploader) throws IOException
        {
            switch (mediaHttpUploader.getUploadState())
            {
                case MEDIA_IN_PROGRESS:
                    if (Util.getMillis() % 2L == 0)
                    {
                        var percent = mediaHttpUploader.getProgress() * 100;
                        ISMPBackup.LOGGER.info("'{}' upload to iSMP Drive: {}%", this.fileName, "%.1f".formatted(percent));
                    }
                    break;
                case MEDIA_COMPLETE:
                    ISMPBackup.LOGGER.info("'{}' has uploaded complete!", this.fileName);
                    break;
                case NOT_STARTED:
                    ISMPBackup.LOGGER.info("Upload not yet started!");
                    break;
                default:
                    break;
            }
        }
    }
}
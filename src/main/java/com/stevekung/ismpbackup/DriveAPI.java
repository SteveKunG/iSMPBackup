package com.stevekung.ismpbackup;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.util.Collections;

import org.slf4j.Logger;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.mojang.logging.LogUtils;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.Util;

public class DriveAPI
{
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String APPLICATION_NAME = "iSMP Backup";
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static NetHttpTransport HTTP_TRANSPORT;

    public static File CREDENTIALS;
    public static Drive DRIVE;

    static
    {
        try
        {
            HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        }
        catch (GeneralSecurityException | IOException e)
        {
            e.printStackTrace();
        }
    }

    public static void init() throws IOException
    {
        LOGGER.info("Initializing Google Drive API");
        DRIVE = new Drive.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials()).setApplicationName(APPLICATION_NAME).build();
    }

    private static Credential getCredentials() throws IOException
    {
        LOGGER.info("Initializing credentials");
        var clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(new FileInputStream(CREDENTIALS)));
        var flow = new GoogleAuthorizationCodeFlow.Builder(HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, Collections.singletonList(DriveScopes.DRIVE_FILE)).setDataStoreFactory(new FileDataStoreFactory(new File(FabricLoader.getInstance().getConfigDir().toFile(), "ismpbackup"))).setAccessType("offline").build();
        var receiver = new LocalServerReceiver.Builder().setPort(8888).build();
        return new AuthorizationCodeInstalledApp(flow, receiver, new DriveBrowser()).authorize("user");
    }

    private static class DriveBrowser implements AuthorizationCodeInstalledApp.Browser
    {
        @Override
        public void browse(String url) throws IOException
        {
            Util.getPlatform().openUri(url);
        }
    }
}
package com.stevekung.ismpbackup;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;

@Config(name = "ismpbackup")
public final class BackupConfig implements ConfigData
{
    public boolean autoUpload = true;
}
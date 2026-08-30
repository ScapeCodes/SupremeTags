package net.noscape.project.supremetags.editorweb;

import java.util.ArrayList;
import java.util.List;

public class DumpPayload {
    public String createdAt;
    public SupremeTagsInfo supremeTags = new SupremeTagsInfo();
    public ServerInfo server = new ServerInfo();
    public EnvironmentInfo environment = new EnvironmentInfo();
    public List<PluginInfo> plugins = new ArrayList<>();
    public ConfigFile config = new ConfigFile();
    public List<ConfigFile> tagFiles = new ArrayList<>();
    public ConfigFile categories = new ConfigFile();

    public static class SupremeTagsInfo {
        public String version;
        public String databaseType;
        public int tagsLoaded;
        public int categoriesLoaded;
        public String personalTags;
    }

    public static class ServerInfo {
        public String version;
        public boolean onlineMode;
        public String supportStatus;
    }

    public static class EnvironmentInfo {
        public String javaVersion;
        public String operatingSystem;
        public String uptime;
        public String allocatedMemory;
    }

    public static class PluginInfo {
        public String name;
        public String version;
        public boolean enabled;
    }

    public static class ConfigFile {
        public String name;
        public String path;
        public String content;
    }
}

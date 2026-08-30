package net.noscape.project.supremetags.editorweb;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.noscape.project.supremetags.SupremeTags;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TagDumpExportService {

    private static final Pattern SENSITIVE_YAML_LINE = Pattern.compile(
            "^(\\s*)([^:#\\n]*?(?:password|pass|secret|token|key|address|host|hostname|ip|port|username|user|database|url)[^:#\\n]*)(\\s*:\\s*)(.*)$",
            Pattern.CASE_INSENSITIVE
    );

    private final SupremeTags plugin;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    public TagDumpExportService(SupremeTags plugin) {
        this.plugin = plugin;
    }

    public String exportJson() {
        return gson.toJson(exportPayload());
    }

    public DumpPayload exportPayload() {
        DumpPayload payload = new DumpPayload();
        payload.createdAt = Instant.now().toString();

        payload.supremeTags.version = plugin.getDescription().getVersion();
        payload.supremeTags.databaseType = plugin.getConfigManager().getConfig("data.yml").get().getString("data.type", "UNKNOWN");
        payload.supremeTags.tagsLoaded = plugin.getTagManager().getTags().size();
        payload.supremeTags.categoriesLoaded = plugin.getCategoryManager().getCatorgies().size();
        payload.supremeTags.personalTags = plugin.getConfig().getBoolean("settings.personal-tags.enable", false) ? "Enabled" : "Disabled";

        payload.server.version = Bukkit.getVersion();
        payload.server.onlineMode = Bukkit.getOnlineMode();
        payload.server.supportStatus = supportStatus(Bukkit.getBukkitVersion());

        Runtime runtime = Runtime.getRuntime();
        payload.environment.javaVersion = System.getProperty("java.version", "Unknown");
        payload.environment.operatingSystem = System.getProperty("os.name", "Unknown") + " " + System.getProperty("os.version", "");
        payload.environment.uptime = formatDuration(Duration.ofMillis(ManagementFactory.getRuntimeMXBean().getUptime()));
        payload.environment.allocatedMemory = formatBytes(runtime.totalMemory()) + " / " + formatBytes(runtime.maxMemory());

        Plugin[] installedPlugins = Bukkit.getPluginManager().getPlugins();
        Arrays.sort(installedPlugins, Comparator.comparing(Plugin::getName, String.CASE_INSENSITIVE_ORDER));
        for (Plugin installedPlugin : installedPlugins) {
            DumpPayload.PluginInfo info = new DumpPayload.PluginInfo();
            info.name = installedPlugin.getName();
            info.version = installedPlugin.getDescription().getVersion();
            info.enabled = installedPlugin.isEnabled();
            payload.plugins.add(info);
        }

        payload.config = file("config.yml", "config.yml", new File(plugin.getDataFolder(), "config.yml"));
        for (String path : plugin.getConfigManager().getTagFilePaths()) {
            payload.tagFiles.add(file(new File(path).getName(), "tags/" + path, new File(plugin.getDataFolder(), "tags/" + path)));
        }
        payload.categories = file("categories.yml", "categories.yml", new File(plugin.getDataFolder(), "categories.yml"));

        return payload;
    }

    private DumpPayload.ConfigFile file(String name, String path, File file) {
        DumpPayload.ConfigFile configFile = new DumpPayload.ConfigFile();
        configFile.name = name;
        configFile.path = path.replace(File.separatorChar, '/');
        configFile.content = readSanitized(file);
        return configFile;
    }

    private String readSanitized(File file) {
        if (file == null || !file.exists() || !file.isFile()) {
            return "# File was not found.";
        }

        try {
            String content = Files.readString(file.toPath(), StandardCharsets.UTF_8);
            return sanitizeYaml(content);
        } catch (IOException exception) {
            return "# Could not read file: " + exception.getMessage();
        }
    }

    private String sanitizeYaml(String content) {
        StringBuilder builder = new StringBuilder();
        String[] lines = content.replace("\r\n", "\n").replace('\r', '\n').split("\n", -1);
        for (String line : lines) {
            Matcher matcher = SENSITIVE_YAML_LINE.matcher(line);
            if (matcher.matches()) {
                builder.append(matcher.group(1))
                        .append(matcher.group(2))
                        .append(matcher.group(3))
                        .append("<redacted>");
            } else {
                builder.append(line);
            }
            builder.append('\n');
        }
        return builder.toString();
    }

    private String supportStatus(String bukkitVersion) {
        Matcher matcher = Pattern.compile("1\\.(\\d+)").matcher(bukkitVersion == null ? "" : bukkitVersion);
        if (!matcher.find()) {
            return "Unknown";
        }

        int minor = Integer.parseInt(matcher.group(1));
        if (minor < 16) {
            return "Unsupported";
        }
        if (minor <= 21) {
            return "Supported";
        }
        return "Newer than tested";
    }

    private String formatDuration(Duration duration) {
        long seconds = duration.getSeconds();
        long days = seconds / 86400;
        long hours = (seconds % 86400) / 3600;
        long minutes = (seconds % 3600) / 60;
        if (days > 0) {
            return String.format(Locale.ROOT, "%dd %dh %dm", days, hours, minutes);
        }
        return String.format(Locale.ROOT, "%dh %dm", hours, minutes);
    }

    private String formatBytes(long bytes) {
        if (bytes <= 0) {
            return "Unknown";
        }

        double mib = bytes / 1024.0 / 1024.0;
        if (mib < 1024) {
            return String.format(Locale.ROOT, "%.0f MiB", mib);
        }
        return String.format(Locale.ROOT, "%.2f GiB", mib / 1024.0);
    }
}

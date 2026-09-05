package net.noscape.project.supremetags.editorweb;

import com.google.gson.JsonElement;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class EditorPayload {

    public int schemaVersion = 1;
    public String editor = "supremetags-web";
    public String exportedAt;
    public PluginInfo plugin = new PluginInfo();
    public EditorData data = new EditorData();

    public static class PluginInfo {
        public String name = "SupremeTags";
        public String version;
        public String latestVersion;
        public String serverVersion;
        public String storageMode;
        public int tagsLoaded;
        public int categoriesLoaded;
    }

    public static class EditorData {
        public List<String> categories = new ArrayList<>();
        public List<String> rarities = new ArrayList<>();
        public List<TagDto> tags = new ArrayList<>();
    }

    public static class TagDto {
        public String identifier;
        public List<String> tag = new ArrayList<>();
        public String permission;
        public List<String> groups = new ArrayList<>();
        public List<String> description = new ArrayList<>();
        public String category;
        public int order;
        public boolean withdrawable;
        public String rarity;
        public String displayName;
        public String displayItem;
        public int customModelData;
        public boolean nameWrapperEnabled;
        public boolean nameWrapperOnly;
        public String nameWrapperFormat;
        public List<String> effects = new ArrayList<>();
        public List<String> abilities = new ArrayList<>();
        public Map<String, String> customPlaceholders = new LinkedHashMap<>();
        public EconomyDto economy = new EconomyDto();
        public VoucherDto voucher = new VoucherDto();
        public List<VariantDto> variants = new ArrayList<>();
        public RequirementsDto requirements = new RequirementsDto();
    }

    public static class EconomyDto {
        public boolean enabled;
        public String type;
        public double amount;
        public String takeCommand;
        public String condition;
    }

    public static class VoucherDto {
        public String material;
        public String displayName;
        public List<String> lore = new ArrayList<>();
        public int customModelData;
        public boolean glow;
    }

    public static class VariantDto {
        public String identifier;
        public JsonElement tag;
        public String permission;
        public List<String> description = new ArrayList<>();
        public String rarity;
        public String unlockedMaterial;
        public String unlockedDisplayName;
        public int unlockedCustomModelData;
        public String lockedMaterial;
        public String lockedDisplayName;
        public int lockedCustomModelData;
    }

    public static class RequirementsDto {
        public boolean enabled;
        public boolean persistUnlock;
        public String mode = "all";
        public List<RequirementDto> list = new ArrayList<>();
    }

    public static class RequirementDto {
        public String name;
        public String type;
        public String permission;
        public String placeholder;
        public String operator;
        public String value;
        public String tag;
        public String economyType;
        public double amount;
        public String display;
        public String loreDisplay;
        public String message;
    }
}

package net.noscape.project.supremetags.storage.tags;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.noscape.project.supremetags.SupremeTags;
import net.noscape.project.supremetags.handlers.Tag;
import net.noscape.project.supremetags.handlers.TagEconomy;
import net.noscape.project.supremetags.handlers.Variant;
import net.noscape.project.supremetags.storage.MySQLDatabase;
import org.bukkit.potion.PotionEffectType;

import java.sql.*;
import java.util.*;
import java.util.logging.Level;

public class MySQLTags {

    private final MySQLDatabase db;
    private final Gson gson;

    public MySQLTags(MySQLDatabase db) {
        this.db = db;
        this.gson = new GsonBuilder().serializeNulls().create();
        createTables();
    }

    private void createTables() {
        String tagsTable = "CREATE TABLE IF NOT EXISTS `tags` (" +
                "identifier VARCHAR(255) PRIMARY KEY," +
                "category VARCHAR(255)," +
                "permission VARCHAR(255)," +
                "order_id INT," +
                "withdrawable TINYINT(1)," +
                "rarity VARCHAR(255)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;";

        String tagsDataTable = "CREATE TABLE IF NOT EXISTS `tags_data` (" +
                "identifier VARCHAR(255) PRIMARY KEY," +
                "tag_json JSON," +
                "description_json JSON," +
                "effects_json JSON," +
                "variants_json JSON," +
                "economy_json JSON," +
                "abilities_json JSON," +
                "FOREIGN KEY (identifier) REFERENCES tags(identifier) ON DELETE CASCADE" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;";

        try (Connection conn = db.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(tagsTable);
            stmt.execute(tagsDataTable);
        } catch (SQLException e) {
            SupremeTags.getInstance().getLogger().log(Level.SEVERE, "MySQLTags: Failed to create tables", e);
        }
    }

    public void saveTag(Tag tag) {
        // MySQL: use INSERT ... ON DUPLICATE KEY UPDATE
        String insertTag = "INSERT INTO `tags` (identifier, category, permission, order_id, withdrawable, rarity) VALUES (?, ?, ?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE category = VALUES(category), permission = VALUES(permission), order_id = VALUES(order_id), withdrawable = VALUES(withdrawable), rarity = VALUES(rarity)";

        String insertData = "INSERT INTO `tags_data` (identifier, tag_json, description_json, effects_json, variants_json, economy_json, abilities_json) VALUES (?, ?, ?, ?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE tag_json = VALUES(tag_json), description_json = VALUES(description_json), effects_json = VALUES(effects_json), variants_json = VALUES(variants_json), economy_json = VALUES(economy_json), abilities_json = VALUES(abilities_json)";

        try (Connection conn = db.getConnection();
             PreparedStatement psTag = conn.prepareStatement(insertTag);
             PreparedStatement psData = conn.prepareStatement(insertData)) {

            conn.setAutoCommit(false);

            psTag.setString(1, tag.getIdentifier());
            psTag.setString(2, tag.getCategory());
            psTag.setString(3, tag.getPermission());
            psTag.setInt(4, tag.getOrder());
            psTag.setInt(5, tag.isWithdrawable() ? 1 : 0);
            psTag.setString(6, tag.getRarity());
            psTag.executeUpdate();

            String tagJson = gson.toJson(tag.getTag());
            String descJson = gson.toJson(tag.getDescription());
            String effectsJson = gson.toJson(serializeEffects(tag.getEffects()));
            String variantsJson = gson.toJson(tag.getVariants());
            String economyJson = gson.toJson(tag.getEconomy());
            String abilitiesJson = gson.toJson(tag.getAbilities());

            psData.setString(1, tag.getIdentifier());
            psData.setString(2, tagJson);
            psData.setString(3, descJson);
            psData.setString(4, effectsJson);
            psData.setString(5, variantsJson);
            psData.setString(6, economyJson);
            psData.setString(7, abilitiesJson);
            psData.executeUpdate();

            conn.commit();
            conn.setAutoCommit(true);
        } catch (SQLException e) {
            SupremeTags.getInstance().getLogger().log(Level.SEVERE, "MySQLTags: Failed to save tag " + tag.getIdentifier(), e);
        }
    }

    public void deleteTag(String identifier) {
        String deleteData = "DELETE FROM tags_data WHERE identifier = ?";
        String deleteTag = "DELETE FROM tags WHERE identifier = ?";

        try (Connection conn = db.getConnection();
             PreparedStatement psData = conn.prepareStatement(deleteData);
             PreparedStatement psTag = conn.prepareStatement(deleteTag)) {

            conn.setAutoCommit(false);

            psData.setString(1, identifier);
            psData.executeUpdate();

            psTag.setString(1, identifier);
            psTag.executeUpdate();

            conn.commit();
            conn.setAutoCommit(true);
        } catch (SQLException e) {
            SupremeTags.getInstance().getLogger().log(Level.SEVERE, "MySQLTags: Failed to delete tag " + identifier, e);
        }
    }

    public Map<String, Tag> loadTags() {
        Map<String, Tag> loaded = new LinkedHashMap<>();
        String q = "SELECT t.identifier, t.category, t.permission, t.order_id, t.withdrawable, t.rarity, " +
                "d.tag_json, d.description_json, d.effects_json, d.variants_json, d.economy_json, d.abilities_json " +
                "FROM tags t LEFT JOIN tags_data d ON t.identifier = d.identifier";

        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(q);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                String identifier = rs.getString("identifier");
                String category = rs.getString("category");
                String permission = rs.getString("permission");
                int order = rs.getInt("order_id");
                boolean withdrawable = rs.getInt("withdrawable") == 1;
                String rarity = rs.getString("rarity");

                String tagJson = rs.getString("tag_json");
                String descJson = rs.getString("description_json");
                String effectsJson = rs.getString("effects_json");
                String variantsJson = rs.getString("variants_json");
                String economyJson = rs.getString("economy_json");
                String abilitiesJson = rs.getString("abilities_json");

                List<String> tagList = tagJson == null ? new ArrayList<>() : gson.fromJson(tagJson, List.class);
                List<String> description = descJson == null ? new ArrayList<>() : gson.fromJson(descJson, List.class);

                Map<PotionEffectType, Integer> effects = deserializeEffects(effectsJson);
                List<Variant> variants = variantsJson == null ? new ArrayList<>() : Arrays.asList(gson.fromJson(variantsJson, Variant[].class));

                TagEconomy economy = economyJson == null ? new TagEconomy("VAULT", 0, false) : gson.fromJson(economyJson, TagEconomy.class);

                List<String> abilities = abilitiesJson == null ? new ArrayList<>() : Arrays.asList(gson.fromJson(abilitiesJson, String[].class));

                Tag t = new Tag(identifier, tagList, category, permission, description, order, withdrawable, rarity, effects, economy, variants);
                t.setAbilities(abilities);

                if (economy != null) {
                    t.setEcoEnabled(economy.isEnabled());
                    t.setEcoType(economy.getType());
                    t.setEcoAmount(economy.getAmount());
                }

                loaded.put(identifier, t);
            }

        } catch (SQLException e) {
            SupremeTags.getInstance().getLogger().log(Level.SEVERE, "MySQLTags: Failed to load tags", e);
        }

        return loaded;
    }

    public void updateTag(Tag tag) {
        saveTag(tag);
    }

    /* ---------------------- Helpers for effects map ---------------------- */

    private Map<String, Integer> serializeEffects(Map<PotionEffectType, Integer> effects) {
        Map<String, Integer> map = new HashMap<>();
        if (effects == null) return map;
        for (Map.Entry<PotionEffectType, Integer> e : effects.entrySet()) {
            if (e.getKey() != null) map.put(e.getKey().getName(), e.getValue());
        }
        return map;
    }

    private Map<PotionEffectType, Integer> deserializeEffects(String json) {
        Map<PotionEffectType, Integer> result = new HashMap<>();
        if (json == null || json.isEmpty()) return result;
        try {
            Map<String, Double> temp = gson.fromJson(json, Map.class);
            if (temp == null) return result;
            for (Map.Entry<String, Double> entry : temp.entrySet()) {
                try {
                    PotionEffectType type = PotionEffectType.getByName(entry.getKey());
                    if (type != null) result.put(type, entry.getValue().intValue());
                } catch (Exception ignored) {}
            }
        } catch (Exception e) {
            SupremeTags.getInstance().getLogger().warning("MySQLTags: Could not deserialize effects json: " + e.getMessage());
        }
        return result;
    }
}

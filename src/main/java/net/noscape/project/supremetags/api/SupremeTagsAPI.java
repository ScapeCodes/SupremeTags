package net.noscape.project.supremetags.api;

import net.noscape.project.supremetags.SupremeTags;
import net.noscape.project.supremetags.handlers.Tag;
import net.noscape.project.supremetags.storage.UserData;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class SupremeTagsAPI {

    public Tag getTag(String identifier) {
        return SupremeTags.getInstance().getTagManager().getTag(identifier);
    }

    public Tag getPlayerTag(UUID uuid) {
        return SupremeTags.getInstance().getTagManager().getTag(UserData.getActive(uuid));
    }

    public boolean hasTag(UUID uuid) {
        return !UserData.getActive(uuid).equalsIgnoreCase("none");
    }

    public List<Tag> getAllTags() {
        return new ArrayList<>(SupremeTags.getInstance().getTagManager().getTags().values());
    }
}

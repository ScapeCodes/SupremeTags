package net.noscape.project.supremetags.commands;

import net.noscape.project.supremetags.SupremeTags;
import net.noscape.project.supremetags.guis.personaltags.PersonalTagsMenu;
import net.noscape.project.supremetags.utils.commands.BaseCommand;
import net.noscape.project.supremetags.utils.commands.Command;
import net.noscape.project.supremetags.utils.commands.CommandArguments;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.Objects;

import static net.noscape.project.supremetags.utils.Utils.msgPlayer;

public class MyTags extends BaseCommand {

    private FileConfiguration messages = SupremeTags.getInstance().getConfigManager().getConfig("messages.yml").get();

    @Command(name = "mytags", permission = "supremetags.mytags")
    @Override
    public void executeAs(CommandArguments command) {
        Player player = command.getPlayer();

        String[] args = command.getArgs();

        String p_tags_disabled = messages.getString("messages.ptags-disabled").replaceAll("%prefix%", Objects.requireNonNull(messages.getString("messages.prefix")));

        if (args.length == 0) {
            if (SupremeTags.getInstance().getConfig().getBoolean("settings.personal-tags.enable")) {
                new PersonalTagsMenu(SupremeTags.getMenuUtil(player)).open();
            } else {
                msgPlayer(player, p_tags_disabled);
            }
        }
    }
}
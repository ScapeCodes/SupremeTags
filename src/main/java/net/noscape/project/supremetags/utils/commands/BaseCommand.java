package net.noscape.project.supremetags.utils.commands;

import net.noscape.project.supremetags.SupremeTags;

public abstract class BaseCommand {

    public BaseCommand() {
        SupremeTags.getInstance().getCommandFramework().registerCommands(this, null);
    }

    public abstract void executeAs(CommandArguments command);
}

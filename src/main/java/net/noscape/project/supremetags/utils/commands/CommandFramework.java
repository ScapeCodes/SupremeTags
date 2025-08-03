package net.noscape.project.supremetags.utils.commands;

import net.noscape.project.supremetags.SupremeTags;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.SimplePluginManager;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.Map.Entry;

import static net.noscape.project.supremetags.utils.Utils.msgPlayer;

public class CommandFramework implements CommandExecutor {

    private SupremeTags plugin;
    private Map<String, Entry<Method, Object>> commandMap = new HashMap<>();
    private CommandMap map;

    public CommandFramework(SupremeTags plugin) {
        this.plugin = plugin;

        if (plugin.getServer().getPluginManager() instanceof SimplePluginManager) {
            SimplePluginManager manager = (SimplePluginManager) plugin.getServer().getPluginManager();
            try {
                Field field = SimplePluginManager.class.getDeclaredField("commandMap");
                field.setAccessible(true);
                map = (CommandMap) field.get(manager);
            } catch (IllegalArgumentException | SecurityException | NoSuchFieldException | IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, org.bukkit.command.Command cmd, String label, String[] args) {
        return handleCommand(sender, cmd, label, args);
    }

    public boolean handleCommand(CommandSender sender, org.bukkit.command.Command cmd, String label, String[] args) {
        for (int i = args.length; i >= 0; i--) {
            StringBuffer buffer = new StringBuffer();
            buffer.append(label.toLowerCase());
            for (int x = 0; x < i; x++) {
                buffer.append("." + args[x].toLowerCase());
            }

            String cmdLabel = buffer.toString();
            if (commandMap.containsKey(cmdLabel)) {
                Method method = commandMap.get(cmdLabel).getKey();
                Object methodObject = commandMap.get(cmdLabel).getValue();
                Command command = method.getAnnotation(Command.class);

                FileConfiguration messages = SupremeTags.getInstance().getConfigManager().getConfig("messages.yml").get();

                if (!command.permission().equals("") && (!sender.hasPermission(command.permission()))) {
                    String no_perm_command_format = SupremeTags.getInstance().getConfigManager().getConfig("messages.yml").get().getString("messages.no-permission").replaceAll("%prefix%", Objects.requireNonNull(messages.getString("messages.prefix")));
                    msgPlayer(sender, no_perm_command_format);
                    return true;
                }

                if (command.inGameOnly() && !(sender instanceof Player)) {
                    String invalid_console_access_format = SupremeTags.getInstance().getConfigManager().getConfig("messages.yml").get().getString("messages.no-console-access").replaceAll("%prefix%", Objects.requireNonNull(messages.getString("messages.prefix")));
                    msgPlayer(sender, invalid_console_access_format);
                    return true;
                }

                try {
                    method.invoke(methodObject, new CommandArguments(sender, cmd, label, args, cmdLabel.split("\\.").length - 1));
                } catch (IllegalArgumentException | IllegalAccessException | InvocationTargetException e) {
                    e.printStackTrace();
                }
                return true;
            }
        }

        defaultCommand(new CommandArguments(sender, cmd, label, args, 0));
        return true;
    }

    public void registerCommands(Object obj, List<String> aliases) {
        for (Method method : obj.getClass().getMethods()) {
            // Check for @Command annotation
            if (method.getAnnotation(Command.class) != null) {
                Command command = method.getAnnotation(Command.class);

                // Verify method signature
                if (method.getParameterTypes().length > 1 || method.getParameterTypes()[0] != CommandArguments.class) {
                    System.out.println("Unable to register command " + method.getName() + ". Unexpected method arguments");
                    continue;
                }

                // Add dynamic aliases if the command name matches the main command
                if (command.name().equalsIgnoreCase(SupremeTags.getInstance().getConfig().getString("settings.commands.main-command"))) {
                    aliases = new ArrayList<>(SupremeTags.getInstance().getConfig().getStringList("settings.commands.aliases"));
                }

                // Register the command with its name and aliases from annotation
                registerCommand(command, command.name(), method, obj);

                if (aliases != null) {
                    for (String alias : aliases) {
                        registerCommand(command, alias, method, obj);
                        registerCompleter(alias, method, obj); // Register completer for alias
                    }
                }
            } else if (method.getAnnotation(Completer.class) != null) {
                Completer completer = method.getAnnotation(Completer.class);

                // Verify method signature for the completer
                if (method.getParameterTypes().length != 1 || method.getParameterTypes()[0] != CommandArguments.class) {
                    System.out.println("Unable to register tab completer " + method.getName() + ". Unexpected method arguments");
                    continue;
                }
                if (method.getReturnType() != List.class) {
                    System.out.println("Unable to register tab completer " + method.getName() + ". Unexpected return type");
                    continue;
                }

                // Register the completer for the command and its aliases
                registerCompleter(completer.name(), method, obj);
                for (String alias : completer.aliases()) {
                    registerCompleter(alias, method, obj);
                }
            }
        }
    }

    public void registerCommand(Command command, String label, Method m, Object obj) {
        if (label.equalsIgnoreCase("tags")) {
            label = SupremeTags.getInstance().getConfig().getString("settings.commands.main-command");
        }

        // Map the main command and its aliases
        commandMap.put(label.toLowerCase(), new AbstractMap.SimpleEntry<>(m, obj));
        commandMap.put(this.plugin.getName() + ':' + label.toLowerCase(), new AbstractMap.SimpleEntry<>(m, obj));

        String cmdLabel = label.replace(".", ",").split(",")[0].toLowerCase();
        org.bukkit.command.Command bukkitCommand = map.getCommand(cmdLabel);

        // Register the command if not already registered
        if (bukkitCommand == null) {
            org.bukkit.command.Command cmd = new BukkitCommand(cmdLabel, this, plugin);
            map.register(plugin.getName(), cmd);
            bukkitCommand = cmd;
        }

        // Set the command's description and usage
        if (!command.description().equalsIgnoreCase("") && cmdLabel.equals(label)) {
            bukkitCommand.setDescription(command.description());
        }
        if (!command.usage().equalsIgnoreCase("") && cmdLabel.equals(label)) {
            bukkitCommand.setUsage(command.usage());
        }

        // Register aliases
        for (String alias : SupremeTags.getInstance().getConfig().getStringList("settings.commands.aliases")) {
            String aliasKey = alias.toLowerCase();

            // Prevent registering aliases multiple times
            if (!commandMap.containsKey(aliasKey)) {
                commandMap.put(aliasKey, new AbstractMap.SimpleEntry<>(m, obj));
                map.register(plugin.getName(), new BukkitCommand(aliasKey, this, plugin));
            }
        }
    }

    public void registerCompleter(String label, Method m, Object obj) {
        if (label.equalsIgnoreCase("tags")) {
            label = SupremeTags.getInstance().getConfig().getString("settings.commands.main-command");
        }

        String cmdLabel = label.replace(".", ",").split(",")[0].toLowerCase();
        org.bukkit.command.Command command = map.getCommand(cmdLabel);

        // Check if command exists, and handle BukkitCommand or PluginCommand
        if (command instanceof BukkitCommand) {
            BukkitCommand bukkitCommand = (BukkitCommand) command;

            // Check if the completer is already set, only add it if it's null
            if (bukkitCommand.completer == null) {
                bukkitCommand.completer = new BukkitCompleter();
            }
            bukkitCommand.completer.addCompleter(label, m, obj);

        } else if (command instanceof PluginCommand) {
            try {
                PluginCommand pluginCommand = (PluginCommand) command;
                Field field = PluginCommand.class.getDeclaredField("completer");
                field.setAccessible(true);
                Object completerField = field.get(pluginCommand);

                // Only set the completer if it's not already set
                if (completerField == null) {
                    BukkitCompleter completer = new BukkitCompleter();
                    completer.addCompleter(label, m, obj);
                    field.set(pluginCommand, completer);
                } else if (completerField instanceof BukkitCompleter) {
                    BukkitCompleter completer = (BukkitCompleter) completerField;
                    completer.addCompleter(label, m, obj);
                } else {
                    System.out.println("Unable to register tab completer for command " + cmdLabel + ". A completer is already set!");
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        } else {
            System.out.println("Command " + cmdLabel + " is not a valid BukkitCommand or PluginCommand!");
        }
    }


    private void defaultCommand(CommandArguments args) {
        args.getSender().sendMessage(args.getLabel() + " is not handled! Oh noes!");
    }

    public Map<String, Entry<Method, Object>> getCommandMap() {
        return commandMap;
    }
}
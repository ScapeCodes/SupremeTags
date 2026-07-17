# Installation

## Requirements

SupremeTags is a Bukkit-style Minecraft plugin.

| Requirement | Notes |
| --- | --- |
| Server software | Paper, Spigot, or compatible forks |
| Minecraft API | Plugin declares API version `1.13` |
| Required dependency | `NBTAPI` |
| Optional dependencies | `ProtocolLib`, `PlaceholderAPI`, `packetevents`, `HeadDatabase`, `VaultAPI`, `Nexo`, `ExcellentEconomy`, `ItemsAdder`, `PlayerPoints`, `SimpleItemGenerator`, `ExecutableItems`, `SCore` |
| Folia | Supported |

!!! warning "Required dependency"
    SupremeTags depends on `NBTAPI`. Install it before starting the server with SupremeTags.

## Install steps

1. Stop your Minecraft server.
2. Download SupremeTags and the required `NBTAPI` dependency.
3. Place both jars in your server `plugins` folder.
4. Install optional dependencies if you need their features.
5. Start the server.
6. Confirm the plugin generated its files under `plugins/SupremeTags`.
7. Run `/tags` in-game.

## Optional dependency usage

| Dependency | Used for |
| --- | --- |
| PlaceholderAPI | Placeholders in chat, scoreboards, tags, conditions, and integrations |
| VaultAPI | Economy costs and permission integration |
| PlayerPoints | Point-based tag costs |
| ExcellentEconomy | Currency-specific tag costs |
| ProtocolLib or packetevents | Packet/chat compatibility features |
| HeadDatabase, ItemsAdder, Nexo, ExecutableItems, SCore | Custom item integrations |

## Updating

1. Stop the server.
2. Back up `plugins/SupremeTags`.
3. Replace the old jar with the new jar.
4. Start the server.
5. Review the console for warnings.
6. Run `/tags reload` after making configuration changes.


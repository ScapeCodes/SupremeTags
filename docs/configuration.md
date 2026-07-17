# Configuration Overview

SupremeTags generates multiple configuration files. Always stop the server or use `/tags reload` after editing files.

## Main files

| File | Purpose |
| --- | --- |
| `config.yml` | Main plugin settings, commands, placeholders, sounds, menus behavior, economy toggles, and personal tags. |
| `messages.yml` | Messages sent to players. |
| `guis.yml` | Menu layouts, items, titles, and lore. |
| `categories.yml` | Category menu definitions. |
| `rarities.yml` | Rarity definitions and filter labels. |
| `tags/*.yml` | Tag definitions. |
| `banned-words.yml` | Banned words for personal or custom tag input. |
| `data.yml` | Plugin data. Avoid manual editing unless instructed. |

## Main settings

| Setting | Description |
| --- | --- |
| `settings.commands.main-command` | Main command name. Default is `tags`. |
| `settings.commands.aliases` | Command aliases. Default includes `tag`. Restart required after changing aliases. |
| `settings.no-permission-menu-action` | Opens the menu instead of sending a no-permission message for blocked subcommands. |
| `settings.bungee-messaging` | Syncs tag data across networked servers. |
| `settings.default-tag` | Starting tag for players, or `none`. |
| `settings.forced-tag` | Prevents players from removing their active tag. |
| `settings.categories` | Enables the category system. |
| `settings.cost-system` | Enables economy-based unlocks. |
| `settings.locked-view` | Lets players see locked tags. |
| `settings.personal-tags.enable` | Enables player-created personal tags. |
| `settings.layout-type` | Menu layout type: `FULL` or `BORDER`. |
| `settings.search-type` | Search UI: `SIGN`, `ANVIL`, or `DIALOG`. |

## Placeholder formatting

The `placeholders` section controls output for:

- `tag`
- `chat`
- `scoreboard`
- `tab`

Each section supports `none-output` and `format`.

Example:

```yaml
placeholders:
  chat:
    none-output: ''
    format: '%tag%'
```


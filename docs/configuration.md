# Configuration Overview

SupremeTags generates multiple configuration files. Always stop the server or use `/tags reload` after editing files.

## Main files

| File | Purpose |
| --- | --- |
| `config.yml` | Main plugin settings, commands, placeholders, sounds, menu behavior, economy toggles, statistics, auto-apply, vouchers, and personal tags. |
| `messages.yml` | Messages sent to players. |
| `guis.yml` | Menu layouts, items, titles, and lore. |
| `categories.yml` | Category menu definitions. |
| `rarities.yml` | Rarity definitions and filter labels. |
| `tags/*.yml` | Tag definitions. |
| `banned-words.yml` | Banned words for personal or custom tag input. |
| `data.yml` | Plugin data. Avoid manual editing unless instructed. |
| `statistics.yml` | Selection totals, active-user counts, and per-player tag history when statistics are enabled. |

## Main settings

| Setting | Description |
| --- | --- |
| `settings.commands.main-command` | Main command name. Default is `tags`. |
| `settings.commands.aliases` | Command aliases. Default includes `tag`. Restart required after changing aliases. |
| `settings.no-permission-menu-action` | Opens the menu instead of sending a no-permission message for blocked subcommands. |
| `settings.proxy-file-syncing` | Syncs tag files across proxy servers when tags are created, edited, deleted, or reloaded. |
| `settings.default-tag` | Starting tag for players, or `none`. |
| `settings.forced-tag` | Prevents players from removing their active tag. |
| `settings.categories` | Enables the category system. |
| `settings.default-category` | Category assigned to newly created tags when no category is provided. |
| `settings.default-tag-file` | File used by `/tags create` when no file location is provided. |
| `settings.cost-system` | Enables economy-based unlocks. |
| `settings.locked-view` | Lets players see locked tags. |
| `settings.personal-tags.enable` | Enables player-created personal tags. |
| `settings.personal-tags.use-creation-dialogs` | Uses Paper dialog creation on supported 1.21.8+ servers. |
| `settings.personal-tags.credits` | Enables and configures Tag Credits, starting balance, creation cost, and help text. |
| `settings.personal-tags.create-requirements` | Controls minimum server age and playtime for creating personal tags. |
| `settings.layout-type` | Menu layout type: `FULL` or `BORDER`. |
| `settings.search-type` | Search UI: `SIGN`, `ANVIL`, or `DIALOG`. |
| `settings.tag-vouchers` | Enables withdrawing eligible tags into voucher items. |
| `settings.voucher-redeem-permission` | Requires `supremetags.voucher.<identifier>` before a voucher can be redeemed. |
| `settings.voucher-redeem-confirmation` | Shows a confirmation menu before redeeming vouchers. |
| `settings.tag-purchase-confirmation` | Shows a confirmation menu before buying economy tags. |
| `settings.tag-select-confirmation` | Shows a confirmation menu before selecting tags. |
| `settings.auto-apply` | Automatically applies selected tags to display names and tab names. |
| `settings.only-show-player-access-tags` | Hides tags the player cannot access. |
| `statistics.enabled` | Tracks tag selections, unique users, active users, and per-player usage. |

## Placeholder formatting

The `placeholders` section controls output for:

- `tag`
- `chat`
- `scoreboard`
- `tab`

Each section supports `none-output`, `format`, and `output`. Output can be `legacy`, `minimessage`, `minimessage-text`, `raw`, or `plain`.

Example:

```yaml
placeholders:
  chat:
    none-output: ''
    format: '%tag%'
    output: 'legacy'
```

## Supported integrations

SupremeTags detects optional plugins and APIs when they are present. Current hooks include PlaceholderAPI, Vault/VaultUnlockedAPI, PlayerPoints, ExcellentEconomy, ProtocolLib, PacketEvents, HeadDatabase, ItemsAdder, Nexo, Oraxen, CraftEngine, SimpleItemGenerator, ExecutableItems/SCore, EssentialsX chat events, MySQL/MariaDB, SQLite, H2, Redis/Jedis, and Folia scheduling.


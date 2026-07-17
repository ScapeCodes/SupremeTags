# Commands

The main command is `/tags`. The default alias is `/tag`. The personal tags command is `/mytags`.

## Player commands

| Command | Permission | Description |
| --- | --- | --- |
| `/tags` | `supremetags.player` | Opens the main tag menu. |
| `/tag` | `supremetags.player` | Alias for `/tags`. |
| `/mytags` | `supremetags.mytags` | Opens the personal tags menu. |
| `/tags search` | `supremetags.search` | Opens the tag search interface. |
| `/tags favourites` | Access to `/tags` | Opens the favourites menu. |
| `/tags view` | `supremetags.view` | Opens your tag showcase view. |
| `/tags view <player>` | `supremetags.view.other` | Opens another player's showcase view. |
| `/tags withdraw <tag>` | `supremetags.withdraw` | Withdraws a tag as a voucher when enabled. |
| `/tags reset [-s]` | Access to `/tags` | Resets your active tag. Use `-s` for silent reset where supported. |
| `/tags set <tag> [player] [-s]` | Tag permission, plus `supremetags.set.other` for other players | Selects a tag for yourself or another player. |

## Admin commands

Most admin commands require:

```text
supremetags.admin
```

| Command | Description |
| --- | --- |
| `/tags help` | Shows help information. |
| `/tags reload` | Reloads SupremeTags configuration and data. |
| `/tags debug` | Prints debug information. |
| `/tags config` | Opens the in-game config editor. |
| `/tags editor` | Opens the tag editor selector. |
| `/tags list` | Shows loaded tag and category counts. |
| `/tags merge` | Imports tags from supported plugins. |
| `/tags merge-free` | Imports from the free SupremeTags format. |
| `/tags create <name> <tag> [fileLocation]` | Creates a new tag. |
| `/tags delete <tag>` | Deletes a tag. |
| `/tags edit <tag> <option> <value>` | Edits a tag option. |
| `/tags removetagp <player> <tag>` | Removes a tag permission from a player. |
| `/tags givevoucher <player> <tag>` | Gives a player a tag voucher. |

## Custom tag commands

| Command | Permission | Description |
| --- | --- | --- |
| `/tags setcustomtag <player> <tag-style>` | `supremetags.setcustomtag` | Sets a player's custom tag. |
| `/tags resetcustomtag <player>` | `supremetags.resetcustomtag` | Resets a player's custom tag. |

## Editable tag options

`/tags edit <tag> <option> <value>` supports these options:

| Option | Example |
| --- | --- |
| `tag` | `/tags edit vip tag &a[VIP]` |
| `permission` | `/tags edit vip permission supremetags.tag.vip` |
| `category` | `/tags edit vip category default` |
| `cost` | `/tags edit vip cost 250` |
| `withdrawable` | `/tags edit vip withdrawable true` |
| `rarity` | `/tags edit vip rarity rare` |


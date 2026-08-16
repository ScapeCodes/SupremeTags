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
| `/tags credits` | Access to `/tags` | Shows your Tag Credits balance. |
| `/tags credits <player>` | Access to `/tags` | Shows another player's Tag Credits balance. |
| `/tags reset [-s]` | `supremetags.reset` | Resets your active tag. Use `-s` for silent reset. |
| `/tags set <tag> [-s]` | `supremetags.set` and tag access | Selects one of your unlocked tags. Use `-s` to suppress selection messages. |

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
| `/tags stats` | Shows total tracked tag selections and the top tag. |
| `/tags stats <tag>` | Shows selection, unique-user, active-user, rank, first-selected, and last-selected statistics for a tag. |
| `/tags merge` | Imports tags from supported plugins. |
| `/tags merge-free` | Imports from the free SupremeTags format. |
| `/tags create <name> <tag> [fileLocation]` | Creates a new tag. |
| `/tags delete <tag>` | Deletes a tag. |
| `/tags move <tag> <folder/file.yml>` | Moves a file-backed tag to another file inside the tags folder. |
| `/tags edit <tag> <option> <value>` | Edits a tag option. |
| `/tags removetagp <player> <tag>` | Removes a tag permission from a player. |
| `/tags givevoucher <player> <tag>` | Gives a player a tag voucher. |
| `/tags reset <player> [-s]` | Resets another player's active tag. |
| `/tags set <tag> <player> [-s]` | Sets another player's active tag. |
| `/tags seteveryone <tag>` | Sets every stored player's active tag. |
| `/tags reseteveryone` | Resets every stored player's active tag. |
| `/tags credits <give\|take\|set> <player> <amount>` | Manages personal Tag Credits. |

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

## Silent arguments

Several player-facing commands accept `-s` as the last argument. When supported, this performs the action without sending the usual success message to the affected player.


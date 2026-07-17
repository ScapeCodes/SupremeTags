# Importing

SupremeTags includes importer support for moving tags from other tag plugins.

## Commands

| Command | Description |
| --- | --- |
| `/tags merge` | Imports from supported tag plugins. |
| `/tags merge-free` | Imports from the free SupremeTags format. |

Both commands are administrator actions and require:

```text
supremetags.admin
```

## Supported importers

The plugin contains importers for:

- AlonsoTags
- DeluxeTags
- EternalTags
- Free SupremeTags

## Recommended migration process

1. Stop the server.
2. Back up the old plugin folder.
3. Back up `plugins/SupremeTags` if it already exists.
4. Start the server with both plugins installed if the importer requires the old plugin data to be present.
5. Run `/tags merge` or `/tags merge-free`.
6. Check generated tag files.
7. Test `/tags` in-game.
8. Remove the old plugin only after confirming the import worked.

!!! tip
    Always test imports on a staging server before using them on a live server.


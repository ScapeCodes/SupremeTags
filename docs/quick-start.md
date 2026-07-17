# Quick Start

This page gets you from a clean install to a working tag menu.

## 1. Give yourself admin access

Add this permission to your administrator group:

```text
supremetags.admin
```

## 2. Open the menu

Use:

```text
/tags
```

Players normally need:

```text
supremetags.player
```

## 3. Give a player access to a tag

Tags usually use permission nodes like:

```text
supremetags.tag.example
```

For example, the default hex support tag uses:

```text
supremetags.tag.hex
```

## 4. Add the selected tag to chat

If you use PlaceholderAPI, add this placeholder to your chat format:

```text
%supremetags_chattag%
```

For a raw selected tag display, use:

```text
%supremetags_tag%
```

## 5. Reload after edits

After changing config files, use:

```text
/tags reload
```

## Common first settings

| Setting | Recommended starting value |
| --- | --- |
| `settings.categories` | `false` until you need category menus |
| `settings.cost-system` | `false` unless using economy unlocks |
| `settings.locked-view` | `true` to let players preview locked tags |
| `settings.tag-vouchers` | `true` if you want voucher withdrawal |
| `settings.personal-tags.enable` | `true` if players can create personal tags |


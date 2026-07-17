# GUIs

Menu layouts, titles, item materials, display names, and lore are configured in `guis.yml`.

## Common menu customization

You can usually customize:

- Inventory titles.
- Item slots.
- Materials.
- Custom model data.
- Glow effects.
- Lore lines.
- Locked and unlocked item states.
- Navigation buttons.
- Filter buttons.

## Layout types

The main layout type is configured in `config.yml`:

```yaml
settings:
  layout-type: 'BORDER'
```

Supported layout types:

| Layout | Description |
| --- | --- |
| `FULL` | Uses up to 36 tag slots. |
| `BORDER` | Uses up to 28 tag slots with a border layout. |

## Recommended workflow

1. Back up `guis.yml`.
2. Change one menu section at a time.
3. Run `/tags reload`.
4. Open the changed menu in-game.
5. Check console for material or sound errors.

## Placeholders in menu text

Common placeholders include:

| Placeholder | Meaning |
| --- | --- |
| `%tag%` | Tag display text. |
| `%tags_amount%` | Amount of tags in a category or menu context. |
| `%identifier%` | Tag identifier. |
| `%player%` | Player name where supported. |


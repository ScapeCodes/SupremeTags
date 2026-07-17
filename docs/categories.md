# Categories

Categories group tags into separate menus. Enable categories with:

```yaml
settings:
  categories: true
```

## Category example

```yaml
categories:
  default:
    title: '&8Menu > &lDefault Tags'
    material: 'NAME_TAG'
    custom-model-data: 0
    cost-category: false
    glow: false
    id_display: '&7&lDefault Tags'
    slot: 11
    lore:
      - '&8&m-----------------------------'
      - ''
      - '&7Default tags: &7%tags_amount%'
      - ''
      - '&8&m-----------------------------'
    filter-labels:
      selected: '&7&lDEFAULT'
      unselected: '&7Default'
    permission: 'supremetags.category.default'
    permission-see-category: false
    no-permission-message: '%prefix% &cYou have no permission to open this category!'
```

## Category options

| Option | Description |
| --- | --- |
| `title` | Inventory title for the category menu. |
| `material` | Item material shown in the main categories menu. |
| `custom-model-data` | Custom model data for the category item. |
| `cost-category` | Marks a category as cost-related. |
| `glow` | Adds item glow. |
| `id_display` | Display name for the category item. |
| `slot` | Slot in the categories menu. |
| `lore` | Lore displayed on the category item. |
| `filter-labels` | Labels used when filtering categories. |
| `permission` | Permission required to open the category. |
| `permission-see-category` | Hides category from players without permission when true. |
| `no-permission-message` | Message sent when access is denied. |


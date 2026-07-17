# Tags

Tags are defined in files under `tags/`. A tag has an identifier, display text, permission, category, rarity, item settings, voucher settings, and optional economy settings.

## Basic tag example

```yaml
tags:
  example:
    tag:
      - '&8[&6Example&8]'
    permission: supremetags.tag.example
    groups: []
    description:
      - '&7An example tag.'
    custom-placeholders: {}
    category: default
    order: 1
    withdrawable: true
    displayname: '&7Tag: %tag%'
    custom-model-data: 0
    display-item: NAME_TAG
    variants: []
    effects: []
    voucher-item:
      material: NAME_TAG
      displayname: '&8[&6Example&8] &f&lVoucher'
      custom-model-data: 0
      glow: true
      lore:
        - '&7&m-----------------------------'
        - '&eClick to equip!'
        - '&7&m-----------------------------'
    rarity: common
    economy:
      enabled: false
      type: VAULT
      amount: 200
```

## Tag options

| Option | Description |
| --- | --- |
| `tag` | One or more displayed tag frames. Multiple lines can animate. |
| `permission` | Permission required to use the tag. |
| `groups` | Permission groups that should receive access more easily. |
| `description` | Lore/description text. |
| `custom-placeholders` | Values for custom tag placeholders. |
| `category` | Category identifier. |
| `order` | Sort order in menus. |
| `withdrawable` | Whether the tag can be withdrawn as a voucher. |
| `displayname` | Menu item display name. |
| `custom-model-data` | Custom model data for resource packs. |
| `display-item` | Bukkit material used in menus. |
| `variants` | Alternate versions of the tag. |
| `rarity` | Rarity identifier. |
| `economy` | Per-tag economy configuration. |

## Economy types

Available economy types include:

- `VAULT`
- `PLAYERPOINTS`
- `EXP_LEVELS`
- `EXCELLENTECONOMY-currencyhere`
- `CUSTOM`

Custom economy example:

```yaml
economy:
  enabled: true
  type: CUSTOM
  take-cmd: 'eco take %player% %amount%'
  condition: '%vault_eco_balance% >= %amount%'
  amount: 200
```

## Variants

Variants are sub-tags attached to a parent tag.

```yaml
variants:
  blue-example:
    enabled: true
    tag:
      - '&8[&bExample&8]'
    permission: supremetags.tag.example.blue
```


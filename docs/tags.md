# Tags

Tags are defined in files under `tags/`. A tag has an identifier, display text, permission, category, rarity, item settings, voucher settings, optional economy settings, variants, effects, custom placeholders, and optional unlock requirements.

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
    requirements:
      enabled: false
      persist-unlock: false
      mode: all
      list: {}
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
| `effects` | Potion effects applied while the tag is active. |
| `voucher-item` | Item created when this tag is withdrawn as a voucher. |
| `rarity` | Rarity identifier. |
| `economy` | Per-tag economy configuration. |
| `requirements` | Optional requirements that must pass before the tag can be selected or persisted as unlocked. |

## Economy types

Available economy types include:

- `VAULT`
- `PLAYERPOINTS`
- `EXP_LEVEL`
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

Variants can define their own permission and optional locked/unlocked item appearance. The selected variant still belongs to the parent tag for metadata such as category, description, economy, and rarity.

## Requirements

Requirements can block a tag until a player meets every rule (`all`) or at least one rule (`any`). When `persist-unlock` is true, a player who passes once is stored as unlocked.

```yaml
requirements:
  enabled: true
  persist-unlock: true
  mode: all
  list:
    playtime:
      type: placeholder
      placeholder: '%statistic_time_played%'
      operator: '>='
      value: 1728000
      lore-display: '&f- &724 hours of playtime'
      message: ''
```

Supported requirement types are:

| Type | Description |
| --- | --- |
| `permission` or `perm` | Requires a permission node, or `none` to always pass. |
| `placeholder` or `papi` | Compares a PlaceholderAPI output with a configured value. |
| `economy` or `balance` | Checks whether the player has enough of the configured economy type. |
| `owns-tag`, `owns_tag`, or `tag` | Requires the player to own or actively use another tag. |

Supported comparison operators include `>=`, `<=`, `>`, `<`, `==`, `=`, `equals`, `!=`, `not`, `contains`, `starts-with`, `starts_with`, `ends-with`, and `ends_with`.


# Rarities

Rarities let you label and sort tags by quality or availability.

## Default rarities

| Identifier | Order | Display name |
| --- | --- | --- |
| `common` | `1` | `&7Common` |
| `uncommon` | `2` | `&7UnCommon` |
| `rare` | `3` | `&7Rare` |
| `legendary` | `4` | `&7Legendary` |

## Rarity example

```yaml
rarities:
  mythical:
    enable: true
    order: 5
    displayname: '&dMythical'
    filter-labels:
      selected: '&d&lMYTHICAL'
      unselected: '&7Mythical'
```

## Assigning a rarity to a tag

```yaml
tags:
  example:
    rarity: mythical
```

Use `/tags reload` after editing rarities.


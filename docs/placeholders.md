# Placeholders

SupremeTags registers the PlaceholderAPI identifier:

```text
supremetags
```

## Basic placeholders

| Placeholder | Description |
| --- | --- |
| `%supremetags_tag%` | Player's selected tag using tag formatting. |
| `%supremetags_chattag%` | Player's selected tag using chat formatting. |
| `%supremetags_tabtag%` | Player's selected tag using tab formatting. |
| `%supremetags_scoreboardtag%` | Player's selected tag using scoreboard formatting. |
| `%supremetags_hastag_selected%` | `true` if the player has an active selected tag. |
| `%supremetags_hastag_tags%` | `true` if the player has access to at least one tag. |
| `%supremetags_tags_amount%` | Total number of loaded tags. |
| `%supremetags_tags_total%` | Total number of loaded tags. |
| `%supremetags_player_track_unlocked%` | Number of tags the player can access. |

## Selected tag metadata

These return information about the player's selected tag.

| Placeholder | Description |
| --- | --- |
| `%supremetags_identifier%` | Selected tag identifier. |
| `%supremetags_description%` | Selected tag description. |
| `%supremetags_permission%` | Selected tag permission. |
| `%supremetags_rarity%` | Selected tag rarity display name. |
| `%supremetags_rarity_raw%` | Selected tag rarity identifier. |
| `%supremetags_category%` | Selected tag category. |
| `%supremetags_cost%` | Selected tag cost amount. |
| `%supremetags_cost_formatted%` | Selected tag cost formatted with currency prefix. |
| `%supremetags_cost_formatted_raw%` | Selected tag cost formatted without currency prefix. |

## Dynamic placeholders

| Placeholder | Description |
| --- | --- |
| `%supremetags_has_access_<identifier>%` | `true` if the player has access to the tag. |
| `%supremetags_track_unlocked_<identifier>%` | Unlock count for a tag identifier. |
| `%supremetags_tag_custom-placeholder_<placeholder>%` | Reads a custom placeholder value from the selected tag. |

## Chat format example

```text
%supremetags_chattag% %player_name%: %message%
```


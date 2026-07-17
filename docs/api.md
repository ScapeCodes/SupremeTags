# Developer API

SupremeTags exposes a small API for other plugins.

## Accessing the API

Use the plugin instance or your preferred dependency handling to access the SupremeTags API class.

```java
SupremeTagsAPI api = new SupremeTagsAPI();
```

## Methods

| Method | Description |
| --- | --- |
| `getTag(String identifier)` | Returns a tag by identifier. |
| `getPlayerTag(UUID uuid)` | Returns the player's active tag. |
| `hasTag(UUID uuid)` | Returns whether the player has a selected tag. |
| `getAllTags()` | Returns all registered tags. |

## Example

```java
SupremeTagsAPI api = new SupremeTagsAPI();

if (api.hasTag(player.getUniqueId())) {
    Tag tag = api.getPlayerTag(player.getUniqueId());
    player.sendMessage("Your tag is " + tag.getIdentifier());
}
```

## Events

SupremeTags includes events for common tag actions:

| Event | Description |
| --- | --- |
| `TagAssignEvent` | Called when a tag is assigned. |
| `TagBuyEvent` | Called when a tag is bought. |
| `TagResetEvent` | Called when a tag is reset. |

## Maven/Gradle usage

If you publish SupremeTags to a Maven repository, add it as a `compileOnly` dependency in your plugin. If not, add the jar locally during development and mark SupremeTags as a soft dependency in your `plugin.yml`.

```yaml
softdepend:
  - SupremeTags
```


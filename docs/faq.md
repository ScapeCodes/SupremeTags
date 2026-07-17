# FAQ

## Why does `/tags` open instead of showing a no-permission message?

Check this setting:

```yaml
settings:
  no-permission-menu-action: true
```

When enabled, blocked subcommands can open the main menu instead of sending a permission error.

## Why are tags not showing in chat?

Make sure your chat plugin uses one of the SupremeTags placeholders, such as:

```text
%supremetags_chattag%
```

Also make sure PlaceholderAPI is installed if your chat plugin depends on PlaceholderAPI placeholders.

## Why can players not use a tag?

Check the tag's `permission` value and give that permission to the player or group.

Example:

```text
supremetags.tag.hex
```

## Do command aliases need a reload?

No. The default configuration notes that adding or removing command aliases requires a full server restart.

## How do I enable categories?

Set:

```yaml
settings:
  categories: true
```

Then configure categories in `categories.yml` and assign each tag a category.

## How do I preview the docs locally?

Install the docs dependencies:

```bash
python -m pip install -r requirements-docs.txt
```

Run the local server:

```bash
mkdocs serve
```

Then open:

```text
http://127.0.0.1:8000
```


# Permissions

## Core permissions

| Permission | Description |
| --- | --- |
| `supremetags.admin` | Access to administrator commands and editors. |
| `supremetags.player` | Access to the main tag menu. |
| `supremetags.mytags` | Access to `/mytags`. |
| `supremetags.search` | Access to `/tags search`. |
| `supremetags.withdraw` | Access to withdraw tags as vouchers. |
| `supremetags.view` | Access to view your tag showcase. |
| `supremetags.view.other` | Access to view another player's tag showcase. |
| `supremetags.reset` | Allows a player to reset their own active tag. |
| `supremetags.reset.other` | Allows resetting another player's active tag. |
| `supremetags.set` | Allows a player to set their own active tag by command. |
| `supremetags.set.other` | Allows setting another player's active tag. |

## Dynamic permissions

| Permission pattern | Description |
| --- | --- |
| `supremetags.tag.<identifier>` | Access to a specific tag. Each tag can define its own permission. |
| `supremetags.category.<category>` | Access to a category when category permissions are enabled. |
| `supremetags.mytags.limit.<group>` | Personal tag limit group. |
| `supremetags.mytags.color` | Allows personal tag input to keep color formatting. Without it, color formatting is stripped. |
| `supremetags.voucher.<identifier>` | Voucher redeem permission when voucher redeem permission is enabled. |

## Extra command permissions

| Permission | Description |
| --- | --- |
| `supremetags.setcustomtag` | Allows setting custom tags for players. |
| `supremetags.resetcustomtag` | Allows resetting custom tags for players. |

## Bypass and admin behavior

`supremetags.admin` is used by the command handlers for reload, debug, config editor, tag editor, list, statistics, merge, create, delete, move, vouchers, mass set/reset, and Tag Credit management. Operators and administrators also bypass some personal tag creation checks and limits.

## Example LuckPerms setup

```text
/lp group default permission set supremetags.player true
/lp group default permission set supremetags.search true
/lp group default permission set supremetags.tag.hex true
/lp group admin permission set supremetags.admin true
```


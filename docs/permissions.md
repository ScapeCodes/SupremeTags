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

## Dynamic permissions

| Permission pattern | Description |
| --- | --- |
| `supremetags.tag.<identifier>` | Access to a specific tag. Each tag can define its own permission. |
| `supremetags.category.<category>` | Access to a category when category permissions are enabled. |
| `supremetags.mytags.limit.<group>` | Personal tag limit group. |
| `supremetags.voucher.<identifier>` | Voucher redeem permission when voucher redeem permission is enabled. |

## Extra command permissions

| Permission | Description |
| --- | --- |
| `supremetags.set.other` | Allows setting a tag for another player. |
| `supremetags.setcustomtag` | Allows setting custom tags for players. |
| `supremetags.resetcustomtag` | Allows resetting custom tags for players. |

## Example LuckPerms setup

```text
/lp group default permission set supremetags.player true
/lp group default permission set supremetags.search true
/lp group default permission set supremetags.tag.hex true
/lp group admin permission set supremetags.admin true
```


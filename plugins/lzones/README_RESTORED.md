# LZones restored source

Recovered from `lzones-2.1.0.jar` with CFR 0.152.

Original artifact:
- File: `C:\Users\lyucaNova\Downloads\Telegram Desktop\lzones-2.1.0.jar`
- SHA-256: `DF5A94617EA04825D008985439E2390E2980A085C3602969C0CDC0228A122B14`

Project type:
- Maven Java 21 project
- Paper API `1.20.1-R0.1-SNAPSHOT`
- Main plugin class: `me.ruslan.protectionstones.ProtectionStonesPlugin`

Build:

```bat
mvn package
```

The restored project was verified with Apache Maven 3.9.9 and Java 21.

Post-decompile cleanup already applied:
- Restored lost generic types in `MessageService`.
- Removed invalid `(Object)` casts from `PersistentDataContainer#set` calls in `ItemService`.
- Restored typed region map loading in `RegionService`.

Custom changes:
- Default region height increased to `height-below: 32` and `height-above: 48`.
- TNT levels 1-4 have reduced `power`; level 5 has reduced `power`, `explosion-power`, and `explosion-radius`.
- Regions now support trusted members, configurable with `general.max-members-per-region` (default `20`).
- New owner commands: `/lr add <player> [region]`, `/lr removeplayer <player> [region]`, `/lr members [region]`.
- Existing server configs are migrated softly: old default height/TNT values are replaced with the new defaults, while custom values are left untouched.
- Siege bomb core damage now checks the actual 1x1x1 core block hitbox, not the whole protected region.
- Siege bombs can no longer be placed directly inside regions where the owner has no build access.
- Falling siege bombs are allowed to land and explode inside regions after being placed outside.

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
- Added level 6 nuclear bomb (`/lr give bomb 6`) with 24-kiloton yield, large charge animation without attraction, stronger expanding knockback shockwave, mushroom cloud, protected-region-safe vertical crater carving, 180-block blast reach, 360-second radiation expanding from 100 to 200 blocks, exposure buildup, and damage after the configured threshold.
- Added nuclear TNT profiles for levels 6-10: Fat Man+ (24 kt), Castle Bravo (15 Mt), Tsar Bomba (50 Mt), Tsar Bomba MAX (100 Mt), and forbidden World Eater (1000 Mt).
- Level 10 TNT now requires a second placement confirmation within 15 seconds and starts a staged world-eater effect after detonation.
- Radiation severity now scales by distance to the blast center and applies nausea plus darkness while players remain contaminated.
- Added radiation armor kit via `/lr give radkit [player]`; a full set still lets exposure build up, but limits it to `explosive.nuclear.radiation-suit-exposure-per-10-seconds` (default `0.5`).
- Radiation nausea and blindness now start only after exposure reaches 70% of the configured damage threshold.
- Added `/tntshop` for buying TNT bombs level 1-5 through Vault economy with configurable prices.

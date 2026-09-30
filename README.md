## Welcome to Schematica Plus!

Current release: **Beta 1.0** (`1.0.0-beta.1`).

Maintained by HackerRouter, based on Lunatrius's Schematica and the GTNH fork.
The mod ID is `schematica_plus`. Installing it alongside original or GTNH
Schematica stops loading with an incompatibility message; replace the old jar.
On first launch, an existing `Schematica.cfg` is copied to `schematica_plus.cfg`
if the new configuration does not already exist.

### Usage:

When holding the tool item,
- Hold `LCONTROL` and scroll to switch current tool mode.
- Use right click and left click to move, place and select.
- Use "Execute" key bind to paste, etc.

Default tool item is `minecraft:stick`.

**See more detailed info on [Curseforge](https://www.curseforge.com/minecraft/mc-mods/schematica-plus) if you are new to the mod.**

### Keybinds:

| Key | Action |
|-----|--------|
| `M` | Open Schematica Plus main menu |
| `N` | Open Save Schematic GUI |
| `Enter` | Execute current tool action (save, paste, fill, delete, replace) |
| `LCTRL + Scroll` | Cycle tool mode (while holding tool item) |
| Left Click | Set point A / Pick primary block (while holding tool item) |
| Right Click | Set point B / Pick secondary block / Place schematic (while holding tool item) |

The main menu links to loading, area selection/saving, instances, controls,
materials and configuration. Direct loading and direct controls have optional,
unbound shortcuts in Controls. The new file browser supports subfolders, name
search and all three input formats; selecting a row does not load it until you
press Load, double-click, or press Enter with the list focused.

The save page supports output-folder selection, `.schematic`/`.schemplus` format
selection and confirmation before replacing an existing file. A queued save is
not yet complete; the final filename and result appear in chat. See the
[UI port notes](compatibility/ui-port.md) for the current scope.

---

If you are playing on GTNH-2.8.4, you can simply replace `Schematica-1.12.6-GTNH.jar` with it.

GTNH compatibility work targets 2.8.4 and 2.9.0-RC-1. See the
[versioned coverage and remaining limitations](compatibility/README.md) before testing.

LunatriusCore is no longer required. Schematica Plus includes the utility classes it uses
under its own internal package, so an external LunatriusCore can still be installed for
other mods. See [third-party notices](THIRD_PARTY_NOTICES.md) for attribution.

For bounded memory use, schematics are limited to 16,777,216 blocks, 32,767 blocks per
axis, and 1,048,576 X/Y array rows. NBT reads have a 128 MiB allocation budget and a
maximum nesting depth of 64. World edits and captures must stay within Y=0..255.
Only one save per player and four queued saves globally are accepted at a time.

Both `.schematic` and `.schemplus` can be loaded regardless of the save-format
preference. Standard files use 12-bit block IDs. Captures containing IDs above 4095
automatically save as `.schemplus`, with the actual filename shown in chat. An
automatic format change adds a numeric suffix if its new filename already exists.
Singleplayer captures use the integrated server's world. Remote client captures
include only synchronized data; GregTech pipe connections are preserved, but
unsynchronized inventories and machine data still require a server-side save.

With NBT saving enabled, captures also retain standard S35 tile update data for
mods that implement `onDataPacket`. Previews restore it after all tile entities
are bound to the schematic world. Original NBT is retained separately so partial
client updates do not erase inventories when saving again or pasting. Older files
cannot recover visual state they never recorded and should be captured again.

Animated textures and renderers using world time/frame interpolation continue to
animate. Machine logic is not ticked inside previews. Private synchronization,
private animation counters, neighbors outside the selection, biome-dependent
textures and arbitrary mod-specific rotations still require adapters. This does
not guarantee identical rendering for every mod; see [verification](TESTING.md).

The printer can place source fluids using registered `ItemBucket` containers,
GregTech volumetric flasks, IC2 universal fluid cells, Forestry and Railcraft buckets. In
survival, a matching container with at least 1000 mB must be in your inventory,
and a neighboring block face must be visible and within reach. The original item
handles consumption and container returns. Flasks/cells require a solid neighbor
without a tile entity to avoid tank interactions. Flowing fluid is left to the
game's simulation. Forestry cans/capsules, drinks and storage-only containers are
not selected because they do not implement world fluid placement.

Integrated-server edits require creative mode and command permission. They run in
bounded batches on server ticks; pressing Execute again cancels the remaining work
and retains edits already made. Multiplayer command fallback is rate-limited and
reports commands sent, not server-confirmed changes. The 1.7.10 chat limit prevents
general NBT transfer: pasting schematics with block/entity NBT through that fallback
is rejected before sending commands. Disable those options for a block-only paste.

![play GTNH in multiplayer](temp.png)

---

I'm currently working on other projects, so development is on hold for now.

If you’d like to contribute to this enhanced mod, feel free to go ahead.

### Changes from GTNH-ver:
- Added most functions from [Litematica](https://github.com/maruohon/litematica/), made it more user-friendly.

For example:
- Loading multiple schematic instances.
- Pasting schematics (including NBT tags, block states, entities, etc.) directly into the world when having permissions.
- Storing blocks and entities with NBT tags.
- Different edit modes, making it a lite version of World Edit (jk).
- Supports `.litematic` (I have zero idea on why I decided to implement this, but it does work really well).
- Modern GUI from [Litematica](https://github.com/maruohon/litematica/) (Still working on this).

---

### Changes from Original:
- Store Coordinates & rotation of schematics per world/server. No more re-entering coordinates for large builds!
- Fix heavy lag when having lotr armor stands/weapon racks in loaded schematic
- Updated Chinese translation

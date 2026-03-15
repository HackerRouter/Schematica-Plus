## Welcome to Schematica Plus!

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
| `M` | Open Schematic Management GUI |
| `N` | Open Save Schematic GUI |
| `Enter` | Execute current tool action (paste, fill, delete, replace) |
| `LCTRL + Scroll` | Cycle tool mode (while holding tool item) |
| Left Click | Set point A / Pick primary block (while holding tool item) |
| Right Click | Set point B / Pick secondary block / Place schematic (while holding tool item) |

---

If you are playing on GTNH-2.8.4, you can simply replace `Schematica-1.12.6-GTNH.jar` with it.

Otherwise, you would need to also install **[LunatriusCore](https://github.com/GTNewHorizons/LunatriusCore/releases)**(>= 1.2.1-GTNH).

![play GTNH in multiplayer](temp.png)

---

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

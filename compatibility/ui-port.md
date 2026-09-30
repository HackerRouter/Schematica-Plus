# Litematica UI adaptation, phase 1

Target: Minecraft 1.7.10, Forge 10.13.4.1614, MCP stable_12, Java 8 bytecode.
The UI is internal to Schematica Plus; it does not register a MaLiLib mod or expose
binary compatibility with modern MaLiLib.

## Source investigation (2026-09-30)

| Project | Relevant findings | Decision |
| --- | --- | --- |
| [ModularUI](https://github.com/GTNewHorizons/ModularUI) | Existing 1.7.10 library; README lists GTNHMixins; LGPL-3.0 | Useful precedent, no new dependency |
| [ModularUI2](https://github.com/GTNewHorizons/ModularUI2/tree/40fdbdabab7767999b84fda0dc5e9ff47e7edb0b) | Widget tree, focus and capture, separate screen wrapper; GTNHLib/NEI dependencies; LGPL-3.0 | Adopt the separation of responsibilities, implement locally |
| [OpenModsLib](https://github.com/GTNewHorizons/OpenModsLib/tree/e5e8177f2dd2297d5aea6bb733955a67ffa63c95) | BaseComposite demonstrates small reusable controls and snapshots when callbacks can mutate children | Keep the component layer independent of game/world data |
| [MaLiLib 26.1.2-0.28.8](https://github.com/sakura-ryoko/malilib/tree/d194eb689d60f13ba39df4ab525dca64b1cf4478) | GuiBase, ButtonGeneric, list/search controls provide the target interaction model; modern Screen/GuiGraphicsExtractor/GLFW cannot be used in 1.7.10 | Adapt screen lifecycle, drawing and input at the boundary |

The supplied Litematica 26.1.2-0.27.8 requires MaLiLib >=0.28.8, while the supplied
MaLiLib snapshot was 0.28.2. A separate checkout of tag 26.1.2-0.28.8 was obtained
for this work. The supplied directories were not modified.

ModularUI2's Stencil supports transformed/holographic interfaces. Our axis-aligned
screens need only nested scissors, avoiding a stencil-buffer requirement. Its
render helpers also reinforce the need to restore texture/blend state after draws.
OpenModsLib's broad child event dispatch is not reused: only the topmost hit and
the focused control receive input here, and a modal blocks the underlying layer.

This phase independently implements the adapter and primitives. No upstream code
or textures are bundled. A later source/asset port must preserve the relevant
upstream license and attribution alongside those files.

## Internal contract

- `UiBounds`, `UiInput` and `UiListModel` are game-independent and covered by tests.
- `UiPanel` owns its children. Coordinates are absolute, in vanilla scaled GUI
  pixels, matching MaLiLib's explicit positioning. Every ancestor clips rendering
  and input. Add order is draw order; hit tests run backwards.
- Focus is unique. Tab traverses visible/enabled controls; mouse capture survives
  leaving a control and is cancelled on removal, disable, screen close or modal
  opening. Modal layers restore earlier focus when dismissed.
- `UiDraw` is the rendering boundary. The 1.7.10 implementation owns GL state and
  converts scissors using ScaledResolution's actual scale, including odd window
  dimensions. Controls never use framebuffer mouse coordinates.
- Layout updates existing controls on resize; typing, selection and scroll state
  belong to the controls/models, not the layout pass.
- Lists filter with Locale.ROOT, retain selection by entry, clamp scrolling and
  draw only visible rows. An entry filtered out cannot be activated with Enter.

## Visual direction

Use Minecraft's font, compact 20-pixel controls, 4-pixel gaps, left-aligned list
content and a title/navigation row. Palette: panel #202020, control #383838,
border #808080, text #E0E0E0, focus #E0C060, selected row #405568. Focus, hover,
disabled and selected states remain distinct. Text and tooltips are clipped or
wrapped to fit the current scaled viewport.

Full main-menu/file/placement/config screens, hotkey chords, subregion data and
schematic backend changes belong to later phases.

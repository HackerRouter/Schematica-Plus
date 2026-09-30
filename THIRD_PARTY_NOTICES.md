# Third-party code

Schematica Plus includes vector utilities, GUI controls and inventory helpers from
LunatriusCore 1.2.1-GTNH, revision `48cf796e6be47d3f3eb57cf0bc0febbe7b53cc82`.
Upstream: https://github.com/GTNewHorizons/LunatriusCore

Copyright (c) 2014 Jadran "Lunatrius" Kotnik. Licensed under the MIT License; the full
license is included in `src/main/resources/META-INF/LICENSE-LunatriusCore.txt` and in
the distributed jar. The copied classes have been relocated to
`com.github.lunatrius.schematica.internal.lunatriuscore` to coexist with external
LunatriusCore installations. Its mod entry point and global GUI hooks are not bundled.

## Litematica and MaLiLib UI

The UI port includes material from Litematica 26.1.2-0.27.8 and MaLiLib
26.1.2-0.28.8, by masa, Sakura-Ryoko and their contributors, under LGPL version 3.
Upstream projects: https://github.com/maruohon/litematica and
https://github.com/maruohon/malilib. The MaLiLib reference checkout is
https://github.com/sakura-ryoko/malilib/tree/d194eb689d60f13ba39df4ab525dca64b1cf4478.
Litematica was supplied as a local source snapshot with the version above.

- `assets/schematica_plus/textures/gui/litematica_widgets.png` is the unmodified
  Litematica `assets/litematica/textures/gui/gui_widgets.png` atlas.
- `assets/schematica_plus_litematica/lang/*.lang` contains selected upstream
  labels converted from JSON to the 1.7.10 language format.
- Java files carrying `SPDX-License-Identifier: LGPL-3.0-only` adapt upstream
  UI layouts, icon coordinates and drawing conventions to the internal 1.7.10
  controls. Their headers identify the 2026 modifications by HackerRouter.

These files retain LGPL-3.0 and are exceptions to the repository's MIT license.
The two upstream license texts and the incorporated GPL version 3 text are
distributed in `META-INF/LICENSE-Litematica-LGPL-3.0.txt`,
`META-INF/LICENSE-MaLiLib-LGPL-3.0.txt` and `META-INF/LICENSE-GPL-3.0.txt`.
The modified source and build scripts are supplied in this repository and its
source distribution: https://github.com/HackerRouter/Schematica-Plus.
No modern Minecraft textures or MaLiLib runtime are bundled; button backgrounds
are resolved from the running Minecraft 1.7.10 resource pack.

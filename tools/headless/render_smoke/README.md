# Native render cache smoke test

This separate Forge test mod launches a disposable integrated superflat world,
waits for real client chunks, and exercises production OpenGL display lists on
the client thread. It closes the client and saves the test world automatically.
Use an isolated client directory and a working OpenGL display, such as Xvfb with
Mesa software rendering. The fixture is not included in production artifacts.

```sh
./gradlew -I tools/headless/render_smoke/init.gradle \
  -Dschematica.smoke.directory=/absolute/path/to/disposable-client runClient
```

The task requires a fresh `render-smoke-result.txt` containing `PASSED`. The log
prints `RENDER_SMOKE_COUNTS` and `RENDER_SMOKE_PASSED`. Checks cover:

- 64 translations in loaded air without additional geometry builds, with
  immediate logical coordinates and fixed lighting.
- Pixel equality between reused and freshly rebuilt display lists, both for a
  complete stone projection and translation parallel to a global slice. The
  framebuffer must contain visible pixels; an empty image cannot pass.
- Rebuilds for explicit dirtiness, obstruction, previously obstructed or unloaded
  areas, world lighting, changed fixed light level, global/local layers, edited
  blocks, custom block classes and tile-bearing blocks.
- Indexed dirty notifications at an exact section boundary, across its neighbor
  halo and outside the projection, after the draw list is reordered.
- Grid copy and renderer identities across translation, updated logical positions,
  replacement after content edits, area entry/exit and disabled-grid cleanup.

The fixture uses native Minecraft 1.7.10 / Forge behavior, with a registered
custom block to exercise the fallback. It does not establish GTNH pack acceptance,
shader compatibility, multiplayer printer behavior or a frame-rate improvement.

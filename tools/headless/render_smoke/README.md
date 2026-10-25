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
prints `RENDER_SMOKE_COUNTS`, `RENDER_SMOKE_SORTING`, `RENDER_SMOKE_VERIFIER` and
`RENDER_SMOKE_PASSED`. Checks cover:

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
- Background verification of a native 48x32x1 placement, correct/missing/wrong-block
  counts, incremental changes, 4,000 snapshot/original comparison-rule pairs,
  movement/content cancellation, pause/resume and world cleanup.
- The printer's `destroyBlocks` dispatch with the individual break toggles off:
  unassisted, assisted-normal and assisted-fast survival mining, fluid/bedrock
  exclusions and the existing creative paths. This prints
  `PRINTER_DESTROY_SMOKE_PASSED`. It checks native client dispatch, not an entire
  multiplayer exchange; the dedicated printer fixture checks actual harvesting,
  durability and drops in each game mode.
- Complete singleplayer printer passes using the real capability handshake,
  client scan, network requests and integrated-server harvesting. Each of four
  64-block fixtures clears extra stone, wrong stone, extra sand or extra glass,
  with 64 native break events and 64 points of wear on the selected tool. Glass
  checks tool priority when its mining speed equals an empty hand. This prints
  `PRINTER_BATCH_SMOKE_PASSED` and uses an unlimited scan time budget to isolate
  dispatch and harvesting from scheduling.
- Opposite-camera transparent quad orders without another model build, nonempty
  rendered pixels, identical-geometry display-list reuse, offscreen job cancellation,
  replacement after edits and complete retained-cache release on deletion. The
  fixture waits for each sorting job within its controlled frame so the unrelated
  normal renderer cannot classify the test-only section as offscreen first.

The fixture uses native Minecraft 1.7.10 / Forge behavior, with a registered
custom block to exercise the fallback. It does not establish GTNH pack acceptance,
shader compatibility, multiplayer printer behavior or a frame-rate improvement.

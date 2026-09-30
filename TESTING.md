# Verification

Run `gradlew build` for compilation, packaging, Checkstyle and unit tests. The tests
exercise bounded file/NBT inputs and pure transformation logic without starting
Minecraft. Forge world initialization requires LaunchWrapper and is not exercised
by the plain JUnit process.

Before a release, use a disposable 1.7.10 world to check:

- Start the client and dedicated server without LunatriusCore; also start with it
  installed for another mod. Open the load, save, control and materials screens.
- Load a two-layer schematic, display one layer, rotate/mirror, then display all
  layers. Verify both layers and entity positions survive.
- Load two differently colored/oriented schematics; switching or unloading one
  must not change the other instance.
- Apply Y then X rotations, reconnect, and compare the restored result. Repeat in
  another dimension and on a differently addressed server with the same display name.
- Paste over a chest, paste luminous blocks and a door, save/reload the world, and
  check inventories, lighting and block behavior. Reject pastes crossing Y=0/256.

Session keys now use save-folder/server-address plus dimension. Legacy entries
keyed only by display name remain in the JSON files but are not automatically
assigned to a world, because that assignment is ambiguous. Reload/place schematics
once to establish the new keys. Ordered transforms apply to newly saved sessions.

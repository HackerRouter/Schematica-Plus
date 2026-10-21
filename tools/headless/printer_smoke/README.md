# Native printer smoke test

This Forge test mod exercises the production printer queue against a real
dedicated-server world, native interaction handlers and Forge protection events.
It uses a fake player with recorded placement calls and suppressed outbound
packets. It does not test client input, network latency or third-party mods.

Use a disposable server directory. Accept the Minecraft EULA there and configure
a loopback address, an unused port and a flat test world in `server.properties`.
The fixture changes blocks and inventories, then stops the server automatically.

```sh
./gradlew -I tools/headless/printer_smoke/init.gradle \
  -Dschematica.smoke.directory=/absolute/path/to/disposable-server runServer
```

The task fails unless the fixture writes a fresh passing result. The log includes
`PRINTER_SMOKE_PASSED` after checking 64-block harvesting, tool durability, drops,
durability reserves, tool NBT changes, interaction and break cancellation, unbreakable blocks,
64 ordered directional placements, inventory relocation, stale targets and
double-slab consumption and independent placement/break quotas.

The test mod has a separate source set and JAR. It is not packaged in the
production mod.

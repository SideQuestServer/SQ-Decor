# SQ Decor

SQ Decor is a decoration mod for Minecraft 1.21.1 (NeoForge) about light and industrial interiors. It adds LEDs and
over 100 colorable lamps with a soft 3D glow, switches and remotes to control them, and the blocks and props to build
workshops, bunkers, clinics and sci-fi rooms around them.

The mod works on its own. CC: Tweaked and Create are optional and only add compatibility.

## Features

- **Lamps and LEDs:** bulbs, tubes, strips, panels, ceiling and wall lights, spotlights with light cones, rotating
  beacons and animated LED strips. Light and housings are colored with dye, the LED tuner or any block in your hand.
- **Control:** LED tuner with color temperature, scenes and automatic mode, light switches, dimmers, motion and
  daylight sensors, timers and a remote with up to 8 groups. Touching LEDs form a network, devices link over channels.
- **Industrial blocks:** 24 white full blocks (diamond plate, concrete, tiles, bricks and more) that take any color or
  the texture of another block.
- **Style sets:** mechanical, industrial, medical and sci-fi, each with lamps, shelves, barrels, pallets and a fence.
- **Props:** frame panels, grating walkway, levers, emergency stop, switch cabinet, fuse box, signs, workshop and office
  furniture, bunker set, computers and screens.
- **Cables and tape:** caution tape and thin cables between hooks, cable racks for up to 10 cables, colorable flat
  cables.
- **Marquee:** LED text panels that join into one line.
- **Image monitor:** a flat screen up to 8 x 8 blocks that shows pictures from a CC: Tweaked computer.
- **Compendium:** an in-game guide that explains the blocks and how to use them.

## Requirements

| | |
|---|---|
| Minecraft | 1.21.1 |
| NeoForge | 21.1.234 or newer |

Optional: CC: Tweaked (peripherals for LEDs, marquee and image monitor), Create 6.0.x (LEDs move on contraptions and
keep their settings in schematics), Sable / Create Aeronautics (light weight on ships through block tags).

## Running on a server

1. Install NeoForge 21.1.234 or newer for Minecraft 1.21.1 on the server.
2. Put `sqdecor-<version>.jar` into the server's `mods` folder.
3. Every player needs the **same** SQ Decor version on the client. The mod is required on both sides.
4. Start the server once. The limits are created in `config/sqdecor-common.toml`. Change them while the server is
   stopped.

Glow, light cones and render distances are set per player in `config/sqdecor-client.toml` or in game under
Mods > SQ Decor > Config.

## Building from source

You need Java 21.

```
./gradlew build
```

The jar ends up in `build/libs/`.

## Use of AI

This mod is developed with the assistance of AI tools, used for:

* Debugging and error analysis for Java and JSON files
* Learning aid for Blockbench modeling and Minecraft modding
* Fast testing and adapting of ideas through feedback
* Project cleanup, structure and log maintenance

AI is a tool, not the creator. All designs, decisions, Models, Textures and features come from me.

## Credits and licenses

- SQ Decor by SideQuestChris, released under the MIT License (see `LICENSE`).

Bug reports and questions: [Discord](https://discord.gg/9DUcANyAvV), channel `#issue-tracker`.

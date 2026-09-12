# Gambler Plus

Client-side Minecraft mod for tracking payments and running side games on Donut SMP.

## Downloads

Pre-built jars live in `built/`. Grab the one for your Minecraft version and drop it in your `mods` folder.

Supported: 1.21.4, 1.21.10, 1.21.11, 26.1, 26.1.1, 26.1.2, 26.2. Requires Fabric Loader.

## Features

- Live tracker for wins, losses, streaks, session totals, and all-time totals.
- Sessions you can name, end, and re-open, with per-player payment breakdowns.
- Remove button on any payment row if a bad line got logged.
- Rakeback tracker with configurable percentage.
- Large payment confirmation dialog with a threshold slider from 1K to 10B.
- Whisper filter so a fake `paid you` in a `/w` message never counts.
- HUD bar and cold-streak toast, both toggleable.
- Auction: snapshot the item in your hand, set a duration, and the highest bidder wins when time runs out. Panel stays visible in game, fireworks and a floating winner name when it ends.
- Timer: type a duration and let it count down as a floating panel.
- Text on GUI: add as many custom text overlays as you want, each with its own font, color, position, and scale.
- Edit HUD: drag panels to move them, scroll while hovering to resize.
- Arrow game support: catches `You bought X <item> for <amount>` from auction house purchases and shows a popup when the amount is 10M or higher.
- Discord button opens https://discord.gg/YnMQRpExwj.

## Building from source

Each version has its own Gradle project. To build a specific one:

```
cd 26.2/fabric-example-mod-26.2
./gradlew build
```

The finished jar ends up in `build/libs/`.

## Credits

Made by q3c (Mishka).

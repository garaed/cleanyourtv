# Ergo 32" Smart TV (CVTE, HiSilicon Hi3751V350, Android 9)

CVTE board `bigfish` / Hi3751V350, brand string HiDPT, 1 GB RAM, Android 9 `userdebug`, firmware
`20200811_153250`, UI at 1280×720 and 160 dpi. Stock Home is CVTE's `com.cvte.tv.launcher.barnard`, which
also carries the only source bar: the remote has no Source key.

| File | What it is |
|---|---|
| `debloat.txt` | The firmware updater and eight phone leftovers, grouped and commented |
| `config.json` | Dock, the CVTE source picker for the Inputs pill, and the fork's neon look |
| `wallpaper.jpg` | 1280×720 neon wallpaper, the frame's glow baked in so the TV draws no blur |

## Turning ADB on

There are no Developer options and no stock Android Settings app (`com.yug.tv.settings` replaces it).

1. Switch to a source (HDMI or AV; "No signal" is fine). The factory menu does not open from Home.
2. Press **MENU**, then **1 1 4 7** quickly. The CVTE factory menu opens.
3. **Debug → ADB ENABLE → ON**. Touch nothing else there, then leave with Back.
4. `adb connect <tv ip>:5555`, and accept the prompt on the TV.

## Install, from the repo root

```bash
export TV_HOST=<tv ip>
tools/disable.sh examples/ergo-32-cvte-hi3751v350/debloat.txt
STOCK_LAUNCHER=none launcher/build-and-install.sh      # CVTE: Barnard stays enabled, Home moves with set-home-activity
tools/venv/bin/python -I tools/tv.py --reboot          # once; check Home and ADB come back
tools/theme.sh examples/ergo-32-cvte-hi3751v350
```

## Undo

```bash
tools/restore.sh examples/ergo-32-cvte-hi3751v350/debloat.txt com.cvte.tv.launcher.barnard
```

## Fork-only theme keys

This fork adds keys the upstream launcher ignores. Each is off unless a theme sets it.

| Key | Type | Meaning |
|---|---|---|
| `neon_color` | `#RRGGBB` | Neon accent: the clock glows in it, the dock band gets a glowing outline, the focus highlight and the top pills take it |
| `clock_size` | 40 to 160 | Home's clock in sp; the date moves under the clock and the top buttons stay at the top |
| `force_locale` | language tag | Home in that language whatever the TV's (`en` keeps every glyph in Outfit, which has no Cyrillic) |
| `weather_panel` | bool | A weather panel under the top-right buttons from Open-Meteo (no key), refreshed at most every 30 minutes while Home is on screen. Needs `weather_latitude` and `weather_longitude`; `weather_place` names the place. Adds the `INTERNET` permission, which upstream does not have |

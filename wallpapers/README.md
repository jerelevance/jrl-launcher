# Wallpapers

Lock-screen loops and stills for the Galaxy Z Fold7, drawn with the same code as the launcher.
Download them from [Releases › wallpapers](https://github.com/jerelevance/jrl-launcher/releases/tag/wallpapers-v1).

| | cover (1080×2520) | inner (1968×2184) |
|---|---|---|
| `loops/jrl-field.mp4` | culture field, rings rippling | same |
| `loops/jrl-glitch.mp4` | cycles home › apps › sys › babel › color › culture with six transitions | same |
| `loops/jrl-breathe.mp4` | JERELEVANCE breathing in round pixels, lime scan | same |
| `stills/jrl-THEME.png` | one per launcher theme, plus `boot` | same |

Every loop is 12 s at 24 fps and wraps seamlessly.

**Set on the phone:** Gallery › open the file › ⋮ › *Set as wallpaper* › *Lock screen*.
Do it once folded (cover screen) and once unfolded (inner screen), each with its own size.
Videos play on the lock screen only.

**Regenerate:** copy the three font files from `app/src/main/assets/www/fonts` (CI fetches them) next to
`render.html`, then `node drive.js stills` or `node drive.js loop field|glitch|breathe cover|inner`, and
encode the frames with ffmpeg.

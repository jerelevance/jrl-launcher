# JRL Launcher

An Android home screen in the JERE//LEVANCE graphic-realism style. A thin Kotlin shell registers as the
HOME app and hosts one full-screen WebView running `app/src/main/assets/www/index.html`; a JavaScript
bridge (`window.JRLAndroid`) gives the page the app list, icons, launching, battery, notification counts,
settings shortcuts and haptics.

## Get it on the phone

1. Make a new GitHub repo (e.g. `jrl-launcher`) and upload everything in this folder, including `.github/`
   and `app/jrl.keystore`. (Web upload: drag the whole folder in. If `.github` doesn't come through, use
   *Add file › Create new file*, name it `.github/workflows/apk.yml`, and paste the file's contents.)
2. The **Build APK** action runs on every push to `main` (about 4 minutes). It publishes a release.
3. On the phone, open `github.com/<you>/jrl-launcher/releases/latest` and tap `jrl-launcher.apk`.
   Android asks once to allow installs from your browser. Allow, install.
4. Try it first from the app drawer (it's listed as **JRL**). When you're happy, press Home and pick
   **JRL** › *Always*, or set it in Settings › Apps › Default apps › Home app.
5. Optional: Sys › Notif › *Grant access* to show notification counts on the top bar and app cards.
   It counts only; it never reads notification content.

To go back to your old launcher at any time: terminal › `launcher`, or Settings › Default apps › Home app.

Every build is signed with the same key (`app/jrl.keystore`), so new builds install over old ones and keep
your dock, recents and settings.

## Using it

| Where | Gesture | Does |
|---|---|---|
| anywhere | swipe left / right | switch module: **sys ‹ home › apps** (random glitch transition) |
| home | swipe up, or tap the prompt | terminal (search) |
| home | swipe down | notification shade |
| home | touch the field | the culture dot matrix ripples |
| any app card | tap / hold | launch / pin, app info, uninstall |
| apps | drag the A–Z rail | jump by letter |
| sys | tap a panel header | collapse it |
| Back | | closes sheet › terminal › returns home |

**Terminal.** Type and the matches appear above the prompt; Enter opens the top one. Commands:
`ls`, `open X`, `info X`, `pin X`, `unpin X`, `dock`, `recent`, `uninstall X`, a number (`12` opens app #12),
`web QUERY`, `url ADDRESS`, `settings wifi|bt|display|sound|battery|launcher|notif|wallpaper`, `shade`,
`notif`, `battery`, `date`, `uptime`, `go home|apps|sys`, `reboot`, `exit`.

`~` escalates to root (there's a login sequence). Root adds: `set fx ripple|scatter|checker|rain|bands|glitch|construct|random`,
`set decode slow|normal|fast`, `set breathe on|off`, `set boot on|off`, `set icons pixel|app`,
`field next|N|list` (home palette: blue+lime, pink+mint, deep green+mint, mint+cobalt, acid+red),
`color lime|red|cyan|white|amber`, `chaos on|off`, `invert on|off`, `neofetch`, `banner`, `hack X`, `rm -rf /`, `logout`.
There's also a date you know.

## Editing the look

Everything visual is `app/src/main/assets/www/index.html` (one file, same code lineage as the site's
`template.html`). Open it in a desktop browser to preview: with no phone bridge it runs on a mock app list.
Push the change and the action builds a new APK.

## Battery

Animations pause whenever the launcher isn't on screen (WebView timers are paused natively). The touch field
runs only on home, at 12 fps idle and 30 fps while touched; the colour drift is stepped (2 updates/s).

## Files

- `app/src/main/java/.../MainActivity.kt` — HOME activity, WebView host, insets, back/home events
- `.../Bridge.kt` — `window.JRLAndroid` methods
- `.../IconHandler.kt` — serves app icons as PNGs on the page's own origin
- `.../NotifListener.kt` — optional notification counter
- `scripts/fetch-fonts.sh` — bundles Archivo, JetBrains Mono, Doto at build time
- `.github/workflows/apk.yml` — builds, uploads the artifact, publishes a release

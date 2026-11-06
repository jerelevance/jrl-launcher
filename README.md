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
| home | swipe up | app drawer with search ready, keyboard up |
| home | swipe down | notification shade |
| anywhere | swipe left / right | switch module: **sys ‹ home › apps** |
| any app | tap / hold | open / pin to home, put in a folder, move, hide, info, uninstall |
| home | tap a folder | open it; rename it at the top; Ungroup splits it back out |
| home | tap the agenda | open that event (or the calendar) |
| home | now playing | prev / play-pause / next; tap the title to open the player |
| apps | find bar | filters as you type; Enter opens the top match |
| apps | drag the A–Z rail | jump by letter |
| floor | tap the prompt | terminal |
| Back | | closes sheet › folder › search › terminal › returns home |

The calendar asks for permission the first time you tap **Show my calendar**. Now playing and the
notification badges both need **notification access** (Sys › Notif › Grant access).

Icons: `set icons app` (real icons, default), `mosaic` (real colours, redrawn as round pixels), `pixel`
(monochrome halftone). These need root (`~`).

**Terminal.** Type and matches appear; Enter opens the top one. `ls`, `open X`, `pin X`, `unpin X`, `home`,
`hide X`, `unhide X`, `hidden`, `recent`, `uninstall X`, a number, `web QUERY`, `url ADDRESS`,
`settings wifi|bt|display|sound|battery|launcher|notif|wallpaper`, `shade`, `notif`, `battery`, `update`,
`version`, `reboot`, `exit`. `~` escalates to root: `set fx|decode|breathe|boot always|smart|off|icons`, `field`,
`color`, `chaos`, `invert`, `neofetch`, `hack`, `rm -rf /`, `logout`.

The boot sequence plays after a phone restart or a UI update (`set boot always|smart|off`).

## Updating without reinstalling

The UI is one file, `app/src/main/assets/www/index.html`. The installed app checks this repo's `main`
branch for a newer copy every 30 minutes (when you come back to home) and applies it the next time you
leave home. Type `update` in the terminal to pull and apply right away.

Only changes to the Kotlin side (new phone permissions, new bridge methods) need a new APK from Releases.
Installing a new APK always resets to the page bundled inside it.

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

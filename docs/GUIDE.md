# JRL // field manual

How to run the JERE//LEVANCE home screen day to day, plus everything hidden in it.

```
    ╷ ┌───╴ ┌───┐ ┌───╴
    │ │     │   │ │
    │ ├──╴  ├─┬─┘ ├──╴
╷   │ │     │ └┐  │
└───┘ └───╴ ╵  ╵  └───╴
```

---

## 1. Install

1. On the phone, open **[Releases › latest](https://github.com/jerelevance/jrl-launcher/releases/latest)** and tap `jrl-launcher.apk`.
2. Android asks once to allow installs from your browser. Allow, then install.
3. Open **JRL** from your app drawer to try it out.
4. To make it your home screen, press Home and pick **JRL › Always**. You can also go to Settings › Apps › Default apps › Home app.

**Getting out:** type `launcher` in the terminal, or go to Settings › Default apps › Home app, and pick your old launcher.

## 2. First five minutes

| Do this | Why |
|---|---|
| Swipe up, find an app, **hold** it › *Pin to home* | Build your home row. Up to 24 apps and folders. |
| Hold an app › *Select several* › tap a few › name it › **Make folder** | Folders in one go. |
| Tap the **agenda** › *Show my calendar* | Your next 7 days on home. Read-only. |
| **Sys** (swipe right) › Notif › *Grant access* | Badges on apps, plus the now-playing controls. |
| Type `calm on` in the terminal | If the dots and ripples are too much. |

## 3. Getting around

```
            ┌──────────┐
   SYS  ‹── │   HOME   │ ──›  APPS
            └──────────┘
```

| Where | Gesture | Does |
|---|---|---|
| home | swipe **up** | apps, with the search box focused and the keyboard up |
| home | swipe **down** | notification shade |
| anywhere | swipe **left / right** | change module (random glitch transition each time) |
| any app | **tap** | open it |
| any app | **hold** | menu: pin, move, folder, select several, hide, info, uninstall |
| floor | tap **visitor@jrl:~$** | terminal |
| apps | drag the **A–Z** rail | jump to a letter |
| apps | **▦ / ≡** button in the find bar | switch between grid and list |
| sys | tap a **panel header** | collapse it |
| Back | | closes, in order: sheet › folder › search › terminal › back to home |
| Home button | | always lands you on home, with everything closed |

## 4. Home

- **Clock** in Doto, with the date, a seconds counter, the week number, and a 24-dot strip for the hours of the day.
- **Agenda:** the next three events from every calendar on the phone. Tap one to open it; tap the header to open your calendar app.
- **Now playing:** shows whatever is playing (Spotify, NTS, YouTube Music, anything). Previous, play/pause and next work, and tapping the title jumps into the player.
- **Your apps:** a 4-across grid sitting low where your thumb is, or a list (see §7).

## 5. Folders

Folders are shown as directories: `MUSIC/`, with the slash in the accent color and, in list view, the number of apps inside.

- **One at a time:** hold an app › *Put in a folder* › pick a folder, or type a new name › **Make**.
- **Several at once:** hold any app in Apps › *Select several*. Tap to pick apps; the bar at the bottom counts them. Then:
  - **Make folder**: type a name first. The folder takes the spot of the first pinned app you picked.
  - **Pin to home**: puts them all on home individually.
- **Add to an existing folder:** open the folder › **Add apps** › tap › **Add to NAME/**.
- **Rename:** open the folder and edit the name at the top.
- **Take one out:** hold it inside the folder › *Take out of folder*.
- **Ungroup:** open the folder › **Ungroup** puts every app back on home.

## 6. Apps

- **Find bar:** filters as you type. It matches the start of a word, any part of the name, the package name, and loose spellings (`yt mus` finds YouTube Music). **Enter** opens the top match. If nothing matches, Enter searches the web.
- **Hide** apps you never open: hold › *Hide from apps*. Hidden apps still show up in terminal search. `hidden` lists them and `unhide X` brings one back.
- A small **square badge** means the app has notifications waiting (needs notification access).

## 7. Looks

**Grid or list.** Tap the ▦ / ≡ button in the find bar, or type `view list` / `view grid`. List view reads like terminal output: plain mono names, no icons, no boxes, `# a` section markers in the drawer, folders as `music/` with a count. When you're picking apps for a folder, rows show `[ ]` and `[x]`. It applies to home, apps and folders.

**Icons** (root):

| `set icons …` | looks like |
|---|---|
| `app` | the real icon. Fastest to recognise. Default. |
| `mosaic` | the icon redrawn as chunky round and square pixels in its own colours, saturated so it holds up on any field |
| `pixel` | a monochrome halftone in the field's ink. Prettiest, slowest to read. |

**Themes** (root): `theme list` shows a swatch for each, and tapping a name applies it. `theme next` cycles through them.

| theme | home · apps · sys |
|---|---|
| `jrl` | blue + lime · orange · acid yellow. The original. |
| `paper` | white · steel blue · lime. Light. |
| `pastel` | pink · mint · lilac. Light. |
| `bauhaus` | cream · mustard · red. Matches the old wallpaper. |
| `mono` | white · grey · black, one red accent |
| `night` | graphite · umber · deep green. Dark. |
| `marathon` | graphite · acid green · magenta |
| `riso` | paper · fluoro pink · riso yellow, riso blue ink |
| `phosphor` | CRT green · amber · cyan on black |
| `ocean` | navy · teal · sky |
| `candy` | cherry · sky · lemon |
| `forest` | pine · moss · rust |
| `acid` | hot pink · cyan · lime. Loud. |

In `jrl`, `field next` / `field N` / `field list` swaps just the home colour: blue+lime, pink+mint, deep green+mint, mint+cobalt, acid+red.

**Quiet it down:** `calm on` removes the dot grid, the home dot field, the giant vertical word, and the tap ripples. `calm off` brings them back. In root, `set dots off` and `set ripple off` control them separately.

**Speed** (root): `set pace fast` is the default. `normal` and `slow` give the transitions more drama.

## 8. The terminal

Tap the prompt. Typing searches; **Enter** runs the highlighted row; **Tab** completes. Tap any underlined name in the output to run it.

**Visitor commands**

```
ls [X]            list apps (optionally filtered)       12          open app #12
open X / o X      open the best match                   info X      app settings
pin X / unpin X   home                                  home        list home
hide X / unhide X / hidden                              recent      last launched
uninstall X       system uninstall dialog               view list|grid
web QUERY / g Q   search the web                        url ADDRESS
settings [wifi|bt|display|sound|battery|launcher|notif|wallpaper|location|airplane|dev|storage|data]
wifi · bt · launcher · shade · notif · battery
date · uptime · whoami · id · uname · pwd · echo · fortune · history · clear
go home|apps|sys · calm on|off · update · version · reboot · exit · help
```

**Root.** Type `~` (or `su`) and sit through the login.

```
theme NAME|next|list          field N|next|list           color lime|red|cyan|white|amber
set icons app|mosaic|pixel    set pace fast|normal|slow   set fx ripple|scatter|checker|rain|bands|glitch|construct|random
set decode slow|normal|fast   set breathe on|off          set boot always|smart|off
set dots on|off               set ripple on|off           chaos on|off      invert on|off
neofetch · banner · skull · hack TARGET · rm -rf / · logout
```

## 9. Fun stuff

- **Boot.** It plays after a phone restart or a UI update. On the last line, tap `[enter]` to land on home, or `[~] root` to drop straight into a root shell. `set boot always` plays it every time.
- **`11011998`.** Try it.
- **`rm -rf /`** in root. Watch it. Nothing is actually deleted.
- **`hack mainframe`** in root. Nothing leaves the phone.
- **`chaos on`** in root. The interface starts glitching on its own. `chaos off` contains it.
- **`set fx construct`** in root. Every module rebuilds itself block by block with labelled, scanlined proxies.
- **`neofetch`**, **`fortune`**, **`skull`**, **`invert on`**.
- **Two characters never settle.** On titles and labels, one or two letters keep flickering after the decode. That's on purpose.
- **Touch the field** on home. It ripples. Leave it alone and it ripples by itself every few seconds.

## 10. Updates

- **Look and behaviour** (anything in `app/src/main/assets/www/index.html`) update over the air. The phone checks this repo every 30 minutes when you come back to home and applies the update the next time you leave home. Type `update` in the terminal to pull and apply right away.
- **Native changes** (new permissions, new phone features) need a new APK from Releases. It installs over the old one and keeps your home, folders and settings.
- Want to preview a change? Open `index.html` in any desktop browser. It runs on a fake app list.

## 11. Troubleshooting

| Problem | Fix |
|---|---|
| Agenda says *calendar not connected* | Tap *Show my calendar* and allow it. |
| No now-playing strip or badges | Sys › Notif › *Grant access*, then turn on **JRL notification count**. |
| Fonts look plain | Close and reopen JRL. The fonts are bundled, so it isn't a network issue. |
| `update` says it can't reach GitHub | Try again on Wi-Fi. Nothing breaks in the meantime. |
| Something looks wrong after an update | Reinstall the latest APK. It resets to the page bundled inside it. |
| Want your old home screen back | `launcher` in the terminal, then pick it. |

## 12. Privacy

- The app reads your **list of installed apps** and their **icons**, so it can show them.
- With your permission, it reads **calendar events**, read-only, to show the agenda.
- With notification access, it reads **how many notifications each app has**. It never reads their content.
- It reads **what's playing**: title, artist and app.
- Nothing leaves the phone. The only network requests are pulling this repo's `index.html` for updates and, if the bundled fonts were missing, Google Fonts.

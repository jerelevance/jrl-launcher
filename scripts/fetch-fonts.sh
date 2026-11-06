#!/usr/bin/env bash
# Pulls Archivo, JetBrains Mono and Doto (variable TTFs) from github.com/google/fonts into the app's assets,
# so the launcher has its type offline. If a download fails the build still succeeds; the page then
# falls back to Google Fonts when the phone is online.
set -u
out="app/src/main/assets/www/fonts"
mkdir -p "$out"
auth=()
[ -n "${GH_TOKEN:-}" ] && auth=(-H "Authorization: Bearer $GH_TOKEN")

get() {
  local dir="$1" name="$2" url
  url=$(curl -fsSL "${auth[@]}" "https://api.github.com/repos/google/fonts/contents/ofl/$dir" \
        | jq -r '[.[] | select(.name|endswith(".ttf")) | select(.name|test("Italic")|not) | .download_url][0] // empty')
  if [ -n "$url" ] && curl -fsSL "$url" -o "$out/$name"; then
    echo "ok   $name  <- $url"
  else
    echo "skip $name (will fall back to Google Fonts online)"
  fi
}

get archivo Archivo.ttf
get jetbrainsmono JetBrainsMono.ttf
get doto Doto.ttf
ls -la "$out"

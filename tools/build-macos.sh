#!/bin/bash

set -euo pipefail

usage() {
  cat <<'EOF'
Usage: ./tools/build-macos.sh [Codex.app] [Destination.app]

Builds a local Codex Native Selector app from the installed macOS Codex app.

Defaults:
  Codex app:       /Applications/ChatGPT.app, then /Applications/Codex.app
  Destination:     ~/Applications/Codex Native Selector.app
EOF
}

fail() {
  printf 'error: %s\n' "$*" >&2
  exit 1
}

plist_set() {
  local plist="$1"
  local key="$2"
  local value="$3"

  /usr/libexec/PlistBuddy -c "Set :$key $value" "$plist" >/dev/null
}

if [[ "${1:-}" == "--help" || "${1:-}" == "-h" ]]; then
  usage
  exit 0
fi
if (( $# > 2 )); then
  usage >&2
  exit 2
fi
if [[ "$(uname -s)" != "Darwin" ]]; then
  fail "the macOS builder must be run on macOS"
fi

workspace="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd -P)"
if (( $# >= 1 )); then
  install_app="$1"
else
  install_app=""
  for candidate in /Applications/ChatGPT.app /Applications/Codex.app \
    "$HOME/Applications/ChatGPT.app" "$HOME/Applications/Codex.app"; do
    if [[ -d "$candidate" ]]; then
      install_app="$candidate"
      break
    fi
  done
fi
[[ -n "$install_app" ]] || fail "Codex was not found; pass its .app path as the first argument"
[[ -d "$install_app" ]] || fail "Codex app bundle not found: $install_app"

command -v node >/dev/null || fail "Node.js 22.12 or newer is required"
command -v ditto >/dev/null || fail "ditto is required"
command -v codesign >/dev/null || fail "codesign is required"

node="$(command -v node)"
asar_cli="$workspace/node_modules/.bin/asar"
[[ -x "$asar_cli" ]] || fail "dependencies are missing; run npm install first"

install_app="$(cd "$(dirname "$install_app")" && pwd -P)/$(basename "$install_app")"
destination="${2:-$HOME/Applications/Codex Native Selector.app}"
[[ "$destination" == *.app ]] || fail "destination must be an .app bundle"
mkdir -p "$(dirname "$destination")"
destination="$(cd "$(dirname "$destination")" && pwd -P)/$(basename "$destination")"
[[ "$destination" != "$install_app" ]] || fail "destination must not overwrite the official Codex app"

source_archive="$install_app/Contents/Resources/app.asar"
source_plist="$install_app/Contents/Info.plist"
[[ -f "$source_archive" ]] || fail "Codex app.asar not found: $source_archive"
[[ -f "$source_plist" ]] || fail "Codex Info.plist not found: $source_plist"

temporary_root="$(mktemp -d "${TMPDIR:-/tmp}/codex-native-selector.XXXXXX")"
trap 'rm -rf "$temporary_root"' EXIT
staging_app="$temporary_root/Codex Native Selector.app"
extracted="$temporary_root/asar-extracted"

printf 'Copying Codex app bundle...\n'
ditto --clone --norsrc --noextattr --noqtn --noacl "$install_app" "$staging_app"

printf 'Extracting app.asar...\n'
"$asar_cli" extract "$source_archive" "$extracted"

destination_archive="$staging_app/Contents/Resources/app.asar"
builder_output="$(
  "$node" "$workspace/tools/build-inplace-asar.mjs" \
    "$source_archive" \
    "$destination_archive" \
    "$workspace/tools/selector-v2.js.txt" \
    "$extracted"
)"
printf '%s\n' "$builder_output"
asar_header_sha256="$(
  printf '%s' "$builder_output" | "$node" -e '
    let input = "";
    process.stdin.setEncoding("utf8");
    process.stdin.on("data", (chunk) => { input += chunk; });
    process.stdin.on("end", () => {
      const value = JSON.parse(input).asarHeaderSha256;
      if (!/^[0-9a-f]{64}$/u.test(value)) process.exit(1);
      process.stdout.write(value);
    });
  '
)" || fail "could not read the patched ASAR header digest"

plist="$staging_app/Contents/Info.plist"
plist_set "$plist" "ElectronAsarIntegrity:Resources/app.asar:hash" "$asar_header_sha256"
plist_set "$plist" "CFBundleDisplayName" "Codex Native Selector"
plist_set "$plist" "CFBundleIdentifier" "com.openai.codex.native-selector"
plist_set "$plist" "CFBundleName" "Codex Native Selector"
plist_set "$plist" "BundleSigningBaseName" "Codex Native Selector"
plist_set "$plist" "CrProductDirName" "com.openai.codex.native-selector"
plist_set "$plist" "MDItemKeywords" "Codex Native Selector"
plist_set "$plist" "CFBundleURLTypes:0:CFBundleURLName" "Codex Native Selector"
for nested_plist in \
  "$staging_app/Contents/PlugIns/CodexDockTilePlugin.plugin/Contents/Info.plist" \
  "$staging_app/Contents/Frameworks/Codex Framework.framework/Versions/Current/Resources/Info.plist" \
  "$staging_app/Contents/Frameworks/Codex Framework.framework/Versions/Current/Helpers/Codex (GPU).app/Contents/Info.plist" \
  "$staging_app/Contents/Frameworks/Codex Framework.framework/Versions/Current/Helpers/Codex (Service).app/Contents/Info.plist" \
  "$staging_app/Contents/Frameworks/Codex Framework.framework/Versions/Current/Helpers/Codex (Alerts).app/Contents/Info.plist" \
  "$staging_app/Contents/Frameworks/Codex Framework.framework/Versions/Current/Helpers/Codex (Renderer).app/Contents/Info.plist"; do
  [[ -f "$nested_plist" ]] || continue
  nested_identifier="$(/usr/libexec/PlistBuddy -c "Print :CFBundleIdentifier" "$nested_plist" 2>/dev/null || true)"
  if [[ "$nested_identifier" == com.openai.codex* ]]; then
    plist_set "$nested_plist" "CFBundleIdentifier" "${nested_identifier/com.openai.codex/com.openai.codex.native-selector}"
  fi
done
if ! /usr/libexec/PlistBuddy \
  -c "Set :LSEnvironment:CODEX_SPARKLE_ENABLED false" \
  "$plist" >/dev/null 2>&1; then
  /usr/libexec/PlistBuddy \
    -c "Add :LSEnvironment:CODEX_SPARKLE_ENABLED string false" \
    "$plist" >/dev/null
fi

printf 'Ad-hoc signing customized app bundle...\n'
codesign --force --deep --sign - "$staging_app"
codesign --verify --deep --strict "$staging_app"

rm -rf "$destination"
mv "$staging_app" "$destination"
if ! codesign --verify --deep --strict "$destination"; then
  fail "the destination altered the signed bundle; use a local path such as ~/Applications instead of an iCloud or File Provider directory"
fi

printf '\nCreated %s\n' "$destination"
printf 'Quit the official Codex app before opening this customized copy.\n'

# Codex Native Selector

> A local customization of the Codex Desktop model selector, built from the Codex installation already present on macOS or Windows.

[![Platform](https://img.shields.io/badge/platform-macOS%20%7C%20Windows-0078D4?style=for-the-badge&logo=apple)](#installation)
[![Codex Desktop](https://img.shields.io/badge/Codex%20Desktop-local%20patch-10A37F?style=for-the-badge)](https://openai.com/codex/)
[![License](https://img.shields.io/badge/license-MIT-6E56CF?style=for-the-badge)](LICENSE)

This project turns Codex's compact model selector into a clearer and faster interface: model variants in tabs, the native reasoning slider, model-specific animated colors, Fast mode, a settings button, and an automatically grouped model menu.

The project does not redistribute Codex or replace the official installation. It generates a local portable copy from the Codex installation already on your computer.

## Preview

```text
┌──────────────────────────────────────────────┐
│  Sol       Terra       Luna       ⚡    ⚙     │
│                                              │
│  ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━●            │
└──────────────────────────────────────────────┘
              5.6 Terra  Very high
```

The ⚙ button opens the available model families, such as `5.6`, `5.5`, `5.4`, and `5.3 Codex Spark`. Variants remain in the top row, so the model menu does not duplicate `Sol`, `Terra`, `Luna`, or `Mini`.

## Features

- Uses Codex Desktop's native model-power slider component.
- Reads reasoning efforts from the real Codex model catalog.
- Shows model variants only when they actually exist.
- Displays a fixed `Full` tab for a family with no selectable variant.
- Groups the model menu automatically by model family.
- Recolors the Ultra animation using the active model family's color while preserving the native slider animation.
- Keeps Fast mode in the same row as the model variants.
- Renders the model menu above the interface through a DOM portal to avoid clipping and z-index issues.
- Creates a separate local copy while leaving the official installation untouched.

## How it works

```mermaid
flowchart LR
    A[Official Codex installation] --> B[Local app.asar]
    B --> C[Temporary extraction]
    C --> D[Targeted bundle patch]
    D --> E[Portable app.asar]
    E --> F[Customized native selector]
    F --> G[Real Codex catalog and efforts]
```

The patch only targets the selector bundle and preserves the original application structure, components, events, and backend. The rest of the application is copied without modification.

## Installation

### Requirements

- macOS 12 or newer, Windows 10, or Windows 11.
- Codex Desktop installed officially.
- Node.js 22.12 or newer.
- Codex fully closed while building and launching.

### macOS

The official macOS bundle is currently installed as `/Applications/ChatGPT.app`, even though it identifies itself as Codex. The builder also detects older `/Applications/Codex.app` installations.

Clone the repository and install the dependencies:

```bash
git clone https://github.com/Mirochill/codex-native-selector.git
cd codex-native-selector
npm install
```

Build the customized app:

```bash
./tools/build-macos.sh
```

To use a nonstandard installation or output path, pass both explicitly:

```bash
./tools/build-macos.sh "/Applications/ChatGPT.app" \
  "$HOME/Applications/Codex Native Selector.app"
```

The default output is `~/Applications/Codex Native Selector.app`. The script extracts and patches a temporary copy, updates Electron's ASAR integrity digest, disables Sparkle updates for the copy, and ad-hoc signs the finished bundle. The official app is not modified. Avoid iCloud Drive and other File Provider destinations because they can attach metadata that invalidates a local app signature.

Quit the official Codex app, then launch:

```bash
open "$HOME/Applications/Codex Native Selector.app"
```

The official and customized copies must not run at the same time because they use the same Codex profile and bundle identity.

### Windows

PowerShell is required in addition to the common requirements above.

#### 1. Clone the repository

```powershell
git clone https://github.com/Mirochill/codex-native-selector.git
Set-Location .\codex-native-selector
npm install
```

#### 2. Find the installed Codex directory

The directory usually looks like this:

```text
C:\Program Files\WindowsApps\OpenAI.Codex_<version>_x64__2p2nqsd0c76g0\app
```

To automatically find the most recent version:

```powershell
$package = Get-ChildItem 'C:\Program Files\WindowsApps' -Directory -Filter 'OpenAI.Codex_*' |
  Sort-Object LastWriteTime |
  Select-Object -Last 1
$installApp = Join-Path $package.FullName 'app'
$installApp
```

If Windows denies access to `WindowsApps`, use the installation path shown in Codex's properties or enter the path manually.

#### 3. Extract the official archive temporarily

This reads the local application but does not modify the official installation:

```powershell
Remove-Item .\work\asar-extracted -Recurse -Force -ErrorAction SilentlyContinue
npx --yes @electron/asar extract `
  (Join-Path $installApp 'resources\app.asar') `
  .\work\asar-extracted
```

#### 4. Build the customized copy

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass `
  -File .\tools\build-portable.ps1 `
  -InstallApp $installApp
```

The result is generated in:

```text
outputs\Codex-Native-Selector\
```

#### 5. Launch Codex Native Selector

Close official Codex, including its tray icon in the Windows notification area, then launch:

```text
outputs\Codex-Native-Selector\Launch Codex Native Selector.cmd
```

The official and customized copies must not run at the same time because they use the same local Codex environment.

## Updating after a Codex update

The Codex frontend bundle can change its filenames or structure after an update. Rebuild from the new installation:

On macOS, quit Codex and rerun:

```bash
./tools/build-macos.sh
```

On Windows, re-extract the updated archive and rebuild:

```powershell
Remove-Item .\work\asar-extracted -Recurse -Force -ErrorAction SilentlyContinue
npx --yes @electron/asar extract `
  (Join-Path $installApp 'resources\app.asar') `
  .\work\asar-extracted
powershell.exe -NoProfile -ExecutionPolicy Bypass `
  -File .\tools\build-portable.ps1 `
  -InstallApp $installApp
```

If the script reports that a chunk or function cannot be found, the Codex version has probably changed. Update the search markers in `tools/build-inplace-asar.mjs` and rebuild.

The builder discovers hashed model-selector and React DOM asset filenames automatically. Minified component names can still change between Codex releases; version-specific mappings live in `tools/selector-compatibility.mjs` and are covered by `npm run check`.

## Advantages

| Area | Benefit |
| --- | --- |
| No Codex fork | The project does not maintain a complete copy of the application. |
| No reinstallation | The copy is rebuilt from Codex already installed locally. |
| Official installation preserved | The installed app or Microsoft Store package is not overwritten. |
| Native components | The slider, its events, and model behavior remain Codex components. |
| Dynamic catalog | Models and reasoning efforts come from Codex's real configuration. |
| Targeted maintenance | The patch focuses on one frontend bundle. |

## Important limitations

- This is not an official plugin: Codex Desktop does not expose a public API for replacing its native UI with a plugin.
- The project currently targets macOS and Windows.
- The copy is tied to the Codex version and frontend chunk names used during the build.
- A Codex update may require changes to the patch.
- The macOS copy is ad-hoc signed for local use because changing `app.asar` invalidates the official signature. It is not a distributable or notarized build.
- The official archive is not included on GitHub because of its size, redistribution concerns, and software ownership.
- The project does not bypass authentication, account limits, or service rules.
- The official instance must be closed before launching the customized copy.
- Available models still depend on the user's account, plan, and Codex backend.

## Troubleshooting

### The application stays on the OpenAI logo

Make sure that `app.asar` was extracted from the currently installed Codex version, then rebuild the copy. An old extracted archive may contain incompatible chunk names.

### The launcher says that another instance is open

Quit Codex from the Windows notification-area icon. Closing only the main window may leave the background process running.

On macOS, quit Codex with <kbd>Command</kbd>+<kbd>Q</kbd> before opening the customized copy.

### macOS says the customized app is damaged or cannot be opened

Rebuild it locally from the official installation, then verify the local signature:

```bash
codesign --verify --deep --strict "$HOME/Applications/Codex Native Selector.app"
```

Do not copy a customized app built on another Mac. The generated bundle is intentionally ad-hoc signed for the machine that built it.

### The selector does not show new models

Rebuild from the newest official archive. This project does not manufacture a model list: it reuses the catalog exposed by Codex.

### The application shows “An error occurred” after a Codex update

Run `npm run check`, extract `app.asar` from the current official installation, and rebuild. React error 130 usually means Codex renamed a minified component used by the selector patch. Update the matching profile in `tools/selector-compatibility.mjs`; do not map JSX targets to JSX-runtime or lazy-loader objects.

### I want to return to official Codex

Close the customized copy, then launch Codex from the Start menu. No uninstall or file restoration is required.

## Repository structure

```text
tools/
├── build-portable.ps1       # Copies the local installation and creates the launcher
├── build-macos.sh           # Builds and ad-hoc signs a separate macOS app bundle
├── build-inplace-asar.mjs   # Applies the targeted app.asar patch
└── selector-v2.js.txt        # Customized selector component

README.md
LICENSE
.gitignore
package.json
```

The `outputs/`, `work/asar-extracted/`, and generated archives are ignored by Git so that a full Codex copy, local profile, or personal data cannot be published accidentally.

## License

The scripts and customization code in this repository are distributed under the [MIT License](LICENSE).

Codex Desktop, its components, resources, and models remain the property of their respective owners. This community project is not affiliated with or endorsed by OpenAI.

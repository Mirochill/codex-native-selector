import crypto from "node:crypto";
import fs from "node:fs";
import path from "node:path";
import { createRequire } from "node:module";
import { pathToFileURL } from "node:url";
import {
  adaptCompactSelector,
  selectSelectorCompatibilityProfile,
} from "./selector-compatibility.mjs";
const require = createRequire(import.meta.url);
const asarLib = path.dirname(require.resolve("@electron/asar"));
const { readArchiveHeaderSync } = await import(`${pathToFileURL(asarLib).href}/disk.js`);
const { Pickle } = await import(`${pathToFileURL(asarLib).href}/pickle.js`);

const sourceArchive = path.resolve(process.argv[2] ?? "source/app/resources/app.asar");
const destinationArchive = path.resolve(
  process.argv[3] ?? "outputs/Codex-Native-Selector/app/resources/app.asar",
);
const extractedRoot = path.resolve(process.argv[5] ?? "work/asar-extracted");
const assetsRoot = path.join(extractedRoot, "webview/assets");
const findSingleAsset = (pattern, description) => {
  const matches = fs.readdirSync(assetsRoot).filter((name) => pattern.test(name));
  if (matches.length !== 1) {
    throw new Error(`Expected one ${description} asset, found ${matches.length}.`);
  }
  return matches[0];
};
const assetSources = fs
  .readdirSync(assetsRoot)
  .filter((name) => name.endsWith(".js"))
  .map((name) => ({
    name,
    source: fs.readFileSync(path.join(assetsRoot, name), "utf8"),
  }));
const selectorMatches = assetSources.flatMap(({ name, source: assetSource }) => {
  try {
    return [
      {
        name,
        profile: selectSelectorCompatibilityProfile(assetSource),
        source: assetSource,
      },
    ];
  } catch {
    return [];
  }
});
if (selectorMatches.length !== 1) {
  throw new Error(
    `Expected one compatible model selector asset, found ${selectorMatches.length}.`,
  );
}
const [{ name: chunkFileName, profile: compatibilityProfile }] = selectorMatches;
for (const marker of compatibilityProfile.assetMarkers ?? []) {
  if (
    !assetSources.some(({ source: assetSource }) => assetSource.includes(marker))
  ) {
    throw new Error(`Required selector asset marker not found: ${marker}`);
  }
}
const chunkRelativePath = `webview/assets/${chunkFileName}`;
const chunkSourcePath = path.join(extractedRoot, chunkRelativePath);

let source = fs.readFileSync(chunkSourcePath, "utf8");
if (compatibilityProfile.reactDomImport) {
  const reactDomFileName = findSingleAsset(
    compatibilityProfile.reactDomAssetPattern,
    "React DOM",
  );
  source = `${compatibilityProfile.reactDomImport(reactDomFileName)}${source}`;
}
source = source.replace(
  compatibilityProfile.catalogBefore,
  compatibilityProfile.catalogAfter,
);

const { fieldsBefore, fieldsAfter, valueBefore, valueAfter } =
  compatibilityProfile;
if (!source.includes(fieldsBefore) || !source.includes(valueBefore)) {
  throw new Error("The original model selection mapping was not found.");
}
source = source.replace(fieldsBefore, fieldsAfter).replace(valueBefore, valueAfter);

const { componentName, nextComponentName } = compatibilityProfile;
const componentStart = source.indexOf(`function ${componentName}(e){`);
const componentEnd = source.indexOf(
  `function ${nextComponentName}(e){`,
  componentStart,
);
if (componentStart < 0 || componentEnd < 0) {
  throw new Error("The original compact selector component was not found.");
}

function compactSelectorTemplate() {/*
function Cs(){if(document.getElementById(`codex-native-selector-style`))return;let e=document.createElement(`style`);e.id=`codex-native-selector-style`,e.textContent=`._SimpleView_15yqt_93.codex-native{min-height:72px;padding:5px 8px 7px;--a:var(--color-token-charts-blue)}.codex-native[data-v=sol]{--a:#f2c14e}.codex-native[data-v=terra]{--a:#62c1b5}.codex-native[data-v=luna]{--a:#9c8cff}.codex-native[data-v=mini]{--a:#77a7ff}.codex-tabs{height:29px;display:flex;gap:4px;margin:0 0 4px;padding:0 2px;border-bottom:1px solid var(--color-token-border)}.codex-tabs button{position:relative;height:29px;padding:3px 8px;color:var(--color-token-text-tertiary);font:inherit;font-size:12px;background:0 0;border:0;border-radius:6px 6px 0 0;cursor:var(--cursor-interaction)}.codex-tabs button:hover{background:var(--color-token-list-hover-background)}.codex-tabs button[data-s=true]{color:var(--a);font-weight:600}.codex-tabs button[data-s=true]:after{content:"";position:absolute;height:2px;inset:27px 7px 0;background:var(--a);border-radius:9px}.codex-native ._Range_m3zgh_451{background:var(--a)}.codex-native ._Thumb_m3zgh_23{box-shadow:0 0 8px color-mix(in srgb,var(--a) 45%,transparent)}`,document.head.append(e)}
function Cf(e){let t=e.match(/^gpt-(\d+\.\d+)(?:-|$)/u);return t?.[1]??e}
function Cv(e,t){let n=`gpt-${t}`;return e===n?`Full`:(e.startsWith(`${n}-`)?e.slice(n.length+1):e).split(`-`).map(e=>e?e[0].toUpperCase()+e.slice(1):e).join(` `)}
function Ck(e,t){let n=`gpt-${t}`;return e===n?`full`:e.startsWith(`${n}-`)?e.slice(n.length+1):`default`}
function Ve(e){let{active:t,fastModeEnabled:n,onDragToMax:r,onSelectPower:i,powerSelections:a,selectedPowerSelection:o,shouldReduceMotion:s,transitionsReady:l}=e;if(o==null)return(0,Z.jsx)(`div`,{className:X.SimpleView});Cs();let u=Cf(o.model),d=[...new Set(a.filter(e=>Cf(e.model)===u).map(e=>e.model))],f=a.filter(e=>e.model===o.model),p=f.map(e=>({id:e.id,isMax:e.reasoningEffort===`ultra`,label:e.modelLabel})),m=e=>{let t=a.find(t=>t.id===e.id);t&&i(t)},h=e=>{let t=a.filter(t=>t.model===e),n=t.find(e=>e.reasoningEffort===o.reasoningEffort)??t.find(e=>e.reasoningEffort===e.defaultReasoningEffort)??t[0];n&&i(n)},g=d.length>1?(0,Z.jsx)(`div`,{className:`codex-tabs`,role:`tablist`,children:d.map(e=>(0,Z.jsx)(`button`,{type:`button`,role:`tab`,"aria-selected":e===o.model,"data-s":e===o.model,onPointerDown:e=>e.preventDefault(),onClick:t=>{t.preventDefault(),t.stopPropagation(),h(e)},children:Cv(e,u)},e))}):null;return(0,Z.jsxs)(`div`,{className:`${X.SimpleView} codex-native`,"data-v":Ck(o.model,u),children:[g,(0,Z.jsx)(ye,{active:t,fastModeEnabled:n,keyboardControlFocused:!1,onDragToMax:r,onSelectOption:m,options:p,selectedOptionId:o.id,shouldReduceMotion:s,transitionsReady:l})]})}
*/}

const compactSelector = adaptCompactSelector(
  fs
    .readFileSync(path.resolve(process.argv[4] ?? "tools/selector-v2.js.txt"), "utf8")
    .replace(/^\uFEFF/u, ""),
  compatibilityProfile,
);
source =
  source.slice(0, componentStart) +
  compactSelector +
  source.slice(componentEnd);

const { controlsName, nextControlsName } = compatibilityProfile;
const controlsStart = source.indexOf(`function ${controlsName}(e){`);
const controlsEnd = source.indexOf(
  `function ${nextControlsName}(e){`,
  controlsStart,
);
if (controlsStart < 0 || controlsEnd < 0) {
  throw new Error("The original compact selector controls were not found.");
}
source =
  source.slice(0, controlsStart) +
  `function ${controlsName}(e){let{ref:t}=e;return(0,${compatibilityProfile.controlsJsxRuntime}.jsx)(\`div\`,{className:\`codex-controls-placeholder\`,ref:t})}` +
  source.slice(controlsEnd);

const { sliderPropsBefore, sliderPropsAfter } = compatibilityProfile;
if (!source.includes(sliderPropsBefore)) {
  throw new Error("The compact selector prop wiring was not found.");
}
source = source.replace(sliderPropsBefore, sliderPropsAfter);

const { advancedStateBefore } = compatibilityProfile;
const { menuViewBefore, menuViewAfter } = compatibilityProfile;
if (!source.includes(menuViewBefore)) {
  throw new Error("The advanced-view state wiring was not found.");
}
source = source
  .replace(advancedStateBefore, advancedStateBefore.replace("i===`advanced`", "!1"))
  .replace(menuViewBefore, menuViewAfter);

const originalChunk = fs.readFileSync(chunkSourcePath);
const modifiedChunk = Buffer.from(source, "utf8");
if (modifiedChunk.length > originalChunk.length) {
  throw new Error(
    `Modified chunk is ${modifiedChunk.length - originalChunk.length} bytes too large.`,
  );
}
const fixedSizeChunk = Buffer.concat([
  modifiedChunk,
  Buffer.alloc(originalChunk.length - modifiedChunk.length, 0x20),
]);

fs.copyFileSync(sourceArchive, destinationArchive);
const rawHeader = readArchiveHeaderSync(destinationArchive);
let entry = rawHeader.header;
for (const part of chunkRelativePath.split("/")) {
  entry = entry.files[part];
}
if (entry.unpacked || entry.size !== fixedSizeChunk.length) {
  throw new Error("Unexpected target chunk metadata in the official archive.");
}

const newHash = crypto.createHash("sha256").update(fixedSizeChunk).digest("hex");
entry.integrity.hash = newHash;
entry.integrity.blocks = [newHash];

const headerPickle = Pickle.createEmpty();
headerPickle.writeString(JSON.stringify(rawHeader.header));
const headerBuffer = headerPickle.toBuffer();
if (headerBuffer.length !== rawHeader.headerSize) {
  throw new Error(
    `Header size changed from ${rawHeader.headerSize} to ${headerBuffer.length}.`,
  );
}

const archive = fs.openSync(destinationArchive, "r+");
try {
  fs.writeSync(archive, headerBuffer, 0, headerBuffer.length, 8);
  const dataOffset = 8 + rawHeader.headerSize + Number.parseInt(entry.offset, 10);
  fs.writeSync(archive, fixedSizeChunk, 0, fixedSizeChunk.length, dataOffset);
} finally {
  fs.closeSync(archive);
}

const verification = readArchiveHeaderSync(destinationArchive);
let verifiedEntry = verification.header;
for (const part of chunkRelativePath.split("/")) {
  verifiedEntry = verifiedEntry.files[part];
}
const archiveFd = fs.openSync(destinationArchive, "r");
let verifiedChunk;
try {
  verifiedChunk = Buffer.alloc(verifiedEntry.size);
  const dataOffset =
    8 + verification.headerSize + Number.parseInt(verifiedEntry.offset, 10);
  fs.readSync(archiveFd, verifiedChunk, 0, verifiedChunk.length, dataOffset);
} finally {
  fs.closeSync(archiveFd);
}
const verifiedHash = crypto
  .createHash("sha256")
  .update(verifiedChunk)
  .digest("hex");
if (verifiedHash !== verifiedEntry.integrity.hash || verifiedHash !== newHash) {
  throw new Error("Patched chunk integrity verification failed.");
}
const asarHeaderSha256 = crypto
  .createHash("sha256")
  .update(JSON.stringify(verification.header))
  .digest("hex");

console.log(
  JSON.stringify(
    {
      archive: destinationArchive,
      archiveBytes: fs.statSync(destinationArchive).size,
      originalChunkBytes: originalChunk.length,
      modifiedCodeBytes: modifiedChunk.length,
      paddedBytes: fixedSizeChunk.length - modifiedChunk.length,
      chunkOffset: entry.offset,
      chunkSha256: newHash,
      headerSize: rawHeader.headerSize,
      asarHeaderSha256,
    },
    null,
    2,
  ),
);

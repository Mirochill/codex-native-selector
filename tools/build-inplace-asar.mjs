import crypto from "node:crypto";
import fs from "node:fs";
import path from "node:path";
import { createRequire } from "node:module";
import { pathToFileURL } from "node:url";
const require = createRequire(import.meta.url);
const asarLib = path.dirname(require.resolve("@electron/asar"));
const { readArchiveHeaderSync } = await import(`${pathToFileURL(asarLib).href}/disk.js`);
const { Pickle } = await import(`${pathToFileURL(asarLib).href}/pickle.js`);

const sourceArchive = path.resolve(process.argv[2] ?? "source/app/resources/app.asar");
const destinationArchive = path.resolve(
  process.argv[3] ?? "outputs/Codex-Native-Selector/app/resources/app.asar",
);
const extractedRoot = path.resolve(process.argv[5] ?? "work/asar-extracted");
const chunkRelativePath =
  "webview/assets/model-and-reasoning-dropdown-DFupBzq6.js";
const chunkSourcePath = path.join(extractedRoot, chunkRelativePath);

let source = fs.readFileSync(chunkSourcePath, "utf8");
source = `import{t as Cp}from"./react-dom-CTTwO1mS.js";${source}`;

const catalogBefore =
  "function K(e){let t=q(ce,e);if(t.length>=4)return t;let n=q(le,e);return n.length>=4?n:[]}";
const catalogAfter = "function K(e){return oe(e)}";
if (!source.includes(catalogBefore)) {
  throw new Error("The original compact catalog function was not found.");
}
source = source.replace(catalogBefore, catalogAfter);

const fieldsBefore = "supportedReasoningEfforts:n})=>{let r=";
const fieldsAfter =
  "supportedReasoningEfforts:n,defaultReasoningEffort:ct})=>{let r=";
const valueBefore = "modelLabel:r,reasoningEffort:e}))})??[]}";
const valueAfter =
  "modelLabel:r,reasoningEffort:e,defaultReasoningEffort:ct}))})??[]}";
if (!source.includes(fieldsBefore) || !source.includes(valueBefore)) {
  throw new Error("The original model selection mapping was not found.");
}
source = source.replace(fieldsBefore, fieldsAfter).replace(valueBefore, valueAfter);

const componentStart = source.indexOf("function Ve(e){");
const componentEnd = source.indexOf("function He(e){", componentStart);
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

const compactSelector = fs
  .readFileSync(path.resolve(process.argv[4] ?? "tools/selector-v2.js.txt"), "utf8")
  .replace(/^\uFEFF/u, "");
source =
  source.slice(0, componentStart) +
  compactSelector +
  source.slice(componentEnd);

const controlsStart = source.indexOf("function rt(e){");
const controlsEnd = source.indexOf("function it(e){", controlsStart);
if (controlsStart < 0 || controlsEnd < 0) {
  throw new Error("The original compact selector controls were not found.");
}
source =
  source.slice(0, controlsStart) +
  "function rt(e){let{ref:t}=e;return(0,Q.jsx)(`div`,{className:`codex-controls-placeholder`,ref:t})}" +
  source.slice(controlsEnd);

const sliderPropsBefore =
  "fastModeEnabled:H,onDragToMax:L,onSelectComplete:a,onSelectPower:z,powerSelections:l,selectedPowerSelection:u,shouldReduceMotion:h,transitionsReady:C";
const sliderPropsAfter =
  "fastModeEnabled:H,onDragToMax:L,onSelectComplete:a,onSelectPower:z,onSelectServiceTier:s,powerSelections:l,selectedPowerSelection:u,serviceTierOptions:f,serviceTierOptionsLoading:p,shouldReduceMotion:h,transitionsReady:C";
if (!source.includes(sliderPropsBefore)) {
  throw new Error("The compact selector prop wiring was not found.");
}
source = source.replace(sliderPropsBefore, sliderPropsAfter);

const advancedStateBefore = "D=(0,ft.useRef)(null),O=i===`advanced`,k=";
const advancedStateAfter = "D=(0,ft.useRef)(null),O=!1,k=";
const menuViewBefore = '"data-view":i,style:N';
const menuViewAfter = '"data-view":`simple`,style:N';
if (!source.includes(advancedStateBefore) || !source.includes(menuViewBefore)) {
  throw new Error("The advanced-view state wiring was not found.");
}
source = source
  .replace(advancedStateBefore, advancedStateAfter)
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
    },
    null,
    2,
  ),
);

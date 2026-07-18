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
if (sourceArchive === destinationArchive) {
  throw new Error(
    "Source and destination archives must differ; the official installation must not be modified.",
  );
}
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

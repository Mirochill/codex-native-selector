import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import test from "node:test";
import {
  adaptCompactSelector,
  selectSelectorCompatibilityProfile,
  selectorCompatibilityProfiles,
} from "./selector-compatibility.mjs";

const template = readFileSync(
  new URL("./selector-v2.js.txt", import.meta.url),
  "utf8",
).replace(/^\uFEFF/u, "");

function sourceFor(profile) {
  return [
    profile.catalogBefore,
    `function ${profile.componentName}(e){}`,
    `function ${profile.nextComponentName}(e){}`,
    `function ${profile.controlsName}(e){}`,
    `function ${profile.nextControlsName}(e){}`,
    profile.advancedStateBefore,
    ...profile.runtimeMarkers,
  ].join(";");
}

test("selects legacy and current profiles only from their complete fingerprints", () => {
  for (const profile of selectorCompatibilityProfiles) {
    assert.equal(selectSelectorCompatibilityProfile(sourceFor(profile)), profile);
  }
});

test("rejects unknown and ambiguous bundle fingerprints", () => {
  assert.throws(
    () => selectSelectorCompatibilityProfile("function Ve(e){}"),
    /found 0/u,
  );
  assert.throws(
    () =>
      selectSelectorCompatibilityProfile(
        selectorCompatibilityProfiles.map(sourceFor).join(";"),
      ),
    /found 2/u,
  );
});

test("rejects symbol-only drift in every runtime marker", () => {
  for (const profile of selectorCompatibilityProfiles) {
    for (const runtimeMarker of profile.runtimeMarkers) {
      const source = sourceFor(profile).replace(runtimeMarker, "renamed-symbol");
      assert.throws(
        () => selectSelectorCompatibilityProfile(source),
        /found 0/u,
        `${profile.name} should reject a changed marker: ${runtimeMarker}`,
      );
    }
  }
});

test("keeps the legacy selector symbols for the legacy profile", () => {
  const selector = adaptCompactSelector(template, selectorCompatibilityProfiles[0]);

  assert.match(selector, /function Ve\(e\)\{/u);
  assert.match(selector, /\(0,Z\.jsx\)\(Qe,/u);
  assert.match(selector, /\(0,Z\.jsx\)\(tt,/u);
  assert.match(selector, /\(0,Z\.jsx\)\(ye,/u);
});

test("maps Codex 26.707 icons and slider to renderable component symbols", () => {
  const selector = adaptCompactSelector(template, selectorCompatibilityProfiles[1]);

  assert.match(selector, /function He\(e\)\{/u);
  assert.match(selector, /\(0,Ke\.useState\)/u);
  assert.match(selector, /l\?\.find\(at\)/u);
  assert.match(selector, /l\?\.find\(ot\)/u);
  assert.match(selector, /\(0,Z\.jsx\)\(\$e,/u);
  assert.match(selector, /\(0,Z\.jsx\)\(nt,/u);
  assert.match(selector, /\(0,Z\.jsx\)\(be,/u);
  assert.doesNotMatch(selector, /\(0,Z\.jsx\)\((?:Qe|tt|ye),/u);
});

test("fails closed when the selector template no longer matches", () => {
  assert.throws(
    () => adaptCompactSelector("function changed(){}", selectorCompatibilityProfiles[1]),
    /Selector template marker not found/u,
  );
});

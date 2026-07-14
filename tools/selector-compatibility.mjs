export const selectorCompatibilityProfiles = [
  {
    name: "legacy",
    catalogBefore:
      "function K(e){let t=q(ce,e);if(t.length>=4)return t;let n=q(le,e);return n.length>=4?n:[]}",
    catalogAfter: "function K(e){return oe(e)}",
    componentName: "Ve",
    nextComponentName: "He",
    controlsName: "rt",
    nextControlsName: "it",
    advancedStateBefore: "D=(0,ft.useRef)(null),O=i===`advanced`,k=",
    hooksName: "Ge",
    fastPredicate: "it",
    standardPredicate: "at",
    fastIcon: "Qe",
    standardIcon: "tt",
    sliderComponent: "ye",
    runtimeMarkers: [
      "Ge=t(n(),1)",
      "function it(e){let{iconKind:t}=e;return t===`fast`}",
      "function at(e){let{value:t}=e;return t==null}",
      "(0,Q.jsx)(Qe,",
      "(0,Q.jsx)(tt,",
      "(0,Z.jsx)(ye,{active:",
    ],
  },
  {
    name: "codex-26.707",
    catalogBefore:
      "function ae(e,t=!1){let n=K(t?[...q,le]:q,e);if(n.length>=4)return n;let r=K(ue,e);return r.length>=4?r:[]}",
    catalogAfter: "function ae(e,t=!1){return se(e)}",
    componentName: "He",
    nextComponentName: "Ue",
    controlsName: "it",
    nextControlsName: "at",
    advancedStateBefore: "D=(0,pt.useRef)(null),O=i===`advanced`,k=",
    hooksName: "Ke",
    fastPredicate: "at",
    standardPredicate: "ot",
    fastIcon: "$e",
    standardIcon: "nt",
    sliderComponent: "be",
    runtimeMarkers: [
      "Ke=t(n(),1)",
      "function at(e){let{iconKind:t}=e;return t===`fast`}",
      "function ot(e){let{value:t}=e;return t==null}",
      "(0,Q.jsx)($e,",
      "(0,Q.jsx)(nt,",
      "(0,Z.jsx)(be,{active:",
    ],
  },
];

function sourceMarkers(profile) {
  return [
    profile.catalogBefore,
    `function ${profile.componentName}(e){`,
    `function ${profile.nextComponentName}(e){`,
    `function ${profile.controlsName}(e){`,
    `function ${profile.nextControlsName}(e){`,
    profile.advancedStateBefore,
    ...profile.runtimeMarkers,
  ];
}

export function selectSelectorCompatibilityProfile(source) {
  const matches = selectorCompatibilityProfiles.filter((profile) =>
    sourceMarkers(profile).every((marker) => source.includes(marker)),
  );
  if (matches.length !== 1) {
    throw new Error(
      `Expected one selector compatibility profile, found ${matches.length}.`,
    );
  }
  return matches[0];
}

export function adaptCompactSelector(template, profile) {
  const replacements = [
    ["function Ve(e){", `function ${profile.componentName}(e){`],
    ["(0,Ge.", `(0,${profile.hooksName}.`],
    ["l?.find(at)", `l?.find(${profile.standardPredicate})`],
    ["l?.find(it)", `l?.find(${profile.fastPredicate})`],
    ["(0,Z.jsx)(Qe,", `(0,Z.jsx)(${profile.fastIcon},`],
    ["(0,Z.jsx)(tt,", `(0,Z.jsx)(${profile.standardIcon},`],
    ["(0,Z.jsx)(ye,", `(0,Z.jsx)(${profile.sliderComponent},`],
  ];

  let selector = template;
  for (const [before, after] of replacements) {
    if (!selector.includes(before)) {
      throw new Error(`Selector template marker not found: ${before}`);
    }
    selector = selector.replaceAll(before, after);
  }

  return selector;
}

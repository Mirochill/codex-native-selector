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
    jsxRuntime: "Z",
    stylesName: "X",
    portalExpression: "Cp().createPortal",
    controlsJsxRuntime: "Q",
    fieldsBefore: "supportedReasoningEfforts:n})=>{let r=",
    fieldsAfter:
      "supportedReasoningEfforts:n,defaultReasoningEffort:ct})=>{let r=",
    valueBefore: "modelLabel:r,reasoningEffort:e}))})??[]}",
    valueAfter:
      "modelLabel:r,reasoningEffort:e,defaultReasoningEffort:ct}))})??[]}",
    sliderPropsBefore:
      "fastModeEnabled:H,onDragToMax:L,onSelectComplete:a,onSelectPower:z,powerSelections:l,selectedPowerSelection:u,shouldReduceMotion:h,transitionsReady:C",
    sliderPropsAfter:
      "fastModeEnabled:H,onDragToMax:L,onSelectComplete:a,onSelectPower:z,onSelectServiceTier:s,powerSelections:l,selectedPowerSelection:u,serviceTierOptions:f,serviceTierOptionsLoading:p,shouldReduceMotion:h,transitionsReady:C",
    menuViewBefore: '"data-view":i,style:N',
    menuViewAfter: '"data-view":`simple`,style:N',
    reactDomAssetPattern: /^react-dom-.*\.js$/u,
    reactDomImport: (fileName) => `import{t as Cp}from"./${fileName}";`,
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
    jsxRuntime: "Z",
    stylesName: "X",
    portalExpression: "Cp().createPortal",
    controlsJsxRuntime: "Q",
    fieldsBefore: "supportedReasoningEfforts:n})=>{let r=",
    fieldsAfter:
      "supportedReasoningEfforts:n,defaultReasoningEffort:ct})=>{let r=",
    valueBefore: "modelLabel:r,reasoningEffort:e}))})??[]}",
    valueAfter:
      "modelLabel:r,reasoningEffort:e,defaultReasoningEffort:ct}))})??[]}",
    sliderPropsBefore:
      "fastModeEnabled:H,onDragToMax:L,onSelectComplete:a,onSelectPower:z,powerSelections:l,selectedPowerSelection:u,shouldReduceMotion:h,transitionsReady:C",
    sliderPropsAfter:
      "fastModeEnabled:H,onDragToMax:L,onSelectComplete:a,onSelectPower:z,onSelectServiceTier:s,powerSelections:l,selectedPowerSelection:u,serviceTierOptions:f,serviceTierOptionsLoading:p,shouldReduceMotion:h,transitionsReady:C",
    menuViewBefore: '"data-view":i,style:N',
    menuViewAfter: '"data-view":`simple`,style:N',
    reactDomAssetPattern: /^react-dom-.*\.js$/u,
    reactDomImport: (fileName) => `import{t as Cp}from"./${fileName}";`,
    runtimeMarkers: [
      "Ke=t(n(),1)",
      "function at(e){let{iconKind:t}=e;return t===`fast`}",
      "function ot(e){let{value:t}=e;return t==null}",
      "(0,Q.jsx)($e,",
      "(0,Q.jsx)(nt,",
      "(0,Z.jsx)(be,{active:",
    ],
  },
  {
    name: "codex-macos-26.707",
    catalogBefore:
      "function rH(e,t=!1){let n=sH(t?[...cH,lH]:cH,e);if(n.length>=4)return n;let r=sH(uH,e);return r.length>=4?r:[]}",
    catalogAfter: "function rH(e,t=!1){return aH(e)}",
    componentName: "tU",
    nextComponentName: "nU",
    controlsName: "vU",
    nextControlsName: "yU",
    advancedStateBefore:
      "E=(0,OU.useRef)(null),D=i===`advanced`,O=",
    hooksName: "aU",
    fastPredicate: "yU",
    standardPredicate: "bU",
    fastIcon: "pU",
    standardIcon: "gU",
    sliderComponent: "MH",
    jsxRuntime: "oU",
    stylesName: "$H",
    portalExpression: "yu.createPortal",
    controlsJsxRuntime: "CU",
    templateReplacements: [
      ["._SimpleView_15yqt_93", "._SimpleView_1k6l7_47"],
      ["._Range_m3zgh_451", "._Range_3jngk_226"],
      ["._Thumb_m3zgh_23", "._Thumb_3jngk_12"],
      ["._Mask_vx1zu_27", "._Mask_notip_14"],
      ["._Burst_1ibg9_151", "._Burst_1pz9e_76"],
    ],
    assetMarkers: [
      "_Range_3jngk_226",
      "_Thumb_3jngk_12",
      "_Mask_notip_14",
      "_Burst_1pz9e_76",
    ],
    fieldsBefore: "supportedReasoningEfforts:n})=>{let r=",
    fieldsAfter:
      "supportedReasoningEfforts:n,defaultReasoningEffort:d})=>{let r=",
    valueBefore: "modelLabel:r,reasoningEffort:e}))})??[]}",
    valueAfter:
      "modelLabel:r,reasoningEffort:e,defaultReasoningEffort:d}))})??[]}",
    sliderPropsBefore:
      "fastModeEnabled:ae,onDragToMax:I,onSelectComplete:a,onSelectPower:R,powerSelections:l,selectedPowerSelection:u,shouldReduceMotion:h,transitionsReady:S",
    sliderPropsAfter:
      "fastModeEnabled:ae,onDragToMax:I,onSelectComplete:a,onSelectPower:R,onSelectServiceTier:s,powerSelections:l,selectedPowerSelection:u,serviceTierOptions:f,serviceTierOptionsLoading:p,shouldReduceMotion:h,transitionsReady:S",
    menuViewBefore: '"data-view":i,style:M',
    menuViewAfter: '"data-view":`simple`,style:M',
    runtimeMarkers: [
      "aU=t(B(),1)",
      "oU=V()",
      "yu=t(si(),1)",
      "function yU(e){let{iconKind:t}=e;return t===`fast`}",
      "function bU(e){let{value:t}=e;return t==null}",
      "(0,CU.jsx)(pU,",
      "(0,CU.jsx)(gU,",
      "(0,oU.jsx)(MH,{active:",
      "_SimpleView_1k6l7_47",
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
    ["(0,Z.", `(0,${profile.jsxRuntime}.`],
    ["X.", `${profile.stylesName}.`],
    ["Cp().createPortal", profile.portalExpression],
    ...(profile.templateReplacements ?? []),
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

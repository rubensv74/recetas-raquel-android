# Gate 70 — Golden Editorial Dark Mode

## Estado

Technical gate: PASS.

Visual gate: pending screenshot review before Gate 70 is considered visually closed.

## Objective

The dark experience must remain unmistakably Recetoria: warm, culinary, editorial and premium. It must not read as a generic black Material theme.

The rejected near-black direction is not reused.

## Warm espresso hierarchy

| Role | Token | Value |
| --- | --- | --- |
| Background | NightBackground | `#2A211B` |
| Surface | NightSurface | `#332820` |
| Raised surface | NightSurfaceRaised | `#3D3027` |
| High surface | NightSurfaceHigh | `#49392E` |
| Highest surface | NightSurfaceHighest | `#554337` |
| Main text | NightText | `#FAF7F2` |
| Secondary text | NightTextSecondary | `#D8CEC4` |
| Outline | NightOutline | `#A89A8E` |

The Material 3 container levels use distinct warm surfaces so cards, panels, form areas and raised components remain legible as separate layers.

## Golden Editorial accents

Brand gold/bronze remains an accent, not a universal surface treatment:

- Primary: `BronzeLight #E7B36F`
- Secondary: `GoldLight #F2C66D`
- Primary container: `#684118`
- Secondary container: `#60491F`

## Food-safety semantics

Food-safety colors keep semantic precedence over branding:

- Confirmed/review attention: amber family
- May contain/informative: blue family
- Critical/error: red family

The dark theme does not convert these signals to gold.

## Accessibility intent

The principal dark combinations retain strong contrast:

- Ivory text on espresso background.
- Secondary warm text on dark surfaces.
- Dark espresso text on light bronze/gold actions.
- Semantic light blue/red/amber content on dark backgrounds.

Final accessibility review remains part of the dedicated accessibility gate.

## Technical validation

Gate 70 requires:

- `assembleDebug`
- `testDebugUnitTest`
- `lintDebug`
- `compileDebugAndroidTestKotlin`
- full `connectedDebugAndroidTest` while the emulator is explicitly in dark mode

The visual gate remains separate from technical success.
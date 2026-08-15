# Product vision and brand exploration — 2026-08-15

Status: working record. This document captures product decisions and rejected directions; it does not approve a commercial name or final visual identity.

## Current product priority

The immediate priority remains completing and validating the local-first Android application. Cloud services, shared catalogs and commercial expansion are intentionally deferred until the core experience is stable.

The v13 culinary catalog remains governed by the existing food-safety architecture:

- BEDCA is the primary nomenclature reference.
- The catalog targets 800–1,200 practical culinary identities across 20+ categories.
- Regulated allergen relationships remain explicit and source-backed.
- The product must never infer universal “allergen-free” or “safe” labels.
- Common domestic Spanish ingredients must be searchable without manual creation.

## UI decisions validated today

The premium experience branch now includes:

- recipe-category selection through a reusable dropdown;
- ingredient-unit selection through a reusable dropdown;
- a category filter in the recipe catalog;
- corrected cancellation/discard navigation from the editor;
- preservation of the ingredient identity and safety architecture.

The local Android gate reported 84 of 85 tests passing after commit `fcb2186`. The remaining failure was an obsolete UI-test selector: the test still searched for the removed chip `category_Principal`, while the new contract opens `filter_category` and selects `Principal`. The correction updates the test to exercise the current dropdown contract. The deprecated no-argument Material 3 menu anchor is also replaced by an explicit non-editable primary anchor.

## Future capability: Google Drive backup

This is a post-completion feature, not current scope.

A first release should provide manual export and restore of:

- recipes and preparation steps;
- links to catalog ingredients and user-created ingredients;
- photographs;
- favorites, notes and user preferences;
- backup schema version, creation date and integrity checksum.

Restore must validate compatibility and integrity before modifying local data. Backups should be encrypted or clearly document their privacy model. Automatic scheduled backup can follow only after manual backup and restore are proven reliable.

## Future capability: shared household catalog

This is also deferred. The target scenario is a family or private group in which every member owns or is entitled to use the app, but all members share one recipe collection.

It requires:

- individual authentication;
- private groups with invitations and membership lifecycle;
- roles such as owner, editor and viewer;
- a shared database and photograph storage;
- offline-first synchronization;
- deterministic conflict handling;
- change history, deletion recovery and audit attribution;
- separation between purchase entitlement and group membership;
- GDPR/privacy controls, account deletion and data portability;
- operational monitoring, quotas, backups and recurring cloud-cost controls.

A sensible scale path is:

1. completed local application;
2. personal encrypted cloud backup;
3. single-user multi-device synchronization;
4. private shared household spaces;
5. commercial collaboration platform.

Cloud and household sharing create recurring infrastructure costs, so they fit a premium subscription or family plan better than a purely one-time local license.

## Product differentiation opportunities

The strongest positioning is not “another large recipe database”, but a private culinary memory that helps a household preserve, understand and safely cook its own recipes.

Potential differentiators:

- provenance for each recipe: author, family branch, occasion, dates, photographs and voice notes;
- assisted digitization of handwritten recipes with mandatory human review;
- linked recipe variants instead of uncontrolled duplicates;
- household dietary profiles backed by traceable evidence, without promising absolute safety;
- responsible substitution suggestions that explain culinary and allergen implications;
- an adaptive cooking mode for hands-busy use, timers and step progression;
- post-cooking history: what changed, outcome, notes and preferred version.

Features not considered distinctive priorities: public social feeds, sheer recipe volume, generic calorie counting, generic chat-based AI, chef hats, pots or other conventional recipe-app decoration.

## Naming exploration

The internal project name “Recetas de Raquel” does not have to become the commercial brand.

Names discussed:

- **Lumbre**: emotionally strong but commercially crowded across restaurants, hospitality, creative/digital services and software; rejected as the leading option.
- **Sabores de Casa**: warm and immediately understandable, but highly descriptive; better as a descriptor than as the sole protectable mark.
- **Sazonario — Sabores de Casa**: expressive candidate, not approved.
- **Recetoria — Recetas con historia**: current working experiment, not approved.
- **Mesa y Memoria** and **Recetario Vivo**: useful positioning references, not final selections.

Before adopting any name, perform current clearance checks in OEPM, EUIPO, domains and app stores. Visual exploration must not be confused with legal availability.

## Visual identity: constraints and feedback

Provisional palette:

| Role | Color |
|---|---|
| Ivory background | `#F7F3EC` |
| Espresso text | `#292621` |
| Copper accent | `#A65F35` |
| Olive support | `#788064` |
| Linen surface | `#E8DDD1` |

The desired identity is modern, warm, premium and domestic, while remaining legible as a 48 px Android icon.

Rejected directions:

- classical or serif “R” monograms: perceived as obsolete;
- abstract house/page or doorway symbols: insufficiently understandable;
- generic open-book concepts;
- stacked recipe-card symbol and its second variant;
- generic restaurant language: chef hats, pots, cutlery and plates;
- symbols that require a verbal explanation before their meaning is visible.

The next exploration must present genuinely different systems, not cosmetic variants of the rejected card stack. It should compare immediate recognition, distinctiveness, small-size behavior and ability to extend to launcher icon, splash screen and wordmark.

## Decision status

- Product name: open.
- Descriptor: open; “Recetas con historia” and “Sabores de Casa” remain useful.
- Logo: open; no generated proposal is production-approved.
- Splash and launcher assets: wait for an approved identity.
- Google Drive backup: recorded, deferred.
- Shared household catalog: recorded, deferred.
- Current engineering gate: dropdown test correction and Android verification.

# DATA QUALITY POLICY

Every safety relation requires:

```text
sourceId
evidenceLevel
relationType
reviewedAt
```

Evidence levels:

```kotlin
enum class EvidenceLevel {
    EU_LEGAL,
    OFFICIAL_SCIENTIFIC,
    OFFICIAL_HEALTH_AUTHORITY,
    MANUFACTURER_LABEL,
    USER_DECLARED,
    UNVERIFIED
}
```

Relation types:

```kotlin
enum class AllergenRelationType {
    INHERENT_SOURCE,
    CONTAINS,
    DERIVED_FROM,
    REGULATED_COMPONENT,
    DECLARED_MAY_CONTAIN,
    POSSIBLE_CROSS_REACTIVITY,
    UNKNOWN
}
```

## Forbidden inference

No clinical relation from fuzzy string match, family/botanical similarity, similar trade name, common culinary use or undocumented model knowledge.

## Legacy migration

Only deterministic and unambiguous mappings may link automatically. Anything ambiguous becomes a custom ingredient. Data loss target: 0.

## Search normalization

Allowed: trim, lowercase, Unicode normalization and a separate accent-insensitive search key. Do not use aggressive stemming or remove qualifying words for identity resolution.

## Processing and exemptions

No global `processed -> less allergenic` rule. EFSA evaluates residual allergenicity/exemptions case-by-case. Exemptions must be explicit, source-backed and versioned.

## UNVERIFIED

Must remain queryable, must not be promoted to official evidence and must never support an absolute safety claim.

## Language

Forbidden: `Safe recipe`, `Allergen free`, `Suitable for allergy sufferers`, `No risk`, `You can eat it`, `100% safe`.

Preferred: `Contains…`, `Derived from…`, `Manufacturer declares may contain…`, `Possible cross-reactivity documented…`, `Requires review`, `Composition may vary`, `Information incomplete`.

Absence of a relation is never proof of absence of risk.

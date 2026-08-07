# EXTENDED ALLERGEN TAXONOMY

EU Annex II is not a complete list of food allergies.

Official evidence supporting extension:

- AESAN notes common allergies can involve fruits and other foods outside Annex II.
- EFSA describes substantial adult reactions involving some Rosaceae fruits and Apiaceae vegetables.
- FAO/WHO Part 5 evaluates regional/national priority allergens that are not all EU Annex II entries; examples include buckwheat and pine nut.
- WHO/IUIS maintains a much broader molecular allergen nomenclature.

## Layers

1. `EU_ANNEX_II` — regulated EU group.
2. `OFFICIALLY_DOCUMENTED_NON_ANNEX_II` — food/allergen supported by a suitable official source but outside Annex II.
3. Molecular nomenclature — WHO/IUIS reference data only.
4. `USER_DECLARED` / `UNVERIFIED` — separate from official evidence.

Critical rule:

```text
WHO/IUIS entry != clinical significance != CONTAINS != food prohibition
```

Cross-reactivity is never inferred from taxonomy alone. Every `POSSIBLE_CROSS_REACTIVITY` relation needs a specific source and never aggregates as confirmed presence.

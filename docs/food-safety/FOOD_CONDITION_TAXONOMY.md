# FOOD CONDITION TAXONOMY

```kotlin
enum class FoodSafetyConditionType {
    FOOD_ALLERGY,
    FOOD_INTOLERANCE,
    CELIAC_DISEASE,
    NON_CELIAC_SENSITIVITY,
    OTHER_MEDICALLY_INDICATED_RESTRICTION
}
```

- `FOOD_ALLERGY`: immune-mediated adverse reaction. AESAN distinguishes IgE and non-IgE mechanisms.
- `FOOD_INTOLERANCE`: separate from allergy; AESAN uses lactose intolerance as a clear example in which the immune mechanism is not the same.
- `CELIAC_DISEASE`: keep separate from wheat allergy and non-coeliac sensitivity.
- `NON_CELIAC_SENSITIVITY`: separate concept; do not silently convert it into a universal blacklist.
- `OTHER_MEDICALLY_INDICATED_RESTRICTION`: extension point only; do not bulk-populate without dedicated evidence review.

Preferences must be separate: vegetarianism, veganism, religious restrictions, tastes, personal preferences and non-medical diets.

Regulation 828/2014 governs food claims: `gluten-free` <= 20 mg/kg; `very low gluten` <= 100 mg/kg under defined conditions. Oats have additional contamination controls. These are food-information rules, not personalised clinical conclusions.

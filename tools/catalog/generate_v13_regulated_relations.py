#!/usr/bin/env python3
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
BASE = ROOT / "app/src/main/assets/ingredient-catalog/v13"
REVIEWED_AT = "2026-08-11"
SOURCE_ID = "EU_FIC_1169_2011"

DAIRY_VARIANTS = {
    "ing-cow-milk", "ing-goat-milk", "ing-sheep-milk", "ing-whole-milk",
    "ing-semi-skimmed-milk", "ing-skimmed-milk", "ing-lactose-free-milk",
}
DAIRY_INHERENT = DAIRY_VARIANTS | {
    "ing-evaporated-milk", "ing-condensed-milk", "ing-milk-powder",
}
EGG_INHERENT = {"ing-chicken-egg", "ing-quail-egg", "ing-duck-egg"}
CRUSTACEANS = {
    "ing-shrimp", "ing-prawn", "ing-king-prawn", "ing-scampi", "ing-lobster",
    "ing-european-lobster", "ing-crab", "ing-spider-crab", "ing-velvet-crab",
    "ing-crayfish", "ing-goose-barnacle",
}
FISH_FORMS = {
    "ing-hake-loin": ("ing-hake", "FORM_OF"),
    "ing-cod-loin": ("ing-cod", "FORM_OF"),
    "ing-salmon-fillet": ("ing-salmon", "FORM_OF"),
    "ing-tuna-loin": ("ing-tuna", "FORM_OF"),
    "ing-smoked-salmon": ("ing-salmon", "DERIVED_FROM"),
}


def load(name):
    return json.loads((BASE / name).read_text(encoding="utf-8"))


def dump(name, value):
    (BASE / name).write_text(
        json.dumps(value, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
    )


def safety_relation(ingredient_id, group_id, relation_type):
    return {
        "id": f"rel-v13-{ingredient_id.removeprefix('ing-')}",
        "ingredientId": ingredient_id,
        "safetyGroupId": group_id,
        "relationType": relation_type,
        "evidenceLevel": "EU_LEGAL",
        "sourceId": SOURCE_ID,
        "notes": None,
        "reviewedAt": REVIEWED_AT,
    }


def lineage_relation(ingredient_id, parent_id, relation_type):
    return {
        "id": f"lin-v13-{ingredient_id.removeprefix('ing-')}",
        "childIngredientId": ingredient_id,
        "parentIngredientId": parent_id,
        "relationType": relation_type,
        "reviewedAt": REVIEWED_AT,
        "sourceReference": None,
        "notes": "Linaje culinario curado para v13; no propaga relaciones de seguridad ni exenciones.",
        "isActive": True,
    }


def main():
    dairy_eggs = load("ingredients-culinary-dairy-eggs-v13.json")
    aquatic = load("ingredients-culinary-aquatic-v13.json")
    additions = dairy_eggs + aquatic
    assert len(dairy_eggs) == 42, len(dairy_eggs)
    assert len(aquatic) == 58, len(aquatic)
    assert len(additions) == 100
    assert len({item["id"] for item in additions}) == 100

    new_safety = []
    new_lineage = []

    for item in dairy_eggs:
        iid = item["id"]
        category = item["categoryId"]
        if category == "cat-dairy-alternatives":
            safety_type = "INHERENT_SOURCE" if iid in DAIRY_INHERENT else "DERIVED_FROM"
            lineage_type = "VARIANT_OF" if iid in DAIRY_VARIANTS else "DERIVED_FROM"
            new_safety.append(safety_relation(iid, "sg-eu-milk", safety_type))
            new_lineage.append(lineage_relation(iid, "ing-milk", lineage_type))
        elif category == "cat-eggs":
            safety_type = "INHERENT_SOURCE" if iid in EGG_INHERENT else "DERIVED_FROM"
            lineage_type = "VARIANT_OF" if iid in EGG_INHERENT else "DERIVED_FROM"
            new_safety.append(safety_relation(iid, "sg-eu-eggs", safety_type))
            new_lineage.append(lineage_relation(iid, "ing-egg", lineage_type))
        else:
            raise AssertionError(f"Unexpected category for {iid}: {category}")

    for item in aquatic:
        iid = item["id"]
        category = item["categoryId"]
        if category == "cat-fish":
            if iid in FISH_FORMS:
                parent, lineage_type = FISH_FORMS[iid]
                new_safety.append(safety_relation(iid, "sg-eu-fish", "DERIVED_FROM"))
                new_lineage.append(lineage_relation(iid, parent, lineage_type))
            else:
                new_safety.append(safety_relation(iid, "sg-eu-fish", "INHERENT_SOURCE"))
                new_lineage.append(lineage_relation(iid, "ing-fish", "VARIANT_OF"))
        elif category == "cat-seafood":
            if iid in CRUSTACEANS:
                new_safety.append(safety_relation(iid, "sg-eu-crustaceans", "INHERENT_SOURCE"))
                new_lineage.append(lineage_relation(iid, "ing-crustaceans", "VARIANT_OF"))
            else:
                new_safety.append(safety_relation(iid, "sg-eu-molluscs", "INHERENT_SOURCE"))
                new_lineage.append(lineage_relation(iid, "ing-molluscs", "VARIANT_OF"))
        else:
            raise AssertionError(f"Unexpected category for {iid}: {category}")

    assert len(new_safety) == 100
    assert len(new_lineage) == 100
    assert len({x["id"] for x in new_safety}) == 100
    assert len({x["id"] for x in new_lineage}) == 100

    existing_safety = load("safety-relations.json")
    existing_ids = {x["id"] for x in existing_safety}
    existing_safety = [x for x in existing_safety if not x["id"].startswith("rel-v13-")]
    assert len(existing_safety) == 33, len(existing_safety)
    assert not ({x["id"] for x in new_safety} & {x["id"] for x in existing_safety})
    dump("safety-relations.json", existing_safety + new_safety)
    dump("ingredient-relations-culinary-regulated-v13.json", new_lineage)

    manifest_path = BASE / "manifest.json"
    manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
    manifest["catalogVersion"] = 13
    manifest["reviewedAt"] = REVIEWED_AT
    ingredient_shards = manifest["files"]["ingredientShards"]
    for name in [
        "ingredients-culinary-dairy-eggs-v13.json",
        "ingredients-culinary-aquatic-v13.json",
    ]:
        if name not in ingredient_shards:
            ingredient_shards.append(name)
    relation_shards = manifest["files"]["ingredientRelationShards"]
    relation_name = "ingredient-relations-culinary-regulated-v13.json"
    if relation_name not in relation_shards:
        relation_shards.append(relation_name)

    counts = manifest["counts"]
    counts["ingredients"] = sum(len(load(name)) for name in ingredient_shards)
    counts["aliases"] = sum(len(load(name)) for name in manifest["files"]["aliasShards"])
    counts["ingredientRelations"] = sum(len(load(name)) for name in relation_shards)
    counts["safetyRelations"] = len(load(manifest["files"]["safetyRelations"]))
    counts["regulatoryExemptions"] = sum(
        len(load(name)) for name in manifest["files"]["regulatoryExemptionShards"]
    )
    counts["categories"] = len(load(manifest["files"]["categories"]))
    counts["safetyGroups"] = len(load(manifest["files"]["safetyGroups"]))
    counts["safetySources"] = len(load(manifest["files"]["safetySources"]))

    assert counts["ingredients"] == 554, counts
    assert counts["aliases"] == 294, counts
    assert counts["ingredientRelations"] == 143, counts
    assert counts["safetyRelations"] == 133, counts
    assert counts["regulatoryExemptions"] == 20, counts

    manifest_path.write_text(
        json.dumps(manifest, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
    )
    print(json.dumps(counts, ensure_ascii=False, sort_keys=True))


if __name__ == "__main__":
    main()

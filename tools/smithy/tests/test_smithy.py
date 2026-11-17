"""Checks the committed smithy.toml and the generated datapack against the export."""

from __future__ import annotations

import json
import unittest
from pathlib import Path

import packexport
import tomllib

from smithy import __main__ as smithy
from smithy import export as smithy_export

HERE = Path(__file__).resolve().parent.parent
EXPORT = HERE.parent.parent / "build" / "datapack-export"
DATAPACK = HERE / "datapack"


class Ledger(unittest.TestCase):
    """smithy.toml, the single source of truth."""

    def setUp(self) -> None:
        self.pack = packexport.load(EXPORT)
        self.raw = tomllib.loads((HERE / "smithy.toml").read_text())
        self.converts = self.raw["convert"]
        self.keeps = self.raw["keep"]
        self.tagged = smithy_export.tagged_items(self.pack)

    def test_every_tagged_item_is_listed(self) -> None:
        unlisted = [item for item in self.tagged if item not in self.converts and item not in self.keeps]
        self.assertEqual(unlisted, [])

    def test_listed_items_are_tagged(self) -> None:
        untagged = [item for item in self.converts if item not in self.tagged]
        self.assertEqual(untagged, [])

    def test_every_conversion_names_a_tool_definition(self) -> None:
        definitions = smithy.tool_definitions(EXPORT)
        for item, made in self.converts.items():
            self.assertIn(made["tool"], definitions, item)

    def test_every_conversion_names_materials_that_exist(self) -> None:
        materials = self._material_ids()
        for item, made in self.converts.items():
            self.assertGreater(len(made["materials"]), 0, item)
            for material in made["materials"]:
                self.assertIn(material, materials, f"{item}: {material}")

    def _material_ids(self) -> frozenset[str]:
        materials: set[str] = set()
        for namespace in ("tconstruct", "tcintegrations"):
            folder = EXPORT / "data" / namespace / "tinkering" / "materials" / "definition"
            materials |= {f"{namespace}:{file.with_suffix('').name}" for file in folder.glob("*.json")}
        # The new materials live in the mod, which the export predates.
        mod = HERE.parent.parent / "mods" / "smithy" / "src" / "main" / "resources" / "data" \
            / "feastcraft_smithy" / "tinkering" / "materials" / "definition"
        materials |= {f"feastcraft_smithy:{file.with_suffix('').name}" for file in mod.glob("*.json")}
        return frozenset(materials)


class Datapack(unittest.TestCase):
    """The generated datapack, which is committed."""

    def setUp(self) -> None:
        self.pack = packexport.load(EXPORT)
        self.disabled = set(tomllib.loads((HERE / "smithy.toml").read_text())["convert"])

    def test_disabled_tag_lists_every_conversion(self) -> None:
        tag_file = DATAPACK / "data" / "feastcraft_smithy" / "tags" / "items" / "disabled.json"
        values = json.loads(tag_file.read_text())["values"]
        self.assertEqual(set(values), self.disabled)

    def test_every_conversion_has_a_file(self) -> None:
        for item in self.disabled:
            namespace, _, name = item.partition(":")
            self.assertTrue((DATAPACK / "data" / "feastcraft_smithy" / "smithy" / "conversions"
                             / namespace / f"{name}.json").exists(), item)

    def test_live_disabled_results_are_overridden(self) -> None:
        """A live recipe whose result is disabled carries a `forge:false` override."""
        overridden = self._overrides()
        for recipe in self.pack.recipes:
            if recipe.result not in self.disabled:
                continue
            with self.subTest(recipe=recipe.id):
                self.assertIn(recipe.id, overridden)

    def test_live_recipes_dont_name_disabled_items(self) -> None:
        """A live recipe that consumes a disabled item is rewritten in the datapack.

        Tinkers melting and casting recipes are exempt: they are how a retired
        item becomes ingots.
        """
        for recipe in self.pack.recipes:
            if recipe.result in self.disabled or recipe.type.startswith("tconstruct:"):
                continue
            namespace, _, path = recipe.id.partition(":")
            source = EXPORT / "data" / namespace / "recipes" / f"{path}.json"
            if not source.exists():
                continue
            written = DATAPACK / "data" / namespace / "recipes" / f"{path}.json"
            current = json.loads(written.read_text()) if written.exists() else json.loads(source.read_text())
            items = [item for _, item in smithy_export.recipe_items(current)]
            with self.subTest(recipe=recipe.id):
                self.assertFalse(self.disabled & set(items))

    def _overrides(self) -> set[str]:
        overridden: set[str] = set()
        for file in (DATAPACK / "data").glob("*/recipes/**/*.json"):
            recipe = json.loads(file.read_text())
            if recipe.get("conditions") == [{"type": "forge:false"}]:
                relative = file.relative_to(DATAPACK / "data")
                # data/<namespace>/recipes/<path>.json, the path may hold slashes.
                namespace = relative.parts[0]
                path = "/".join(relative.parts[2:]).removesuffix(".json")
                overridden.add(f"{namespace}:{path}")
        return overridden


if __name__ == "__main__":
    unittest.main()

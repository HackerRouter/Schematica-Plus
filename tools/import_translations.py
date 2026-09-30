import argparse
import json
from pathlib import Path


VERSIONS = {"litematica": "26.1.2-0.27.8", "malilib": "26.1.2-0.28.8"}
OUTPUT = Path(__file__).resolve().parents[1] / "src/main/resources/assets/schematica_plus_litematica/lang"


def unique_entries(pairs):
    entries = {}
    for key, value in pairs:
        if key in entries:
            raise ValueError(f"Duplicate translation key: {key}")
        if not isinstance(value, str) or any(c in key for c in "=\r\n"):
            raise ValueError(f"Invalid translation: {key}")
        entries[key] = value
    return entries


def legacy_locale(code):
    language, separator, region = code.partition("_")
    return language + separator + region.upper()


def escape(value):
    return value.replace("\\", "\\\\").replace("\r", "\\r").replace("\n", "\\n").replace("\t", "\\t")


def convert(roots):
    locales = {}
    for mod, root in roots.items():
        directory = root / f"src/main/resources/assets/{mod}/lang"
        files = sorted(directory.glob("*.json"))
        if not (directory / "en_us.json").is_file():
            raise ValueError(f"Missing English translations: {directory}")
        for file in files:
            entries = json.loads(file.read_text(encoding="utf-8"), object_pairs_hook=unique_entries)
            locale = locales.setdefault(legacy_locale(file.stem), {})
            for key, value in entries.items():
                if key in locale:
                    raise ValueError(f"Unexpected or conflicting key in {file}: {key}")
                locale[key] = value
    header = "# SPDX-License-Identifier: LGPL-3.0-only\n"
    header += "# " + "; ".join(f"{mod} {version}" for mod, version in VERSIONS.items()) + ".\n"
    header += "# Converted for Minecraft 1.7.10 by HackerRouter, 2026. Regenerate with tools/import_translations.py.\n"
    return {
        code + ".lang": (header + "".join(f"{key}={escape(value)}\n" for key, value in sorted(entries.items()))).encode("utf-8")
        for code, entries in sorted(locales.items())
    }


def main():
    parser = argparse.ArgumentParser(description="Import all upstream Litematica and MaLiLib translations without renaming keys.")
    for mod in VERSIONS:
        parser.add_argument("--" + mod, type=Path, required=True, help="Upstream source root")
    parser.add_argument("--check", action="store_true", help="Check generated files without writing")
    args = parser.parse_args()
    files = convert({mod: getattr(args, mod) for mod in VERSIONS})
    stale = {file.name for file in OUTPUT.glob("*.lang")} - files.keys()
    if stale:
        parser.error("Unexpected language files; review before removing: " + ", ".join(sorted(stale)))
    if args.check:
        different = [name for name, data in files.items() if not (OUTPUT / name).is_file() or (OUTPUT / name).read_bytes() != data]
        if different:
            parser.exit(1, "Translations need regeneration: " + ", ".join(different) + "\n")
    else:
        OUTPUT.mkdir(parents=True, exist_ok=True)
        for name, data in files.items():
            (OUTPUT / name).write_bytes(data)
    print(f"{'Checked' if args.check else 'Imported'} {len(files)} language files.")


if __name__ == "__main__":
    main()

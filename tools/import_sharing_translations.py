import argparse
import json
import re
from pathlib import Path


SOURCES = {
    "syncmatica": ("syncmatica", "syncmatica.", "sakura-ryoko/syncmatica 26.3-0.3.20"),
    "schematic_synchronizer": ("schematic-synchronizer", "schematic_synchronizer.", "xiader-45/Schematic-Synchronizer 26.2"),
}
ROOT = Path(__file__).resolve().parent
OUTPUT = ROOT.parent / "src/main/resources/assets/schematica_plus_sharing/lang"
SUPPLEMENTS = ROOT / "translations"


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


def repair_placeholders(value):
    return re.sub(r"(?<!%)%(?=\u00a7|$)", "%s", value.replace("{}", "%s"))


def escape(value):
    return value.replace("\\", "\\\\").replace("\r", "\\r").replace("\n", "\\n").replace("\t", "\\t")


def read(file, code, prefix, locales, supplement):
    entries = json.loads(file.read_text(encoding="utf-8"), object_pairs_hook=unique_entries)
    locale = locales.setdefault(legacy_locale(code), {})
    for key, value in entries.items():
        if not key.startswith(prefix):
            continue
        if key in locale and not supplement:
            raise ValueError(f"Conflicting key in {file}: {key}")
        if supplement and key in locale:
            continue
        locale[key] = repair_placeholders(value)


def convert(roots):
    locales = {}
    for mod, root in roots.items():
        assets, prefix, _ = SOURCES[mod]
        directory = root / f"src/main/resources/assets/{assets}/lang"
        if not (directory / "en_us.json").is_file():
            raise ValueError(f"Missing English translations: {directory}")
        for file in sorted(directory.glob("*.json")):
            read(file, file.stem, prefix, locales, False)
        for file in sorted(SUPPLEMENTS.glob(f"{mod}_*.json")):
            read(file, file.stem[len(mod) + 1:], prefix, locales, True)
    header = "# SPDX-License-Identifier: CC0-1.0\n"
    header += "# " + "; ".join(source for _, _, source in SOURCES.values()) + ".\n"
    header += "# Converted for Minecraft 1.7.10 by HackerRouter, 2026. Regenerate with tools/import_sharing_translations.py.\n"
    return {
        code + ".lang": (header + "".join(f"{key}={escape(value)}\n" for key, value in sorted(entries.items()))).encode("utf-8")
        for code, entries in sorted(locales.items())
    }


def main():
    parser = argparse.ArgumentParser(description="Import the Syncmatica and Schematic Synchronizer translations without renaming keys.")
    parser.add_argument("--syncmatica", type=Path, required=True, help="Upstream source root")
    parser.add_argument("--schematic-synchronizer", dest="schematic_synchronizer", type=Path, required=True, help="Upstream source root")
    parser.add_argument("--check", action="store_true", help="Check generated files without writing")
    args = parser.parse_args()
    files = convert({mod: getattr(args, mod) for mod in SOURCES})
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

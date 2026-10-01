#!/usr/bin/env python3
"""Generates assets/schematica/data/legacy_block_states.txt from the Minecraft 1.13.2 block state flattening table.

The table maps every 1.12 block state (name + all properties, as 1.12 Litematica writes them) to its numeric
id << 4 | meta and to the flattened 1.13 state. Modern Litematica converts .litematic files of version < 5 with the
same table, so writing states exactly as listed keeps them readable there and in 1.12.2 Litematica.

Usage: tools/generate_legacy_block_states.py <work dir>   (needs network access, a JDK with javap and unzip)
Output line format: idMeta TAB new_state TAB old_state [TAB old_state ...], states as name[key=value,...].
For each idMeta the first old state of its first line is the one written on export.
"""
import json
import os
import re
import subprocess
import sys
import urllib.request

MANIFEST = "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json"
OUTPUT = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "assets", "schematica", "data",
                      "legacy_block_states.txt")


def fetch_json(url):
    with urllib.request.urlopen(url) as response:
        return json.load(response)


def server_jar(work):
    path = os.path.join(work, "server-1.13.2.jar")
    if not os.path.exists(path):
        version = next(v for v in fetch_json(MANIFEST)["versions"] if v["id"] == "1.13.2")
        urllib.request.urlretrieve(fetch_json(version["url"])["downloads"]["server"]["url"], path)
    return path


def flattening_class(work, jar):
    classes = os.path.join(work, "classes")
    os.makedirs(classes, exist_ok=True)
    subprocess.run(["unzip", "-q", "-o", jar, "*.class", "-d", classes], check=True)
    best, count = None, 0
    for name in os.listdir(classes):
        if not name.endswith(".class"):
            continue
        with open(os.path.join(classes, name), "rb") as handle:
            found = handle.read().count(b"Properties:{")
        if found > count:
            best, count = name, found
    return os.path.join(classes, best)


def register_calls(class_file):
    code = subprocess.run(["javap", "-p", "-c", "-constants", class_file], check=True, capture_output=True, text=True).stdout
    lines = code.split("\n")
    start = next(i for i, line in enumerate(lines) if line.strip() == "static {};")
    entries, state, number, new, olds = [], "idle", 0, None, []
    for line in lines[start:]:
        match = re.match(r"\s*\d+: (\w+)\s*(.*)", line)
        if not match:
            continue
        op, arg = match.groups()
        if op in ("sipush", "bipush") or op.startswith("iconst_"):
            value = int(arg.split()[0]) if op in ("sipush", "bipush") else (-1 if op == "iconst_m1" else int(op[7:]))
            if state == "idle":
                number, state = value, "int"
            continue
        if op in ("ldc", "ldc_w"):
            text = re.search(r"// String (.*)$", line).group(1).replace("\\'", "'")
            if state == "int":
                new, olds, state = text, [], "new"
            elif state == "arr":
                olds.append(text)
            continue
        if op == "anewarray" and state == "new":
            state = "arr"
            continue
        if op == "invokestatic" and "(ILjava/lang/String;[Ljava/lang/String;)V" in line and state == "arr":
            entries.append((number, new, olds))
            state = "idle"
            continue
        if op not in ("dup", "aastore") and state != "arr":
            state = "idle"
    return entries


def parse(snbt):
    match = re.fullmatch(r"\{Name:'([^']+)'(?:,Properties:\{(.*)\})?\}", snbt)
    if not match:
        raise ValueError(snbt)
    props = re.findall(r"(\w+):'([^']*)'", match.group(2) or "")
    return match.group(1), props


def state(snbt):
    name, props = parse(snbt)
    return name + ("[" + ",".join(k + "=" + v for k, v in sorted(props)) + "]" if props else "")


def preferred(new, olds):
    """The old state sharing the most property values with the flattened state, then the one with the most
    unset-looking values, so actual-state properties (stair shape, connections, pot contents) stay at defaults."""
    target = set(parse(new)[1])
    plain = {"false", "none", "empty", "straight"}
    return max(olds, key=lambda old: (len(target & set(parse(old)[1])),
                                      sum(value in plain for _, value in parse(old)[1]), -olds.index(old)))


def main():
    work = sys.argv[1] if len(sys.argv) > 1 else "build/legacy-states"
    os.makedirs(work, exist_ok=True)
    entries = register_calls(flattening_class(work, server_jar(work)))
    seen, lines = set(), []
    for number, new, olds in entries:
        if not olds:
            continue
        if number not in seen:
            seen.add(number)
            first = preferred(new, olds)
            olds = [first] + [old for old in olds if old != first]
        lines.append("\t".join([str(number), state(new)] + [state(old) for old in olds]))
    os.makedirs(os.path.dirname(OUTPUT), exist_ok=True)
    with open(OUTPUT, "w", encoding="utf-8", newline="\n") as handle:
        handle.write("# Generated by tools/generate_legacy_block_states.py from Minecraft 1.13.2 block state flattening data.\n")
        handle.write("\n".join(lines) + "\n")
    print(len(lines), "lines,", len(seen), "id/meta values")


if __name__ == "__main__":
    main()

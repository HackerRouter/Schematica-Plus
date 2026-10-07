#!/usr/bin/env python3
"""Client mod list of a GTNH release from a DreamAssemblerXXL checkout.

  mod_list.py DAXXL_DIR 2.9.0-RC-2 mods.json

DAXXL: https://github.com/GTNewHorizons/DreamAssemblerXXL (releases/manifests/<version>.json and gtnh-assets.json).
"""
import json
import os
import sys

daxxl, version, out = sys.argv[1:4]
assets = json.load(open(os.path.join(daxxl, 'gtnh-assets.json')))
manifest = json.load(open(os.path.join(daxxl, 'releases', 'manifests', version + '.json')))
by_name = {m['name']: m for m in assets['mods']}
mods, missing = [], []
for name, info in {**manifest['github_mods'], **manifest['external_mods']}.items():
    entry = next((v for v in by_name.get(name, {}).get('versions', []) if v['version_tag'] == info['version']), None)
    if entry is None:
        missing.append((name, info['version']))
        continue
    mods.append({'name': name, 'version': info['version'], 'side': info['side'], 'url': entry.get('download_url') or '',
                 'browser_url': entry.get('browser_download_url') or '', 'filename': entry.get('filename')})
json.dump(mods, open(out, 'w'), indent=1)
print(len(mods), 'mods; not in gtnh-assets.json:', missing)

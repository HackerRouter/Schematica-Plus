#!/usr/bin/env python3
"""Downloads the client mods listed by mod_list.py.   get_mods.py mods.json MODS_DIR

GitHub API downloads are rate limited without a token, so GTNH mods are taken from the GTNH Maven (nexus) first,
then the release asset. Anything still failing is printed; fetch it by hand (see the handout).
"""
import concurrent.futures
import json
import os
import sys
import urllib.request

NEXUS = 'https://nexus.gtnewhorizons.com/repository/public/com/github/GTNewHorizons/{n}/{v}/{n}-{v}.jar'
mods, out = json.load(open(sys.argv[1])), sys.argv[2]
os.makedirs(out, exist_ok=True)


def fetch(url, dest, headers=None):
    request = urllib.request.Request(url, headers=headers or {'User-Agent': 'Mozilla/5.0'})
    with urllib.request.urlopen(request, timeout=120) as response, open(dest + '.part', 'wb') as f:
        while True:
            block = response.read(1 << 16)
            if not block:
                break
            f.write(block)
    os.replace(dest + '.part', dest)


def one(mod):
    if mod['side'] == 'SERVER':
        return mod['name'], 'server only'
    dest = os.path.join(out, mod['filename'] or mod['name'] + '-' + mod['version'] + '.jar')
    if os.path.exists(dest):
        return mod['name'], 'present'
    tries = []
    if 'api.github.com' in mod['url']:
        tries.append((NEXUS.format(n=mod['name'], v=mod['version']), None))
        tries.append((mod['url'], {'Accept': 'application/octet-stream', 'User-Agent': 'Mozilla/5.0'}))
    elif mod['url']:
        tries.append((mod['url'], None))
    if mod.get('browser_url'):
        tries.append((mod['browser_url'], None))
    error = 'no url'
    for url, headers in tries:
        try:
            fetch(url, dest, headers)
            return mod['name'], 'ok ' + url.split('/')[2]
        except Exception as e:
            error = str(e)
    return mod['name'], 'FAIL ' + error


with concurrent.futures.ThreadPoolExecutor(8) as pool:
    for name, status in pool.map(one, mods):
        print(name, status, flush=True)

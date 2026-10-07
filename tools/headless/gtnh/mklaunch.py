#!/usr/bin/env python3
"""Java argument file for a GTNH instance from lwjgl3ify's MultiMC patches (no launcher needed).

  mklaunch.py MMC_DIR LIBS_DIR GAME_DIR ASSETS_DIR OUT_ARGS [--xmx 8G]

MMC_DIR: lwjgl3ify-<version>-multimc.zip unpacked (patches/*.json, libraries/). Libraries are downloaded into LIBS_DIR
(Maven layout). ASSETS_DIR: a Minecraft 1.7.10 asset tree, e.g. ~/.gradle/caches/retro_futura_gradle/assets after
the development client ran once. Later patches replace a library with the same group:artifact(:classifier), as MultiMC
does; listing both Guava 15 and 17 breaks Forge's access transformers.
"""
import argparse
import json
import os
import urllib.request

parser = argparse.ArgumentParser()
for name in ('mmc', 'libs', 'game', 'assets', 'out'):
    parser.add_argument(name)
parser.add_argument('--xmx', default='8G')
args = parser.parse_args()

patches = [json.load(open(os.path.join(args.mmc, 'patches', f))) for f in os.listdir(os.path.join(args.mmc, 'patches'))]
patches.sort(key=lambda p: p.get('order', 0))


def allowed(lib):
    result = not lib.get('rules')
    for rule in lib.get('rules', []):
        system = rule.get('os', {}).get('name')
        if system is None or system == 'linux':
            result = rule['action'] == 'allow'
    return result


def path_of(name):
    parts = name.split(':')
    classifier = '-' + parts[3] if len(parts) > 3 else ''
    return '%s/%s/%s/%s-%s%s.jar' % (parts[0].replace('.', '/'), parts[1], parts[2], parts[1], parts[2], classifier)


def key_of(name):
    parts = name.split(':')
    return ':'.join(parts[:2] + parts[3:])


def get(url, dest):
    if os.path.exists(dest):
        return
    os.makedirs(os.path.dirname(dest), exist_ok=True)
    try:
        with urllib.request.urlopen(urllib.request.Request(url, headers={'User-Agent': 'Mozilla/5.0'}), timeout=120) as r:
            open(dest + '.part', 'wb').write(r.read())
        os.replace(dest + '.part', dest)
    except Exception as e:
        print('could not download', url, e)


classpath, jvm, tweakers, main, game_args, main_jar = {}, [], [], None, None, None
for patch in patches:
    for lib in patch.get('-libraries', []):
        classpath.pop(key_of(lib['name']), None)
    for lib in patch.get('libraries', []) + patch.get('+libraries', []):
        if not allowed(lib):
            continue
        rel = path_of(lib['name'])
        if lib.get('MMC-hint') == 'local':
            dest = os.path.join(args.mmc, 'libraries', os.path.basename(rel))
        else:
            dest = os.path.join(args.libs, rel)
            url = lib.get('downloads', {}).get('artifact', {}).get('url') or lib.get('url', 'https://libraries.minecraft.net/').rstrip('/') + '/' + rel
            get(url, dest)
        classpath[key_of(lib['name'])] = dest
    jvm += patch.get('+jvmArgs', [])
    tweakers += patch.get('+tweakers', [])
    main = patch.get('mainClass') or main
    game_args = patch.get('minecraftArguments') or game_args
    if patch.get('mainJar'):
        jar = patch['mainJar']
        main_jar = os.path.join(args.libs, path_of(jar['name']))
        get(jar['downloads']['artifact']['url'], main_jar)

values = {'${auth_player_name}': 'Developer', '${version_name}': '1.7.10', '${game_directory}': os.path.abspath(args.game),
          '${assets_root}': os.path.abspath(os.path.expanduser(args.assets)), '${assets_index_name}': '1.7.10',
          '${auth_uuid}': '0' * 32, '${auth_access_token}': '0', '${user_properties}': '{}', '${user_type}': 'legacy'}
for key, value in values.items():
    game_args = game_args.replace(key, value)
command = ['-Xms4G', '-Xmx' + args.xmx] + jvm + ['-cp', ':'.join(list(classpath.values()) + [main_jar]), main] + game_args.split()
for tweaker in tweakers:
    command += ['--tweakClass', tweaker]
open(args.out, 'w').write('\n'.join(command))
print(main, len(classpath) + 1, 'class path entries ->', args.out)

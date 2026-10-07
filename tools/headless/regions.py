#!/usr/bin/env python3
"""Reads a 1.7.10 world's region files (vanilla or EndlessIDs sections) to check pastes without trusting the game UI.

  tiles   REGION_DIR [--box x0,y0,z0,x1,y1,z1]              tile entity ids, counts and bounds
  verify  SCHEMATIC REGION_DIR --box ...                     finds the paste transform from tile positions and checks
                                                             GT mFacing / pipe mConnections, AE2 part sides and
                                                             orientation_forward/up, ExtendedFacing presence
  blocks  SCHEMATIC LEVEL_DAT REGION_DIR --matrix M --offset T   compares every block name and metadata
  around  LEVEL_DAT REGION_DIR --box ...                     non-air blocks on the one-block shell outside the box
                                                             (structures cut by a selection box)

The world must be saved first (the client autosaves every 45 s; region file times show it).
Needs nbtlib (pip install nbtlib).
"""
import argparse
import collections
import glob
import gzip
import io
import itertools
import json
import os
import struct
import zlib

import nbtlib

DIRS = [(0, -1, 0), (0, 1, 0), (0, 0, -1), (0, 0, 1), (-1, 0, 0), (1, 0, 0)]
NAMES = ['DOWN', 'UP', 'NORTH', 'SOUTH', 'WEST', 'EAST']


def chunks(path):
    data = open(path, 'rb').read()
    for i in range(1024):
        offset = struct.unpack('>I', data[i * 4:i * 4 + 4])[0]
        if not offset:
            continue
        sector = (offset >> 8) * 4096
        length, kind = struct.unpack('>IB', data[sector:sector + 5])
        raw = data[sector + 5:sector + 4 + length]
        raw = zlib.decompress(raw) if kind == 2 else gzip.decompress(raw)
        yield nbtlib.File.parse(io.BytesIO(raw))


def region_files(region_dir):
    return sorted(glob.glob(os.path.join(region_dir, '*.mca')))


def parse_box(text):
    values = [int(v) for v in text.split(',')]
    return values[:3], values[3:]


def inside(pos, box):
    low, high = box
    return all(low[i] <= pos[i] <= high[i] for i in range(3))


def tiles_in(region_dir, box=None):
    found = {}
    for path in region_files(region_dir):
        for chunk in chunks(path):
            for tile in chunk['Level'].get('TileEntities', []):
                pos = (int(tile['x']), int(tile['y']), int(tile['z']))
                if box is None or inside(pos, box):
                    found[pos] = tile
    return found


def block_names(level_dat):
    level = nbtlib.load(level_dat)
    return {int(e['V']): str(e['K'])[1:] for e in level['FML']['ItemData'] if str(e['K']).startswith('\x01')}


def nibble(array, index):
    return 0 if array is None else (int(array[index >> 1]) >> (4 * (index & 1))) & 15


def world_blocks(region_dir, wanted_chunks, keep):
    """{(x, y, z): (id, meta)} for positions keep() accepts in the given chunk columns."""
    result, skipped = {}, 0
    for path in region_files(region_dir):
        for chunk in chunks(path):
            level = chunk['Level']
            cx, cz = int(level['xPos']), int(level['zPos'])
            if (cx, cz) not in wanted_chunks:
                continue
            for section in level['Sections']:
                if 'Blocks' not in section:
                    skipped += 1
                    continue
                sy = int(section['Y'])
                blocks, add, high = section['Blocks'], section.get('Add'), section.get('BlocksB2Hi')
                data, data1, data2 = section['Data'], section.get('Data1High'), section.get('Data2')
                for index in range(4096):
                    pos = (cx * 16 + (index & 15), sy * 16 + (index >> 8), cz * 16 + ((index >> 4) & 15))
                    if not keep(pos):
                        continue
                    block = (int(blocks[index]) & 255) | (nibble(add, index) << 8) | (nibble(high, index) << 12)
                    meta = nibble(data, index) | (nibble(data1, index) << 4) | ((int(data2[index]) & 255) << 8 if data2 is not None else 0)
                    result[pos] = (block, meta)
    if skipped:
        print('note: skipped', skipped, 'sections without a Blocks array (other storage format)')
    return result


def apply(matrix, vector, offset=(0, 0, 0)):
    return tuple(sum(matrix[r][k] * vector[k] for k in range(3)) + offset[r] for r in range(3))


def horizontal_transforms():
    for perm in itertools.permutations(range(3)):
        if perm[1] != 1:
            continue
        for sx, sz in itertools.product((1, -1), repeat=2):
            matrix = [[0] * 3 for _ in range(3)]
            matrix[0][perm[0]] = sx
            matrix[1][1] = 1
            matrix[2][perm[2]] = sz
            yield matrix


def determinant(m):
    return (m[0][0] * (m[1][1] * m[2][2] - m[1][2] * m[2][1]) - m[0][1] * (m[1][0] * m[2][2] - m[1][2] * m[2][0])
            + m[0][2] * (m[1][0] * m[2][1] - m[1][1] * m[2][0]))


def cmd_tiles(args):
    box = parse_box(args.box) if args.box else None
    counts, positions = collections.Counter(), collections.defaultdict(list)
    for pos, tile in tiles_in(args.region_dir, box).items():
        counts[str(tile['id'])] += 1
        positions[str(tile['id'])].append(pos)
    for name, count in counts.most_common(args.top):
        p = positions[name]
        print(count, name, tuple(min(v[i] for v in p) for i in range(3)), tuple(max(v[i] for v in p) for i in range(3)))


def cmd_verify(args):
    schematic = nbtlib.load(args.schematic)
    source = {(int(t['x']), int(t['y']), int(t['z'])): t for t in schematic['TileEntities']}
    target = tiles_in(args.region_dir, parse_box(args.box))
    print('schematic tiles', len(source), 'world tiles in box', len(target))
    best = None
    for matrix in horizontal_transforms():
        moved = [apply(matrix, p) for p in source]
        low = [min(p[i] for p in moved) for i in range(3)]
        world_low = [min(p[i] for p in target) for i in range(3)] if target else [0, 0, 0]
        offset = [world_low[i] - low[i] for i in range(3)]
        hits = sum(1 for p, t in source.items()
                   if str(target.get(apply(matrix, p, offset), {}).get('id', '')) == str(t['id']))
        if best is None or hits > best[0]:
            best = (hits, matrix, offset)
    hits, matrix, offset = best
    print('matrix', json.dumps(matrix), 'offset', json.dumps(offset), 'mirror' if determinant(matrix) < 0 else 'rotation',
          'position+id matches', hits, '/', len(source))
    sides = [DIRS.index(apply(matrix, d)) for d in DIRS]
    stats, bad = collections.Counter(), collections.defaultdict(list)

    def check(key, ok, detail):
        stats[key + (' ok' if ok else ' BAD')] += 1
        if not ok:
            bad[key].append(detail)

    for pos, s in source.items():
        d = target.get(apply(matrix, pos, offset))
        if d is None or str(d['id']) != str(s['id']):
            stats['missing ' + str(s['id'])] += 1
            continue
        if 'mFacing' in s:
            facing = int(s['mFacing'])
            expected = sides[facing] if facing < 6 else facing
            check('gt_facing', int(d.get('mFacing', -1)) == expected, (pos, facing, int(d.get('mFacing', -1)), expected))
        if 'mConnections' in s:
            mask = int(s['mConnections']) & 63
            expected = sum(1 << sides[i] for i in range(6) if mask >> i & 1)
            check('pipe_connections', (int(d.get('mConnections', -1)) & 63) == expected, (pos, mask, int(d.get('mConnections', -1)), expected))
        parts = sorted(int(k[4:]) for k in s if k.startswith('def:'))
        if parts:
            expected = sorted(sides[i] if i < 6 else i for i in parts)
            check('ae2_part_sides', sorted(int(k[4:]) for k in d if k.startswith('def:')) == expected, (pos, parts, expected))
        for key in ('orientation_forward', 'orientation_up'):
            if key in s and str(s[key]) in NAMES:
                expected = NAMES[sides[NAMES.index(str(s[key]))]]
                check(key, str(d.get(key)) == expected, (pos, str(s[key]), str(d.get(key)), expected))
        if 'eRotation' in s:
            stats['gt_extended_facing (check the controller in game)'] += 1
    for key, value in sorted(stats.items()):
        print(' ', key, value)
    for key, value in bad.items():
        print('BAD', key, len(value), value[:6])
    print('pipes on the box edge lose connections to missing outside neighbours; GT does that itself')


def cmd_blocks(args):
    schematic = nbtlib.load(args.schematic)
    width, height, length = int(schematic['Width']), int(schematic['Height']), int(schematic['Length'])
    blocks, add, data = schematic['Blocks'], schematic['AddBlocks'], schematic['Data']
    data_high = schematic.get('SchematicaPlusDataHigh')
    extended = str(schematic.get('SchematicaBlockIdEncoding', '')) == 'unsigned16'
    mapping = {int(v): k for k, v in schematic['SchematicaMapping'].items()}
    matrix, offset = json.loads(args.matrix), json.loads(args.offset)
    expected = {}
    for y in range(height):
        for z in range(length):
            for x in range(width):
                i = (y * length + z) * width + x
                if extended:
                    block = (int(blocks[i]) & 255) | ((int(add[i]) & 255) << 8)
                else:
                    packed = int(add[i >> 1]) if i >> 1 < len(add) else 0
                    block = (int(blocks[i]) & 255) | (((packed >> (4 * (i & 1))) & 15) << 8)
                meta = (int(data[i]) & 255) | ((int(data_high[i]) & 255) << 8 if data_high is not None else 0)
                expected[apply(matrix, (x, y, z), offset)] = (mapping.get(block, '?%d' % block), meta)
    names = block_names(args.level_dat)
    world = world_blocks(args.region_dir, {(p[0] >> 4, p[2] >> 4) for p in expected}, expected.__contains__)
    same, name_diff, meta_diff = 0, collections.Counter(), collections.Counter()
    for pos, (name, meta) in expected.items():
        block, world_meta = world.get(pos, (None, 0))
        world_name = '<no section>' if block is None else names.get(block, '?%d' % block)
        if world_name != name and not (name == 'minecraft:air' and block is None):
            name_diff[(name, world_name)] += 1
        elif block is not None and world_meta != meta:
            meta_diff[(name, meta, world_meta)] += 1
        else:
            same += 1
    print('cells', len(expected), 'identical', same)
    print('name differences', name_diff.most_common(20))
    print('metadata differences (schematic -> world)', meta_diff.most_common(30))


def cmd_around(args):
    low, high = parse_box(args.box)
    names = block_names(args.level_dat)

    def shell(pos):
        return not inside(pos, (low, high)) and inside(pos, ([v - 1 for v in low], [v + 1 for v in high]))

    columns = {(x >> 4, z >> 4) for x in range(low[0] - 1, high[0] + 2) for z in range(low[2] - 1, high[2] + 2)}
    counts = collections.Counter()
    for (x, y, z), (block, _) in world_blocks(args.region_dir, columns, shell).items():
        name = names.get(block, '?%d' % block)
        if name == 'minecraft:air':
            continue
        side = 'y-' if y < low[1] else 'y+' if y > high[1] else 'x-' if x < low[0] else 'x+' if x > high[0] else 'z-' if z < low[2] else 'z+'
        counts[(side, name)] += 1
    for key, value in sorted(counts.items()):
        print(key, value)


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    sub = parser.add_subparsers(dest='command', required=True)
    p = sub.add_parser('tiles'); p.add_argument('region_dir'); p.add_argument('--box'); p.add_argument('--top', type=int, default=40)
    p.set_defaults(run=cmd_tiles)
    p = sub.add_parser('verify'); p.add_argument('schematic'); p.add_argument('region_dir'); p.add_argument('--box', required=True)
    p.set_defaults(run=cmd_verify)
    p = sub.add_parser('blocks'); p.add_argument('schematic'); p.add_argument('level_dat'); p.add_argument('region_dir')
    p.add_argument('--matrix', required=True); p.add_argument('--offset', required=True)
    p.set_defaults(run=cmd_blocks)
    p = sub.add_parser('around'); p.add_argument('level_dat'); p.add_argument('region_dir'); p.add_argument('--box', required=True)
    p.set_defaults(run=cmd_around)
    args = parser.parse_args()
    args.run(args)


if __name__ == '__main__':
    main()

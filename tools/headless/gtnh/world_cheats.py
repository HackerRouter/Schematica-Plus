#!/usr/bin/env python3
"""Creative mode and commands for a COPY of a world (never the user's original).   world_cheats.py level.dat"""
import sys

import nbtlib

level = nbtlib.load(sys.argv[1])
data = level['Data']
data['allowCommands'] = nbtlib.Byte(1)
data['GameType'] = nbtlib.Int(1)
if 'Player' in data:
    data['Player']['playerGameType'] = nbtlib.Int(1)
level.save()
print('commands', int(nbtlib.load(sys.argv[1])['Data']['allowCommands']), 'game type 1')

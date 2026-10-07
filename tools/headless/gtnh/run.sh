#!/bin/bash
# Starts a GTNH instance made by mklaunch.py on a virtual display.  run.sh INSTANCE_DIR LAUNCH_ARGS [LOG]
INSTANCE=$1; ARGS=$2; LOG=${3:-$INSTANCE/../game.log}
DISPLAY_NUM=${DISPLAY_NUM:-:98}
JAVA21=${JAVA21:-/usr/lib/jvm/java-21-openjdk-amd64/bin/java}
pgrep -f "Xvfb $DISPLAY_NUM " >/dev/null || (Xvfb "$DISPLAY_NUM" -screen 0 1280x720x24 -nolisten tcp >/tmp/xvfb${DISPLAY_NUM#:}.log 2>&1 &)
sleep 2
cd "$INSTANCE" || exit 1
DISPLAY=$DISPLAY_NUM LIBGL_ALWAYS_SOFTWARE=1 "$JAVA21" @"$ARGS" > "$LOG" 2>&1
echo "exit $?" >> "$LOG"

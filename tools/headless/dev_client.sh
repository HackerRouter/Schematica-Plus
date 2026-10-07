#!/bin/bash
# The development client (runClient21: Java 21, lwjgl3ify) on a virtual display with Mesa software GL.
# Run from the repository root. Log: run/client.log. The game directory is run/ (git-ignored).
DISPLAY_NUM=${DISPLAY_NUM:-:99}
pgrep -f "Xvfb $DISPLAY_NUM " >/dev/null || (Xvfb "$DISPLAY_NUM" -screen 0 1280x720x24 -nolisten tcp >/tmp/xvfb${DISPLAY_NUM#:}.log 2>&1 &)
sleep 2
mkdir -p run
DISPLAY=$DISPLAY_NUM LIBGL_ALWAYS_SOFTWARE=1 LC_ALL=C.UTF-8 ./gradlew runClient21 --no-daemon "$@" > run/client.log 2>&1
echo "exit $?" >> run/client.log

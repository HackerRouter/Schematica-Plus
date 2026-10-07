#!/bin/bash
# Input and screenshots for a game on a headless X display.
#   DISPLAY_NUM (default :99), SHOT_DIR (default /tmp/shots)
#   x.sh shot NAME         screenshot of the whole display to $SHOT_DIR/NAME.png
#   x.sh clk X Y           click; LWJGL misses clicks without a pause between press and release
#   x.sh look DX DY        turn the camera in small relative steps (absolute moves do not turn it)
#   x.sh key NAME / x.sh tap NAME   key press (tap holds it 0.2 s, needed for in-game keys)
#   x.sh chat TEXT         open chat with T, type, Enter (commands start with /)
#   x.sh window            move and size the game window to fill the display
#   x.sh ...               anything else is passed to xdotool
export DISPLAY=${DISPLAY_NUM:-:99}
SHOT_DIR=${SHOT_DIR:-/tmp/shots}
case "$1" in
  shot) mkdir -p "$SHOT_DIR"; import -window root "$SHOT_DIR/$2.png" ;;
  clk) xdotool mousemove "$2" "$3"; sleep 0.2; xdotool mousedown 1; sleep 0.15; xdotool mouseup 1 ;;
  rclk) xdotool mousemove "$2" "$3"; sleep 0.2; xdotool mousedown 3; sleep 0.15; xdotool mouseup 3 ;;
  look) dx=$2; dy=$3; n=$(( (${dx#-} > ${dy#-} ? ${dx#-} : ${dy#-}) / 8 + 1 ))
        for i in $(seq 1 $n); do xdotool mousemove_relative -- $((dx / n)) $((dy / n)); sleep 0.03; done ;;
  tap) xdotool keydown "$2"; sleep 0.2; xdotool keyup "$2" ;;
  chat) xdotool keydown t; sleep 0.2; xdotool keyup t; sleep 1; xdotool type --delay 50 -- "$2"; sleep 0.3
        xdotool keydown Return; sleep 0.2; xdotool keyup Return ;;
  window) w=$(xdotool search --name "Minecraft|GT: New Horizons" | head -1); xdotool windowmove "$w" 0 0; xdotool windowsize "$w" 1280 720 ;;
  *) xdotool "$@" ;;
esac

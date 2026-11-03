#!/usr/bin/env bash
# One-time pairing for the Controller app, started FROM THE LAPTOP. When the laptop
# starts the pairing it reads the phone's service list, including the keyboard that
# Controller adds. Pairing from the phone side skips that, and the laptop then treats
# the phone as an audio device only.
#
# Before running, on the phone:
#   1. Settings > Bluetooth: forget this laptop if it is listed.
#   2. Open Controller (that adds the keyboard), then go back to Settings > Bluetooth
#      and leave that screen open (that makes the phone visible).
set -euo pipefail
HID=00001124-0000-1000-8000-00805f9b34fb  # Bluetooth "Human Interface Device" service

info() { bluetoothctl info "$1" 2>/dev/null; }
name_of() { info "$1" | sed -n 's/^\s*Name: //p'; }
phones() { for m in $(bluetoothctl devices | cut -d' ' -f2); do info "$m" | grep -q "Icon: phone" && echo "$m"; done; }

# Old phone pairings were saved without the keyboard service: forget them, remember who.
mine=""
for mac in $(bluetoothctl devices Paired | cut -d' ' -f2); do
  if info "$mac" | grep -q "Icon: phone"; then
    echo "Forgetting old pairing: $(name_of "$mac")"
    mine="$mine $mac"
    bluetoothctl remove "$mac" >/dev/null
  fi
done

echo "Looking for the phone (keep its Bluetooth settings screen open)..."
bluetoothctl --timeout 45 scan on >/dev/null &
scan=$!
mac="" ask=0
for _ in $(seq 45); do
  sleep 1
  found=$(phones)
  for m in $mine; do echo "$found" | grep -qx "$m" && { mac=$m; break 2; }; done
  if [ -z "$mine" ] && [ -n "$found" ]; then mac=$(echo "$found" | head -1); ask=1; break; fi
done
kill "$scan" 2>/dev/null || true
[ -n "$mac" ] || { echo "No phone found. Is its Bluetooth settings screen open?"; exit 1; }

name=$(name_of "$mac")
if [ "$ask" = 1 ]; then
  read -rp "Pair with '$name'? [y/N] " yes
  [ "$yes" = y ] || [ "$yes" = Y ] || exit 1
fi

echo "Pairing with $name. Tap Pair on the phone when it asks."
bluetoothctl --timeout 30 pair "$mac" >/dev/null || true
info "$mac" | grep -q "Paired: yes" || { echo "Pairing failed. Run this again."; exit 1; }
bluetoothctl trust "$mac" >/dev/null

for _ in $(seq 10); do info "$mac" | grep -q "$HID" && break; sleep 1; done
if ! info "$mac" | grep -q "$HID"; then
  echo "Paired, but the phone shows no keyboard service. Open Controller on the phone, then run this again."
  exit 1
fi

# Keyboard only: connecting everything would also pull the phone's audio to the laptop.
bluetoothctl --timeout 15 connect "$mac" "$HID" >/dev/null || true
echo "Done. Controller on the phone should now say: Connected to $(bluetoothctl show | sed -n 's/^\s*Name: //p')"
echo "Phone audio tip: Settings > Bluetooth > gear next to the laptop > turn off Audio and Calls."

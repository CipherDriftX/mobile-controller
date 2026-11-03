#!/usr/bin/env bash
# One-time pairing for the Controller app.
# Makes the laptop visible for 3 minutes, waits for the phone to pair, trusts it
# (so it can reconnect later without asking), then hides the laptop again.
set -euo pipefail

paired() { bluetoothctl devices Paired | cut -d' ' -f2 | sort; }
before=$(paired)
name=$(bluetoothctl show | sed -n 's/^\s*Name: //p')

bluetoothctl discoverable on >/dev/null
echo "Laptop is visible as '$name' for 3 minutes."
echo "On the phone: open Controller, then Settings > Bluetooth > tap '$name' > Pair."

for _ in $(seq 90); do
  sleep 2
  new=$(comm -13 <(echo "$before") <(paired))
  if [ -n "$new" ]; then
    for mac in $new; do bluetoothctl trust "$mac" >/dev/null; done
    bluetoothctl discoverable off >/dev/null
    echo "Paired and trusted. Go back to the Controller app."
    exit 0
  fi
done
bluetoothctl discoverable off >/dev/null
echo "No phone paired in 3 minutes. Run this again."
exit 1

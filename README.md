# mobile-controller

Press laptop keys from your phone: Up, Down, Left, Right, Space.
Made for scrolling a PDF from across the room.

## Android app (works anywhere)

The phone becomes a real Bluetooth keyboard. No Wi-Fi, no server, nothing to install
on the laptop. Campus Wi-Fi can't block it.

**Download:** [controller.apk](https://github.com/CipherDriftX/mobile-controller/releases/latest/download/controller.apk)
(Android 9 or newer, about 17 KB)

### Install and pair (once)

1. Open the link above on the phone and install the APK. Android asks to allow
   installs from the browser: allow it.
2. Open **Controller** and allow Bluetooth.
3. On the laptop, run:
   ```bash
   ~/code/mobile-controller/pair-laptop.sh
   ```
4. On the phone: Settings > Bluetooth > tap the laptop's name (`omarchy`) > Pair.
5. Go back to Controller. It connects by itself.

### Use

Open Controller. It reconnects to the last laptop. Click the PDF window on the laptop
so it has focus, then press keys. Hold a key to repeat it.

If the top line says "Not connected", tap it to retry.

### How it works

Android 9+ has a built-in Bluetooth keyboard mode (`BluetoothHidDevice`). The app
registers as a keyboard and sends an 8-byte report on finger down (key pressed) and
finger up (key released). The laptop treats it like any Bluetooth keyboard, so key
repeat works the same way it does on a real keyboard.

One Java file, no libraries. Build with `android/build.sh` (no Gradle).

### If it doesn't work

- "This phone can't act as a Bluetooth keyboard": the phone's firmware has this
  Bluetooth mode turned off. No app can fix that. Use the web version on a network
  that allows it.
- "Another keyboard app is running": close the other Bluetooth keyboard app.
- Pairing worked but it won't connect: on the phone, forget the laptop in Bluetooth
  settings, then pair again with `pair-laptop.sh` while Controller is open.

## Web version (same Wi-Fi only)

```
phone browser --(Wi-Fi, HTTP)--> controller.py on laptop --> wtype -k Down --> PDF scrolls
```

- Laptop: one Python file, standard library only. Port 8765.
- Keys are sent with `wtype`, which works on Wayland (Hyprland).
- Only 5 keys are allowed. Everything else is refused.
- A random token in the URL, new on every start, so others on the same Wi-Fi can't press keys.

Once, open the firewall port (Omarchy has `ufw` on):

```bash
sudo ufw allow 8765/tcp
```

Every time:

```bash
~/code/mobile-controller/controller.py
```

Scan the QR code in the terminal with the phone camera, click on the PDF window,
then tap the buttons. Buttons turn red when the laptop does not answer.

Campus Wi-Fi often blocks phone-to-laptop traffic. BAU campus Wi-Fi does (checked
2026-11-03: port open in ufw, but zero connections from the phone reached the laptop).
Use the Android app there, or a phone hotspot (no mobile data needed).

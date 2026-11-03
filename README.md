# mobile-controller

Press laptop keys from your phone. Up, Down, Left, Right, Space.
Made for scrolling a PDF from across the room.

## How it works

```
phone browser --(Wi-Fi, HTTP)--> controller.py on laptop --> wtype -k Down --> PDF scrolls
```

- Laptop: one Python file, standard library only. Port 8765.
- Keys are sent with `wtype`, which works on Wayland (Hyprland).
- Phone: nothing to install. Open the page in the browser.
- Only 5 keys are allowed. Everything else is refused.
- A random token in the URL, new on every start, so others on the same Wi-Fi can't press keys.

## Use

Once, open the firewall port (Omarchy has `ufw` on):

```bash
sudo ufw allow 8765/tcp
```

Every time:

```bash
~/code/mobile-controller/controller.py
```

1. Scan the QR code in the terminal with the phone camera.
2. Click on the PDF window on the laptop so it has focus.
3. Tap the buttons. Hold a button to repeat.

Buttons turn red when the laptop does not answer.

## If the phone can't open the page

- Firewall: run the `ufw` command above.
- Campus Wi-Fi often blocks phone-to-laptop traffic. Turn on the phone hotspot,
  connect the laptop to it, and restart `controller.py`. It prints the new URL.
- Phone and laptop must be on the same network.

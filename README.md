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

#!/usr/bin/env python3
"""Phone -> laptop keys. Phone opens a page, taps a button, laptop presses the key."""
import secrets
import shutil
import socket
import subprocess
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
from urllib.parse import parse_qs, urlsplit

PORT = 8765
KEYS = {"Up", "Down", "Left", "Right", "space"}  # only these, nothing else
TOKEN = secrets.token_urlsafe(8)  # new every start; phone gets it from the URL
PAGE = (Path(__file__).parent / "page.html").read_bytes()


def lan_ip():
    """IP the phone can reach. UDP connect sends nothing, it only picks the route."""
    with socket.socket(socket.AF_INET, socket.SOCK_DGRAM) as s:
        try:
            s.connect(("10.255.255.255", 1))
            return s.getsockname()[0]
        except OSError:
            return "127.0.0.1"


class Handler(BaseHTTPRequestHandler):
    protocol_version = "HTTP/1.1"  # keep-alive: phone reuses one connection, less lag

    def do_GET(self):
        if not self.token_ok():
            return self.reply(403, b"bad token\n")
        self.reply(200, PAGE, "text/html; charset=utf-8")

    def do_POST(self):
        if not self.token_ok():
            return self.reply(403, b"bad token\n")
        key = urlsplit(self.path).path.removeprefix("/k/")
        if key not in KEYS:
            return self.reply(404, b"unknown key\n")
        r = subprocess.run(["wtype", "-k", key], capture_output=True, text=True)
        if r.returncode:
            print("wtype failed:", r.stderr.strip())
            return self.reply(500, b"wtype failed\n")
        self.reply(204)

    def log_request(self, code="-", size="-"):
        if int(code) >= 400:  # show problems only, not every key press
            super().log_request(code, size)

    def token_ok(self):
        given = parse_qs(urlsplit(self.path).query).get("t", [""])[0]
        return secrets.compare_digest(given.encode(), TOKEN.encode())

    def reply(self, code, body=b"", ctype="text/plain; charset=utf-8"):
        self.send_response(code)
        self.send_header("Content-Type", ctype)
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)


if __name__ == "__main__":
    try:
        server = ThreadingHTTPServer(("0.0.0.0", PORT), Handler)
    except OSError as e:
        raise SystemExit(f"Port {PORT} busy ({e.strerror}). Already running in another terminal?")
    url = f"http://{lan_ip()}:{PORT}/?t={TOKEN}"
    if shutil.which("qrencode"):
        subprocess.run(["qrencode", "-t", "ANSIUTF8", "-m", "2", url])
    print(f"Scan the QR or open on phone (same Wi-Fi):\n  {url}")
    print("Keys go to the focused window. Ctrl+C to stop.")
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        print("\nStopped.")

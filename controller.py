#!/usr/bin/env python3
"""Phone -> laptop keys. Phone opens a page, taps a button, laptop presses the key."""
import secrets
import subprocess
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from urllib.parse import parse_qs, urlsplit

PORT = 8765
KEYS = {"Up", "Down", "Left", "Right", "space"}  # only these, nothing else
TOKEN = secrets.token_urlsafe(8)  # new every start; phone gets it from the URL


class Handler(BaseHTTPRequestHandler):
    protocol_version = "HTTP/1.1"  # keep-alive: phone reuses one connection, less lag

    def do_GET(self):
        if not self.token_ok():
            return self.reply(403, b"bad token\n")
        self.reply(200, b"mobile-controller is running\n")

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

    def token_ok(self):
        given = parse_qs(urlsplit(self.path).query).get("t", [""])[0]
        return secrets.compare_digest(given, TOKEN)

    def reply(self, code, body=b"", ctype="text/plain; charset=utf-8"):
        self.send_response(code)
        self.send_header("Content-Type", ctype)
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)


if __name__ == "__main__":
    print(f"token: {TOKEN}")
    ThreadingHTTPServer(("0.0.0.0", PORT), Handler).serve_forever()

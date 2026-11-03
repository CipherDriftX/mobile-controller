#!/usr/bin/env python3
"""Phone -> laptop keys. Phone opens a page, taps a button, laptop presses the key."""
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer

PORT = 8765


class Handler(BaseHTTPRequestHandler):
    protocol_version = "HTTP/1.1"  # keep-alive: phone reuses one connection, less lag

    def do_GET(self):
        self.reply(200, b"mobile-controller is running\n")

    def reply(self, code, body=b"", ctype="text/plain; charset=utf-8"):
        self.send_response(code)
        self.send_header("Content-Type", ctype)
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)


if __name__ == "__main__":
    ThreadingHTTPServer(("0.0.0.0", PORT), Handler).serve_forever()

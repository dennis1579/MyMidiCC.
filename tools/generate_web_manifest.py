#!/usr/bin/env python3
"""Generate a deterministic manifest for offline updates; Python 3 stdlib only."""
from __future__ import annotations
import argparse
import hashlib
import json
from pathlib import Path

WEB_DIR = Path(__file__).resolve().parent.parent / "web"
MANIFEST = WEB_DIR / "manifest.json"
MAX_FILES = 256
MAX_FILE_SIZE = 2 * 1024 * 1024
MAX_TOTAL_SIZE = 12 * 1024 * 1024


def manifest_bytes() -> bytes:
    items = []
    total = 0
    for file in sorted(WEB_DIR.rglob("*")):
        if not file.is_file() or file == MANIFEST:
            continue
        rel = file.relative_to(WEB_DIR).as_posix()
        parts = rel.split("/")
        if any(p in ("", ".", "..") for p in parts) or not all(
            p.replace("-", "").replace("_", "").replace(".", "").isalnum() for p in parts
        ):
            raise ValueError("Unsupported file path: " + rel)
        size = file.stat().st_size
        total += size
        if size > MAX_FILE_SIZE or total > MAX_TOTAL_SIZE:
            raise ValueError("Web package exceeds update size limit")
        items.append({"path": rel, "sha256": hashlib.sha256(file.read_bytes()).hexdigest()})
    if not any(f["path"] == "index.html" for f in items):
        raise ValueError("web/index.html is required")
    if len(items) > MAX_FILES:
        raise ValueError("Too many web files")
    return (json.dumps({"schema": 1, "files": items}, indent=2, ensure_ascii=False) + "\n").encode("utf-8")


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args()
    expected = manifest_bytes()
    if args.check:
        if not MANIFEST.exists() or MANIFEST.read_bytes() != expected:
            raise SystemExit("web/manifest.json is outdated; run generate_web_manifest.py")
        print("Web manifest verified")
    else:
        MANIFEST.write_bytes(expected)
        print("Updated", MANIFEST, "with", len(json.loads(expected)["files"]), "files")


if __name__ == "__main__":
    main()

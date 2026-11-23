#!/usr/bin/env python3
"""Regression guard for documented Roland JD-08 receive-only MIDI mapping."""
from __future__ import annotations
import json
import re
from pathlib import Path

html = (Path(__file__).resolve().parents[1] / "web/index.html").read_text(encoding="utf-8")
assert html.count('name: "Roland JD-08"') == 1
section = html.split('    name: "Roland JD-08"', 1)[1].split('\n  },\n  {', 1)[0]
rows_section = section.split("    rows: [\n", 1)[1].split("\n    ]", 1)[0]
rows = [json.loads(line.strip().rstrip(",")) for line in rows_section.splitlines() if line.strip()]
received = set([1, 3, 5, 7, 9, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 35, 41, 46, 47, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 58, 59, 60, 61, 62, 63, 64, 66, 72, 79, 80, 81, 82, 83, 85, 86, 87, 89, 90, 91, 92, 93, 94, 95, 96, 97, 102, 103, 104, 105, 106, 107, 108, 110, 111, 112, 115, 116, 117, 118, 119])
channel_mode = {120, 121, 123, 124, 125, 126}
numbers = [int(row[0]) for row in rows]
assert len(rows) == 87 and len(set(numbers)) == 87, (len(rows), numbers)
assert set(numbers) == received | channel_mode
assert not {68, 69, 70, 71} & set(numbers)  # transmit only, not externally controllable
assert all(row[2] == "CC" for row in rows)
assert next(row for row in rows if row[0] == "3")[1] == "TVF Cutoff"
assert next(row for row in rows if row[0] == "9")[1] == "TVF Resonance"
assert next(row for row in rows if row[0] == "72")[3] == "0–108 (Empfang)"
assert "https://static.roland.com/manuals/jd-08_reference/eng/17812001.html" in section
assert "Systemkanal" in section and "CH.A" in section and "CH.B" in section
assert html.count('name: "') >= 16
assert "<script>" in html and "</script>" in html
print("PASS: Roland JD-08, 81 part CCs + 6 channel-mode CCs; 17 devices")

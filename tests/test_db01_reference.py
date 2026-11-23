#!/usr/bin/env python3
"""Regression checks for the limited, source-supported Erica Synths DB-01 MIDI input."""
from pathlib import Path
import json

html = (Path(__file__).resolve().parents[1] / "web/index.html").read_text(encoding="utf-8")
assert html.count('name: "Erica Synths Bassline DB-01"') == 1
section = html.split('    name: "Erica Synths Bassline DB-01"', 1)[1].split('\n  },\n  {', 1)[0]
rows_section = section.split("    rows: [\n", 1)[1].split("\n    ]", 1)[0]
rows = [json.loads(line.strip().rstrip(",")) for line in rows_section.splitlines() if line.strip()]
assert rows == [["1", "Modulation Wheel (laut Praxis: VCF-Cutoff-Offset)", "CC", "0–127"]]
assert "Praxisberichten" in section and "nicht als eigener MIDI-CC-Eintrag bestätigt" in section
for item in ("Pitch Bend", "MIDI Clock", "Program Change", "MIDI-IN-Kanal", "Keyboard-/Arpeggiator-Modus", "MIP"):
    assert item in section, item
assert "https://www.ericasynths.lv/media/BASSLINE_DB-01_manual.pdf" in section
assert "https://www.ericasynths.lv/news/post/bassline-107" in section
assert len([line for line in html.splitlines() if line.strip().startswith('name: "')]) == 17
print("PASS: DB-01 CC1 only; unverified filter mapping labeled; 17 devices")

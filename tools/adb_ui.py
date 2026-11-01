"""Tiny ADB UI driver for live testing (used by /adb-live-test).

Usage:
  python tools/adb_ui.py dump                 # list visible text / content-desc nodes with bounds
  python tools/adb_ui.py tap "<text or desc>" # tap the first node whose text or content-desc contains it
  python tools/adb_ui.py shot <file.png>      # save a screenshot
"""
import html
import os
import re
import subprocess
import sys
import time

ADB = os.environ.get("ADB") or os.path.join(
    os.environ.get("LOCALAPPDATA", ""), "Android", "Sdk", "platform-tools", "adb.exe"
)


def adb(*args, binary=False):
    out = subprocess.run([ADB, *args], capture_output=True, check=False)
    return out.stdout if binary else out.stdout.decode("utf-8", "replace")


def nodes():
    adb("shell", "uiautomator", "dump", "/sdcard/ui.xml")
    xml = adb("exec-out", "cat", "/sdcard/ui.xml")
    result = []
    for m in re.finditer(r"<node [^>]*>", xml):
        n = m.group(0)
        attr = lambda k: html.unescape((re.search(k + r'="([^"]*)"', n) or [None, ""])[1])
        b = [int(v) for v in re.findall(r"\d+", attr("bounds"))]
        result.append({"text": attr(" text"), "desc": attr("content-desc"),
                       "clickable": attr("clickable") == "true", "bounds": b})
    return result


def tap(label):
    all_nodes = nodes()
    # Prefer clickable nodes (buttons) over plain text that merely contains the label.
    ordered = [n for n in all_nodes if n["clickable"]] + [n for n in all_nodes if not n["clickable"]]
    exact = [n for n in ordered if (n["text"] or n["desc"]).strip().lower() == label.lower()]
    for n in exact + ordered:
        b = n["bounds"]
        if label.lower() in (n["text"] + " " + n["desc"]).lower() and len(b) == 4 and b[2] > b[0] and b[3] > b[1]:
            x1, y1, x2, y2 = n["bounds"]
            adb("shell", "input", "tap", str((x1 + x2) // 2), str((y1 + y2) // 2))
            print(f"tapped '{label}' at {(x1 + x2) // 2},{(y1 + y2) // 2}")
            return True
    print(f"NOT FOUND: '{label}'")
    return False


if __name__ == "__main__":
    sys.stdout.reconfigure(encoding="utf-8")  # Windows consoles default to cp1252
    cmd = sys.argv[1]
    if cmd == "dump":
        for n in nodes():
            if n["text"] or n["desc"]:
                print(("* " if n["clickable"] else "  ") + repr(n["text"] or n["desc"])[:60], n["bounds"])
    elif cmd == "tap":
        ok = tap(sys.argv[2])
        time.sleep(float(sys.argv[3]) if len(sys.argv) > 3 else 1.2)
        sys.exit(0 if ok else 1)
    elif cmd == "shot":
        open(sys.argv[2], "wb").write(adb("exec-out", "screencap", "-p", binary=True))

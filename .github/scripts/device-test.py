#!/usr/bin/env python3
"""Goes through Vocabuilder's main screens on a connected device or emulator, as a user would: studies a deck, with
the buttons and with swipes, then the daily review, then opens the word list and the settings. Fails if the app
crashes or a screen doesn't show up.

Each step leaves a screenshot and the screen's view hierarchy in the output directory, with logcat and, after a crash,
the crash (crash.txt) for the workflow to retrace.

Usage: device-test.py <APK> <output directory>
"""

import os
import re
import subprocess
import sys
import time
import xml.etree.ElementTree as ElementTree

NAMESPACE = "io.github.abhik9.vocabuilder"

apk, out = sys.argv[1], sys.argv[2]
os.makedirs(out, exist_ok=True)
steps = 0
package = None


def adb(*args, check=True, timeout=120):
    result = subprocess.run(["adb", *args], capture_output=True, text=True, timeout=timeout)
    if check and result.returncode != 0:
        raise RuntimeError(f"adb {' '.join(args)}: {result.stderr.strip() or result.stdout.strip()}")
    return result.stdout


def log(message):
    print(message, flush=True)
    with open(os.path.join(out, "summary.txt"), "a") as summary:
        summary.write(message + "\n")


def save_logs():
    with open(os.path.join(out, "logcat.txt"), "w") as file:
        file.write(adb("logcat", "-d", "-v", "time", check=False))
    crash = adb("logcat", "-d", "-b", "crash", check=False)
    if crash.strip():
        with open(os.path.join(out, "crash.txt"), "w") as file:
            file.write(crash)
    return crash


def visible_text(hierarchy):
    """The texts and descriptions on the screen, top to bottom, to show in the log what it looks like."""
    nodes = ElementTree.fromstring(hierarchy).iter("node")
    return " | ".join(text for node in nodes for text in (node.get("text"), node.get("content-desc")) if text)


def fail(message):
    log(f"FAIL: {message}")
    try:
        log(f"On the screen: {visible_text(screen())}")
    except Exception as e:  # The app may be gone: what matters is the failure.
        log(f"Couldn't read the screen: {e}")
    crash = save_logs()
    if crash.strip():
        log("== Crash (retraced in crash-retraced.txt)\n" + crash)
    sys.exit(1)


def check_alive(step):
    if not adb("shell", "pidof", package, check=False).strip():
        fail(f"the app is no longer running after: {step}")


def screen():
    """The current screen's view hierarchy. Dumping fails while the screen is changing, so it's retried."""
    for _ in range(10):
        output = adb("exec-out", "uiautomator", "dump", "/dev/tty", check=False)
        start, end = output.find("<?xml"), output.rfind("</hierarchy>")
        if start >= 0 and end > start:
            return output[start:end + len("</hierarchy>")]
        time.sleep(1)
    raise RuntimeError("Could not dump the screen")


def find(hierarchy, pattern):
    regex = re.compile(pattern)
    for node in ElementTree.fromstring(hierarchy).iter("node"):
        if regex.fullmatch(node.get("text", "")) or regex.fullmatch(node.get("content-desc", "")):
            return node
    return None


def center(node):
    x1, y1, x2, y2 = map(int, re.findall(r"\d+", node.get("bounds")))
    return (x1 + x2) // 2, (y1 + y2) // 2


def snapshot(name):
    """Saves the screen as a numbered screenshot and view hierarchy."""
    global steps
    steps += 1
    base = os.path.join(out, f"{steps:02d}-{name}")
    with open(base + ".png", "wb") as file:
        file.write(subprocess.run(["adb", "exec-out", "screencap", "-p"], capture_output=True, timeout=60).stdout)
    hierarchy = screen()
    with open(base + ".xml", "w") as file:
        file.write(hierarchy)
    return hierarchy


def wait_for(pattern, step, timeout=30):
    deadline = time.time() + timeout
    while time.time() < deadline:
        check_alive(step)
        node = find(screen(), pattern)
        if node is not None:
            return node
        time.sleep(1)
    snapshot("timeout")
    fail(f"'{pattern}' didn't show up after: {step}")


def tap(pattern, timeout=30):
    step = f"tap '{pattern}'"
    x, y = center(wait_for(pattern, step, timeout))
    adb("shell", "input", "tap", str(x), str(y))
    time.sleep(2)
    check_alive(step)
    snapshot(re.sub(r"\W+", "-", pattern).strip("-").lower()[:40])
    log(f"ok: {step}")


def swipe(direction):
    width, height = map(int, re.search(r"(\d+)x(\d+)", adb("shell", "wm", "size")).groups())
    y = height * 2 // 5
    left, right = width // 5, width * 4 // 5
    start, end = (left, right) if direction == "right" else (right, left)
    adb("shell", "input", "swipe", str(start), str(y), str(end), str(y), "300")
    time.sleep(2)
    step = f"swipe {direction}"
    check_alive(step)
    log(f"ok: {step}, then on the screen: {visible_text(snapshot(f'swipe-{direction}'))}")


def back():
    adb("shell", "input", "keyevent", "KEYCODE_BACK")
    time.sleep(2)
    check_alive("back")


def study_one_card(answer):
    """Turns the current card over and answers it with a button, or with a swipe."""
    tap("Show meaning")
    if answer in ("left", "right"):
        swipe(answer)
    else:
        tap(answer)


# The emulator reports a completed boot before every system service answers.
for _ in range(60):
    if all(": found" in adb("shell", "service", "check", name, check=False) for name in ("package", "window", "input")):
        break
    time.sleep(3)
time.sleep(10)

log(
    f"Device: {adb('shell', 'getprop', 'ro.product.model').strip()}, "
    f"Android {adb('shell', 'getprop', 'ro.build.version.release').strip()} "
    f"(API {adb('shell', 'getprop', 'ro.build.version.sdk').strip()})"
)
adb("install", "-r", apk, timeout=300)
packages = adb("shell", "pm", "list", "packages", NAMESPACE).split()
package = next(p.removeprefix("package:") for p in packages if p.startswith("package:"))
log(f"Installed {package}")
adb("logcat", "-b", "all", "-c", check=False)

adb("shell", "am", "start", "-W", "-n", f"{package}/{NAMESPACE}.MainActivity")
# The first start copies the word lists into the database.
wait_for("Today", "start", timeout=120)
snapshot("today")
log("ok: start")

# A deck, answered with the buttons, then with swipes.
tap("Decks")
tap("Deck 1")  # The first one: Common · Deck 1.
tap(r"Study \d+ words?")
study_one_card("Knew it")
study_one_card("Didn't know")
study_one_card("right")
study_one_card("left")
back()
back()

# The daily review.
tap("Today")
if find(screen(), "Start") is not None:
    tap("Start")
    study_one_card("Knew it")
    back()

tap("Words")
tap("Settings")

save_logs()
log("PASS: no crash")

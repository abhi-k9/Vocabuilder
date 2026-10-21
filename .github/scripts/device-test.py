#!/usr/bin/env python3
"""Goes through Vocabuilder's main screens on a connected device or emulator, as a user would: studies a deck, with
the buttons and with swipes, looking up a synonym on the way, then the daily review, then opens the word list and the
settings. Fails if the app crashes, a screen doesn't show up, a synonym has no meaning or a swipe doesn't answer the
card as it should.

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


def first_text_bounds(hierarchy):
    """Where the first text on the screen is, which on a study screen is the card's word."""
    node = next((n for n in ElementTree.fromstring(hierarchy).iter("node") if n.get("text")), None)
    return f"'{node.get('text')}' at {node.get('bounds')}" if node is not None else "no text"


def words_done(hierarchy):
    """How many of the session's words are done, from the study screen's "2 of 50"."""
    node = find(hierarchy, r"\d+ of \d+")
    if node is None:
        fail("the study screen shows no count of words done")
    return int(node.get("text").split()[0])


def swipe(direction):
    """Drags the card sideways step by step, as a finger would, showing where it is on the way, then checks that it
    answered the card: right as known, which counts the word as done, left as missed, which doesn't."""
    width, height = map(int, re.search(r"(\d+)x(\d+)", adb("shell", "wm", "size")).groups())
    y = height * 2 // 5
    xs = list(range(width // 5, width * 4 // 5 + 1, width // 20))
    if direction == "left":
        xs.reverse()
    hierarchy = screen()
    before, done = first_text_bounds(hierarchy), words_done(hierarchy)
    adb("shell", "input", "motionevent", "DOWN", str(xs[0]), str(y))
    for x in xs[1:]:
        adb("shell", "input", "motionevent", "MOVE", str(x), str(y))
    midway = screen()
    adb("shell", "input", "motionevent", "UP", str(xs[-1]), str(y))
    time.sleep(2)
    step = f"swipe {direction}"
    check_alive(step)
    log(f"{step}: before, {before}; midway, {first_text_bounds(midway)}: {visible_text(midway)}")
    after = snapshot(f"swipe-{direction}")
    if find(after, "Show meaning") is None:
        fail(f"{step} didn't answer the card")
    expected = done + 1 if direction == "right" else done
    if words_done(after) != expected:
        fail(f"{step} answered the card the wrong way: {done} words done before, {words_done(after)} after")
    log(f"ok: {step}, then on the screen: {visible_text(after)}")


PARTS_OF_SPEECH = ("noun", "verb", "adjective", "adverb")
looked_up = False


def look_up_synonym():
    """On a turned card, taps its first synonym if one shows, and checks that its meaning does, then closes it."""
    global looked_up
    nodes = list(ElementTree.fromstring(screen()).iter("node"))
    texts = [node.get("text", "") for node in nodes]
    if looked_up or "Synonyms" not in texts:
        return
    chip = next((node for node in nodes[texts.index("Synonyms") + 1 :] if node.get("text")), None)
    if chip is None or chip.get("text") == "Antonyms":
        return
    synonym = chip.get("text")
    x, y = center(chip)
    adb("shell", "input", "tap", str(x), str(y))
    time.sleep(2)
    step = f"look up '{synonym}'"
    check_alive(step)
    sheet = snapshot("look-up")
    shown = visible_text(sheet)
    # The sheet's title is the synonym, apart from its chip, with a part of speech below it for each meaning.
    nodes = list(ElementTree.fromstring(sheet).iter("node"))
    title = next((n for n in nodes if n.get("text") == synonym and n.get("bounds") != chip.get("bounds")), None)
    if title is None:
        fail(f"{step} didn't show it")
    top = center(title)[1]
    if "There's no meaning to show" in shown or not any(
        n.get("text") in PARTS_OF_SPEECH and center(n)[1] > top for n in nodes
    ):
        fail(f"{step} showed no meaning")
    log(f"ok: {step}, then on the screen: {shown}")
    looked_up = True
    back()


def back():
    adb("shell", "input", "keyevent", "KEYCODE_BACK")
    time.sleep(2)
    check_alive("back")


def study_one_card(answer):
    """Turns the current card over, looks up a synonym if none was yet, and answers it with a button or a swipe."""
    tap("Show meaning")
    look_up_synonym()
    if answer in ("left", "right"):
        swipe(answer)
    else:
        tap(answer)


# The emulator reports a completed boot before every system service answers: wait for the window manager and input,
# and until the package manager actually answers (on Android 17, "service check" finds it before "cmd" can reach it).
def system_ready():
    return (
        adb("shell", "pm", "path", "android", check=False).startswith("package:")
        and all(": found" in adb("shell", "service", "check", name, check=False) for name in ("window", "input"))
    )


def wait_until_ready():
    for _ in range(60):
        if system_ready():
            break
        time.sleep(3)
    time.sleep(10)


wait_until_ready()

log(
    f"Device: {adb('shell', 'getprop', 'ro.product.model').strip()}, "
    f"Android {adb('shell', 'getprop', 'ro.build.version.release').strip()} "
    f"(API {adb('shell', 'getprop', 'ro.build.version.sdk').strip()})"
)
# On Android 17 emulators, the system can still be settling after the checks above, and an install then fails with
# "Can't find service: package" or "Broken pipe": retry, copying the APK to the device first ("--no-streaming").
for attempt in range(4):
    try:
        adb("install", "-r", *(["--no-streaming"] if attempt else []), apk, timeout=300)
        break
    except RuntimeError as e:
        if attempt == 3:
            raise
        log(f"retrying the install: {e}")
        time.sleep(15)
        wait_until_ready()
packages = adb("shell", "pm", "list", "packages", NAMESPACE).split()
package = next(p.removeprefix("package:") for p in packages if p.startswith("package:"))
log(f"Installed {package}")
adb("logcat", "-b", "all", "-c", check=False)

# Just installed, the app's activity may not be registered yet on a system still settling, as on Android 17
# emulators: "Activity class ... does not exist".
for attempt in range(5):
    try:
        adb("shell", "am", "start", "-W", "-n", f"{package}/{NAMESPACE}.MainActivity")
        break
    except RuntimeError as e:
        if attempt == 4:
            raise
        log(f"retrying the start: {e}")
        time.sleep(10)
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

if not looked_up:
    log("warning: no card showed a synonym to look up")
save_logs()
log("PASS: no crash")

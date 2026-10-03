"""Validate the built app, not only Xcode's source build settings."""
import plistlib
import sys
from pathlib import Path

app = Path(sys.argv[1]) if len(sys.argv) > 1 else Path(
    "iosApp/build/DerivedData/Build/Products/Debug-iphonesimulator/CoffeeDial.app"
)
with (app / "Info.plist").open("rb") as source:
    info = plistlib.load(source)
if info.get("CADisableMinimumFrameDurationOnPhone") is not True:
    raise SystemExit("FAIL: Compose requires CADisableMinimumFrameDurationOnPhone = boolean true")
print("OK: built iOS app passes the Compose Info.plist requirement")

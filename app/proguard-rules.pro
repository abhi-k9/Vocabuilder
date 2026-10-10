# Libraries used here (Hilt, Room, kotlinx.serialization) ship their own consumer R8 rules, so few
# extra keep rules are needed.

# Repackage into a single package to shrink the APK a little more.
-repackageclasses

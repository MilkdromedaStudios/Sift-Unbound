#!/usr/bin/env bash
# Boots a dedicated dev server, generates a 128x128 patch of the Sift and checks that:
#  - the server starts and stops without errors from the mod or its datapack
#  - every Sift biome can be located
#  - world generation spawns blubs
#  - /sift commands work
# Usage: tools/server_smoke_test.sh   (from the repository root; needs Java 25)
set -euo pipefail
cd "$(dirname "$0")/.."
GRADLE=${GRADLE:-./gradlew}
LOG=build/server-smoke.log
mkdir -p run build
rm -rf run/world
echo "eula=true" > run/eula.txt
printf 'online-mode=false\nlevel-seed=2027\nview-distance=6\nsimulation-distance=6\nmax-tick-time=-1\n' > run/server.properties

{
  # wait until the server is ready
  for _ in $(seq 1 300); do grep -q 'Done (' "$LOG" 2>/dev/null && break; sleep 1; done
  echo "sift credits"
  echo "sift tide"
  echo "execute in sift_unbound:the_sift run forceload add -64 -64 63 63"
  for _ in $(seq 1 240); do grep -q 'Marked .* chunks\|chunks in .* to be force loaded\|Chunk at .* is now force loaded' "$LOG" 2>/dev/null && break; sleep 1; done
  sleep 20
  for b in sift_meadows pallid_grove soul_wetlands husklands lullaby_hills; do
    echo "execute in sift_unbound:the_sift run locate biome sift_unbound:$b"; sleep 4
  done
  echo "execute in sift_unbound:the_sift if entity @e[type=sift_unbound:blub]"
  sleep 3
  echo "stop"
} | timeout 900 $GRADLE runServer --console=plain > "$LOG" 2>&1 || true

fail=0
check() { if grep -qE "$1" "$LOG"; then echo "ok   - $2"; else echo "FAIL - $2"; fail=1; fi; }
check 'Done \(' "server started"
check 'unofficial fan mod' "/sift credits"
check 'Sift tide: ' "/sift tide"
for b in sift_meadows pallid_grove soul_wetlands husklands lullaby_hills; do check "nearest sift_unbound:$b is at" "biome $b generates"; done
check 'Test passed\. Count: [1-9]' "world generation spawned blubs"
if grep -E '\[(Server thread|Worker-Main-[0-9]+)/ERROR\]' "$LOG" | grep -vE 'server.properties' | head -5 | grep -q .; then
  echo "FAIL - errors in the server log:"; grep -E '/ERROR\]' "$LOG" | grep -v server.properties | head -10; fail=1
else
  echo "ok   - no errors logged"
fi
exit $fail

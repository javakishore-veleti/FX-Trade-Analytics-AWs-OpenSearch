#!/bin/bash
# Polls the admin (4200) and customer (4201) portals until both respond,
# then opens each in the default browser. Used by
# `npm run localhost:app:ui:all-up` so the developer doesn't have to flip
# back and forth waiting for ng serve to finish compiling.
#
# macOS-only (uses `open`). On Linux: replace `open` with `xdg-open`.
# On Windows / WSL: `start` or `cmd.exe /c start`.

set -e

ADMIN_URL=http://localhost:4200
CUSTOMER_URL=http://localhost:4201

echo "⏳ Waiting for portals to finish compiling..."
attempts=0
max_attempts=120   # 4 min cap (2s * 120)
until curl -sf -o /dev/null "$ADMIN_URL" && curl -sf -o /dev/null "$CUSTOMER_URL"; do
  attempts=$((attempts + 1))
  if [ "$attempts" -ge "$max_attempts" ]; then
    echo "❌ Portals didn't come up after $((max_attempts * 2))s — giving up. Check the ng serve logs."
    exit 1
  fi
  sleep 2
done

echo "✅ Opening $ADMIN_URL"
open "$ADMIN_URL" 2>/dev/null || true
echo "✅ Opening $CUSTOMER_URL"
open "$CUSTOMER_URL" 2>/dev/null || true

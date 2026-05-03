#!/bin/bash
# Stops both Angular portals (admin: 4200, customer: 4201).

kill_port() {
  PORT=$1
  NAME=$2
  PID=$(lsof -ti:$PORT || true)
  if [ -n "$PID" ]; then
    echo "🔥 Killing $NAME (port $PORT, pid $PID)"
    kill -9 $PID
  else
    echo "✅ $NAME already stopped (port $PORT)"
  fi
}

echo "🛑 Stopping UI portals..."
kill_port 4200 "Admin Portal"
kill_port 4201 "Customer Portal"
echo "✅ Done."

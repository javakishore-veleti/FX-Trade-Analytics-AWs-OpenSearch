#!/bin/bash
# Reports which Angular portals are currently listening.

check_port() {
  PORT=$1
  NAME=$2
  if lsof -i :$PORT > /dev/null 2>&1; then
    echo "✅ $NAME running on port $PORT (http://localhost:$PORT)"
  else
    echo "❌ $NAME NOT running (port $PORT)"
  fi
}

echo "🖥️ UI Portals:"
check_port 4200 "Admin Portal"
check_port 4201 "Customer Portal"

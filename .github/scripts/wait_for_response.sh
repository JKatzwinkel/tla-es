#!/bin/bash

url=$1
timeout=$2

until curl -sf "$url"; do
  sleep 5 && timeout=$((timeout - 5))
  if [[ ${timeout} -lt 0 ]]; then
    exit 1
  fi
done

exit 0

#!/bin/bash
set -e

SERVER="$1"
SHARE="$2"
MOUNT_PATH="$3"
USERNAME="$4"
PASSWORD="$5"

echo "🔧 Mounting //${SERVER}/${SHARE} to ${MOUNT_PATH}"
sudo mkdir -p "$MOUNT_PATH"
sudo mount -t cifs "//${SERVER}/${SHARE}" "$MOUNT_PATH" \
    -o username="$USERNAME",password="$PASSWORD",rw,uid=$(id -u),gid=$(id -g)

mountpoint -q "$MOUNT_PATH" || {
    echo "❌ Mount failed"
    exit 1
}

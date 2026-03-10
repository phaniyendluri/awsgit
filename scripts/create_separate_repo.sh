#!/usr/bin/env bash
set -euo pipefail

if [[ $# -lt 1 ]]; then
  echo "Usage: $0 <target-directory> [remote-git-url]"
  exit 1
fi

TARGET_DIR="$1"
REMOTE_URL="${2:-}"

mkdir -p "$TARGET_DIR"

rsync -av --delete \
  --exclude '.git' \
  --exclude 'target' \
  --exclude '.idea' \
  --exclude '.vscode' \
  --exclude 'file1' --exclude 'file2' --exclude 'file3' --exclude 'file4' \
  ./ "$TARGET_DIR/"

cd "$TARGET_DIR"

if [[ ! -d .git ]]; then
  git init
fi

git add .
if ! git diff --cached --quiet; then
  git commit -m "Initial production-ready recipe finder"
fi

if [[ -n "$REMOTE_URL" ]]; then
  git remote remove origin >/dev/null 2>&1 || true
  git remote add origin "$REMOTE_URL"
  git branch -M main
  git push -u origin main
fi

echo "Separate repository prepared at: $TARGET_DIR"

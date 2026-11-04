#!/usr/bin/env bash
# Publishes the current playlet-lib build to the "tv-updates" branch. TVs running this fork download
# https://raw.githubusercontent.com/killercats1/playlet/tv-updates/playlet-lib.zip at startup (see
# playlet-app BootstrapScene), so a publish reaches them on their next launch without reinstalling.
# The branch holds a single commit with the latest zip; each publish replaces it.
#
# Usage: tools/publish-tv-update.sh   (after `npm run build:lib:prod`, which writes dist/playlet-lib.zip)
set -euo pipefail
cd "$(dirname "$0")/.."

zip="dist/playlet-lib.zip"
if [ ! -f "$zip" ]; then
    echo "No $zip: run npm run build:lib:prod first" >&2
    exit 1
fi
version=$(node -p "require('./package.json').version")
sha=$(git rev-parse --short HEAD)

work=$(mktemp -d)
trap 'git worktree remove --force "$work" >/dev/null 2>&1 || true; rm -rf "$work"' EXIT
git worktree add --detach "$work" >/dev/null
cp "$zip" "$work/playlet-lib.zip.new"
(
    cd "$work"
    git checkout -q --orphan tv-updates-publish
    git rm -rq --cached . >/dev/null 2>&1 || true
    find . -mindepth 1 -maxdepth 1 ! -name .git ! -name playlet-lib.zip.new -exec rm -rf {} +
    mv playlet-lib.zip.new playlet-lib.zip
    cat > README.md <<README
# Playlet TV updates

The latest build of this fork's app code (playlet-lib), version ${version}, from commit ${sha}.
TVs with this fork installed download playlet-lib.zip from here every time Playlet starts, and fall
back to the copy inside the installed app when it can't be downloaded. Published by
tools/publish-tv-update.sh; each publish replaces this branch.
README
    git add playlet-lib.zip README.md
    git commit -qm "TV update ${version} (${sha})"
    git push -f origin HEAD:tv-updates
)
echo "Published ${version} to tv-updates"

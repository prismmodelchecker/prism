#!/bin/bash

# Usage: src/scripts/bump_version.sh 4.10.2 "" (for a clean release)
# Usage: src/scripts/bump_version.sh 4.10.2 dev

NEW_VERSION=$1
NEW_SUFFIX=$2
FILE="src/prism/Version.java"
VERSION_STRING="versionString"
SUFFIX_STRING="versionSuffixString"

echo "Current version info:"
grep "$VERSION_STRING" "$FILE"
grep "$SUFFIX_STRING" "$FILE"

if [ -z "$NEW_VERSION" ]; then
    echo "Usage: $0 <version-number> <suffix>"
    exit 1
fi

echo "Bumping $FILE to $NEW_VERSION (suffix: '$NEW_SUFFIX')..."

sed -i "" "s/public static String $VERSION_STRING = \".*\";/public static String $VERSION_STRING = \"$NEW_VERSION\";/" "$FILE"

sed -i "" "s/public static String $SUFFIX_STRING = \".*\";/public static String $SUFFIX_STRING = \"$NEW_SUFFIX\";/" "$FILE"

git diff "$FILE"


#!/bin/bash

# Script to download the source/binary releases of some version

# Usage: ./prism-download.sh prism 4.10.2
# Usage: ./prism-download.sh prism-games 3.2.4

TOOL=$1
VERSION=$2

DISTRS=("src" "linux64-x86" "linux64-arm" "mac64-x86" "mac64-arm" "win64-x86")

if [ -z "$TOOL" -o -z "$VERSION" ]; then
    echo "Usage: $0 <tool> <version>"
    echo "where <tool> is prism or prism-games"
    exit 1
fi

if [[ -n "$TOOL" && -n "$VERSION" ]]; then
	for DISTR in "${DISTRS[@]}"; do
		if [[ "$DISTR" == win* ]]; then
			DISTR_FILE="${TOOL}-${VERSION}-${DISTR}-installer.exe"
			DISTR_PERM=755
		else
			DISTR_FILE="${TOOL}-${VERSION}-${DISTR}.tar.gz"
			DISTR_PERM=644
		fi
		wget -O "${DISTR_FILE}" "https://github.com/prismmodelchecker/${TOOL}/releases/download/v${VERSION}/${DISTR_FILE}"
		chmod ${DISTR_PERM} "${DISTR_FILE}"
	done
fi

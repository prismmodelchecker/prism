#!/bin/sh

# The detection of javac below can handle cases:
# - where javac is a symbolic link
# - where there is actually a chain of symbolic links
# - where there are relative symbolic links
# - where some links/files are not called javac (e.g. ecj)
# - where there are directory names including spaces
# Note: The code would be simpler if we could rely on
#       the existence of "readlink -f" but we can't.

DETECT_JAVAC=`which javac`
if [ -f "$DETECT_JAVAC" ]; then
  DETECT_JAVAC_DIR=`dirname "$DETECT_JAVAC"`
  DETECT_JAVAC_EXE=`basename "$DETECT_JAVAC"`
  cd "$DETECT_JAVAC_DIR"
  while [ -h ./"$DETECT_JAVAC_EXE" ]; do
	DETECT_JAVAC=`readlink ./javac`
	DETECT_JAVAC_DIR=`dirname "$DETECT_JAVAC"`
	DETECT_JAVAC_EXE=`basename "$DETECT_JAVAC"`
	cd "$DETECT_JAVAC_DIR"
	DETECT_JAVAC_DIR=`pwd`
	#pwd
  done
  echo "$DETECT_JAVAC_DIR"/"$DETECT_JAVAC_EXE"
fi

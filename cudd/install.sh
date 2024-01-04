#!/bin/sh

# PRISM-customised install script for CUDD
# Needs to be run from the containing directory

# Only re-run configure if Makefile doesn't already exist

if [ ! -f Makefile ]; then

  # Hacking of time-stamps (lost in git) to avoid autoconf issues
  sleep 1; touch aclocal.m4
  sleep 1; touch configure; touch config.h.in; touch `find . -name Makefile.in -print`
  
  # Collate arguments to be passed to configure
  CONFIGURE_ARGS=()
  if [ ! -z "$CFLAGS" ]; then
    CONFIGURE_ARGS+=("CFLAGS=$CFLAGS")
  fi
  if [ ! -z "$CXXFLAGS" ]; then
    CONFIGURE_ARGS+=("CXXFLAGS=$CXXFLAGS")
  fi
  if [ ! -z "$CC" ]; then
    CONFIGURE_ARGS+=("CC=$CC")
  fi
  if [ ! -z "$MAKE" ]; then
    CONFIGURE_ARGS+=("MAKE=$MAKE")
  fi
  
  # Build Makefile
  ./configure --prefix="$(pwd)" "${CONFIGURE_ARGS[@]}" || exit 1
  
fi

# Build, install (locally)

make || exit 1
make install || exit 1

# Install links to headers not installed by make install

mkdir -p include # should already exist though
for HEADER in cudd/cuddInt.h config.h st/st.h mtr/mtr.h epd/epd.h util/util.h
#for HEADER in $(find . -name "*.h")
do
  if [ ! -L include/`basename $HEADER` ]; then
	ln -s ../$HEADER include
  fi
done

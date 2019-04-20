#!/bin/sh

# PRISM-customised *un*install script for CUDD
# Needs to be run from the containing directory

if [ -f Makefile ]; then
  make uninstall
  make distclean
fi

rm -rf include
